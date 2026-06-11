package com.tabletaide.ide.agent

import com.tabletaide.ide.data.LlmProvider
import com.tabletaide.ide.data.LlmProviderStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiClientImpl @Inject constructor(
    private val httpClient: OkHttpClient,
    private val providerStore: LlmProviderStore,
) : LlmClient {

    override fun streamMessage(
        model: String,
        systemPrompt: String?,
        messages: JSONArray,
        tools: JSONArray?,
        maxTokens: Int,
    ): Flow<StreamEvent> = channelFlow {
        val key = providerStore.resolveApiKey(LlmProvider.GEMINI)
        if (key.isBlank()) {
            send(StreamEvent.Failure("Missing Gemini API key. Add one from AI Architect > API keys."))
            send(StreamEvent.Finished)
            return@channelFlow
        }

        val systemInstruction = if (!systemPrompt.isNullOrBlank()) {
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
        } else {
            null
        }

        val functionDeclarations = if (tools != null && tools.length() > 0) {
            JSONArray().apply {
                for (i in 0 until tools.length()) {
                    val tool = tools.getJSONObject(i)
                    val name = tool.optString("name")
                    val description = tool.optString("description", "")
                    val schema = tool.optJSONObject("input_schema") ?: JSONObject()
                    put(
                        JSONObject().apply {
                            put("name", name)
                            put("description", description)
                            put("parameters", schema)
                        }
                    )
                }
            }
        } else {
            null
        }

        val toolConfig = if (functionDeclarations != null && functionDeclarations.length() > 0) {
            JSONObject().apply {
                put("functionCallingConfig", JSONObject().apply {
                    put("mode", "AUTO")
                    put("allowedFunctionNames", functionDeclarations.let { decls ->
                        JSONArray().apply {
                            for (i in 0 until decls.length()) {
                                put(decls.getJSONObject(i).optString("name"))
                            }
                        }
                    })
                })
            }
        } else {
            null
        }

        val body = JSONObject().apply {
            put("contents", GeminiMessageCodec.convertMessages(messages))
            if (systemInstruction != null) {
                put("systemInstruction", systemInstruction)
            }
            if (functionDeclarations != null && functionDeclarations.length() > 0) {
                put("tools", JSONArray().put(
                    JSONObject().apply {
                        put("functionDeclarations", functionDeclarations)
                    }
                ))
            }
            if (toolConfig != null) {
                put("toolConfig", toolConfig)
            }
            put("generationConfig", buildGenerationConfig(model, maxTokens, functionDeclarations != null))
        }

        val media = "application/json; charset=utf-8".toMediaType()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?key=$key"
        val request = Request.Builder()
            .url(url)
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody(media))
            .build()

        val toolCallMap = mutableMapOf<String, PartialToolCall>()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: response.message
                    send(StreamEvent.Failure("HTTP ${response.code}: $err"))
                    send(StreamEvent.Finished)
                    return@use
                }
                val source = response.body?.source() ?: run {
                    send(StreamEvent.Failure("Empty body"))
                    send(StreamEvent.Finished)
                    return@use
                }
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.isBlank()) continue
                    val event = parseGeminiStreamLine(line, toolCallMap)
                    if (event != null) send(event)
                }
            }
        } catch (e: Exception) {
            send(StreamEvent.Failure(e.message ?: e.toString()))
        }
        send(StreamEvent.Finished)
    }.flowOn(Dispatchers.IO)

    private fun parseGeminiStreamLine(line: String, toolCallMap: MutableMap<String, PartialToolCall>): StreamEvent? {
        if (!line.startsWith("[")) return null
        return try {
            val root = JSONArray(line).getJSONObject(0)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("text")) {
                    val text = part.optString("text", "")
                    if (text.isNotEmpty()) return StreamEvent.TextDelta(text)
                }
                if (part.has("functionCall")) {
                    val funcCall = part.getJSONObject("functionCall")
                    val name = funcCall.optString("name", "unknown")
                    val args = funcCall.optJSONObject("args") ?: JSONObject()
                    val callId = funcCall.optString("id").trim().ifEmpty {
                        "call_${System.currentTimeMillis()}_$name"
                    }
                    return StreamEvent.ToolUseComplete(
                        id = callId,
                        name = name,
                        input = args,
                    )
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private data class PartialToolCall(
        val name: String,
        val id: String,
        val args: StringBuilder = StringBuilder(),
    )

    private fun buildGenerationConfig(
        model: String,
        maxTokens: Int,
        hasTools: Boolean,
    ): JSONObject = JSONObject().apply {
        put("maxOutputTokens", maxTokens)
        if (hasTools && model.lowercase().startsWith("gemini-3")) {
            put(
                "thinkingConfig",
                JSONObject().apply {
                    put("thinkingLevel", "MEDIUM")
                },
            )
        }
    }
}
