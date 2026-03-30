package com.tabletaide.ide.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabletaide.ide.IdeConstants
import com.tabletaide.ide.agent.AnthropicClient
import com.tabletaide.ide.agent.StreamEvent
import com.tabletaide.ide.agent.ToolRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val client: AnthropicClient,
    private val toolRouter: ToolRouter,
) : ViewModel() {

    private val apiHistory = JSONArray()
    private val toolsJson = toolRouter.toolDefinitions()

    private val _lines = MutableStateFlow<List<ChatLine>>(emptyList())
    val lines: StateFlow<List<ChatLine>> = _lines.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun sendUserMessage(text: String) {
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
            runAgentRounds()
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

    private suspend fun runAgentRounds() {
        var round = 0
        while (round < IdeConstants.MAX_TOOL_ROUNDS) {
            round++
            startAssistantSlot()
            val textBuf = StringBuilder()
            val toolCalls = mutableListOf<ToolCall>()
            var streamError: String? = null

            client.streamMessage(
                model = IdeConstants.ANTHROPIC_MODEL,
                systemPrompt = IdeConstants.SYSTEM_PROMPT,
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
                appendToolLine(call.name, resultText)
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

    private fun appendToolLine(name: String, detail: String) {
        val clipped = if (detail.length > 2000) detail.take(2000) + "…" else detail
        _lines.update {
            it + ChatLine.Tool(UUID.randomUUID().toString(), name, clipped)
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
        data class Tool(override val id: String, val name: String, val detail: String) : ChatLine()
    }
}
