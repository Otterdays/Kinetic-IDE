package com.tabletaide.ide.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TreeFilterTest {

    @Test
    fun `filter keeps matching rows and ancestor folders`() {
        val rows = listOf(
            ExplorerItem(path = "app", displayName = "app", isDirectory = true, depth = 0),
            ExplorerItem(path = "app/src", displayName = "src", isDirectory = true, depth = 1),
            ExplorerItem(
                path = "app/src/MainActivity.kt",
                displayName = "MainActivity.kt",
                isDirectory = false,
                depth = 2,
            ),
            ExplorerItem(path = "docs", displayName = "docs", isDirectory = true, depth = 0),
            ExplorerItem(path = "docs/notes.md", displayName = "notes.md", isDirectory = false, depth = 1),
        )

        val filtered = filterExplorerItems(rows, "mainact")

        assertEquals(
            listOf("app", "app/src", "app/src/MainActivity.kt"),
            filtered.map { it.path },
        )
    }

    @Test
    fun `blank query returns all rows unchanged`() {
        val rows = listOf(
            ExplorerItem(path = "src", displayName = "src", isDirectory = true, depth = 0),
            ExplorerItem(path = "src/main.kt", displayName = "main.kt", isDirectory = false, depth = 1),
        )

        assertEquals(rows, filterExplorerItems(rows, "  "))
    }
}
