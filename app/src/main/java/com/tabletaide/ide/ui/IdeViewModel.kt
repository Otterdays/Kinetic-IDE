package com.tabletaide.ide.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabletaide.ide.data.EditorSessionStore
import com.tabletaide.ide.data.PersistedEditorSnapshot
import com.tabletaide.ide.data.PersistedTab
import com.tabletaide.ide.data.TreeRow
import com.tabletaide.ide.data.WorkspaceMutationBus
import com.tabletaide.ide.data.WorkspaceRepository
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
import com.tabletaide.ide.IdeConstants
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
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

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

    /** Bumps when undo stacks change without tab.content changing (Compose refresh). */
    private val _undoRedoEpoch = MutableStateFlow(0L)
    val undoRedoEpoch: StateFlow<Long> = _undoRedoEpoch.asStateFlow()

    private val undoByPath = mutableMapOf<String, ArrayDeque<String>>()
    private val redoByPath = mutableMapOf<String, ArrayDeque<String>>()
    private val burstBaselineByPath = mutableMapOf<String, String>()
    private val burstJobs = mutableMapOf<String, Job>()

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
        tryRestorePersistedSession()
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
        refreshTree()
        _tabs.value = emptyList()
        _selectedTabIndex.value = 0
        _status.value = "Workspace opened"
        _railSection.value = RailSection.Explorer
        bumpUndoRedo()
        persistDraftIfPossible()
    }

    /** Best-effort session restore ([TRACE: DOCS/ROADMAP.md] Epic 1.1 crash-safe drafts). */
    fun tryRestorePersistedSession() {
        val snapshot = sessionStore.loadSnapshotOrNull() ?: return
        if (!_hasPersistPermission(snapshot.workspaceUri)) return
        val uri = Uri.parse(snapshot.workspaceUri)
        workspace.setRootFromUri(uri)
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
    }

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
            return
        }
        viewModelScope.launch {
            openOrSelectRelativePath(row.path, row.displayName)
        }
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
                // NOTE: surface conflict/external change later (ROADMAP conflict prompts).
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
}
