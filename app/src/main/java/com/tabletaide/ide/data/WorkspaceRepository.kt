package com.tabletaide.ide.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private val mutex = Mutex()
    private var root: DocumentFile? = null

    fun hasRoot(): Boolean = root != null

    fun setRootFromUri(treeUri: Uri) {
        root = DocumentFile.fromTreeUri(appContext, treeUri)
    }

    fun rootOrNull(): DocumentFile? = root

    suspend fun readText(relativePath: String): Result<String> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val file = resolveFile(relativePath)
                    ?: return@withContext Result.failure(IllegalArgumentException("File not found: $relativePath"))
                if (!file.isFile) {
                    return@withContext Result.failure(IllegalArgumentException("Not a file: $relativePath"))
                }
                appContext.contentResolver.openInputStream(file.uri)?.use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
                }?.let { Result.success(it) }
                    ?: Result.failure(IllegalStateException("Cannot open: $relativePath"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun writeText(relativePath: String, content: String): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val parentPath = relativePath.substringBeforeLast('/', "")
                val name = relativePath.substringAfterLast('/')
                val parent = if (parentPath.isEmpty()) {
                    root ?: return@withContext Result.failure(IllegalStateException("No workspace root"))
                } else {
                    resolveOrCreateDirectory(parentPath)
                        ?: return@withContext Result.failure(IllegalArgumentException("Cannot create parent: $parentPath"))
                }
                val existing = parent.findFile(name)
                val target = if (existing != null && existing.isFile) {
                    existing
                } else {
                    parent.createFile("text/plain", name)
                        ?: return@withContext Result.failure(IllegalStateException("Cannot create file: $relativePath"))
                }
                appContext.contentResolver.openOutputStream(target.uri, "wt")?.use { out ->
                    OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                        writer.write(content)
                    }
                } ?: return@withContext Result.failure(IllegalStateException("Cannot write: $relativePath"))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun listTreeRows(): List<TreeRow> {
        val r = root ?: return emptyList()
        val out = mutableListOf<TreeRow>()
        fun walk(doc: DocumentFile, prefix: String, depth: Int) {
            val name = doc.name ?: return
            val path = if (prefix.isEmpty()) name else "$prefix/$name"
            val isDir = doc.isDirectory
            out.add(TreeRow(path = path, uri = doc.uri, displayName = name, isDirectory = isDir, depth = depth))
            if (isDir) {
                val kids = doc.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
                kids.forEach { child -> walk(child, path, depth + 1) }
            }
        }
        val roots = r.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
        roots.forEach { walk(it, "", 0) }
        return out
    }

    private fun resolveFile(relativePath: String): DocumentFile? {
        val r = root ?: return null
        if (relativePath.isBlank()) return null
        var current: DocumentFile = r
        val segments = relativePath.split('/').filter { it.isNotEmpty() }
        for ((index, segment) in segments.withIndex()) {
            val next = current.findFile(segment) ?: return null
            if (index == segments.lastIndex) return next
            if (!next.isDirectory) return null
            current = next
        }
        return null
    }

    private fun resolveOrCreateDirectory(relativePath: String): DocumentFile? {
        val r = root ?: return null
        if (relativePath.isBlank()) return r
        var current: DocumentFile = r
        for (segment in relativePath.split('/').filter { it.isNotEmpty() }) {
            val found = current.findFile(segment)
            current = when {
                found != null && found.isDirectory -> found
                found != null && found.isFile -> return null
                else -> current.createDirectory(segment) ?: return null
            }
        }
        return current
    }
}

data class TreeRow(
    val path: String,
    val uri: Uri,
    val displayName: String,
    val isDirectory: Boolean,
    val depth: Int,
)
