package com.tabletaide.ide.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch

@Composable
fun TabletIdeScreen(
    ideVm: IdeViewModel = hiltViewModel(),
    agentVm: AgentViewModel = hiltViewModel(),
) {
    val tree by ideVm.tree.collectAsState()
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

    val chatLines by agentVm.lines.collectAsState()
    val busy by agentVm.busy.collectAsState()
    val agentError by agentVm.error.collectAsState()
    val provider by agentVm.provider.collectAsState()

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(Modifier.fillMaxSize()) {
            KineticNavRail(
                section = railSection,
                onSection = ideVm::setRailSection,
                onOpenWorkspace = { openFolder() },
                onSearchStub = {
                    scope.launch { snackbar.showSnackbar("Search — coming soon") }
                },
                onExtensionsStub = {
                    scope.launch { snackbar.showSnackbar("Extensions — coming soon") }
                },
            )
            if (railSection != RailSection.Search && railSection != RailSection.Extensions) {
                FileTreePane(
                    rows = tree,
                    selectedPath = activePath,
                    hasWorkspaceRoot = ideVm.hasWorkspaceRoot(),
                    onOpenWorkspace = { openFolder() },
                    onSelectFile = ideVm::openOrSelectFile,
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
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        BreadcrumbBar(activePath)
                        CodeEditorPane(
                            value = editorField,
                            onValueChange = { tv ->
                                ideVm.onEditorValueChange(tv.text, tv.selection.start, tv.selection.end)
                            },
                            tabPath = activePath,
                            scrollInitialPx = activeTab?.scrollPx ?: 0,
                            onScrollPxCommitted = { path, px -> ideVm.reportEditorScroll(path, px) },
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
                    AgentChatPanel(
                        lines = chatLines,
                        busy = busy,
                        error = agentError,
                        currentProvider = provider,
                        onSend = { msg ->
                            agentVm.sendUserMessage(msg, ideVm.buildAgentWorkspaceContext())
                        },
                        onClear = agentVm::clearConversation,
                        onProviderChange = agentVm::setProvider,
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight(),
                    )
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
    }
}
