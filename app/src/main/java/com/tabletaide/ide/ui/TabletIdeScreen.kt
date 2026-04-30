package com.tabletaide.ide.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    val filteredTree by ideVm.filteredTree.collectAsState()
    val explorerTreeFilterQuery by ideVm.explorerTreeFilterQuery.collectAsState()
    val tabs by ideVm.tabs.collectAsState()
    val selectedTabIndex by ideVm.selectedTabIndex.collectAsState()
    val railSection by ideVm.railSection.collectAsState()
    val status by ideVm.status.collectAsState()
    val undoRedoEpoch by ideVm.undoRedoEpoch.collectAsState()

    val canUndoNow = remember(undoRedoEpoch, tabs, selectedTabIndex) {
        ideVm.canUndo()
    }
    val canRedoNow = remember(undoRedoEpoch, tabs, selectedTabIndex) {
        ideVm.canRedo()
    }
    val themeMode by ideVm.themeMode.collectAsState()

    val chatLines by agentVm.lines.collectAsState()
    val busy by agentVm.busy.collectAsState()
    val agentError by agentVm.error.collectAsState()
    val provider by agentVm.provider.collectAsState()
    val credentials by agentVm.credentials.collectAsState()

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

    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var dirtyCloseTabIndex by remember { mutableStateOf<Int?>(null) }
    var commandPaletteVisible by remember { mutableStateOf(false) }
    var commandPaletteQuery by remember { mutableStateOf("") }
    var apiKeysDialogVisible by remember { mutableStateOf(false) }
    var themeDialogVisible by remember { mutableStateOf(false) }
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

    val openTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) { }
        ideVm.openWorkspaceRoot(uri)
    }

    fun openFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        openTreeLauncher.launch(intent)
    }

    val paletteCommands = remember(
        tabs.size,
        tabs.count { it.dirty },
        undoRedoEpoch,
        selectedTabIndex,
        busy,
        activePath,
    ) {
        listOf(
            PaletteCommand(
                title = "Open workspace",
                subtitle = "Pick a folder (SAF)",
                keywords = listOf("open", "folder", "saf", "project"),
                onInvoke = { openFolder() },
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
                subtitle = "Stub — Termux later",
                keywords = listOf("run", "execute", "debug", "play"),
                onInvoke = {
                    scope.launch {
                        snackbar.showSnackbar(
                            "Execute — connect Termux or a runner in a later phase",
                        )
                    }
                },
            ),
        )
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
    if (themeDialogVisible) {
        ThemeModeDialog(
            current = themeMode,
            onDismiss = { themeDialogVisible = false },
            onSelect = { mode ->
                ideVm.setThemeMode(mode)
                themeDialogVisible = false
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (commandPaletteVisible || apiKeysDialogVisible || themeDialogVisible) {
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
                onOpenWorkspace = { openFolder() },
                onSearch = { commandPaletteVisible = true },
                onExtensionsStub = {
                    scope.launch { snackbar.showSnackbar("Extensions — coming soon") }
                },
                onOpenSettings = { themeDialogVisible = true },
            )
            if (railSection != RailSection.Search && railSection != RailSection.Extensions) {
                FileTreePane(
                    rows = filteredTree,
                    treeFilterQuery = explorerTreeFilterQuery,
                    onTreeFilterQueryChange = ideVm::setExplorerTreeFilterQuery,
                    selectedPath = activePath,
                    favoritePaths = explorerFavoritesList,
                    recentPaths = explorerRecentsList,
                    hasWorkspaceRoot = ideVm.hasWorkspaceRoot(),
                    onOpenWorkspace = { openFolder() },
                    onSelectFile = ideVm::openOrSelectFile,
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
                    onSelectTab = ideVm::selectTab,
                    onCloseTab = ::requestCloseTab,
                    onSave = ideVm::saveCurrentFile,
                    onSaveAll = ideVm::saveAllDirtyTabs,
                    onUndo = ideVm::undo,
                    onRedo = ideVm::redo,
                    onExecute = {
                        scope.launch {
                            snackbar.showSnackbar("Execute — connect Termux or a runner in a later phase")
                        }
                    },
                    agentBusy = busy,
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
                            error = agentError,
                            currentProvider = provider,
                            credentials = credentials,
                            onSend = { msg ->
                                agentVm.sendUserMessage(msg, ideVm.buildAgentWorkspaceContext())
                            },
                            onClear = agentVm::clearConversation,
                            onProviderChange = agentVm::setProvider,
                            onOpenApiKeys = { apiKeysDialogVisible = true },
                            onRevertToolMutation = agentVm::revertToolMutation,
                            onApplyToolMutation = agentVm::applyToolMutation,
                            modifier = Modifier
                                .weight(1f - editorW)
                                .fillMaxHeight(),
                        )
                    }
                }
                KineticStatusBar(
                    activePath = activePath,
                    agentBusy = busy,
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
    }
}

@Composable
private fun ThemeModeDialog(
    current: KineticThemeMode,
    onDismiss: () -> Unit,
    onSelect: (KineticThemeMode) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme mode") },
        text = {
            Column {
                KineticThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = current == mode,
                            onClick = { onSelect(mode) },
                        )
                        TextButton(onClick = { onSelect(mode) }) {
                            Text(mode.displayName)
                        }
                    }
                }
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
