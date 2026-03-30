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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

    val chatLines by agentVm.lines.collectAsState()
    val busy by agentVm.busy.collectAsState()
    val agentError by agentVm.error.collectAsState()

    val activePath = tabs.getOrNull(selectedTabIndex)?.path
    val editorText = tabs.getOrNull(selectedTabIndex)?.content ?: ""

    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                    onOpenWorkspace = { openFolder() },
                    onSelectFile = ideVm::openOrSelectFile,
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
                    onSelectTab = ideVm::selectTab,
                    onCloseTab = ideVm::closeTab,
                    onSave = ideVm::saveCurrentFile,
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
                            text = editorText,
                            onTextChange = ideVm::onEditorTextChange,
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
                        onSend = agentVm::sendUserMessage,
                        onClear = agentVm::clearConversation,
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
