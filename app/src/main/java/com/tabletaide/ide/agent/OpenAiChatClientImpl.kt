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

enum class OpenAiChatEndpoint(
    val provider: LlmProvider,
    val url: String,
    val missingKeyMessage: String,
) {
    OPENAI(
        provider = LlmProvider.OPENAI,
        url = "https://api.openai.com/v1/chat/completions",
        missingKeyMessage = "Missing OpenAI API key. Add one from AI Architect > API keys.",
    ),
    GROK(
        provider = LlmProvider.GROK,
        url = "https://api.x.ai/v1/chat/completions",
        missingKeyMessage = "Missing Grok (xAI) API key. Add one from AI Architect > API keys.",
    ),
    OPENROUTER(
        provider = LlmProvider.OPENROUTER,
        url = "https://openrouter.ai/api/v1/chat/completions",
        missingKeyMessage = "Missing OpenRouter API key. Add one from AI Architect > API keys.",
    ),
}

@Singleton
class OpenAiClientImpl @Inject constructor(
    private val httpClient: OkHttpClient,
    private val providerStore: LlmProviderStore,
) : LlmClient {
    override fun streamMessage(
        model: String,
        systemPrompt: String?,
        messages: JSONArray,
        tools: JSONArray?,
        maxTokens: Int,
    ): Flow<StreamEvent> = OpenAiChatStreamEngine.stream(
        httpClient = httpClient,
        endpoint = OpenAiChatEndpoint.OPENAI,
        apiKey = providerStore.resolveApiKey(LlmProvider.OPENAI),
        model = model,
        systemPrompt = systemPrompt,
        messages = messages,
        tools = tools,
        maxTokens = maxTokens,
    )
}

@Singleton
class GrokClientImpl @Inject constructor(
    private val httpClient: OkHttpClient,
    private val providerStore: LlmProviderStore,
) : LlmClient {
    override fun streamMessage(
        model: String,
        systemPrompt: String?,
        messages: JSONArray,
        tools: JSONArray?,
        maxTokens: Int,
    ): Flow<StreamEvent> = OpenAiChatStreamEngine.stream(
        httpClient = httpClient,
        endpoint = OpenAiChatEndpoint.GROK,
        apiKey = providerStore.resolveApiKey(LlmProvider.GROK),
        model = model,
        systemPrompt = systemPrompt,
        messages = messages,
        tools = tools,
        maxTokens = maxTokens,
    )
}

@Singleton
class OpenRouterClientImpl @Inject constructor(
    private val httpClient: OkHttpClient,
    private val providerStore: LlmProviderStore,
) : LlmClient {
    override fun streamMessage(
        model: String,
        systemPrompt: String?,
        messages: JSONArray,
        tools: JSONArray?,
        maxTokens: Int,
    ): Flow<StreamEvent> = OpenAiChatStreamEngine.stream(
        httpClient = httpClient,
        endpoint = OpenAiChatEndpoint.OPENROUTER,
        apiKey = providerStore.resolveApiKey(LlmProvider.OPENROUTER),
        model = model,
        systemPrompt = systemPrompt,
        messages = messages,
        tools = tools,
        maxTokens = maxTokens,
        extraHeaders = mapOf(
            "HTTP-Referer" to "https://github.com/Otterdays/Kinetic-IDE",
            "X-OpenRouter-Title" to "Kinetic IDE",
        ),
    )
}

internal object OpenAiChatStreamEngine {

    fun stream(
        httpClient: OkHttpClient,
        endpoint: OpenAiChatEndpoint,
        apiKey: String,
        model: String,
        systemPrompt: String?,
        messages: JSONArray,
        tools: JSONArray?,
        maxTokens: Int,
        extraHeaders: Map<String, String> = emptyMap(),
    ): Flow<StreamEvent> = channelFlow {
        if (apiKey.isBlank()) {
            send(StreamEvent.Failure(endpoint.missingKeyMessage))
            send(StreamEvent.Finished)
            return@channelFlow
        }

        val openAiMessages = JSONArray()
        if (!systemPrompt.isNullOrBlank()) {
            openAiMessages.put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                },
            )
        }
        val converted = OpenAiMessageCodec.convertMessages(messages)
        for (i in 0 until converted.length()) {
            openAiMessages.put(converted.getJSONObject(i))
        }

        val body = JSONObject().apply {
            put("model", model)
            put("messages", openAiMessages)
            put("max_tokens", maxTokens)
            put("stream", true)
            val openAiTools = OpenAiMessageCodec.convertTools(tools)
            if (openAiTools != null && openAiTools.length() > 0) {
                put("tools", openAiTools)
                put("tool_choice", "auto")
            }
        }

        val requestBuilder = Request.Builder()
            .url(endpoint.url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("content-type", "application/json")
        extraHeaders.forEach { (name, value) ->
            requestBuilder.addHeader(name, value)
        }
        val request = requestBuilder
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val parser = OpenAiStreamParser()

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
                    if (line.isBlank() || !line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") continue
                    parser.parseLine(data).forEach { send(it) }
                }
                parser.flushToolCalls().forEach { send(it) }
            }
        } catch (e: Exception) {
            send(StreamEvent.Failure(e.message ?: e.toString()))
        }
        send(StreamEvent.Finished)
    }.flowOn(Dispatchers.IO)

    private class OpenAiStreamParser {
        private val toolBuffers = mutableMapOf<Int, ToolBuffer>()

        fun parseLine(data: String): List<StreamEvent> {
            return try {
                val root = JSONObject(data)
                val choices = root.optJSONArray("choices") ?: return emptyList()
                if (choices.length() == 0) return emptyList()
                val choice = choices.getJSONObject(0)
                val events = mutableListOf<StreamEvent>()
                val delta = choice.optJSONObject("delta")
                if (delta != null) {
                    delta.optString("content", "").takeIf { it.isNotEmpty() }?.let {
                        events.add(StreamEvent.TextDelta(it))
                    }
                    val toolCalls = delta.optJSONArray("tool_calls")
                    if (toolCalls != null) {
                        for (i in 0 until toolCalls.length()) {
                            val toolDelta = toolCalls.getJSONObject(i)
                            val index = toolDelta.optInt("index", i)
                            val buffer = toolBuffers.getOrPut(index) { ToolBuffer() }
                            toolDelta.optString("id").takeIf { it.isNotBlank() }?.let { buffer.id = it }
                            val function = toolDelta.optJSONObject("function") ?: continue
                            function.optString("name").takeIf { it.isNotBlank() }?.let { buffer.name = it }
                            val argsChunk = function.optString("arguments", "")
                            if (argsChunk.isNotEmpty()) {
                                buffer.arguments.append(argsChunk)
                            }
                            tryCompleteTool(index)?.let { events.add(it) }
                        }
                    }
                }
                if (choice.optString("finish_reason") == "tool_calls") {
                    events.addAll(flushToolCalls())
                }
                events
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun flushToolCalls(): List<StreamEvent> {
            val events = mutableListOf<StreamEvent>()
            toolBuffers.keys.toList().forEach { index ->
                tryCompleteTool(index)?.let { events.add(it) }
            }
            return events
        }

        private fun tryCompleteTool(index: Int): StreamEvent.ToolUseComplete? {
            val buffer = toolBuffers[index] ?: return null
            if (buffer.id == null || buffer.name == null || buffer.arguments.isEmpty()) return null
            val input = try {
                JSONObject(buffer.arguments.toString())
            } catch (_: Exception) {
                return null
            }
            toolBuffers.remove(index)
            return StreamEvent.ToolUseComplete(
                id = buffer.id!!,
                name = buffer.name!!,
                input = input,
            )
        }
    }

    private class ToolBuffer {
        var id: String? = null
        var name: String? = null
        val arguments = StringBuilder()
    }
}
