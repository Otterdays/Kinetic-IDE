package com.tabletaide.ide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tabletaide.ide.data.AgentToolPolicyMode
import com.tabletaide.ide.data.AgentToolRiskClass
import com.tabletaide.ide.data.AgentTrustPolicyState
import com.tabletaide.ide.data.AuditEntry
import com.tabletaide.ide.data.LlmCredentialState
import com.tabletaide.ide.data.LlmProvider
import com.tabletaide.ide.ui.theme.KineticThemeMode
import kotlinx.coroutines.launch

private fun clampEditorAgentFraction(value: Float): Float = value.coerceIn(0.22f, 0.82f)

/** Snap editor fraction toward palette presets when drag ends ([TRACE: DOCS/ROADMAP.md] Epic 1.3). */
private fun snapEditorAgentFraction(value: Float): Float {
    val c = clampEditorAgentFraction(value)
    val presets = listOf(0.3f, 0.5f, 0.7f)
    val nearest = presets.minByOrNull { kotlin.math.abs(it - c) } ?: return c
    return if (kotlin.math.abs(nearest - c) <= 0.11f) nearest else c
}

@Composable
fun TabletIdeScreen(
    ideVm: IdeViewModel = hiltViewModel(),
    agentVm: AgentViewModel = hiltViewModel(),
) {
    val explorerTreeState by ideVm.explorerTreeUiState.collectAsState()
    val explorerTreeFilterQuery by ideVm.explorerTreeFilterQuery.collectAsState()
    val tabs by ideVm.tabs.collectAsState()
    val selectedTabIndex by ideVm.selectedTabIndex.collectAsState()
    val railSection by ideVm.railSection.collectAsState()
    val status by ideVm.status.collectAsState()
    val undoRedoEpoch by ideVm.undoRedoEpoch.collectAsState()
    val gitRepoState by ideVm.gitRepoState.collectAsState()
    val gitCommitDialogState by ideVm.gitCommitDialogState.collectAsState()
    val commandRunnerState by ideVm.commandRunnerState.collectAsState()
    val runCommandDialogState by ideVm.runCommandDialogState.collectAsState()

    val canUndoNow = remember(undoRedoEpoch, tabs, selectedTabIndex) {
        ideVm.canUndo()
    }
    val canRedoNow = remember(undoRedoEpoch, tabs, selectedTabIndex) {
        ideVm.canRedo()
    }
    val themeMode by ideVm.themeMode.collectAsState()

    val chatLines by agentVm.lines.collectAsState()
    val busy by agentVm.busy.collectAsState()
    val enhancingPrompt by agentVm.enhancingPrompt.collectAsState()
    val agentError by agentVm.error.collectAsState()
    val provider by agentVm.provider.collectAsState()
    val credentials by agentVm.credentials.collectAsState()
    val telemetrySummary by agentVm.telemetrySummary.collectAsState()
    val auditEntries by agentVm.auditEntries.collectAsState()
    val auditLoading by agentVm.auditLoading.collectAsState()
    val composerDraft by agentVm.composerDraft.collectAsState()
    val trustPolicyState by agentVm.trustPolicyState.collectAsState()
    val agentToolApprovalState by agentVm.agentToolApprovalState.collectAsState()

    val explorerRecentsList by ideVm.explorerRecents.collectAsState()
    val explorerFavoritesList by ideVm.explorerFavorites.collectAsState()

    val activeTab = tabs.getOrNull(selectedTabIndex)
    val activePath = activeTab?.path
    val editorText = activeTab?.content ?: ""
    val len = editorText.length
    val editorField = TextFieldValue(
        editorText,
        TextRange(
            activeTab?.selectionStart?.coerceIn(0, len) ?: 0,
            activeTab?.selectionEnd?.coerceIn(0, len) ?: 0,
        ),
    )

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var dirtyCloseTabIndex by remember { mutableStateOf<Int?>(null) }
    var commandPaletteVisible by remember { mutableStateOf(false) }
    var commandPaletteQuery by remember { mutableStateOf("") }
    var apiKeysDialogVisible by remember { mutableStateOf(false) }
    var settingsDialogVisible by remember { mutableStateOf(false) }
    var auditTimelineVisible by remember { mutableStateOf(false) }
    var editorAgentFraction by rememberSaveable { mutableFloatStateOf(0.65f) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                ideVm.persistDraftIfPossible()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val openFolder = rememberWorkspaceTreePicker(onTreePicked = ideVm::openWorkspaceRoot)

    val paletteCommands = remember(
        tabs.size,
        tabs.count { it.dirty },
        undoRedoEpoch,
        selectedTabIndex,
        busy,
        activePath,
        gitRepoState.available,
        gitRepoState.clean,
        gitRepoState.canPush,
        gitRepoState.branchName,
        commandRunnerState.available,
        commandRunnerState.busy,
        commandRunnerState.lastCommand,
        trustPolicyState.fileMutationMode,
        trustPolicyState.destructiveMode,
        trustPolicyState.commandMode,
    ) {
        listOf(
            PaletteCommand(
                title = "Open workspace",
                subtitle = "Pick a folder (SAF)",
                keywords = listOf("open", "folder", "saf", "project"),
                onInvoke = openFolder,
            ),
            PaletteCommand(
                title = "Focus explorer",
                subtitle = "Show file tree",
                keywords = listOf("explorer", "tree", "sidebar"),
                onInvoke = { ideVm.setRailSection(RailSection.Explorer) },
            ),
            PaletteCommand(
                title = "Save",
                subtitle = "Active tab",
                keywords = listOf("save"),
                enabled = tabs.isNotEmpty(),
                onInvoke = { ideVm.saveCurrentFile() },
            ),
            PaletteCommand(
                title = "Save all",
                subtitle = "All dirty tabs",
                keywords = listOf("save", "all"),
                enabled = tabs.any { it.dirty },
                onInvoke = { ideVm.saveAllDirtyTabs() },
            ),
            PaletteCommand(
                title = "Generate commit message",
                subtitle = if (gitRepoState.available) {
                    "AI draft from real git status and diff"
                } else {
                    "Open a git repo root first"
                },
                keywords = listOf("git", "commit", "message", "ai", "generate"),
                enabled = gitRepoState.available,
                onInvoke = { ideVm.showCommitDialog(autoGenerateMessage = true) },
            ),
            PaletteCommand(
                title = "Commit",
                subtitle = if (gitRepoState.available) {
                    "Review message and commit current repo changes"
                } else {
                    "Open a git repo root first"
                },
                keywords = listOf("git", "commit", "changes"),
                enabled = gitRepoState.available,
                onInvoke = { ideVm.showCommitDialog() },
            ),
            PaletteCommand(
                title = "Commit and push",
                subtitle = if (gitRepoState.canPush) {
                    "Commit current changes and push tracked branch"
                } else {
                    "Current branch needs a tracked upstream"
                },
                keywords = listOf("git", "commit", "push", "publish", "remote"),
                enabled = gitRepoState.available,
                onInvoke = { ideVm.showCommitDialog() },
            ),
            PaletteCommand(
                title = "Next tab",
                subtitle = "Cycle editor tabs forward",
                keywords = listOf("next", "tab", "cycle", "forward"),
                enabled = tabs.size > 1,
                onInvoke = {
                    val next = (selectedTabIndex + 1).mod(tabs.size)
                    ideVm.selectTab(next)
                },
            ),
            PaletteCommand(
                title = "Previous tab",
                subtitle = "Cycle editor tabs backward",
                keywords = listOf("previous", "tab", "cycle", "back"),
                enabled = tabs.size > 1,
                onInvoke = {
                    val prev = if (selectedTabIndex - 1 < 0) tabs.lastIndex else selectedTabIndex - 1
                    ideVm.selectTab(prev)
                },
            ),
            PaletteCommand(
                title = "Undo",
                subtitle = "Active buffer",
                keywords = listOf("undo"),
                enabled = canUndoNow,
                onInvoke = { ideVm.undo() },
            ),
            PaletteCommand(
                title = "Redo",
                subtitle = "Active buffer",
                keywords = listOf("redo"),
                enabled = canRedoNow,
                onInvoke = { ideVm.redo() },
            ),
            PaletteCommand(
                title = "Toggle favorite (active file)",
                subtitle = activePath?.substringAfterLast('/') ?: "No file open",
                keywords = listOf("star", "favorite", "favourite", "pin"),
                enabled = tabs.isNotEmpty(),
                onInvoke = { ideVm.toggleFavoriteActiveTab() },
            ),
            PaletteCommand(
                title = "API keys",
                subtitle = "Configure Anthropic and Gemini on device",
                keywords = listOf("api", "key", "keys", "llm", "provider", "ai"),
                enabled = !busy,
                onInvoke = { apiKeysDialogVisible = true },
            ),
            PaletteCommand(
                title = "Agent trust settings",
                subtitle = buildString {
                    append("Files ")
                    append(trustPolicyState.fileMutationMode.displayName)
                    append(" · Destructive ")
                    append(trustPolicyState.destructiveMode.displayName)
                    append(" · Commands ")
                    append(trustPolicyState.commandMode.displayName)
                },
                keywords = listOf("trust", "approval", "policy", "agent", "safety", "settings"),
                enabled = !busy,
                onInvoke = { settingsDialogVisible = true },
            ),
            PaletteCommand(
                title = "Theme: Dark",
                subtitle = "Studio dark mode",
                keywords = listOf("theme", "dark", "ui", "appearance"),
                enabled = themeMode != KineticThemeMode.DARK,
                onInvoke = { ideVm.setThemeMode(KineticThemeMode.DARK) },
            ),
            PaletteCommand(
                title = "Theme: Light",
                subtitle = "Bright workspace mode",
                keywords = listOf("theme", "light", "ui", "appearance"),
                enabled = themeMode != KineticThemeMode.LIGHT,
                onInvoke = { ideVm.setThemeMode(KineticThemeMode.LIGHT) },
            ),
            PaletteCommand(
                title = "Theme: High contrast",
                subtitle = "Extra visual contrast",
                keywords = listOf("theme", "contrast", "accessibility", "ui"),
                enabled = themeMode != KineticThemeMode.HIGH_CONTRAST,
                onInvoke = { ideVm.setThemeMode(KineticThemeMode.HIGH_CONTRAST) },
            ),
            PaletteCommand(
                title = "Audit timeline",
                subtitle = "Browse persistent agent tool history",
                keywords = listOf("audit", "history", "log", "agent", "timeline", "receipt"),
                enabled = true,
                onInvoke = { auditTimelineVisible = true },
            ),
            PaletteCommand(
                title = "Clear agent chat",
                subtitle = "Reset AI Architect conversation",
                keywords = listOf("clear", "chat", "agent", "ai"),
                enabled = !busy,
                onInvoke = { agentVm.clearConversation() },
            ),
            PaletteCommand(
                title = "Layout: editor 70% · agent 30%",
                subtitle = "Split preset",
                keywords = listOf("layout", "split", "70", "agent", "panel"),
                onInvoke = { editorAgentFraction = clampEditorAgentFraction(0.7f) },
            ),
            PaletteCommand(
                title = "Layout: editor 50% · agent 50%",
                subtitle = "Split preset",
                keywords = listOf("layout", "split", "50", "equal"),
                onInvoke = { editorAgentFraction = clampEditorAgentFraction(0.5f) },
            ),
            PaletteCommand(
                title = "Layout: editor 30% · agent 70%",
                subtitle = "Split preset",
                keywords = listOf("layout", "split", "30", "wide", "chat"),
                onInvoke = { editorAgentFraction = clampEditorAgentFraction(0.3f) },
            ),
            PaletteCommand(
                title = "Execute / Run",
                subtitle = when {
                    commandRunnerState.busy -> "Cancel the running command from the same shell"
                    commandRunnerState.available -> "Run a shell command in the current workspace"
                    else -> commandRunnerState.availabilityMessage ?: "Command execution unavailable"
                },
                keywords = listOf("run", "execute", "debug", "play"),
                enabled = commandRunnerState.available && !commandRunnerState.busy,
                onInvoke = ideVm::showRunCommandDialog,
            ),
            PaletteCommand(
                title = "Rerun last command",
                subtitle = commandRunnerState.lastCommand.ifBlank { "No previous command" },
                keywords = listOf("rerun", "run", "repeat", "terminal"),
                enabled = commandRunnerState.available &&
                    !commandRunnerState.busy &&
                    commandRunnerState.lastCommand.isNotBlank(),
                onInvoke = ideVm::rerunLastCommand,
            ),
            PaletteCommand(
                title = "Cancel running command",
                subtitle = commandRunnerState.currentCommand.ifBlank { "No command is active" },
                keywords = listOf("cancel", "stop", "kill", "terminal"),
                enabled = commandRunnerState.busy,
                onInvoke = ideVm::cancelRunningCommand,
            ),
            PaletteCommand(
                title = "Clear terminal output",
                subtitle = "Reset terminal, output, and debug panes",
                keywords = listOf("clear", "terminal", "output", "debug"),
                enabled = commandRunnerState.available,
                onInvoke = ideVm::clearRunnerOutput,
            ),
        )
    }

    LaunchedEffect(auditTimelineVisible) {
        if (auditTimelineVisible) agentVm.loadAuditEntries()
    }

    LaunchedEffect(status) {
        val s = status ?: return@LaunchedEffect
        snackbar.showSnackbar(s)
    }

    fun requestCloseTab(index: Int) {
        if (tabs.getOrNull(index)?.dirty == true) {
            dirtyCloseTabIndex = index
        } else {
            ideVm.closeTabUnchecked(index)
        }
    }

    dirtyCloseTabIndex?.let { closeIdx ->
        val label = tabs.getOrNull(closeIdx)?.fileName ?: "this file"
        AlertDialog(
            onDismissRequest = { dirtyCloseTabIndex = null },
            title = { Text("Unsaved changes") },
            text = { Text("Save changes to $label before closing?") },
            dismissButton = {
                Row {
                    TextButton(onClick = { dirtyCloseTabIndex = null }) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            ideVm.closeTabUnchecked(closeIdx)
                            dirtyCloseTabIndex = null
                        },
                    ) {
                        Text("Discard")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ideVm.saveDirtyTabThenClose(closeIdx) {
                            dirtyCloseTabIndex = null
                        }
                    },
                ) {
                    Text("Save")
                }
            },
        )
    }

    if (apiKeysDialogVisible) {
        ApiKeysDialog(
            credentials = credentials,
            onDismiss = { apiKeysDialogVisible = false },
            onSave = { targetProvider, apiKey ->
                agentVm.setApiKey(targetProvider, apiKey)
                scope.launch { snackbar.showSnackbar("${targetProvider.displayName} API key saved") }
            },
        )
    }
    if (settingsDialogVisible) {
        SettingsDialog(
            currentTheme = themeMode,
            trustPolicyState = trustPolicyState,
            onDismiss = { settingsDialogVisible = false },
            onThemeSelect = { mode ->
                ideVm.setThemeMode(mode)
            },
            onTrustModeSelect = agentVm::setTrustPolicyMode,
        )
    }
    GitCommitDialog(
        gitState = gitRepoState,
        dialogState = gitCommitDialogState,
        onDismiss = ideVm::hideCommitDialog,
        onDraftChange = ideVm::updateCommitDraftMessage,
        onAuthorNameChange = ideVm::updateCommitAuthorName,
        onAuthorEmailChange = ideVm::updateCommitAuthorEmail,
        onStageUntrackedChange = ideVm::updateCommitStageUntracked,
        onGenerateMessage = ideVm::generateCommitMessage,
        onCommit = { ideVm.commitChanges(pushAfterCommit = false) },
        onCommitAndPush = { ideVm.commitChanges(pushAfterCommit = true) },
    )
    RunCommandDialog(
        dialogState = runCommandDialogState,
        runnerState = commandRunnerState,
        onDismiss = ideVm::hideRunCommandDialog,
        onCommandChange = ideVm::updateRunCommandText,
        onRun = ideVm::executeRunCommandFromDialog,
    )
    AgentToolApprovalDialog(
        approvalState = agentToolApprovalState,
        runnerState = commandRunnerState,
        onApprove = agentVm::approvePendingTool,
        onDeny = agentVm::denyPendingTool,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (
                    commandPaletteVisible ||
                    apiKeysDialogVisible ||
                    settingsDialogVisible ||
                    auditTimelineVisible ||
                    gitCommitDialogState.visible ||
                    agentToolApprovalState.visible
                ) {
                    return@onPreviewKeyEvent false
                }
                when {
                    e.key == Key.P && e.isCtrlPressed -> {
                        commandPaletteVisible = true
                        true
                    }
                    e.key == Key.S && e.isCtrlPressed -> {
                        if (e.isShiftPressed) ideVm.saveAllDirtyTabs() else ideVm.saveCurrentFile()
                        true
                    }
                    e.key == Key.W && e.isCtrlPressed -> {
                        if (tabs.isNotEmpty()) requestCloseTab(selectedTabIndex)
                        true
                    }
                    e.key == Key.Tab && e.isCtrlPressed && tabs.size > 1 -> {
                        val next = if (e.isShiftPressed) {
                            if (selectedTabIndex - 1 < 0) tabs.lastIndex else selectedTabIndex - 1
                        } else {
                            (selectedTabIndex + 1).mod(tabs.size)
                        }
                        ideVm.selectTab(next)
                        true
                    }
                    else -> false
                }
            },
    ) {
        Row(Modifier.fillMaxSize()) {
            KineticNavRail(
                section = railSection,
                onSection = ideVm::setRailSection,
                onOpenWorkspace = openFolder,
                onSearch = { commandPaletteVisible = true },
                onExtensionsStub = {
                    scope.launch { snackbar.showSnackbar("Extensions — coming soon") }
                },
                onOpenSettings = { settingsDialogVisible = true },
            )
            if (railSection != RailSection.Search && railSection != RailSection.Extensions) {
                FileTreePane(
                    rows = explorerTreeState.rows,
                    treeLoading = explorerTreeState.loading,
                    treeEmptyMessage = explorerTreeState.emptyMessage,
                    filterActive = explorerTreeState.filterActive,
                    treeFilterQuery = explorerTreeFilterQuery,
                    onTreeFilterQueryChange = ideVm::setExplorerTreeFilterQuery,
                    selectedPath = activePath,
                    favoritePaths = explorerFavoritesList,
                    recentPaths = explorerRecentsList,
                    hasWorkspaceRoot = ideVm.hasWorkspaceRoot(),
                    onOpenWorkspace = openFolder,
                    onSelectFile = ideVm::openOrSelectFile,
                    onToggleDirectory = ideVm::toggleExplorerDirectory,
                    onOpenPinnedPath = ideVm::openExplorerPinnedPath,
                    onToggleFavoritePath = ideVm::toggleExplorerFavorite,
                    onCreateFile = ideVm::explorerCreateEmptyFile,
                    onCreateFolderRelative = ideVm::explorerCreateFolder,
                    onRename = ideVm::explorerRename,
                    onDuplicate = ideVm::explorerDuplicateFile,
                    onDelete = ideVm::explorerDelete,
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                KineticTopBar(
                    tabs = tabs,
                    selectedIndex = selectedTabIndex,
                    canUndoNow = canUndoNow,
                    canRedoNow = canRedoNow,
                    gitState = gitRepoState,
                    runnerBusy = commandRunnerState.busy,
                    onSelectTab = ideVm::selectTab,
                    onCloseTab = ::requestCloseTab,
                    onSave = ideVm::saveCurrentFile,
                    onSaveAll = ideVm::saveAllDirtyTabs,
                    onUndo = ideVm::undo,
                    onRedo = ideVm::redo,
                    onOpenCommitDialog = ideVm::showCommitDialog,
                    onExecute = {
                        if (commandRunnerState.busy) {
                            ideVm.cancelRunningCommand()
                        } else {
                            ideVm.showRunCommandDialog()
                        }
                    },
                    agentBusy = busy,
                )
                CapabilityBanner(
                    hasWorkspaceRoot = ideVm.hasWorkspaceRoot(),
                    gitState = gitRepoState,
                    runnerState = commandRunnerState,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    val totalPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                    val editorW = clampEditorAgentFraction(editorAgentFraction)
                    Row(Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(editorW)
                                .fillMaxHeight(),
                        ) {
                            BreadcrumbBar(activePath)
                            CodeEditorPane(
                                value = editorField,
                                onValueChange = { tv ->
                                    ideVm.onEditorValueChange(
                                        tv.text,
                                        tv.selection.start,
                                        tv.selection.end,
                                    )
                                },
                                tabPath = activePath,
                                scrollInitialPx = activeTab?.scrollPx ?: 0,
                                onScrollPxCommitted = { path, px ->
                                    ideVm.reportEditorScroll(path, px)
                                },
                                enabled = tabs.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            )
                            TerminalPanel(
                                runnerState = commandRunnerState,
                                onRun = ideVm::showRunCommandDialog,
                                onCancel = ideVm::cancelRunningCommand,
                                onClear = ideVm::clearRunnerOutput,
                                modifier = Modifier
                                    .height(192.dp)
                                    .fillMaxWidth(),
                            )
                        }
                        EditorAgentSplitDivider(
                            totalWidthPx = totalPx,
                            onDragFractionDelta = { delta ->
                                editorAgentFraction =
                                    clampEditorAgentFraction(editorAgentFraction + delta)
                            },
                            onDragEnd = {
                                editorAgentFraction = snapEditorAgentFraction(editorAgentFraction)
                            },
                        )
                        AgentChatPanel(
                            lines = chatLines,
                            busy = busy,
                            enhancingPrompt = enhancingPrompt,
                            error = agentError,
                            currentProvider = provider,
                            credentials = credentials,
                            telemetrySummary = telemetrySummary,
                            composerDraft = composerDraft,
                            onDraftChange = agentVm::updateComposerDraft,
                            onSend = {
                                agentVm.sendComposerDraft(ideVm.buildAgentWorkspaceContext())
                            },
                            onEnhancePrompt = {
                                agentVm.enhanceComposerDraft(ideVm.buildAgentWorkspaceContext())
                            },
                            onClear = agentVm::clearConversation,
                            onProviderChange = agentVm::setProvider,
                            onOpenApiKeys = { apiKeysDialogVisible = true },
                            onRevertToolMutation = agentVm::revertToolMutation,
                            onApplyToolMutation = agentVm::applyToolMutation,
                            onRevertCommandMutation = agentVm::revertCommandMutation,
                            onReapplyCommandMutation = agentVm::reapplyCommandMutation,
                            modifier = Modifier
                                .weight(1f - editorW)
                                .fillMaxHeight(),
                        )
                    }
                }
                KineticStatusBar(
                    activePath = activePath,
                    agentBusy = busy,
                    gitState = gitRepoState,
                )
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(48.dp),
        )
        IdeCommandPalette(
            visible = commandPaletteVisible,
            query = commandPaletteQuery,
            onQueryChange = { commandPaletteQuery = it },
            commands = paletteCommands,
            onDismiss = {
                commandPaletteVisible = false
                commandPaletteQuery = ""
            },
        )
        AuditTimelinePanel(
            visible = auditTimelineVisible,
            entries = auditEntries,
            loading = auditLoading,
            onDismiss = { auditTimelineVisible = false },
            onClear = { agentVm.clearAuditEntries() },
            onRefresh = { agentVm.loadAuditEntries() },
        )
    }
}

@Composable
private fun SettingsDialog(
    currentTheme: KineticThemeMode,
    trustPolicyState: AgentTrustPolicyState,
    onDismiss: () -> Unit,
    onThemeSelect: (KineticThemeMode) -> Unit,
    onTrustModeSelect: (AgentToolRiskClass, AgentToolPolicyMode) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "Theme",
                    color = MaterialTheme.colorScheme.onSurface,
                )
                KineticThemeMode.entries.forEach { mode ->
                    SettingsRadioRow(
                        selected = currentTheme == mode,
                        label = mode.displayName,
                        onClick = { onThemeSelect(mode) },
                    )
                }

                Text(
                    "Agent trust",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    "Read-only tools always run automatically. File and destructive tools can auto-run, ask, or be denied. Shell commands stay ask-or-deny only.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TrustModeSection(
                    title = AgentToolRiskClass.FILE_MUTATION.displayName,
                    current = trustPolicyState.fileMutationMode,
                    onSelect = { onTrustModeSelect(AgentToolRiskClass.FILE_MUTATION, it) },
                )
                TrustModeSection(
                    title = AgentToolRiskClass.DESTRUCTIVE.displayName,
                    current = trustPolicyState.destructiveMode,
                    onSelect = { onTrustModeSelect(AgentToolRiskClass.DESTRUCTIVE, it) },
                )
                TrustModeSection(
                    title = AgentToolRiskClass.COMMAND.displayName,
                    current = trustPolicyState.commandMode,
                    availableModes = listOf(
                        AgentToolPolicyMode.ASK,
                        AgentToolPolicyMode.DENY,
                    ),
                    onSelect = { onTrustModeSelect(AgentToolRiskClass.COMMAND, it) },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun TrustModeSection(
    title: String,
    current: AgentToolPolicyMode,
    availableModes: List<AgentToolPolicyMode> = AgentToolPolicyMode.entries,
    onSelect: (AgentToolPolicyMode) -> Unit,
) {
    Text(
        title,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 12.dp),
    )
    availableModes.forEach { mode ->
        SettingsRadioRow(
            selected = current == mode,
            label = mode.displayName,
            onClick = { onSelect(mode) },
        )
    }
}

@Composable
private fun SettingsRadioRow(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        TextButton(onClick = onClick) {
            Text(label)
        }
    }
}

@Composable
private fun ApiKeysDialog(
    credentials: LlmCredentialState,
    onDismiss: () -> Unit,
    onSave: (LlmProvider, String) -> Unit,
) {
    var anthropicKey by remember(credentials.anthropicApiKey) {
        mutableStateOf(credentials.anthropicApiKey)
    }
    var geminiKey by remember(credentials.geminiApiKey) {
        mutableStateOf(credentials.geminiApiKey)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API keys") },
        text = {
            Column {
                Text(
                    "Keys are saved on this device and used immediately. Build-time local.properties keys still work as fallback.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = anthropicKey,
                    onValueChange = { anthropicKey = it },
                    label = { Text("Anthropic API key") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                )
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    label = { Text("Gemini API key") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(LlmProvider.ANTHROPIC, anthropicKey)
                    onSave(LlmProvider.GEMINI, geminiKey)
                    onDismiss()
                },
            ) {
                Text("Save")
            }
        },
    )
}
