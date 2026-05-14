package com.tabletaide.ide.ui

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabletaide.ide.IdeConstants
import com.tabletaide.ide.agent.GitCommitMessageService
import com.tabletaide.ide.data.CloneTargetResolver
import com.tabletaide.ide.data.CommandRunnerUiState
import com.tabletaide.ide.data.EditorSessionStore
import com.tabletaide.ide.data.ExplorerPinsStore
import com.tabletaide.ide.data.GitCommitDialogState
import com.tabletaide.ide.data.GitCommitRequest
import com.tabletaide.ide.data.GitCommitService
import com.tabletaide.ide.data.GitAuthStore
import com.tabletaide.ide.data.GitCloneFailure
import com.tabletaide.ide.data.GitCloneRequest
import com.tabletaide.ide.data.GitCloneService
import com.tabletaide.ide.data.GitCloneSuccess
import com.tabletaide.ide.data.GitIdentity
import com.tabletaide.ide.data.GitIdentityStore
import com.tabletaide.ide.data.GitPushRequest
import com.tabletaide.ide.data.GitPushService
import com.tabletaide.ide.data.GitRepoUiState
import com.tabletaide.ide.data.GitRepositoryReady
import com.tabletaide.ide.data.GitRepositoryResolver
import com.tabletaide.ide.data.GitRemoteSpec
import com.tabletaide.ide.data.GitSavedAuthState
import com.tabletaide.ide.data.GitStatusService
import com.tabletaide.ide.data.GitStatusSnapshot
import com.tabletaide.ide.data.InAppCommandRunner
import com.tabletaide.ide.data.SupportedCloneTarget
import com.tabletaide.ide.data.PersistedEditorSnapshot
import com.tabletaide.ide.data.PersistedTab
import com.tabletaide.ide.data.RecentWorkspaceEntry
import com.tabletaide.ide.data.RecentWorkspacesStore
import com.tabletaide.ide.data.RunCommandDialogState
import com.tabletaide.ide.data.StarterProjectTemplate
import com.tabletaide.ide.data.TreeRow
import com.tabletaide.ide.data.WorkspaceMutationBus
import com.tabletaide.ide.data.WorkspaceRepository
import com.tabletaide.ide.ui.theme.KineticThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorTab(
    val path: String,
    val fileName: String,
    val content: String,
    val lastPersisted: String,
    /** Vertical scroll pixel offset for the outer editor scroller ([TRACE: DOCS/ROADMAP.md] Epic 1.1). */
    val scrollPx: Int = 0,
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
) {
    val dirty: Boolean get() = content != lastPersisted
}

enum class RailSection { Projects, Explorer, Search, Extensions }
enum class AppLaunchSurface { BOOTING, STARTUP_GATEWAY, IDE_SHELL }

private const val UndoDebounceMs = 450L
private const val AutosaveIntervalMs = 45_000L
private const val DraftPeriodicMs = 12_000L
private const val MaxUndoSteps = 80
private const val MaxUndoStepsLargeBuffer = 24

@HiltViewModel
class IdeViewModel @Inject constructor(
    private val workspace: WorkspaceRepository,
    mutationBus: WorkspaceMutationBus,
    private val sessionStore: EditorSessionStore,
    private val explorerPins: ExplorerPinsStore,
    private val recentWorkspacesStore: RecentWorkspacesStore,
    private val gitAuthStore: GitAuthStore,
    private val cloneTargetResolver: CloneTargetResolver,
    private val gitCloneService: GitCloneService,
    private val gitRepositoryResolver: GitRepositoryResolver,
    private val gitStatusService: GitStatusService,
    private val gitCommitService: GitCommitService,
    private val gitPushService: GitPushService,
    private val gitIdentityStore: GitIdentityStore,
    private val gitCommitMessageService: GitCommitMessageService,
    private val commandRunner: InAppCommandRunner,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val uiPrefs by lazy {
        appContext.getSharedPreferences("kinetic_ui_settings", Context.MODE_PRIVATE)
    }
    private val themeModeKey = "theme_mode"

    val explorerRecents: StateFlow<List<String>> = explorerPins.recents
    val explorerFavorites: StateFlow<List<String>> = explorerPins.favorites
    val recentWorkspaces: StateFlow<List<RecentWorkspaceEntry>> = recentWorkspacesStore.entries

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<KineticThemeMode> = _themeMode.asStateFlow()

    private val _cloneUiState = MutableStateFlow(GitCloneUiState())
    val cloneUiState: StateFlow<GitCloneUiState> = _cloneUiState.asStateFlow()

    private val _gitRepoState = MutableStateFlow(
        GitRepoUiState(message = "Open a git repository root to use commit and push."),
    )
    val gitRepoState: StateFlow<GitRepoUiState> = _gitRepoState.asStateFlow()

    private val _gitCommitDialogState = MutableStateFlow(GitCommitDialogState())
    val gitCommitDialogState: StateFlow<GitCommitDialogState> = _gitCommitDialogState.asStateFlow()

    private val _runCommandDialogState = MutableStateFlow(RunCommandDialogState())
    val runCommandDialogState: StateFlow<RunCommandDialogState> = _runCommandDialogState.asStateFlow()

    val commandRunnerState: StateFlow<CommandRunnerUiState> = commandRunner.uiState

    private val _appLaunchSurface = MutableStateFlow(AppLaunchSurface.BOOTING)
    val appLaunchSurface: StateFlow<AppLaunchSurface> = _appLaunchSurface.asStateFlow()

    private val _tabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val tabs: StateFlow<List<EditorTab>> = _tabs.asStateFlow()

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    private val _railSection = MutableStateFlow(RailSection.Explorer)
    val railSection: StateFlow<RailSection> = _railSection.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _tree = MutableStateFlow<List<TreeRow>>(emptyList())
    val tree: StateFlow<List<TreeRow>> = _tree.asStateFlow()

    private val _explorerTreeFilterQuery = MutableStateFlow("")
    val explorerTreeFilterQuery: StateFlow<String> = _explorerTreeFilterQuery.asStateFlow()

    private val _filteredTree = MutableStateFlow<List<TreeRow>>(emptyList())
    /** Explorer rows after `explorerTreeFilterQuery` ([TRACE: DOCS/ROADMAP.md] Epic 1.2 virtualization). */
    val filteredTree: StateFlow<List<TreeRow>> = _filteredTree.asStateFlow()

    /** Bumps when undo stacks change without tab.content changing (Compose refresh). */
    private val _undoRedoEpoch = MutableStateFlow(0L)
    val undoRedoEpoch: StateFlow<Long> = _undoRedoEpoch.asStateFlow()

    private val undoByPath = mutableMapOf<String, ArrayDeque<String>>()
    private val redoByPath = mutableMapOf<String, ArrayDeque<String>>()
    private val burstBaselineByPath = mutableMapOf<String, String>()
    private val burstJobs = mutableMapOf<String, Job>()
    private var gitRefreshJob: Job? = null
    private var gitRepository: GitRepositoryReady? = null
    private var gitSnapshot: GitStatusSnapshot? = null

    init {
        mutationBus.writes
            .onEach { path ->
                val idx = _tabs.value.indexOfFirst { it.path == path }
                if (idx >= 0 && !_tabs.value[idx].dirty) {
                    reloadTabAt(idx)
                }
                refreshTree()
            }
            .launchIn(viewModelScope)
        commandRunner.events
            .onEach { _status.value = it }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            while (isActive) {
                delay(AutosaveIntervalMs)
                autoSaveAllDirtyQuiet()
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(DraftPeriodicMs)
                persistDraftIfPossible()
            }
        }
        recentWorkspacesStore.pruneUnavailable(validPersistedWorkspaceUris())
        val restored = tryRestorePersistedSession()
        _appLaunchSurface.value = if (restored) {
            AppLaunchSurface.IDE_SHELL
        } else {
            AppLaunchSurface.STARTUP_GATEWAY
        }
        explorerPins.bindWorkspace(workspace.rootTreeUriOrNull())
        commandRunner.bindWorkspace(workspace.rootTreeUriOrNull())
    }

    fun hasWorkspaceRoot(): Boolean = workspace.hasRoot()

    /** Open tabs + active file + optional selection snippet for the LLM system prompt ([TRACE: DOCS/ROADMAP.md] Epic 2.3). */
    fun buildAgentWorkspaceContext(): String {
        if (!hasWorkspaceRoot()) return ""
        val tabs = _tabs.value
        if (tabs.isEmpty()) {
            return "Workspace is open; no files are open in the editor."
        }
        val paths = tabs.map { it.path }
        val active = tabs.getOrNull(_selectedTabIndex.value) ?: return ""
        val openList = paths.joinToString(", ")
        return buildString {
            append("Open editor tabs (paths relative to workspace): ")
            append(openList)
            append(". Active file: ")
            append(active.path)
            append(".")
            val selStart = active.selectionStart.coerceIn(0, active.content.length)
            val selEnd = active.selectionEnd.coerceIn(0, active.content.length)
            val lo = minOf(selStart, selEnd)
            val hi = maxOf(selStart, selEnd)
            if (hi > lo) {
                val snippet = active.content.substring(lo, hi).take(2_000)
                if (snippet.isNotEmpty()) {
                    append(" Non-empty selection in active file:\n```\n")
                    append(snippet)
                    append("\n```")
                }
            }
        }
    }

    fun setRailSection(section: RailSection) {
        _railSection.value = section
    }

    fun setThemeMode(mode: KineticThemeMode) {
        if (_themeMode.value == mode) return
        uiPrefs.edit().putString(themeModeKey, mode.id).apply()
        _themeMode.value = mode
        _status.value = "Theme: ${mode.displayName}"
    }

    fun setExplorerTreeFilterQuery(query: String) {
        _explorerTreeFilterQuery.value = query
        recomputeFilteredExplorerRows()
    }

    private fun recomputeFilteredExplorerRows() {
        _filteredTree.value = filterTreeRows(_tree.value, _explorerTreeFilterQuery.value)
    }

    fun canUndo(): Boolean {
        val tab = _tabs.value.getOrNull(_selectedTabIndex.value) ?: return false
        val path = tab.path
        if (undoByPath[path]?.isNotEmpty() == true) return true
        val baseline = burstBaselineByPath[path] ?: return false
        return baseline != tab.content
    }

    fun canRedo(): Boolean {
        val path = _tabs.value.getOrNull(_selectedTabIndex.value)?.path ?: return false
        return redoByPath[path]?.isNotEmpty() == true
    }

    fun undo() {
        val idx = _selectedTabIndex.value
        val tab = _tabs.value.getOrNull(idx) ?: return
        val path = tab.path
        commitPendingBurstImmediately(path)
        val stack = undoByPath[path] ?: return
        val prev = stack.removeLastOrNull() ?: return
        redoByPath.getOrPut(path) { ArrayDeque() }.addLast(tab.content)
        _tabs.update { list ->
            list.mapIndexed { i, t ->
                if (i == idx) {
                    val nl = prev.length
                    t.copy(
                        content = prev,
                        selectionStart = t.selectionStart.coerceIn(0, nl),
                        selectionEnd = t.selectionEnd.coerceIn(0, nl),
                    )
                } else {
                    t
                }
            }
        }
        bumpUndoRedo()
    }

    fun redo() {
        val idx = _selectedTabIndex.value
        val tab = _tabs.value.getOrNull(idx) ?: return
        val path = tab.path
        commitPendingBurstImmediately(path)
        val rstack = redoByPath[path] ?: return
        val next = rstack.removeLastOrNull() ?: return
        undoByPath.getOrPut(path) { ArrayDeque() }.addLast(tab.content)
        _tabs.update { list ->
            list.mapIndexed { i, t ->
                if (i == idx) {
                    val nl = next.length
                    t.copy(
                        content = next,
                        selectionStart = t.selectionStart.coerceIn(0, nl),
                        selectionEnd = t.selectionEnd.coerceIn(0, nl),
                    )
                } else {
                    t
                }
            }
        }
        bumpUndoRedo()
    }

    fun openWorkspaceRoot(treeUri: Uri) {
        burstJobs.values.forEach { it.cancel() }
        burstJobs.clear()
        burstBaselineByPath.clear()
        undoByPath.clear()
        redoByPath.clear()
        workspace.setRootFromUri(treeUri)
        explorerPins.bindWorkspace(treeUri)
        commandRunner.bindWorkspace(treeUri)
        recentWorkspacesStore.recordWorkspace(treeUri, workspace.displayNameForTreeUri(treeUri))
        refreshTree()
        _tabs.value = emptyList()
        _selectedTabIndex.value = 0
        _status.value = "Workspace opened"
        _railSection.value = RailSection.Explorer
        _appLaunchSurface.value = AppLaunchSurface.IDE_SHELL
        _cloneUiState.value = GitCloneUiState()
        _gitCommitDialogState.value = GitCommitDialogState()
        bumpUndoRedo()
        persistDraftIfPossible()
    }

    /** Best-effort session restore ([TRACE: DOCS/ROADMAP.md] Epic 1.1 crash-safe drafts). */
    fun tryRestorePersistedSession(): Boolean {
        val snapshot = sessionStore.loadSnapshotOrNull() ?: return false
        if (!_hasPersistPermission(snapshot.workspaceUri)) return false
        val uri = Uri.parse(snapshot.workspaceUri)
        workspace.setRootFromUri(uri)
        recentWorkspacesStore.recordWorkspace(uri, workspace.displayNameForTreeUri(uri))
        refreshTree()
        val restored = snapshot.tabs.map { row ->
            EditorTab(
                path = row.path,
                fileName = row.fileName,
                content = row.content,
                lastPersisted = row.lastPersisted,
                scrollPx = row.scrollPx,
                selectionStart = row.selectionStart,
                selectionEnd = row.selectionEnd,
            )
        }
        _tabs.value = restored
        _selectedTabIndex.value = if (restored.isEmpty()) {
            0
        } else {
            snapshot.selectedIndex.coerceIn(0, restored.lastIndex)
        }
        val keep = restored.mapTo(mutableSetOf()) { it.path }
        undoByPath.keys.filter { it !in keep }.forEach { key ->
            undoByPath.remove(key)
            redoByPath.remove(key)
        }
        burstBaselineByPath.clear()
        burstJobs.values.forEach { it.cancel() }
        burstJobs.clear()
        bumpUndoRedo()
        if (restored.isNotEmpty()) {
            _status.value = "Restored previous session (${restored.size} buffers)"
        }
        return true
    }

    fun openRecentWorkspace(uriString: String) {
        if (!_hasPersistPermission(uriString)) {
            recentWorkspacesStore.removeWorkspace(uriString)
            _status.value = "Workspace permission expired. Pick the folder again."
            return
        }
        openWorkspaceRoot(Uri.parse(uriString))
    }

    fun createStarterProject(
        parentTreeUri: Uri,
        projectName: String,
        template: StarterProjectTemplate,
    ) {
        viewModelScope.launch {
            workspace.createStarterProject(parentTreeUri, projectName, template).fold(
                onSuccess = { projectTreeUri ->
                    openWorkspaceRoot(projectTreeUri)
                    _status.value = "Created project ${projectName.trim()}"
                },
                onFailure = { _status.value = it.message },
            )
        }
    }

    fun peekSavedGitAuth(repoUrl: String): GitSavedAuthState? {
        val remote = GitRemoteSpec.parseHttps(repoUrl) ?: return null
        return gitAuthStore.peek(remote.host)
    }

    fun hasAllFilesAccess(): Boolean = Environment.isExternalStorageManager()

    fun clearCloneFeedback() {
        _cloneUiState.value = GitCloneUiState()
    }

    fun showCommitDialog(autoGenerateMessage: Boolean = false) {
        viewModelScope.launch {
            val state = resolveGitSnapshot(refreshUi = true) ?: run {
                _status.value = _gitRepoState.value.message
                return@launch
            }
            val (_, snapshot) = state
            val identity = preferredIdentity(snapshot)
            _gitCommitDialogState.value = GitCommitDialogState(
                visible = true,
                draftMessage = _gitCommitDialogState.value.draftMessage,
                authorName = identity.name,
                authorEmail = identity.email,
                stageUntracked = true,
            )
            if (autoGenerateMessage && !snapshot.clean) {
                generateCommitMessage()
            }
        }
    }

    fun hideCommitDialog() {
        _gitCommitDialogState.value = GitCommitDialogState()
    }

    fun showRunCommandDialog(prefill: String = commandRunnerState.value.lastCommand) {
        val runnerState = commandRunnerState.value
        if (!runnerState.available) {
            _status.value = runnerState.availabilityMessage ?: "Command execution is unavailable."
            return
        }
        _runCommandDialogState.value = RunCommandDialogState(
            visible = true,
            command = prefill,
        )
    }

    fun hideRunCommandDialog() {
        _runCommandDialogState.value = RunCommandDialogState()
    }

    fun updateRunCommandText(command: String) {
        _runCommandDialogState.update { it.copy(command = command, errorMessage = null) }
    }

    fun executeRunCommandFromDialog() {
        val command = _runCommandDialogState.value.command.trim()
        commandRunner.run(command).fold(
            onSuccess = {
                _runCommandDialogState.value = RunCommandDialogState()
            },
            onFailure = {
                _runCommandDialogState.update { state ->
                    state.copy(errorMessage = it.message ?: "Could not start the command.")
                }
            },
        )
    }

    fun rerunLastCommand() {
        commandRunner.rerunLastCommand().onFailure {
            _status.value = it.message ?: "No previous command is available to rerun."
        }
    }

    fun cancelRunningCommand() {
        commandRunner.cancel()
    }

    fun clearRunnerOutput() {
        commandRunner.clearOutput()
    }

    fun updateCommitDraftMessage(message: String) {
        _gitCommitDialogState.update { it.copy(draftMessage = message) }
    }

    fun updateCommitAuthorName(name: String) {
        _gitCommitDialogState.update { it.copy(authorName = name) }
    }

    fun updateCommitAuthorEmail(email: String) {
        _gitCommitDialogState.update { it.copy(authorEmail = email) }
    }

    fun updateCommitStageUntracked(enabled: Boolean) {
        _gitCommitDialogState.update { it.copy(stageUntracked = enabled) }
    }

    fun generateCommitMessage() {
        if (_gitCommitDialogState.value.generatingMessage || _gitCommitDialogState.value.busy) return
        viewModelScope.launch {
            val currentDialog = _gitCommitDialogState.value
            val state = resolveGitSnapshot(refreshUi = true) ?: run {
                _gitCommitDialogState.update {
                    it.copy(errorMessage = _gitRepoState.value.message)
                }
                return@launch
            }
            val (repository, snapshot) = state
            if (snapshot.clean) {
                _gitCommitDialogState.update {
                    it.copy(errorMessage = "No repo changes available for a commit message.")
                }
                return@launch
            }
            _gitCommitDialogState.update {
                it.copy(
                    visible = true,
                    draftMessage = currentDialog.draftMessage,
                    generatingMessage = true,
                    busy = false,
                    errorMessage = null,
                    progressMessage = "Generating commit message…",
                )
            }
            val gitContext = gitStatusService.buildAiContext(repository, snapshot)
            val prompt = gitContext.getOrElse {
                _gitCommitDialogState.update { state ->
                    state.copy(
                        generatingMessage = false,
                        progressMessage = null,
                        errorMessage = it.message ?: "Could not build git context for AI message generation.",
                    )
                }
                return@launch
            }
            val generated = gitCommitMessageService.generateCommitMessage(prompt)
            generated.fold(
                onSuccess = { message ->
                    _gitCommitDialogState.update { state ->
                        state.copy(
                            draftMessage = message,
                            generatingMessage = false,
                            progressMessage = null,
                            errorMessage = null,
                        )
                    }
                },
                onFailure = {
                    _gitCommitDialogState.update { state ->
                        state.copy(
                            generatingMessage = false,
                            progressMessage = null,
                            errorMessage = it.message ?: "Commit message generation failed.",
                        )
                    }
                },
            )
        }
    }

    fun commitChanges(pushAfterCommit: Boolean) {
        if (_gitCommitDialogState.value.busy || _gitCommitDialogState.value.generatingMessage) return
        viewModelScope.launch {
            val currentDialog = _gitCommitDialogState.value
            val state = resolveGitSnapshot(refreshUi = true) ?: run {
                _gitCommitDialogState.update {
                    it.copy(errorMessage = _gitRepoState.value.message)
                }
                return@launch
            }
            val (repository, snapshot) = state
            if (snapshot.clean) {
                if (pushAfterCommit && snapshot.aheadCount > 0) {
                    _gitCommitDialogState.update {
                        it.copy(
                            busy = true,
                            progressMessage = "Pushing tracked branch…",
                            errorMessage = null,
                        )
                    }
                    pushTrackedBranch(repository, snapshot).fold(
                        onSuccess = {
                            resolveGitSnapshot(refreshUi = true)
                            _gitCommitDialogState.value = GitCommitDialogState()
                            _status.value = it.message
                        },
                        onFailure = {
                            resolveGitSnapshot(refreshUi = true)
                            _gitCommitDialogState.update { dialog ->
                                dialog.copy(
                                    busy = false,
                                    progressMessage = null,
                                    errorMessage = it.message ?: "Push failed.",
                                )
                            }
                        },
                    )
                    return@launch
                }
                _gitCommitDialogState.update {
                    it.copy(errorMessage = "No changes available to commit.")
                }
                return@launch
            }
            if (pushAfterCommit && !snapshot.hasTrackedUpstream) {
                _gitCommitDialogState.update {
                    it.copy(errorMessage = "Current branch has no tracked upstream yet.")
                }
                return@launch
            }
            val author = GitIdentity(
                name = currentDialog.authorName,
                email = currentDialog.authorEmail,
            )
            _gitCommitDialogState.update {
                it.copy(
                    busy = true,
                    progressMessage = if (pushAfterCommit) {
                        "Committing and pushing…"
                    } else {
                        "Committing…"
                    },
                    errorMessage = null,
                )
            }
            val commitResult = gitCommitService.commit(
                request = GitCommitRequest(
                    repository = repository,
                    message = currentDialog.draftMessage,
                    author = author,
                    stageUntracked = currentDialog.stageUntracked,
                ),
                onProgress = { progress ->
                    _gitCommitDialogState.update { dialog ->
                        dialog.copy(
                            busy = true,
                            progressMessage = progress,
                            errorMessage = null,
                        )
                    }
                },
            )
            val commit = commitResult.getOrElse {
                _gitCommitDialogState.update { dialog ->
                    dialog.copy(
                        busy = false,
                        progressMessage = null,
                        errorMessage = it.message ?: "Commit failed.",
                    )
                }
                return@launch
            }
            if (!pushAfterCommit) {
                gitIdentityStore.save(author)
                resolveGitSnapshot(refreshUi = true)
                _gitCommitDialogState.value = GitCommitDialogState()
                _status.value = "Committed ${commit.shortCommitId} on ${commit.branchName}"
                return@launch
            }
            val upstreamBranch = snapshot.upstreamBranch
            val remoteName = snapshot.upstreamRemoteName
            val remoteUrl = snapshot.upstreamRemoteUrl
            if (upstreamBranch.isNullOrBlank() || remoteName.isNullOrBlank() || remoteUrl.isNullOrBlank()) {
                resolveGitSnapshot(refreshUi = true)
                _gitCommitDialogState.update { dialog ->
                    dialog.copy(
                        busy = false,
                        progressMessage = null,
                        errorMessage = "Commit succeeded, but the current branch has no tracked upstream.",
                    )
                }
                _status.value = "Committed ${commit.shortCommitId} locally. Push was skipped."
                return@launch
            }
            pushTrackedBranch(repository, snapshot).fold(
                onSuccess = {
                    gitIdentityStore.save(author)
                    resolveGitSnapshot(refreshUi = true)
                    _gitCommitDialogState.value = GitCommitDialogState()
                    _status.value = "Committed ${commit.shortCommitId} and pushed ${snapshot.branchName}"
                },
                onFailure = {
                    resolveGitSnapshot(refreshUi = true)
                    _gitCommitDialogState.update { dialog ->
                        dialog.copy(
                            busy = false,
                            progressMessage = null,
                            errorMessage = (it.message ?: "Push failed.") +
                                " Commit ${commit.shortCommitId} was created locally.",
                        )
                    }
                    _status.value = "Commit ${commit.shortCommitId} created locally, but push failed."
                },
            )
        }
    }

    fun clearSavedGitAuth(repoUrl: String) {
        val remote = GitRemoteSpec.parseHttps(repoUrl) ?: return
        gitAuthStore.clear(remote.host)
        _cloneUiState.value = GitCloneUiState()
    }

    fun cloneRepository(
        parentTreeUri: Uri,
        repoUrl: String,
        usernameInput: String,
        tokenInput: String,
        useSavedToken: Boolean,
        saveToken: Boolean,
    ) {
        viewModelScope.launch {
            val remote = GitRemoteSpec.parseHttps(repoUrl)
            if (remote == null) {
                _cloneUiState.value = GitCloneUiState(
                    errorMessage = "Enter a valid HTTPS repository URL.",
                )
                return@launch
            }
            val savedAuth = gitAuthStore.load(remote.host)
            val username = usernameInput.trim().ifEmpty {
                savedAuth?.username ?: com.tabletaide.ide.data.defaultGitUsernameForHost(remote.host)
            }
            val token = when {
                useSavedToken -> savedAuth?.token.orEmpty()
                else -> tokenInput.trim()
            }
            if (token.isBlank()) {
                _cloneUiState.value = GitCloneUiState(
                    errorMessage = "A personal access token is required for HTTPS clone.",
                )
                return@launch
            }
            when (val target = cloneTargetResolver.resolve(parentTreeUri, remote)) {
                is SupportedCloneTarget -> {
                    _cloneUiState.value = GitCloneUiState(
                        busy = true,
                        progressMessage = "Preparing clone…",
                    )
                    when (
                        val result = gitCloneService.clone(
                            request = GitCloneRequest(
                                remote = remote,
                                target = target,
                                username = username,
                                token = token,
                                saveAuth = saveToken || useSavedToken,
                            ),
                            onProgress = { message ->
                                _cloneUiState.value = GitCloneUiState(
                                    busy = true,
                                    progressMessage = message,
                                )
                            },
                        )
                    ) {
                        is GitCloneSuccess -> {
                            val repoTreeUri =
                                workspace.childTreeUri(parentTreeUri, result.remote.repoName)
                            if (repoTreeUri == null) {
                                _cloneUiState.value = GitCloneUiState(
                                    errorMessage = "Clone finished but Kinetic could not reopen the cloned repo. Open it manually from the chosen folder.",
                                )
                                return@launch
                            }
                            openWorkspaceRoot(repoTreeUri)
                            _status.value = "Cloned ${result.remote.repoName}"
                        }
                        is GitCloneFailure -> {
                            _cloneUiState.value = GitCloneUiState(
                                errorMessage = result.error.userMessage,
                            )
                        }
                    }
                }
                else -> {
                    val message = (target as? com.tabletaide.ide.data.UnsupportedCloneTarget)
                        ?.userMessage
                        ?: "Clone target is not supported."
                    _cloneUiState.value = GitCloneUiState(errorMessage = message)
                }
            }
        }
    }

    fun persistDraftIfPossible() {
        val uri = workspace.rootTreeUriOrNull()?.toString() ?: return
        sessionStore.saveSnapshot(
            PersistedEditorSnapshot(
                workspaceUri = uri,
                selectedIndex = _selectedTabIndex.value.coerceAtLeast(0),
                tabs = _tabs.value.map {
                    PersistedTab(
                        path = it.path,
                        fileName = it.fileName,
                        content = it.content,
                        lastPersisted = it.lastPersisted,
                        scrollPx = it.scrollPx,
                        selectionStart = it.selectionStart,
                        selectionEnd = it.selectionEnd,
                    )
                },
            ),
        )
    }

    fun refreshTree() {
        _tree.value = workspace.listTreeRows()
        recomputeFilteredExplorerRows()
        refreshGitState()
    }

    private fun refreshGitState() {
        gitRefreshJob?.cancel()
        gitRefreshJob = viewModelScope.launch {
            resolveGitSnapshot(refreshUi = true)
        }
    }

    private suspend fun resolveGitSnapshot(
        refreshUi: Boolean,
    ): Pair<GitRepositoryReady, GitStatusSnapshot>? {
        return when (val resolution = gitRepositoryResolver.resolveWorkspace(workspace.rootTreeUriOrNull())) {
            is GitRepositoryReady -> {
                val snapshot = gitStatusService.loadStatus(resolution).getOrElse {
                    gitRepository = null
                    gitSnapshot = null
                    if (refreshUi) {
                        _gitRepoState.value = GitRepoUiState(
                            message = it.message ?: "Git status is unavailable for this workspace.",
                        )
                    }
                    return null
                }
                gitRepository = resolution
                gitSnapshot = snapshot
                if (refreshUi) {
                    _gitRepoState.value = GitRepoUiState.fromSnapshot(snapshot)
                }
                resolution to snapshot
            }
            else -> {
                gitRepository = null
                gitSnapshot = null
                if (refreshUi) {
                    val message = (resolution as? com.tabletaide.ide.data.GitRepositoryUnavailable)
                        ?.userMessage
                        ?: "Open a git repository root to use commit and push."
                    _gitRepoState.value = GitRepoUiState(message = message)
                }
                null
            }
        }
    }

    private fun preferredIdentity(snapshot: GitStatusSnapshot): GitIdentity {
        val stored = gitIdentityStore.load()
        return GitIdentity(
            name = snapshot.identity.name.ifBlank { stored.name },
            email = snapshot.identity.email.ifBlank { stored.email },
        )
    }

    private suspend fun pushTrackedBranch(
        repository: GitRepositoryReady,
        snapshot: GitStatusSnapshot,
    ) = gitPushService.push(
        request = GitPushRequest(
            repository = repository,
            branchName = snapshot.branchName,
            upstreamBranch = snapshot.upstreamBranch.orEmpty(),
            remoteName = snapshot.upstreamRemoteName.orEmpty(),
            remoteUrl = snapshot.upstreamRemoteUrl.orEmpty(),
        ),
        onProgress = { progress ->
            _gitCommitDialogState.update { dialog ->
                dialog.copy(
                    busy = true,
                    progressMessage = progress,
                    errorMessage = null,
                )
            }
        },
    )

    private suspend fun openOrSelectRelativePath(path: String, displayNameFallback: String) {
        val existing = _tabs.value.indexOfFirst { it.path == path }
        if (existing >= 0) {
            _selectedTabIndex.value = existing
            return
        }
        workspace.readText(path).fold(
            onSuccess = { text ->
                undoByPath.remove(path)
                redoByPath.remove(path)
                burstJobs.remove(path)?.cancel()
                burstBaselineByPath.remove(path)
                val dn = displayNameFallback.ifBlank { path.substringAfterLast('/') }
                val editorTab = EditorTab(
                    path = path,
                    fileName = dn,
                    content = text,
                    lastPersisted = text,
                )
                _tabs.update { it + editorTab }
                _selectedTabIndex.value = _tabs.value.lastIndex
                _status.value = null
                explorerPins.recordFileOpened(path)
                bumpUndoRedo()
                persistDraftIfPossible()
            },
            onFailure = {
                _status.value = it.message
            },
        )
    }

    fun openOrSelectFile(row: TreeRow) {
        if (row.isDirectory) return
        val existing = _tabs.value.indexOfFirst { it.path == row.path }
        if (existing >= 0) {
            selectTab(existing)
            explorerPins.recordFileOpened(row.path)
            return
        }
        viewModelScope.launch {
            openOrSelectRelativePath(row.path, row.displayName)
        }
    }

    /** Open a workspace-relative file path from recents/favorites ([TRACE: DOCS/ROADMAP.md] Epic 1.2). */
    fun openExplorerPinnedPath(path: String) {
        viewModelScope.launch {
            val name = path.substringAfterLast('/').ifBlank { path }
            openOrSelectRelativePath(path.trim('/'), name)
        }
    }

    fun toggleExplorerFavorite(path: String) {
        explorerPins.toggleFavorite(path)
    }

    fun toggleFavoriteActiveTab() {
        val path = _tabs.value.getOrNull(_selectedTabIndex.value)?.path ?: return
        explorerPins.toggleFavorite(path)
    }

    fun selectTab(index: Int) {
        if (_tabs.value.isEmpty()) return
        val clamped = index.coerceIn(0, _tabs.value.lastIndex)
        val oldIdx = _selectedTabIndex.value
        if (oldIdx == clamped) return
        _tabs.value.getOrNull(oldIdx)?.let { oldTab ->
            commitPendingBurstImmediately(oldTab.path)
            viewModelScope.launch {
                autosaveTabIfDirty(oldIdx)
                persistDraftIfPossible()
            }
        }
        _selectedTabIndex.value = clamped
    }

    /** Save then close ([TRACE: DOCS/ROADMAP.md] dirty tab guard). */
    fun saveDirtyTabThenClose(index: Int, onComplete: () -> Unit = {}) {
        val tab = _tabs.value.getOrNull(index) ?: return onComplete()
        viewModelScope.launch {
            commitPendingBurstImmediately(tab.path)
            workspace.writeText(tab.path, tab.content).fold(
                onSuccess = {
                    _tabs.update { tabs ->
                        tabs.mapIndexed { i, t ->
                            if (i == index) t.copy(lastPersisted = t.content) else t
                        }
                    }
                    redoByPath[tab.path]?.clear()
                    bumpUndoRedo()
                    refreshTree()
                    closeTabUnchecked(index)
                    persistDraftIfPossible()
                    _status.value = "Saved ${tab.fileName}"
                    onComplete()
                },
                onFailure = {
                    _status.value = it.message
                    onComplete()
                },
            )
        }
    }

    fun closeTabUnchecked(index: Int) {
        val list = _tabs.value.toMutableList()
        if (index !in list.indices) return
        val removed = list.removeAt(index)
        burstJobs.remove(removed.path)?.cancel()
        burstBaselineByPath.remove(removed.path)
        undoByPath.remove(removed.path)
        redoByPath.remove(removed.path)
        _tabs.value = list
        when {
            list.isEmpty() -> _selectedTabIndex.value = 0
            _selectedTabIndex.value >= list.size -> _selectedTabIndex.value = list.lastIndex.coerceAtLeast(0)
            index < _selectedTabIndex.value -> _selectedTabIndex.update { (it - 1).coerceAtLeast(0) }
        }
        bumpUndoRedo()
        viewModelScope.launch {
            persistDraftIfPossible()
        }
    }

    fun reportEditorScroll(path: String?, scrollPx: Int) {
        if (path.isNullOrBlank()) return
        val idx = _tabs.value.indexOfFirst { it.path == path }
        if (idx < 0) return
        if (_tabs.value[idx].scrollPx == scrollPx) return
        _tabs.update { tabs ->
            tabs.mapIndexed { i, t ->
                if (i == idx) t.copy(scrollPx = scrollPx) else t
            }
        }
    }

    fun onEditorValueChange(rawText: String, selectionStart: Int, selectionEnd: Int) {
        val idx = _selectedTabIndex.value
        if (idx !in _tabs.value.indices) return
        val tab = _tabs.value[idx]
        val len = rawText.length
        val ss = selectionStart.coerceIn(0, len)
        val ee = selectionEnd.coerceIn(0, len)
        if (tab.content != rawText) {
            scheduleBurstCommit(tab.path, tab.content)
            _tabs.update { tabs ->
                tabs.mapIndexed { i, t ->
                    if (i == idx) {
                        t.copy(content = rawText, selectionStart = ss, selectionEnd = ee)
                    } else {
                        t
                    }
                }
            }
            return
        }
        if (tab.selectionStart != ss || tab.selectionEnd != ee) {
            _tabs.update { tabs ->
                tabs.mapIndexed { i, t ->
                    if (i == idx) t.copy(selectionStart = ss, selectionEnd = ee) else t
                }
            }
        }
    }

    fun explorerCreateEmptyFile(parentDirRelativeOrEmpty: String, leafName: String) {
        if (!workspace.hasRoot()) return
        viewModelScope.launch {
            workspace.createEmptyFile(parentDirRelativeOrEmpty.trim('/'), leafName).fold(
                onSuccess = { path ->
                    refreshTree()
                    openOrSelectRelativePath(path, leafName.trim())
                    persistDraftIfPossible()
                    _status.value = "Created $leafName"
                },
                onFailure = { _status.value = it.message },
            )
        }
    }

    fun explorerCreateFolder(relativeFolderPath: String) {
        if (!workspace.hasRoot()) return
        viewModelScope.launch {
            workspace.createDirectory(relativeFolderPath.trim('/')).fold(
                onSuccess = {
                    refreshTree()
                    persistDraftIfPossible()
                    _status.value = "Created folder"
                },
                onFailure = { _status.value = it.message },
            )
        }
    }

    fun explorerRename(row: TreeRow, newLeafName: String) {
        if (!workspace.hasRoot()) return
        viewModelScope.launch {
            workspace.renameLeaf(row.path, newLeafName.trim()).fold(
                onSuccess = { newRel ->
                    val nm = newRel.substringAfterLast('/')
                    val oldPath = row.path
                    remapEditorStacks(oldPath, newRel)
                    _tabs.update { tabs ->
                        tabs.map { t ->
                            if (t.path != oldPath) t
                            else t.copy(path = newRel, fileName = nm)
                        }
                    }
                    refreshTree()
                    persistDraftIfPossible()
                    explorerPins.renameTrackedPath(oldPath, newRel)
                    _status.value = "Renamed to $nm"
                },
                onFailure = { _status.value = it.message },
            )
        }
    }

    fun explorerDuplicateFile(row: TreeRow) {
        if (!workspace.hasRoot() || row.isDirectory) return
        viewModelScope.launch {
            workspace.duplicateFile(row.path).fold(
                onSuccess = { path ->
                    refreshTree()
                    openOrSelectRelativePath(path, path.substringAfterLast('/'))
                    _status.value = "Duplicated"
                },
                onFailure = { _status.value = it.message },
            )
        }
    }

    fun explorerDelete(row: TreeRow) {
        if (!workspace.hasRoot()) return
        viewModelScope.launch {
            workspace.deleteNode(row.path).fold(
                onSuccess = {
                    val ix = _tabs.value.indexOfFirst { it.path == row.path }
                    if (ix >= 0) {
                        closeTabUnchecked(ix)
                    }
                    refreshTree()
                    persistDraftIfPossible()
                    explorerPins.removeTrackedPath(row.path, row.isDirectory)
                    _status.value = "Deleted ${row.displayName}"
                },
                onFailure = { _status.value = it.message },
            )
        }
    }

    fun saveCurrentFile() {
        val idx = _selectedTabIndex.value
        saveTabAt(idx)
    }

    fun saveAllDirtyTabs() {
        viewModelScope.launch {
            val indices = _tabs.value.indices.filter { _tabs.value[it].dirty }
            if (indices.isEmpty()) {
                _status.value = "Nothing to save"
                return@launch
            }
            var failures = 0
            indices.forEach { i ->
                val tab = _tabs.value[i]
                commitPendingBurstImmediately(tab.path)
                workspace.writeText(tab.path, tab.content).fold(
                    onSuccess = {
                        _tabs.update { list ->
                            list.mapIndexed { j, t ->
                                if (j == i) t.copy(lastPersisted = t.content) else t
                            }
                        }
                        redoByPath[tab.path]?.clear()
                        bumpUndoRedo()
                    },
                    onFailure = {
                        failures++
                        _status.value = it.message
                    },
                )
            }
            if (failures == 0) {
                _status.value = "Saved ${indices.size} file(s)"
                refreshTree()
            }
            persistDraftIfPossible()
        }
    }

    private fun bumpUndoRedo() {
        _undoRedoEpoch.update { it + 1 }
    }

    private fun scheduleBurstCommit(path: String, contentBeforeStroke: String) {
        if (!burstBaselineByPath.containsKey(path)) {
            burstBaselineByPath[path] = contentBeforeStroke
        }
        burstJobs[path]?.cancel()
        burstJobs[path] = viewModelScope.launch {
            delay(UndoDebounceMs)
            commitPendingBurstImmediately(path)
        }
    }

    private fun commitPendingBurstImmediately(path: String) {
        burstJobs.remove(path)?.cancel()
        val baseline = burstBaselineByPath.remove(path) ?: return
        val tab = _tabs.value.firstOrNull { it.path == path } ?: return
        if (baseline == tab.content) return
        val stack = undoByPath.getOrPut(path) { ArrayDeque() }
        stack.addLast(baseline)
        val cap = undoCapForPath(path)
        while (stack.size > cap) stack.removeFirst()
        redoByPath[path]?.clear()
        bumpUndoRedo()
    }

    private fun undoCapForPath(path: String): Int {
        val tab = _tabs.value.firstOrNull { it.path == path } ?: return MaxUndoSteps
        val lines = tab.content.count { it == '\n' } + 1
        return if (tab.content.length >= IdeConstants.LARGE_FILE_CHAR_THRESHOLD ||
            lines >= IdeConstants.LARGE_FILE_LINE_SOFT_THRESHOLD
        ) {
            MaxUndoStepsLargeBuffer
        } else {
            MaxUndoSteps
        }
    }

    private fun remapEditorStacks(oldPath: String, newPath: String) {
        if (oldPath == newPath) return
        burstJobs.remove(oldPath)?.cancel()
        burstBaselineByPath.remove(oldPath)?.let { burstBaselineByPath[newPath] = it }
        undoByPath[newPath] = undoByPath.remove(oldPath) ?: ArrayDeque()
        redoByPath[newPath] = redoByPath.remove(oldPath) ?: ArrayDeque()
        bumpUndoRedo()
    }

    private suspend fun autosaveTabIfDirty(tabIndex: Int) {
        val tab = _tabs.value.getOrNull(tabIndex) ?: return
        if (!tab.dirty) return
        commitPendingBurstImmediately(tab.path)
        workspace.writeText(tab.path, tab.content).fold(
            onSuccess = {
                val path = tab.path
                _tabs.update { list ->
                    list.map { t ->
                        if (t.path == path) t.copy(lastPersisted = t.content) else t
                    }
                }
                redoByPath[path]?.clear()
                bumpUndoRedo()
            },
            onFailure = {
                _status.value = "Auto-save failed for ${tab.fileName}: ${it.message ?: "unknown error"}"
            },
        )
    }

    private fun autoSaveAllDirtyQuiet() {
        if (!workspace.hasRoot()) return
        viewModelScope.launch {
            val dirtyIndices = _tabs.value.indices.filter { _tabs.value[it].dirty }
            dirtyIndices.forEach { autosaveTabIfDirty(it) }
            persistDraftIfPossible()
        }
    }

    private fun reloadTabAt(index: Int) {
        val tab = _tabs.value.getOrNull(index) ?: return
        val path = tab.path
        viewModelScope.launch {
            if (_tabs.value.getOrNull(index)?.dirty == true) return@launch
            workspace.readText(path).fold(
                onSuccess = { text ->
                    _tabs.update { tabs ->
                        tabs.mapIndexed { i, t ->
                            if (i == index) {
                                t.copy(content = text, lastPersisted = text)
                            } else {
                                t
                            }
                        }
                    }
                },
                onFailure = { _status.value = it.message },
            )
        }
    }

    private fun saveTabAt(index: Int) {
        val tab = _tabs.value.getOrNull(index) ?: run {
            _status.value = "No file open"
            return
        }
        viewModelScope.launch {
            commitPendingBurstImmediately(tab.path)
            workspace.writeText(tab.path, tab.content).fold(
                onSuccess = {
                    _tabs.update { tabs ->
                        tabs.mapIndexed { i, t ->
                            if (i == index) t.copy(lastPersisted = t.content) else t
                        }
                    }
                    redoByPath[tab.path]?.clear()
                    bumpUndoRedo()
                    _status.value = "Saved ${tab.fileName}"
                    refreshTree()
                    persistDraftIfPossible()
                },
                onFailure = { _status.value = it.message },
            )
        }
    }

    private fun _hasPersistPermission(uriStr: String): Boolean {
        return appContext.contentResolver.persistedUriPermissions.any {
            it.uri?.toString() == uriStr && it.isReadPermission && it.isWritePermission
        }
    }

    private fun validPersistedWorkspaceUris(): Set<String> {
        return appContext.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission && it.isWritePermission }
            .map { it.uri.toString() }
            .toSet()
    }

    private fun loadThemeMode(): KineticThemeMode {
        val id = uiPrefs.getString(themeModeKey, null)
        return KineticThemeMode.entries.find { it.id == id } ?: KineticThemeMode.DARK
    }
}
