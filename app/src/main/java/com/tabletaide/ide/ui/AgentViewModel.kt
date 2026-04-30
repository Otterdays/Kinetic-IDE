package com.tabletaide.ide.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabletaide.ide.IdeConstants
import com.tabletaide.ide.agent.AnthropicClientImpl
import com.tabletaide.ide.agent.GeminiClientImpl
import com.tabletaide.ide.agent.LlmClient
import com.tabletaide.ide.agent.StreamEvent
import com.tabletaide.ide.agent.ToolRouter
import com.tabletaide.ide.data.LlmProvider
import com.tabletaide.ide.data.LlmProviderStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val anthropicClient: AnthropicClientImpl,
    private val geminiClient: GeminiClientImpl,
    private val toolRouter: ToolRouter,
    private val providerStore: LlmProviderStore,
) : ViewModel() {

    private fun getClient(): LlmClient = when (_provider.value) {
        LlmProvider.ANTHROPIC -> anthropicClient
        LlmProvider.GEMINI -> geminiClient
    }

    private val apiHistory = JSONArray()
    private val toolsJson = toolRouter.toolDefinitions()

    private val _provider = MutableStateFlow(providerStore.getProvider())
    val provider: StateFlow<LlmProvider> = _provider.asStateFlow()

    private val _lines = MutableStateFlow<List<ChatLine>>(emptyList())
    val lines: StateFlow<List<ChatLine>> = _lines.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setProvider(provider: LlmProvider) {
        providerStore.setProvider(provider)
        _provider.value = provider
    }

    fun sendUserMessage(text: String, systemPromptAppendix: String = "") {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            appendUserLine(trimmed)
            val userContent = JSONArray().put(
                JSONObject().put("type", "text").put("text", trimmed),
            )
            apiHistory.put(
                JSONObject().put("role", "user").put("content", userContent),
            )
            runAgentRounds(systemPromptAppendix.trim())
            _busy.value = false
        }
    }

    fun clearConversation() {
        if (_busy.value) return
        apiHistory.length().let { len ->
            for (i in len - 1 downTo 0) apiHistory.remove(i)
        }
        _lines.value = emptyList()
        _error.value = null
    }

    private suspend fun runAgentRounds(systemPromptAppendix: String = "") {
        val systemPrompt = buildString {
            append(IdeConstants.SYSTEM_PROMPT)
            if (systemPromptAppendix.isNotEmpty()) {
                append("\n\n")
                append(systemPromptAppendix)
            }
        }
        var round = 0
        while (round < IdeConstants.MAX_TOOL_ROUNDS) {
            round++
            startAssistantSlot()
            val textBuf = StringBuilder()
            val toolCalls = mutableListOf<ToolCall>()
            var streamError: String? = null

            val model = when (_provider.value) {
                LlmProvider.ANTHROPIC -> IdeConstants.ANTHROPIC_MODEL
                LlmProvider.GEMINI -> IdeConstants.GEMINI_MODEL
            }
            getClient().streamMessage(
                model = model,
                systemPrompt = systemPrompt,
                messages = apiHistory,
                tools = toolsJson,
                maxTokens = IdeConstants.MAX_OUTPUT_TOKENS,
            ).collect { ev ->
                when (ev) {
                    is StreamEvent.TextDelta -> {
                        textBuf.append(ev.text)
                        updateStreamingAssistant(textBuf.toString())
                    }
                    is StreamEvent.ToolUseComplete -> toolCalls.add(
                        ToolCall(id = ev.id, name = ev.name, input = ev.input),
                    )
                    is StreamEvent.Failure -> streamError = ev.message
                    is StreamEvent.Finished -> Unit
                }
            }

            if (streamError != null) {
                _error.value = streamError
                finalizeStreamingAssistant(textBuf.toString().ifEmpty { "(error)" })
                return
            }

            finalizeStreamingAssistant(textBuf.toString())
            pushAssistantToApi(textBuf.toString(), toolCalls)

            if (toolCalls.isEmpty()) return

            val toolResults = JSONArray()
            for (call in toolCalls) {
                val resultText = toolRouter.dispatch(call.name, call.input)
                appendToolLine(call.name, call.input, resultText)
                toolResults.put(
                    JSONObject()
                        .put("type", "tool_result")
                        .put("tool_use_id", call.id)
                        .put("content", resultText),
                )
            }
            apiHistory.put(
                JSONObject().put("role", "user").put("content", toolResults),
            )
        }
        _error.value = "Stopped: max tool rounds (${IdeConstants.MAX_TOOL_ROUNDS})"
    }

    private fun appendUserLine(text: String) {
        _lines.update {
            it + ChatLine.User(UUID.randomUUID().toString(), text)
        }
    }

    private fun startAssistantSlot() {
        val id = UUID.randomUUID().toString()
        _lines.update { it + ChatLine.AssistantStreaming(id, "") }
    }

    private fun updateStreamingAssistant(partial: String) {
        _lines.update { list ->
            val last = list.lastOrNull()
            if (last is ChatLine.AssistantStreaming) {
                list.dropLast(1) + last.copy(text = partial)
            } else {
                list
            }
        }
    }

    private fun finalizeStreamingAssistant(final: String) {
        _lines.update { list ->
            val last = list.lastOrNull()
            if (last is ChatLine.AssistantStreaming) {
                list.dropLast(1) + ChatLine.AssistantDone(last.id, final)
            } else {
                list
            }
        }
    }

    private fun appendToolLine(name: String, input: JSONObject, resultText: String) {
        val inputJson = try {
            input.toString(2)
        } catch (_: Exception) {
            input.toString()
        }
        val full = if (resultText.length > TOOL_RESULT_UI_MAX_CHARS) {
            resultText.take(TOOL_RESULT_UI_MAX_CHARS) + "\n… (truncated for chat UI)"
        } else {
            resultText
        }
        val preview = if (full.length <= TOOL_PREVIEW_CHARS) full else full.take(TOOL_PREVIEW_CHARS) + "…"
        _lines.update {
            it + ChatLine.Tool(
                id = UUID.randomUUID().toString(),
                name = name,
                inputJson = inputJson,
                resultFull = full,
                preview = preview,
            )
        }
    }

    private fun pushAssistantToApi(text: String, tools: List<ToolCall>) {
        val content = JSONArray()
        if (text.isNotBlank()) {
            content.put(JSONObject().put("type", "text").put("text", text))
        }
        for (t in tools) {
            content.put(
                JSONObject()
                    .put("type", "tool_use")
                    .put("id", t.id)
                    .put("name", t.name)
                    .put("input", t.input),
            )
        }
        if (content.length() == 0) return
        apiHistory.put(JSONObject().put("role", "assistant").put("content", content))
    }

    private data class ToolCall(val id: String, val name: String, val input: JSONObject)

    sealed class ChatLine {
        abstract val id: String

        data class User(override val id: String, val text: String) : ChatLine()
        data class AssistantStreaming(override val id: String, val text: String) : ChatLine()
        data class AssistantDone(override val id: String, val text: String) : ChatLine()
        data class Tool(
            override val id: String,
            val name: String,
            val inputJson: String,
            val resultFull: String,
            val preview: String,
        ) : ChatLine()
    }

    private companion object {
        private const val TOOL_RESULT_UI_MAX_CHARS = 100_000
        private const val TOOL_PREVIEW_CHARS = 600
    }
}
