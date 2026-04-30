package com.tabletaide.ide.ui

import com.tabletaide.ide.data.TreeRow

/** Ordered-character fuzzy match (substring-style) on path + display name. */
fun pathMatchesExplorerQuery(path: String, displayName: String, query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.lowercase().filter { !it.isWhitespace() }
    if (q.isEmpty()) return true
    val hay = "${path.lowercase()}/${displayName.lowercase()}"
    var idx = 0
    for (c in q) {
        val j = hay.indexOf(c, idx)
        if (j < 0) return false
        idx = j + 1
    }
    return true
}

private fun ancestorPathsOf(relativePath: String): Set<String> {
    if (relativePath.isEmpty()) return emptySet()
    val parts = relativePath.split('/').filter { it.isNotEmpty() }
    if (parts.size <= 1) return emptySet()
    val out = mutableSetOf<String>()
    for (i in 0 until parts.lastIndex) {
        out.add(parts.subList(0, i + 1).joinToString("/"))
    }
    return out
}

/** Keeps matched rows plus ancestor folders so the tree stays navigable. */
fun filterTreeRows(rows: List<TreeRow>, query: String): List<TreeRow> {
    if (query.isBlank()) return rows
    val matched = rows
        .filter { pathMatchesExplorerQuery(it.path, it.displayName, query) }
        .map { it.path }
        .toSet()
    val keep = matched.toMutableSet()
    for (p in matched) {
        keep.addAll(ancestorPathsOf(p))
    }
    return rows.filter { it.path in keep }
}
