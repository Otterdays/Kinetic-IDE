package com.tabletaide.ide.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabletaide.ide.data.TreeRow
import com.tabletaide.ide.data.WorkspaceMutationBus
import com.tabletaide.ide.data.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorTab(
    val path: String,
    val fileName: String,
    val content: String,
    val dirty: Boolean,
)

enum class RailSection { Projects, Explorer, Search, Extensions }

@HiltViewModel
class IdeViewModel @Inject constructor(
    private val workspace: WorkspaceRepository,
    mutationBus: WorkspaceMutationBus,
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
    }

    fun setRailSection(section: RailSection) {
        _railSection.value = section
    }

    fun openWorkspaceRoot(treeUri: Uri) {
        workspace.setRootFromUri(treeUri)
        refreshTree()
        _tabs.value = emptyList()
        _selectedTabIndex.value = 0
        _status.value = "Workspace opened"
        _railSection.value = RailSection.Explorer
    }

    fun refreshTree() {
        _tree.value = workspace.listTreeRows()
    }

    fun openOrSelectFile(row: TreeRow) {
        if (row.isDirectory) return
        val existing = _tabs.value.indexOfFirst { it.path == row.path }
        if (existing >= 0) {
            _selectedTabIndex.value = existing
            return
        }
        viewModelScope.launch {
            workspace.readText(row.path).fold(
                onSuccess = { text ->
                    val tab = EditorTab(
                        path = row.path,
                        fileName = row.displayName,
                        content = text,
                        dirty = false,
                    )
                    _tabs.update { it + tab }
                    _selectedTabIndex.value = _tabs.value.lastIndex
                    _status.value = null
                },
                onFailure = {
                    _status.value = it.message
                },
            )
        }
    }

    fun selectTab(index: Int) {
        if (index in _tabs.value.indices) {
            _selectedTabIndex.value = index
        }
    }

    fun closeTab(index: Int) {
        val list = _tabs.value.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        _tabs.value = list
        when {
            list.isEmpty() -> _selectedTabIndex.value = 0
            _selectedTabIndex.value >= list.size -> _selectedTabIndex.value = list.lastIndex.coerceAtLeast(0)
            index < _selectedTabIndex.value -> _selectedTabIndex.update { (it - 1).coerceAtLeast(0) }
        }
    }

    fun onEditorTextChange(text: String) {
        val idx = _selectedTabIndex.value
        if (idx !in _tabs.value.indices) return
        _tabs.update { tabs ->
            tabs.mapIndexed { i, t ->
                if (i == idx) t.copy(content = text, dirty = true) else t
            }
        }
    }

    fun saveCurrentFile() {
        val idx = _selectedTabIndex.value
        val tab = _tabs.value.getOrNull(idx) ?: run {
            _status.value = "No file open"
            return
        }
        viewModelScope.launch {
            workspace.writeText(tab.path, tab.content).fold(
                onSuccess = {
                    _tabs.update { tabs ->
                        tabs.mapIndexed { i, t ->
                            if (i == idx) t.copy(dirty = false) else t
                        }
                    }
                    _status.value = "Saved ${tab.fileName}"
                    refreshTree()
                },
                onFailure = { _status.value = it.message },
            )
        }
    }

    private fun reloadTabAt(index: Int) {
        val path = _tabs.value.getOrNull(index)?.path ?: return
        viewModelScope.launch {
            if (_tabs.value.getOrNull(index)?.dirty == true) return@launch
            workspace.readText(path).fold(
                onSuccess = { text ->
                    _tabs.update { tabs ->
                        tabs.mapIndexed { i, t ->
                            if (i == index) t.copy(content = text, dirty = false) else t
                        }
                    }
                },
                onFailure = { _status.value = it.message },
            )
        }
    }
}
