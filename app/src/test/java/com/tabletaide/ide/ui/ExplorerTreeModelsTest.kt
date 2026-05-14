package com.tabletaide.ide.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplorerTreeModelsTest {

    @Test
    fun `sort puts directories before files and orders by name`() {
        val sorted = sortExplorerItems(
            listOf(
                ExplorerItem(path = "zeta.txt", displayName = "zeta.txt", isDirectory = false, depth = 0),
                ExplorerItem(path = "alpha", displayName = "alpha", isDirectory = true, depth = 0),
                ExplorerItem(path = "beta", displayName = "beta", isDirectory = true, depth = 0),
                ExplorerItem(path = "aardvark.kt", displayName = "aardvark.kt", isDirectory = false, depth = 0),
            ),
        )

        assertEquals(
            listOf("alpha", "beta", "aardvark.kt", "zeta.txt"),
            sorted.map { it.displayName },
        )
    }

    @Test
    fun `visible rows only include descendants for expanded directories`() {
        val src = ExplorerItem(path = "src", displayName = "src", isDirectory = true, depth = 0)
        val app = ExplorerItem(path = "src/app", displayName = "app", isDirectory = true, depth = 1)
        val main = ExplorerItem(
            path = "src/app/Main.kt",
            displayName = "Main.kt",
            isDirectory = false,
            depth = 2,
        )

        val rows = buildVisibleExplorerRows(
            rootPaths = listOf(src.path),
            nodes = mapOf(
                src.path to ExplorerTreeNode(src, childPaths = listOf(app.path), childrenLoaded = true),
                app.path to ExplorerTreeNode(app, childPaths = listOf(main.path), childrenLoaded = true),
                main.path to ExplorerTreeNode(main),
            ),
            expandedPaths = setOf(src.path),
            loadingPaths = emptySet(),
        )

        assertEquals(listOf("src", "src/app"), rows.map { it.item.path })
        assertTrue(rows.first().expanded)
        assertFalse(rows.last().expanded)
    }

    @Test
    fun `collapsed large tree only returns visible top level rows`() {
        val nodes = buildMap<String, ExplorerTreeNode> {
            repeat(500) { index ->
                val dir = ExplorerItem(
                    path = "dir$index",
                    displayName = "dir$index",
                    isDirectory = true,
                    depth = 0,
                )
                val child = ExplorerItem(
                    path = "dir$index/file$index.kt",
                    displayName = "file$index.kt",
                    isDirectory = false,
                    depth = 1,
                )
                put(
                    dir.path,
                    ExplorerTreeNode(dir, childPaths = listOf(child.path), childrenLoaded = true),
                )
                put(child.path, ExplorerTreeNode(child))
            }
        }

        val visible = buildVisibleExplorerRows(
            rootPaths = (0 until 500).map { "dir$it" },
            nodes = nodes,
            expandedPaths = emptySet(),
            loadingPaths = emptySet(),
        )

        assertEquals(500, visible.size)
        assertTrue(visible.all { it.item.depth == 0 })
    }
}
