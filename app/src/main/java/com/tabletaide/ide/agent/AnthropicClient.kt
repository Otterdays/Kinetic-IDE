package com.tabletaide.ide.agent

import com.tabletaide.ide.BuildConfig
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

interface LlmClient {
    fun streamMessage(
        model: String,
        systemPrompt: String?,
        messages: JSONArray,
        tools: JSONArray?,
        maxTokens: Int,
    ): Flow<StreamEvent>
}

@Singleton
class AnthropicClientImpl @Inject constructor(
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
        val key = providerStore.getCredentialState().anthropicApiKey.ifBlank {
            BuildConfig.ANTHROPIC_API_KEY
        }
        if (key.isBlank()) {
            send(StreamEvent.Failure("Missing Anthropic API key. Add one from AI Architect > API keys."))
            send(StreamEvent.Finished)
            return@channelFlow
        }

        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", maxTokens)
            put("stream", true)
            put("messages", messages)
            if (systemPrompt != null) {
                put("system", systemPrompt)
            }
            if (tools != null && tools.length() > 0) {
                put("tools", tools)
            }
        }

        val media = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", key)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody(media))
            .build()

        val toolBuffers = mutableMapOf<Int, ToolBuffer>()

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
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") continue
                    val event = parseSseData(data, toolBuffers)
                    if (event != null) send(event)
                }
            }
        } catch (e: Exception) {
            send(StreamEvent.Failure(e.message ?: e.toString()))
        }
        send(StreamEvent.Finished)
    }.flowOn(Dispatchers.IO)

    private fun parseSseData(data: String, toolBuffers: MutableMap<Int, ToolBuffer>): StreamEvent? {
        return try {
            val root = JSONObject(data)
            when (root.optString("type")) {
                "content_block_delta" -> {
                    val index = root.optInt("index", -1)
                    val delta = root.optJSONObject("delta") ?: return null
                    when (delta.optString("type")) {
                        "text_delta" -> {
                            val t = delta.optString("text", "")
                            if (t.isNotEmpty()) StreamEvent.TextDelta(t) else null
                        }
                        "input_json_delta" -> {
                            val partial = delta.optString("partial_json", "")
                            if (index >= 0 && partial.isNotEmpty()) {
                                toolBuffers.getOrPut(index) { ToolBuffer() }.input.append(partial)
                            }
                            null
                        }
                        else -> null
                    }
                }
                "content_block_start" -> {
                    val block = root.optJSONObject("content_block") ?: return null
                    val index = root.optInt("index", -1)
                    if (index < 0) return null
                    when (block.optString("type")) {
                        "tool_use" -> {
                            val id = block.optString("id")
                            val name = block.optString("name")
                            toolBuffers.getOrPut(index) { ToolBuffer() }.apply {
                                this.id = id
                                this.name = name
                            }
                            null
                        }
                        else -> null
                    }
                }
                "content_block_stop" -> {
                    val index = root.optInt("index", -1)
                    val buf = toolBuffers.remove(index) ?: return null
                    if (buf.id != null && buf.name != null && buf.input.isNotEmpty()) {
                        val input = try {
                            JSONObject(buf.input.toString())
                        } catch (_: Exception) {
                            JSONObject()
                        }
                        StreamEvent.ToolUseComplete(
                            id = buf.id!!,
                            name = buf.name!!,
                            input = input,
                        )
                    } else {
                        null
                    }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private class ToolBuffer {
        var id: String? = null
        var name: String? = null
        val input = StringBuilder()
    }
}
