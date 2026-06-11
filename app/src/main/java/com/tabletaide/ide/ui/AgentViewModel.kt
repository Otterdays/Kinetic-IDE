package com.tabletaide.ide.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabletaide.ide.IdeConstants
import com.tabletaide.ide.agent.LlmClientResolver
import com.tabletaide.ide.agent.PromptEnhancementService
import com.tabletaide.ide.agent.StreamEvent
import com.tabletaide.ide.agent.ToolRouter
import com.tabletaide.ide.data.AgentAuditStore
import com.tabletaide.ide.data.AgentToolApprovalState
import com.tabletaide.ide.data.AgentTelemetryCodec
import com.tabletaide.ide.data.AgentTelemetryStore
import com.tabletaide.ide.data.AgentToolPolicyMode
import com.tabletaide.ide.data.AgentToolRiskClass
import com.tabletaide.ide.data.AgentTrustPolicyState
import com.tabletaide.ide.data.AgentTrustStore
import com.tabletaide.ide.data.AuditEntry
import com.tabletaide.ide.data.CommandPreview
import com.tabletaide.ide.data.CommandRiskClassifier
import com.tabletaide.ide.data.CommandSnapshotDiff
import com.tabletaide.ide.data.LlmCredentialState
import com.tabletaide.ide.data.GeminiModelParser
import com.tabletaide.ide.data.LlmModelCatalog
import com.tabletaide.ide.data.LlmModelFetchService
import com.tabletaide.ide.data.LlmProvider
import com.tabletaide.ide.data.LlmProviderStore
import com.tabletaide.ide.data.LlmSelectionState
import com.tabletaide.ide.data.ModelPickerUiState
import com.tabletaide.ide.data.SnapshotConflict
import com.tabletaide.ide.data.TelemetryEvent
import com.tabletaide.ide.data.TelemetryEventType
import com.tabletaide.ide.data.TelemetrySummary
import com.tabletaide.ide.data.WorkspaceRepository
import com.tabletaide.ide.data.WorkspaceSnapshot
import com.tabletaide.ide.data.WorkspaceSnapshotStore
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
    private val llmClientResolver: LlmClientResolver,
    private val promptEnhancementService: PromptEnhancementService,
    private val toolRouter: ToolRouter,
    private val trustStore: AgentTrustStore,
    private val providerStore: LlmProviderStore,
    private val modelFetchService: LlmModelFetchService,
    private val workspace: WorkspaceRepository,
    private val snapshotStore: WorkspaceSnapshotStore,
    private val auditStore: AgentAuditStore,
    private val telemetryStore: AgentTelemetryStore,
) : ViewModel() {

    private val apiHistory = JSONArray()
    private val toolsJson = toolRouter.toolDefinitions()
    private val toolSchemaError = toolRouter.validateToolDefinitions(toolsJson)

    private val _provider = MutableStateFlow(providerStore.getProvider())
    val provider: StateFlow<LlmProvider> = _provider.asStateFlow()

    private val _selection = MutableStateFlow(providerStore.getSelection())
    val selection: StateFlow<LlmSelectionState> = _selection.asStateFlow()

    private val _credentials = MutableStateFlow(providerStore.getCredentialState())
    val credentials: StateFlow<LlmCredentialState> = _credentials.asStateFlow()

    private val _modelPickerState = MutableStateFlow(ModelPickerUiState())
    val modelPickerState: StateFlow<ModelPickerUiState> = _modelPickerState.asStateFlow()

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
    private val sessionId = UUID.randomUUID().toString()
    private var sessionStarted = false

    private val _telemetrySummary = MutableStateFlow(TelemetrySummary())
    val telemetrySummary: StateFlow<TelemetrySummary> = _telemetrySummary.asStateFlow()

    private val _auditEntries = MutableStateFlow<List<AuditEntry>>(emptyList())
    val auditEntries: StateFlow<List<AuditEntry>> = _auditEntries.asStateFlow()

    private val _auditLoading = MutableStateFlow(false)
    val auditLoading: StateFlow<Boolean> = _auditLoading.asStateFlow()

    init {
        viewModelScope.launch {
            refreshTelemetrySummary()
            if (_credentials.value.hasAnyKey()) {
                loadModelCatalog()
            }
        }
    }

    fun loadModelCatalog(force: Boolean = false) {
        if (_modelPickerState.value.loading) return
        if (_modelPickerState.value.loaded && !force) return
        viewModelScope.launch {
            _modelPickerState.update { it.copy(loading = true, errorMessage = null) }
            val creds = _credentials.value
            val allModels = mutableListOf<com.tabletaide.ide.data.LlmModelOption>()
            var fetchError: String? = null
            for (provider in LlmProvider.entries) {
                if (!creds.hasKey(provider)) continue
                modelFetchService.fetchForProvider(provider).fold(
                    onSuccess = { allModels += it },
                    onFailure = { error ->
                        fetchError = error.message ?: "Failed to load models for ${provider.displayName}"
                    },
                )
            }
            _modelPickerState.value = ModelPickerUiState(
                loading = false,
                models = allModels,
                loaded = true,
                errorMessage = fetchError,
            )
            refreshSelectionDisplayName()
            maybeAutoSelectFirstModel()
        }
    }

    private fun refreshSelectionDisplayName() {
        val current = _selection.value
        val displayName = LlmModelCatalog.displayNameFor(_modelPickerState.value.models, current.modelId)
        if (displayName != null && displayName != current.modelDisplayName) {
            _selection.value = current.copy(modelDisplayName = displayName)
        }
    }

    private fun maybeAutoSelectFirstModel() {
        val current = _selection.value
        if (current.hasModel) return
        if (!_credentials.value.hasKey(current.provider)) return
        val providerModels = _modelPickerState.value.models.filter { it.provider == current.provider }
        if (providerModels.isEmpty()) return
        val preferredId = when (current.provider) {
            LlmProvider.GEMINI -> GeminiModelParser.preferredDefaultModelId(providerModels)
            else -> providerModels.first().id
        } ?: return
        val match = providerModels.find { it.id == preferredId } ?: providerModels.first()
        setSelection(match.provider, match.id)
    }

    fun loadAuditEntries() {
        viewModelScope.launch {
            _auditLoading.value = true
            _auditEntries.value = auditStore.loadAll().reversed()
            _auditLoading.value = false
        }
    }

    fun clearAuditEntries() {
        viewModelScope.launch {
            auditStore.clear()
            _auditEntries.value = emptyList()
        }
    }

    fun setProvider(provider: LlmProvider) {
        providerStore.setProvider(provider)
        _provider.value = provider
        _selection.value = providerStore.getSelection()
        _error.value = null
    }

    fun setSelection(provider: LlmProvider, modelId: String) {
        if (!providerStore.getCredentialState().hasKey(provider)) {
            _error.value = "Add an API key for ${provider.displayName} before selecting this model."
            return
        }
        providerStore.setSelection(provider, modelId)
        _provider.value = provider
        val displayName = LlmModelCatalog.displayNameFor(_modelPickerState.value.models, modelId)
        _selection.value = LlmSelectionState(
            provider = provider,
            modelId = modelId,
            modelDisplayName = displayName,
        )
        _error.value = null
    }

    fun setApiKey(provider: LlmProvider, apiKey: String) {
        providerStore.setApiKey(provider, apiKey)
        _credentials.value = providerStore.getCredentialState()
        _error.value = null
        _modelPickerState.value = ModelPickerUiState()
        loadModelCatalog(force = true)
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
        if (!_selection.value.hasModel) {
            _error.value = "No model selected. Open the model picker — catalog may show Coming soon until loaded."
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
        if (!_selection.value.hasModel) {
            _error.value = "No model selected. Open the model picker — catalog may show Coming soon until loaded."
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
        if (!_selection.value.hasModel) {
            _error.value = "No model selected. Open the model picker — catalog may show Coming soon until loaded."
            return
        }
        viewModelScope.launch {
            try {
                _busy.value = true
                _error.value = null
                appendUserLine(trimmed)
                ensureTelemetrySessionStarted()
                val userContent = JSONArray().put(
                    JSONObject().put("type", "text").put("text", trimmed),
                )
                apiHistory.put(
                    JSONObject().put("role", "user").put("content", userContent),
                )
                runAgentRounds(
                    userText = trimmed,
                    systemPromptAppendix = systemPromptAppendix.trim(),
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Unexpected agent failure."
                recordTelemetry(
                    type = TelemetryEventType.ERROR_RECORDED,
                    payload = JSONObject()
                        .put("message", e.message ?: "Unexpected agent failure.")
                        .put("source", "send_user_message"),
                )
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

    private suspend fun runAgentRounds(userText: String, systemPromptAppendix: String = "") {
        val systemPrompt = buildString {
            append(IdeConstants.SYSTEM_PROMPT)
            if (systemPromptAppendix.isNotEmpty()) {
                append("\n\n")
                append(systemPromptAppendix)
            }
        }
        val turnId = UUID.randomUUID().toString()
        recordTelemetry(
            type = TelemetryEventType.TURN_STARTED,
            turnId = turnId,
            payload = JSONObject()
                .put("provider", _provider.value.displayName)
                .put("userTextHash", AgentTelemetryCodec.sha256(userText))
                .put("userTextChars", userText.length),
        )
        val contextTokens = AgentTelemetryCodec.estimateTokens(systemPromptAppendix)
        recordTelemetry(
            type = TelemetryEventType.CONTEXT_BUILT,
            turnId = turnId,
            payload = JSONObject()
                .put("systemPromptTokens", AgentTelemetryCodec.estimateTokens(IdeConstants.SYSTEM_PROMPT))
                .put("appendixTokens", contextTokens)
                .put("appendixChars", systemPromptAppendix.length)
                .put("source", "workspace_context_builder"),
        )
        val promptTokens = AgentTelemetryCodec.estimateTokens(systemPrompt) +
            AgentTelemetryCodec.estimateTokens(apiHistory.toString())
        recordTelemetry(
            type = TelemetryEventType.PROMPT_SENT,
            turnId = turnId,
            payload = JSONObject()
                .put("promptTokens", promptTokens)
                .put("tokenSource", "rough_char_estimator_v1")
                .put("messageCount", apiHistory.length())
                .put("toolCount", toolsJson.length()),
        )
        var round = 0
        var completionTokensForTurn = 0L
        while (round < IdeConstants.MAX_TOOL_ROUNDS) {
            round++
            startAssistantSlot()
            val textBuf = StringBuilder()
            val toolCalls = mutableListOf<ToolCall>()
            var streamError: String? = null
            var firstTokenMs: Long? = null

            val activeProvider = _provider.value
            val model = llmClientResolver.modelFor(activeProvider)
            val modelSpanId = UUID.randomUUID().toString()
            val modelStartedAt = System.currentTimeMillis()
            if (model.isBlank()) {
                streamError =
                    "No model selected for ${activeProvider.displayName}. Pick a model or wait for Coming soon providers."
            } else {
                llmClientResolver.clientFor(activeProvider).streamMessage(
                    model = model,
                    systemPrompt = systemPrompt,
                    messages = apiHistory,
                    tools = toolsJson,
                    maxTokens = IdeConstants.MAX_OUTPUT_TOKENS,
                ).collect { ev ->
                    when (ev) {
                        is StreamEvent.TextDelta -> {
                            if (firstTokenMs == null) {
                                firstTokenMs = System.currentTimeMillis() - modelStartedAt
                            }
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
            }
            val modelDurationMs = System.currentTimeMillis() - modelStartedAt
            val completionTokens = AgentTelemetryCodec.estimateTokens(textBuf.toString())
            completionTokensForTurn += completionTokens

            if (streamError != null) {
                _error.value = "${activeProvider.displayName} · $model: $streamError"
                finalizeStreamingAssistant(textBuf.toString().ifEmpty { "(error)" })
                recordTelemetry(
                    type = TelemetryEventType.ERROR_RECORDED,
                    turnId = turnId,
                    spanId = modelSpanId,
                    payload = JSONObject()
                        .put("message", streamError)
                        .put("provider", _provider.value.displayName)
                        .put("model", model)
                        .put("round", round),
                )
                recordTelemetry(
                    type = TelemetryEventType.MODEL_COMPLETED,
                    turnId = turnId,
                    spanId = modelSpanId,
                    payload = JSONObject()
                        .put("provider", _provider.value.displayName)
                        .put("model", model)
                        .put("status", "FAILED")
                        .put("round", round)
                        .put("durationMs", modelDurationMs)
                        .put("firstTokenMs", firstTokenMs ?: -1L)
                        .put("completionTokens", completionTokens)
                        .put("toolCalls", toolCalls.size)
                        .put("tokenSource", "rough_char_estimator_v1"),
                )
                return
            }

            finalizeStreamingAssistant(textBuf.toString())
            recordTelemetry(
                type = TelemetryEventType.MODEL_COMPLETED,
                turnId = turnId,
                spanId = modelSpanId,
                payload = JSONObject()
                    .put("provider", _provider.value.displayName)
                    .put("model", model)
                    .put("status", "OK")
                    .put("round", round)
                    .put("durationMs", modelDurationMs)
                    .put("firstTokenMs", firstTokenMs ?: -1L)
                    .put("completionTokens", completionTokens)
                    .put("toolCalls", toolCalls.size)
                    .put("stopReason", if (toolCalls.isEmpty()) "end_turn" else "tool_use")
                    .put("tokenSource", "rough_char_estimator_v1"),
            )
            pushAssistantToApi(textBuf.toString(), toolCalls)

            if (toolCalls.isEmpty()) {
                recordTurnCost(turnId, promptTokens, completionTokensForTurn)
                return
            }

            val toolResults = JSONArray()
            val assistantExplanation = textBuf.toString().trim()
            for (call in toolCalls) {
                val toolSpanId = call.id.ifBlank { UUID.randomUUID().toString() }
                val provider = _provider.value
                val startedAtMillis = System.currentTimeMillis()
                recordTelemetry(
                    type = TelemetryEventType.TOOL_REQUESTED,
                    turnId = turnId,
                    spanId = toolSpanId,
                    parentSpanId = modelSpanId,
                    payload = JSONObject()
                        .put("toolName", call.name)
                        .put("target", toolTargetSummary(call.name, call.input))
                        .put("inputHash", AgentTelemetryCodec.sha256(call.input.toString()))
                        .put("inputChars", call.input.toString().length),
                )
                val pathRaw = call.input.optString("path").trim().trim('/')
                val captureMutation =
                    pathRaw.isNotBlank() &&
                    (call.name == "write_file" || call.name == "edit_file")
                val beforeSnap = if (captureMutation) {
                    workspace.readText(pathRaw).getOrNull() ?: ""
                } else {
                    ""
                }
                val commandPreview: CommandPreview? = if (call.name == "run_command") {
                    val cmd = call.input.optString("command").trim()
                    if (cmd.isNotEmpty()) CommandRiskClassifier.classify(cmd) else null
                } else null
                val riskClass = riskClassForTool(call.name)
                val policyMode = _trustPolicyState.value.modeFor(riskClass)
                val approvalOutcome = when (policyMode) {
                    AgentToolPolicyMode.AUTO -> ToolApprovalOutcome.AUTO
                    AgentToolPolicyMode.DENY -> ToolApprovalOutcome.BLOCKED
                    AgentToolPolicyMode.ASK -> {
                        val approvalStartedAt = System.currentTimeMillis()
                        recordTelemetry(
                            type = TelemetryEventType.APPROVAL_REQUESTED,
                            turnId = turnId,
                            spanId = toolSpanId,
                            payload = JSONObject()
                                .put("toolName", call.name)
                                .put("riskClass", riskClass.displayName)
                                .put("policyMode", policyMode.displayName)
                                .put("target", toolTargetSummary(call.name, call.input)),
                        )
                        val approved = requestToolApproval(
                            call = call,
                            riskClass = riskClass,
                            policyMode = policyMode,
                            assistantExplanation = assistantExplanation,
                            commandPreview = commandPreview,
                        )
                        recordTelemetry(
                            type = TelemetryEventType.APPROVAL_DECIDED,
                            turnId = turnId,
                            spanId = toolSpanId,
                            payload = JSONObject()
                                .put("toolName", call.name)
                                .put("decision", if (approved) "approved" else "denied")
                                .put("waitMs", System.currentTimeMillis() - approvalStartedAt),
                        )
                        if (approved) ToolApprovalOutcome.APPROVED else ToolApprovalOutcome.DENIED
                    }
                }
                val willExecute = approvalOutcome == ToolApprovalOutcome.AUTO ||
                    approvalOutcome == ToolApprovalOutcome.APPROVED
                val captureCommandSnapshot = call.name == "run_command" && willExecute
                val preCommandSnapshot: WorkspaceSnapshot? = if (captureCommandSnapshot) {
                    snapshotStore.snapshot().also {
                        recordTelemetry(
                            type = TelemetryEventType.CHECKPOINT_CREATED,
                            turnId = turnId,
                            spanId = toolSpanId,
                            payload = JSONObject()
                                .put("toolName", call.name)
                                .put("snapshotFiles", it.files.size)
                                .put("reason", "before_run_command"),
                        )
                    }
                } else null

                recordTelemetry(
                    type = TelemetryEventType.TOOL_STARTED,
                    turnId = turnId,
                    spanId = toolSpanId,
                    payload = JSONObject()
                        .put("toolName", call.name)
                        .put("willExecute", willExecute)
                        .put("approvalOutcome", approvalOutcome.displayName),
                )
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
                val commandMutation: CommandMutationAction? = if (captureCommandSnapshot &&
                    preCommandSnapshot != null &&
                    !resultText.startsWith("Error:")
                ) {
                    val post = snapshotStore.snapshot()
                    val diff = snapshotStore.diff(preCommandSnapshot, post)
                    if (diff.isEmpty) {
                        null
                    } else {
                        CommandMutationAction(
                            command = call.input.optString("command").trim(),
                            pre = preCommandSnapshot,
                            post = post,
                            diff = diff,
                        )
                    }
                } else null
                val durationMs = System.currentTimeMillis() - startedAtMillis
                val status = if (resultText.startsWith("Error:")) "FAILED" else "OK"
                recordTelemetry(
                    type = TelemetryEventType.TOOL_COMPLETED,
                    turnId = turnId,
                    spanId = toolSpanId,
                    payload = JSONObject()
                        .put("toolName", call.name)
                        .put("target", toolTargetSummary(call.name, call.input))
                        .put("status", status)
                        .put("durationMs", durationMs)
                        .put("resultHash", AgentTelemetryCodec.sha256(resultText))
                        .put("resultChars", resultText.length)
                        .put("riskClass", riskClass.displayName)
                        .put("policyMode", policyMode.displayName)
                        .put("approvalOutcome", approvalOutcome.displayName),
                )
                if (mutation != null || commandMutation != null) {
                    recordTelemetry(
                        type = TelemetryEventType.MUTATION_APPLIED,
                        turnId = turnId,
                        spanId = toolSpanId,
                        payload = JSONObject()
                            .put("toolName", call.name)
                            .put("filePath", mutation?.path.orEmpty())
                            .put("commandDiff", commandMutation?.diff?.summary().orEmpty())
                            .put("checkpointed", preCommandSnapshot != null),
                    )
                }
                appendToolLine(
                    name = call.name,
                    input = call.input,
                    resultText = resultText,
                    mutation = mutation,
                    commandMutation = commandMutation,
                    receipt = ToolReceipt(
                        providerName = provider.displayName,
                        toolName = call.name,
                        target = toolTargetSummary(call.name, call.input),
                        status = status,
                        riskClass = riskClass.displayName,
                        policyMode = policyMode.displayName,
                        approvalOutcome = approvalOutcome.displayName,
                        startedAt = receiptTimeFormat.format(Date(startedAtMillis)),
                        durationMs = durationMs,
                    ),
                )
                val mutSummary = buildString {
                    mutation?.let { append("file:${it.path}") }
                    commandMutation?.let {
                        if (isNotEmpty()) append("; ")
                        append("cmd:${it.diff.summary()}")
                    }
                }.ifEmpty { "none" }
                auditStore.append(
                    AuditEntry(
                        id = call.id,
                        timestamp = auditStore.formatTimestamp(startedAtMillis),
                        epochMs = startedAtMillis,
                        provider = provider.displayName,
                        toolName = call.name,
                        target = toolTargetSummary(call.name, call.input),
                        status = status,
                        riskClass = riskClass.displayName,
                        policyMode = policyMode.displayName,
                        approvalOutcome = approvalOutcome.displayName,
                        durationMs = durationMs,
                        mutationSummary = mutSummary,
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
        recordTelemetry(
            type = TelemetryEventType.ERROR_RECORDED,
            turnId = turnId,
            payload = JSONObject()
                .put("message", "Stopped: max tool rounds (${IdeConstants.MAX_TOOL_ROUNDS})")
                .put("source", "agent_round_loop"),
        )
        recordTurnCost(turnId, promptTokens, completionTokensForTurn)
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
                    recordTelemetryFromUi(
                        TelemetryEventType.MUTATION_REVERTED,
                        JSONObject()
                            .put("source", "tool_row")
                            .put("path", m.path)
                            .put("status", "OK"),
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
                    recordTelemetryFromUi(
                        TelemetryEventType.ERROR_RECORDED,
                        JSONObject()
                            .put("source", "tool_row_revert")
                            .put("path", m.path)
                            .put("message", e.message ?: "Revert write failed"),
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
                    recordTelemetryFromUi(
                        TelemetryEventType.MUTATION_APPLIED,
                        JSONObject()
                            .put("source", "tool_row_apply_again")
                            .put("path", m.path)
                            .put("status", "OK"),
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
                    recordTelemetryFromUi(
                        TelemetryEventType.ERROR_RECORDED,
                        JSONObject()
                            .put("source", "tool_row_apply_again")
                            .put("path", m.path)
                            .put("message", e.message ?: "Apply write failed"),
                    )
                },
            )
        }
    }

    fun revertCommandMutation(chatLineId: String) {
        if (_busy.value) return
        viewModelScope.launch {
            val line = _lines.value.find { it is ChatLine.Tool && it.id == chatLineId } as? ChatLine.Tool
                ?: return@launch
            val cm = line.commandMutation ?: return@launch
            if (cm.phase != CommandMutationPhase.APPLIED_ON_DISK) return@launch
            val report = snapshotStore.restoreChanged(
                diff = cm.diff,
                reference = cm.post,
                target = cm.pre,
            )
            val nextPhase = when {
                report.conflicts.isEmpty() -> CommandMutationPhase.REVERTED
                report.restoredPaths.isEmpty() -> CommandMutationPhase.CONFLICT
                else -> CommandMutationPhase.REVERTED
            }
            patchCommandMutationLine(
                chatLineId,
                cm.copy(phase = nextPhase, conflicts = report.conflicts),
            )
            recordTelemetryFromUi(
                TelemetryEventType.CHECKPOINT_RESTORED,
                JSONObject()
                    .put("source", "command_row_revert")
                    .put("commandHash", AgentTelemetryCodec.sha256(cm.command))
                    .put("status", if (report.conflicts.isEmpty()) "OK" else "PARTIAL")
                    .put("restoredPaths", report.restoredPaths.size)
                    .put("conflicts", report.conflicts.size),
            )
        }
    }

    fun reapplyCommandMutation(chatLineId: String) {
        if (_busy.value) return
        viewModelScope.launch {
            val line = _lines.value.find { it is ChatLine.Tool && it.id == chatLineId } as? ChatLine.Tool
                ?: return@launch
            val cm = line.commandMutation ?: return@launch
            if (cm.phase != CommandMutationPhase.REVERTED) return@launch
            val report = snapshotStore.restoreChanged(
                diff = cm.diff,
                reference = cm.pre,
                target = cm.post,
            )
            val nextPhase = when {
                report.conflicts.isEmpty() -> CommandMutationPhase.APPLIED_ON_DISK
                report.restoredPaths.isEmpty() -> CommandMutationPhase.CONFLICT
                else -> CommandMutationPhase.APPLIED_ON_DISK
            }
            patchCommandMutationLine(
                chatLineId,
                cm.copy(phase = nextPhase, conflicts = report.conflicts),
            )
            recordTelemetryFromUi(
                TelemetryEventType.MUTATION_APPLIED,
                JSONObject()
                    .put("source", "command_row_reapply")
                    .put("commandHash", AgentTelemetryCodec.sha256(cm.command))
                    .put("status", if (report.conflicts.isEmpty()) "OK" else "PARTIAL")
                    .put("restoredPaths", report.restoredPaths.size)
                    .put("conflicts", report.conflicts.size),
            )
        }
    }

    private fun patchCommandMutationLine(chatLineId: String, newMutation: CommandMutationAction) {
        _lines.update { list ->
            list.map { line ->
                if (line is ChatLine.Tool && line.id == chatLineId) {
                    line.copy(commandMutation = newMutation)
                } else {
                    line
                }
            }
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
        commandMutation: CommandMutationAction?,
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
                commandMutation = commandMutation,
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
        commandPreview: CommandPreview? = null,
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
            commandPreview = commandPreview,
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

    private suspend fun ensureTelemetrySessionStarted() {
        if (sessionStarted) return
        sessionStarted = true
        recordTelemetry(
            type = TelemetryEventType.SESSION_STARTED,
            payload = JSONObject()
                .put("provider", _provider.value.displayName)
                .put("workspace", workspace.rootTreeUriOrNull()?.toString().orEmpty())
                .put("appSurface", "agent_chat"),
        )
    }

    private suspend fun recordTelemetry(
        type: TelemetryEventType,
        turnId: String? = null,
        spanId: String? = null,
        parentSpanId: String? = null,
        redactionClass: String = "metadata",
        payload: JSONObject = JSONObject(),
    ) {
        telemetryStore.append(
            TelemetryEvent(
                sessionId = sessionId,
                turnId = turnId,
                spanId = spanId,
                parentSpanId = parentSpanId,
                type = type,
                redactionClass = redactionClass,
                payload = payload,
                payloadHash = AgentTelemetryCodec.sha256(payload.toString()),
            ),
        )
        refreshTelemetrySummary()
    }

    private fun recordTelemetryFromUi(type: TelemetryEventType, payload: JSONObject) {
        viewModelScope.launch {
            ensureTelemetrySessionStarted()
            recordTelemetry(type = type, payload = payload)
        }
    }

    private suspend fun recordTurnCost(
        turnId: String,
        promptTokens: Long,
        completionTokens: Long,
    ) {
        val cost = AgentTelemetryCodec.estimateCost(promptTokens, completionTokens)
        recordTelemetry(
            type = TelemetryEventType.COST_ESTIMATED,
            turnId = turnId,
            payload = JSONObject()
                .put("promptTokens", cost.inputTokens)
                .put("completionTokens", cost.outputTokens)
                .put("estimatedCostUsd", cost.estimatedUsd)
                .put("source", cost.source)
                .put("confidence", "low_estimate"),
        )
    }

    private suspend fun refreshTelemetrySummary() {
        _telemetrySummary.value = telemetryStore.summary()
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
            val commandMutation: CommandMutationAction?,
            val receipt: ToolReceipt,
        ) : ChatLine()
    }

    enum class CommandMutationPhase {
        APPLIED_ON_DISK,
        REVERTED,
        CONFLICT,
    }

    data class CommandMutationAction(
        val command: String,
        val pre: WorkspaceSnapshot,
        val post: WorkspaceSnapshot,
        val diff: CommandSnapshotDiff,
        val phase: CommandMutationPhase = CommandMutationPhase.APPLIED_ON_DISK,
        val conflicts: List<SnapshotConflict> = emptyList(),
    )

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
