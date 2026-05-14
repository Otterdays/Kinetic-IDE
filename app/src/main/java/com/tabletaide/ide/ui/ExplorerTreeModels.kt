package com.tabletaide.ide.ui

import com.tabletaide.ide.data.TreeRow

data class ExplorerItem(
    val path: String,
    val displayName: String,
    val isDirectory: Boolean,
    val depth: Int,
) {
    companion object {
        fun from(row: TreeRow): ExplorerItem = ExplorerItem(
            path = row.path,
            displayName = row.displayName,
            isDirectory = row.isDirectory,
            depth = row.depth,
        )
    }
}

data class ExplorerTreeNode(
    val item: ExplorerItem,
    val childPaths: List<String> = emptyList(),
    val childrenLoaded: Boolean = false,
)

data class ExplorerVisibleRow(
    val item: ExplorerItem,
    val canExpand: Boolean,
    val expanded: Boolean = false,
    val loadingChildren: Boolean = false,
)

data class ExplorerTreeUiState(
    val rows: List<ExplorerVisibleRow> = emptyList(),
    val loading: Boolean = false,
    val emptyMessage: String? = null,
    val filterActive: Boolean = false,
)

fun sortExplorerItems(items: List<ExplorerItem>): List<ExplorerItem> =
    items.sortedWith(
        compareBy<ExplorerItem>({ !it.isDirectory }, { it.displayName.lowercase() }, { it.path.lowercase() }),
    )

fun buildVisibleExplorerRows(
    rootPaths: List<String>,
    nodes: Map<String, ExplorerTreeNode>,
    expandedPaths: Set<String>,
    loadingPaths: Set<String>,
): List<ExplorerVisibleRow> {
    val out = mutableListOf<ExplorerVisibleRow>()

    fun appendPath(path: String) {
        val node = nodes[path] ?: return
        val expanded = path in expandedPaths
        out += ExplorerVisibleRow(
            item = node.item,
            canExpand = node.item.isDirectory,
            expanded = expanded,
            loadingChildren = path in loadingPaths,
        )
        if (node.item.isDirectory && expanded) {
            node.childPaths.forEach(::appendPath)
        }
    }

    rootPaths.forEach(::appendPath)
    return out
}
