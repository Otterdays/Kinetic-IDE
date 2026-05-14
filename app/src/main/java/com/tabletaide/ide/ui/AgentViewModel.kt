package com.tabletaide.ide.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabletaide.ide.IdeConstants
import com.tabletaide.ide.agent.AnthropicClientImpl
import com.tabletaide.ide.agent.GeminiClientImpl
import com.tabletaide.ide.agent.LlmClient
import com.tabletaide.ide.agent.PromptEnhancementService
import com.tabletaide.ide.agent.StreamEvent
import com.tabletaide.ide.agent.ToolRouter
import com.tabletaide.ide.data.AgentToolApprovalState
import com.tabletaide.ide.data.AgentToolPolicyMode
import com.tabletaide.ide.data.AgentToolRiskClass
import com.tabletaide.ide.data.AgentTrustPolicyState
import com.tabletaide.ide.data.AgentTrustStore
import com.tabletaide.ide.data.LlmCredentialState
import com.tabletaide.ide.data.LlmProvider
import com.tabletaide.ide.data.LlmProviderStore
import com.tabletaide.ide.data.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val anthropicClient: AnthropicClientImpl,
    private val geminiClient: GeminiClientImpl,
    private val promptEnhancementService: PromptEnhancementService,
    private val toolRouter: ToolRouter,
    private val trustStore: AgentTrustStore,
    private val providerStore: LlmProviderStore,
    private val workspace: WorkspaceRepository,
) : ViewModel() {

    private fun getClient(): LlmClient = when (_provider.value) {
        LlmProvider.ANTHROPIC -> anthropicClient
        LlmProvider.GEMINI -> geminiClient
    }

    private val apiHistory = JSONArray()
    private val toolsJson = toolRouter.toolDefinitions()
    private val toolSchemaError = toolRouter.validateToolDefinitions(toolsJson)

    private val _provider = MutableStateFlow(providerStore.getProvider())
    val provider: StateFlow<LlmProvider> = _provider.asStateFlow()

    private val _credentials = MutableStateFlow(providerStore.getCredentialState())
    val credentials: StateFlow<LlmCredentialState> = _credentials.asStateFlow()

    private val _lines = MutableStateFlow<List<ChatLine>>(emptyList())
    val lines: StateFlow<List<ChatLine>> = _lines.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _composerDraft = MutableStateFlow("")
    val composerDraft: StateFlow<String> = _composerDraft.asStateFlow()

    private val _enhancingPrompt = MutableStateFlow(false)
    val enhancingPrompt: StateFlow<Boolean> = _enhancingPrompt.asStateFlow()

    private val _trustPolicyState = MutableStateFlow(trustStore.load())
    val trustPolicyState: StateFlow<AgentTrustPolicyState> = _trustPolicyState.asStateFlow()

    private val _agentToolApprovalState = MutableStateFlow(AgentToolApprovalState())
    val agentToolApprovalState: StateFlow<AgentToolApprovalState> =
        _agentToolApprovalState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var pendingToolApproval: CompletableDeferred<Boolean>? = null

    fun setProvider(provider: LlmProvider) {
        providerStore.setProvider(provider)
        _provider.value = provider
        _error.value = null
    }

    fun setApiKey(provider: LlmProvider, apiKey: String) {
        providerStore.setApiKey(provider, apiKey)
        _credentials.value = providerStore.getCredentialState()
        _error.value = null
    }

    fun setTrustPolicyMode(
        riskClass: AgentToolRiskClass,
        mode: AgentToolPolicyMode,
    ) {
        if (riskClass == AgentToolRiskClass.READ_ONLY) return
        val updated = _trustPolicyState.value.withMode(riskClass, mode)
        trustStore.save(updated)
        _trustPolicyState.value = updated
    }

    fun updateComposerDraft(text: String) {
        _composerDraft.value = text
    }

    fun sendComposerDraft(systemPromptAppendix: String = "") {
        val trimmed = _composerDraft.value.trim()
        if (trimmed.isEmpty()) return
        if (_busy.value || _enhancingPrompt.value) return
        toolSchemaError?.let {
            _error.value = it
            return
        }
        if (!_credentials.value.hasKey(_provider.value)) {
            _error.value = "API key required. Tap the key icon in AI Architect to add one."
            return
        }
        _composerDraft.value = ""
        sendUserMessage(trimmed, systemPromptAppendix)
    }

    fun enhanceComposerDraft(workspaceContext: String = "") {
        val draft = _composerDraft.value.trim()
        if (draft.isEmpty()) return
        if (_busy.value || _enhancingPrompt.value) return
        if (!_credentials.value.hasKey(_provider.value)) {
            _error.value = "API key required. Tap the key icon in AI Architect to add one."
            return
        }
        viewModelScope.launch {
            try {
                _enhancingPrompt.value = true
                _error.value = null
                val enhanced = promptEnhancementService.enhancePrompt(
                    draft = draft,
                    workspaceContext = workspaceContext.trim(),
                )
                enhanced.fold(
                    onSuccess = { _composerDraft.value = it },
                    onFailure = {
                        _error.value = it.message ?: "Prompt enhancement failed."
                    },
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Prompt enhancement failed."
            } finally {
                _enhancingPrompt.value = false
            }
        }
    }

    fun sendUserMessage(text: String, systemPromptAppendix: String = "") {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _busy.value || _enhancingPrompt.value) return
        toolSchemaError?.let {
            _error.value = it
            return
        }
        if (!_credentials.value.hasKey(_provider.value)) {
            _error.value = "API key required. Tap the key icon in AI Architect to add one."
            return
        }
        viewModelScope.launch {
            try {
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
            } catch (e: Exception) {
                _error.value = e.message ?: "Unexpected agent failure."
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearConversation() {
        if (_busy.value || _enhancingPrompt.value) return
        apiHistory.length().let { len ->
            for (i in len - 1 downTo 0) apiHistory.remove(i)
        }
        _lines.value = emptyList()
        _error.value = null
    }

    fun approvePendingTool() {
        pendingToolApproval?.complete(true)
        pendingToolApproval = null
        _agentToolApprovalState.value = AgentToolApprovalState()
    }

    fun denyPendingTool() {
        pendingToolApproval?.complete(false)
        pendingToolApproval = null
        _agentToolApprovalState.value = AgentToolApprovalState()
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
            val assistantExplanation = textBuf.toString().trim()
            for (call in toolCalls) {
                val provider = _provider.value
                val startedAtMillis = System.currentTimeMillis()
                val pathRaw = call.input.optString("path").trim().trim('/')
                val captureMutation =
                    pathRaw.isNotBlank() &&
                    (call.name == "write_file" || call.name == "edit_file")
                val beforeSnap = if (captureMutation) {
                    workspace.readText(pathRaw).getOrNull() ?: ""
                } else {
                    ""
                }
                val riskClass = riskClassForTool(call.name)
                val policyMode = _trustPolicyState.value.modeFor(riskClass)
                val approvalOutcome = when (policyMode) {
                    AgentToolPolicyMode.AUTO -> ToolApprovalOutcome.AUTO
                    AgentToolPolicyMode.DENY -> ToolApprovalOutcome.BLOCKED
                    AgentToolPolicyMode.ASK -> {
                        val approved = requestToolApproval(
                            call = call,
                            riskClass = riskClass,
                            policyMode = policyMode,
                            assistantExplanation = assistantExplanation,
                        )
                        if (approved) ToolApprovalOutcome.APPROVED else ToolApprovalOutcome.DENIED
                    }
                }
                val resultText = when (approvalOutcome) {
                    ToolApprovalOutcome.AUTO,
                    ToolApprovalOutcome.APPROVED -> toolRouter.dispatch(call.name, call.input)
                    ToolApprovalOutcome.DENIED ->
                        "Error: ${call.name} denied by user"
                    ToolApprovalOutcome.BLOCKED ->
                        "Error: ${call.name} blocked by trust policy (${policyMode.displayName})"
                }
                val mutation = if (captureMutation &&
                    pathRaw.isNotBlank() &&
                    !resultText.startsWith("Error:")
                ) {
                    val afterSnap = workspace.readText(pathRaw).getOrNull() ?: ""
                    ToolMutationAction(
                        path = pathRaw,
                        beforeSnapshot = beforeSnap,
                        afterSnapshot = afterSnap,
                    )
                } else {
                    null
                }
                val durationMs = System.currentTimeMillis() - startedAtMillis
                appendToolLine(
                    name = call.name,
                    input = call.input,
                    resultText = resultText,
                    mutation = mutation,
                    receipt = ToolReceipt(
                        providerName = provider.displayName,
                        toolName = call.name,
                        target = toolTargetSummary(call.name, call.input),
                        status = if (resultText.startsWith("Error:")) "FAILED" else "OK",
                        riskClass = riskClass.displayName,
                        policyMode = policyMode.displayName,
                        approvalOutcome = approvalOutcome.displayName,
                        startedAt = receiptTimeFormat.format(Date(startedAtMillis)),
                        durationMs = durationMs,
                    ),
                )
                toolResults.put(
                    JSONObject()
                        .put("type", "tool_result")
                        .put("tool_use_id", call.id)
                        .put("tool_name", call.name)
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

    fun revertToolMutation(chatLineId: String) {
        if (_busy.value) return
        viewModelScope.launch {
            val line = _lines.value.find { it is ChatLine.Tool && it.id == chatLineId } as? ChatLine.Tool
                ?: return@launch
            val m = line.mutation ?: return@launch
            if (m.phase != ToolMutationPhase.APPLIED_ON_DISK) return@launch

            val cur = workspace.readText(m.path).getOrElse { e ->
                patchToolMutationLine(
                    chatLineId,
                    m.copy(
                        phase = ToolMutationPhase.CONFLICT,
                        conflictDetail = e.message ?: "Could not read file",
                    ),
                )
                return@launch
            }
            if (cur != m.afterSnapshot) {
                patchToolMutationLine(
                    chatLineId,
                    m.copy(
                        phase = ToolMutationPhase.CONFLICT,
                        conflictDetail = "File changed since agent write; revert blocked.",
                    ),
                )
                return@launch
            }
            workspace.writeText(m.path, m.beforeSnapshot).fold(
                onSuccess = {
                    patchToolMutationLine(
                        chatLineId,
                        m.copy(phase = ToolMutationPhase.REVERTED, conflictDetail = null),
                    )
                },
                onFailure = { e ->
                    patchToolMutationLine(
                        chatLineId,
                        m.copy(
                            phase = ToolMutationPhase.CONFLICT,
                            conflictDetail = e.message ?: "Revert write failed",
                        ),
                    )
                },
            )
        }
    }

    fun applyToolMutation(chatLineId: String) {
        if (_busy.value) return
        viewModelScope.launch {
            val line = _lines.value.find { it is ChatLine.Tool && it.id == chatLineId } as? ChatLine.Tool
                ?: return@launch
            val m = line.mutation ?: return@launch
            if (m.phase != ToolMutationPhase.REVERTED) return@launch

            val cur = workspace.readText(m.path).getOrElse { e ->
                patchToolMutationLine(
                    chatLineId,
                    m.copy(
                        phase = ToolMutationPhase.CONFLICT,
                        conflictDetail = e.message ?: "Could not read file",
                    ),
                )
                return@launch
            }
            if (cur != m.beforeSnapshot) {
                patchToolMutationLine(
                    chatLineId,
                    m.copy(
                        phase = ToolMutationPhase.CONFLICT,
                        conflictDetail = "File changed since revert; apply blocked.",
                    ),
                )
                return@launch
            }
            workspace.writeText(m.path, m.afterSnapshot).fold(
                onSuccess = {
                    patchToolMutationLine(
                        chatLineId,
                        m.copy(phase = ToolMutationPhase.APPLIED_ON_DISK, conflictDetail = null),
                    )
                },
                onFailure = { e ->
                    patchToolMutationLine(
                        chatLineId,
                        m.copy(
                            phase = ToolMutationPhase.CONFLICT,
                            conflictDetail = e.message ?: "Apply write failed",
                        ),
                    )
                },
            )
        }
    }

    private fun patchToolMutationLine(chatLineId: String, newMutation: ToolMutationAction) {
        _lines.update { list ->
            list.map { line ->
                if (line is ChatLine.Tool && line.id == chatLineId) {
                    line.copy(mutation = newMutation)
                } else {
                    line
                }
            }
        }
    }

    private fun appendToolLine(
        name: String,
        input: JSONObject,
        resultText: String,
        mutation: ToolMutationAction?,
        receipt: ToolReceipt,
    ) {
        val inputJson = formatJson(input)
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
                mutation = mutation,
                receipt = receipt,
            )
        }
    }

    private fun toolTargetSummary(name: String, input: JSONObject): String {
        val path = input.optString("path").takeIf { it.isNotBlank() }
        return when (name) {
            "list_files" -> "workspace"
            "search_files" -> input.optString("pattern").takeIf { it.isNotBlank() }?.let {
                "pattern: $it"
            } ?: "workspace"
            "run_command" -> input.optString("command").takeIf { it.isNotBlank() } ?: "workspace"
            "rename_path" -> {
                val newName = input.optString("new_name").takeIf { it.isNotBlank() }
                if (path != null && newName != null) "$path -> $newName" else path ?: "workspace"
            }
            else -> path ?: "workspace"
        }
    }

    private suspend fun requestToolApproval(
        call: ToolCall,
        riskClass: AgentToolRiskClass,
        policyMode: AgentToolPolicyMode,
        assistantExplanation: String,
    ): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        pendingToolApproval = deferred
        _agentToolApprovalState.value = AgentToolApprovalState(
            visible = true,
            requestId = call.id,
            toolName = call.name,
            riskClass = riskClass,
            target = toolTargetSummary(call.name, call.input),
            workspacePath = workspace.rootTreeUriOrNull()?.toString().orEmpty(),
            inputJson = formatJson(call.input),
            assistantExplanation = assistantExplanation
                .ifBlank { "The assistant did not include a rationale before requesting this action." }
                .take(APPROVAL_EXPLANATION_MAX_CHARS),
            policyMode = policyMode,
        )
        return try {
            deferred.await()
        } finally {
            if (pendingToolApproval === deferred) {
                pendingToolApproval = null
            }
            _agentToolApprovalState.value = AgentToolApprovalState()
        }
    }

    private fun riskClassForTool(name: String): AgentToolRiskClass = when (name) {
        "list_files", "read_file", "search_files" -> AgentToolRiskClass.READ_ONLY
        "write_file", "edit_file", "create_directory" -> AgentToolRiskClass.FILE_MUTATION
        "rename_path", "delete_path" -> AgentToolRiskClass.DESTRUCTIVE
        "run_command" -> AgentToolRiskClass.COMMAND
        else -> AgentToolRiskClass.DESTRUCTIVE
    }

    private fun formatJson(input: JSONObject): String {
        return try {
            input.toString(2)
        } catch (_: Exception) {
            input.toString()
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

    private enum class ToolApprovalOutcome(val displayName: String) {
        AUTO("Auto"),
        APPROVED("Approved"),
        DENIED("Denied"),
        BLOCKED("Blocked"),
    }

    enum class ToolMutationPhase {
        APPLIED_ON_DISK,
        REVERTED,
        CONFLICT,
    }

    data class ToolMutationAction(
        val path: String,
        val beforeSnapshot: String,
        val afterSnapshot: String,
        val phase: ToolMutationPhase = ToolMutationPhase.APPLIED_ON_DISK,
        val conflictDetail: String? = null,
    )

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
            val mutation: ToolMutationAction?,
            val receipt: ToolReceipt,
        ) : ChatLine()
    }

    data class ToolReceipt(
        val providerName: String,
        val toolName: String,
        val target: String,
        val status: String,
        val riskClass: String,
        val policyMode: String,
        val approvalOutcome: String,
        val startedAt: String,
        val durationMs: Long,
    )

    private companion object {
        private const val TOOL_RESULT_UI_MAX_CHARS = 100_000
        private const val TOOL_PREVIEW_CHARS = 600
        private const val APPROVAL_EXPLANATION_MAX_CHARS = 800
        private val receiptTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    }
}
