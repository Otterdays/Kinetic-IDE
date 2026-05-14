package com.tabletaide.ide.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
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
    private val mutationBus: WorkspaceMutationBus,
) {
    private val mutex = Mutex()
    private var root: DocumentFile? = null
    private var rootTreeUri: Uri? = null

    fun hasRoot(): Boolean = root != null

    fun setRootFromUri(treeUri: Uri) {
        rootTreeUri = treeUri
        root = DocumentFile.fromTreeUri(appContext, treeUri)
    }

    /** Persisted SAF tree URI for session restore ([TRACE: DOCS/ROADMAP.md] M1). */
    fun rootTreeUriOrNull(): Uri? = rootTreeUri

    fun rootOrNull(): DocumentFile? = root

    fun displayNameForTreeUri(treeUri: Uri): String =
        DocumentFile.fromTreeUri(appContext, treeUri)?.name?.trim().orEmpty()
            .ifEmpty { "Workspace" }

    fun childTreeUri(parentTreeUri: Uri, childName: String): Uri? {
        val parent = DocumentFile.fromTreeUri(appContext, parentTreeUri) ?: return null
        val child = parent.findFile(childName)?.takeIf { it.isDirectory } ?: return null
        return buildTreeUri(child)
    }

    suspend fun createStarterProject(
        parentTreeUri: Uri,
        projectName: String,
        template: StarterProjectTemplate,
    ): Result<Uri> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val safeProjectName = sanitizeLeafName(projectName)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Invalid project name"),
                    )
                val parent = DocumentFile.fromTreeUri(appContext, parentTreeUri)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Cannot access selected location"),
                    )
                if (!parent.isDirectory) {
                    return@withContext Result.failure(
                        IllegalStateException("Selected location is not a folder"),
                    )
                }
                if (parent.findFile(safeProjectName) != null) {
                    return@withContext Result.failure(
                        IllegalStateException("Project already exists: $safeProjectName"),
                    )
                }
                val projectRoot = parent.createDirectory(safeProjectName)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Cannot create project folder"),
                    )
                seedStarterProject(projectRoot, safeProjectName, template)
                val projectTreeUri = buildTreeUri(projectRoot)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Cannot open new project folder"),
                    )
                Result.success(projectTreeUri)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

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
                mutationBus.notifyFileWritten(relativePath)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun createEmptyFile(parentDirRelativePath: String, leafName: String): Result<String> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val safe = sanitizeLeafName(leafName) ?: return@withContext Result.failure(
                        IllegalArgumentException("Invalid file name"),
                    )
                    val parent = resolveDirectory(parentDirRelativePath.trim('/'))
                        ?: return@withContext Result.failure(IllegalStateException("Cannot resolve parent folder"))
                    if (parent.findFile(safe) != null) {
                        return@withContext Result.failure(IllegalStateException("Already exists: $safe"))
                    }
                    parent.createFile("text/plain", safe)
                        ?: return@withContext Result.failure(IllegalStateException("Cannot create: $safe"))
                    val rel = joinRelative(parentDirRelativePath.trim('/'), safe)
                    mutationBus.notifyFileWritten(rel)
                    Result.success(rel)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }

    suspend fun createDirectory(relativeFolderPath: String): Result<String> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val trimmed = relativeFolderPath.trim('/')
                    if (trimmed.isEmpty()) {
                        return@withContext Result.failure(IllegalArgumentException("Empty folder path"))
                    }
                    val lastSlash = trimmed.lastIndexOf('/')
                    val parentPath = if (lastSlash < 0) "" else trimmed.substring(0, lastSlash)
                    val leaf = if (lastSlash < 0) trimmed else trimmed.substring(lastSlash + 1)
                    val parentDir = resolveOrCreateDirectory(parentPath)
                        ?: return@withContext Result.failure(IllegalStateException("Cannot create parent"))
                    val existing = parentDir.findFile(leaf)
                    return@withContext when {
                        existing != null && existing.isDirectory -> Result.success(trimmed)
                        existing != null -> Result.failure(
                            IllegalStateException("Name already used by a file"),
                        )
                        parentDir.createDirectory(leaf) != null -> Result.success(trimmed)
                        else -> Result.failure(IllegalStateException("Cannot create folder"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }

    suspend fun renameLeaf(relativePath: String, newLeafName: String): Result<String> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val safe = sanitizeLeafName(newLeafName) ?: return@withContext Result.failure(
                        IllegalArgumentException("Invalid file name"),
                    )
                    val src = resolveFile(relativePath)
                        ?: return@withContext Result.failure(IllegalArgumentException("Not found: $relativePath"))
                    val parentSeg = parentSegmentOf(relativePath)
                    val parentDoc = resolveDirectory(parentSeg)
                        ?: return@withContext Result.failure(IllegalStateException("Cannot resolve parent"))
                    if (parentDoc.findFile(safe) != null) {
                        return@withContext Result.failure(IllegalStateException("Already exists: $safe"))
                    }
                    if (!src.renameTo(safe)) {
                        return@withContext Result.failure(IllegalStateException("Rename failed"))
                    }
                    val newRel = joinRelative(parentSeg.trimEnd('/'), safe)
                    mutationBus.notifyFileWritten(newRel)
                    Result.success(newRel)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }

    suspend fun duplicateFile(relativePath: String): Result<String> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val src = resolveFile(relativePath)
                        ?: return@withContext Result.failure(IllegalArgumentException("Not found: $relativePath"))
                    if (!src.isFile) {
                        return@withContext Result.failure(IllegalArgumentException("Not a file: $relativePath"))
                    }
                    val leaf = relativePath.substringAfterLast('/')
                    val parentSeg = parentSegmentOf(relativePath)
                    val parentDoc = resolveDirectory(parentSeg)
                        ?: return@withContext Result.failure(IllegalStateException("Cannot resolve parent"))
                    val destName = uniqueLeafName(parentDoc, duplicateLeafName(leaf))
                    val txt = appContext.contentResolver.openInputStream(src.uri)?.use { inp ->
                        BufferedReader(InputStreamReader(inp, Charsets.UTF_8)).readText()
                    } ?: return@withContext Result.failure(IllegalStateException("Cannot read source"))
                    val created = parentDoc.createFile(src.type ?: "text/plain", destName)
                        ?: return@withContext Result.failure(IllegalStateException("Cannot create copy"))
                    appContext.contentResolver.openOutputStream(created.uri, "wt")?.use { out ->
                        OutputStreamWriter(out, Charsets.UTF_8).use { writer -> writer.write(txt) }
                    } ?: return@withContext Result.failure(IllegalStateException("Cannot write copy"))
                    val newRel = joinRelative(parentSeg, destName)
                    mutationBus.notifyFileWritten(newRel)
                    Result.success(newRel)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }

    suspend fun deleteNode(relativePath: String): Result<Unit> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val doc =
                        resolveFile(relativePath) ?: return@withContext Result.failure(
                            IllegalArgumentException("Not found: $relativePath"),
                        )
                    if (!deleteTree(doc)) {
                        return@withContext Result.failure(IllegalStateException("Delete failed"))
                    }
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }

    fun listDirectoryRows(
        relativeDirPath: String = "",
        includeGitMetadata: Boolean = false,
    ): Result<List<TreeRow>> = runCatching {
        val trimmed = relativeDirPath.trim('/')
        val dir = if (trimmed.isEmpty()) {
            root ?: error("No workspace root")
        } else {
            resolveDirectory(trimmed) ?: error("Directory not found: $trimmed")
        }
        check(dir.isDirectory) { "Not a directory: ${trimmed.ifBlank { "/" }}" }
        val depth = if (trimmed.isEmpty()) 0 else trimmed.split('/').count { it.isNotEmpty() }
        listChildren(dir, trimmed, depth, includeGitMetadata)
    }

    fun listTreeRows(includeGitMetadata: Boolean = false): List<TreeRow> {
        val r = root ?: return emptyList()
        val out = mutableListOf<TreeRow>()
        fun walk(doc: DocumentFile, prefix: String, depth: Int) {
            val row = toTreeRow(doc, prefix, depth, includeGitMetadata) ?: return
            out.add(row)
            if (row.isDirectory) {
                listChildDocuments(doc, includeGitMetadata).forEach { child ->
                    walk(child, row.path, depth + 1)
                }
            }
        }
        listChildDocuments(r, includeGitMetadata).forEach { child ->
            walk(child, "", 0)
        }
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

    private fun parentSegmentOf(relativePath: String): String {
        val trimmed = relativePath.trim('/')
        val idx = trimmed.lastIndexOf('/')
        return if (idx < 0) "" else trimmed.substring(0, idx).trim('/')
    }

    private fun resolveDirectory(parentRelativeTrimmed: String): DocumentFile? {
        val trimmed = parentRelativeTrimmed.trim('/')
        val r = root ?: return null
        if (trimmed.isEmpty()) return r
        val doc = resolveFile(trimmed)
        return doc?.takeIf { it.isDirectory }
    }

    private fun listChildren(
        dir: DocumentFile,
        prefix: String,
        depth: Int,
        includeGitMetadata: Boolean,
    ): List<TreeRow> = listChildDocuments(dir, includeGitMetadata)
        .mapNotNull { child -> toTreeRow(child, prefix, depth, includeGitMetadata) }
        .sortedWith(
            compareBy<TreeRow>({ !it.isDirectory }, { it.displayName.lowercase() }, { it.path.lowercase() }),
        )

    private fun listChildDocuments(
        dir: DocumentFile,
        includeGitMetadata: Boolean,
    ): List<DocumentFile> = dir.listFiles()
        .filter { child -> includeGitMetadata || child.name != ".git" }
        .sortedWith(compareBy({ !it.isDirectory }, { it.name?.lowercase().orEmpty() }, { it.uri.toString() }))

    private fun toTreeRow(
        doc: DocumentFile,
        prefix: String,
        depth: Int,
        includeGitMetadata: Boolean,
    ): TreeRow? {
        val name = doc.name ?: return null
        if (!includeGitMetadata && name == ".git") return null
        val path = if (prefix.isEmpty()) name else "$prefix/$name"
        return TreeRow(
            path = path,
            uri = doc.uri,
            displayName = name,
            isDirectory = doc.isDirectory,
            depth = depth,
        )
    }

    private fun joinRelative(parentRelativeTrimmed: String, leaf: String): String {
        val p = parentRelativeTrimmed.trim('/').trimEnd()
        val safeLeaf = leaf.trim('/')
        return if (p.isEmpty()) safeLeaf else "$p/$safeLeaf"
    }

    private fun sanitizeLeafName(name: String): String? {
        val t = name.trim()
        if (t.isEmpty() || t.contains('/') || t.contains('\\')) return null
        if (t == "." || t == "..") return null
        return t
    }

    private fun duplicateLeafName(leaf: String): String {
        val dot = leaf.lastIndexOf('.')
        return if (dot <= 0) {
            "${leaf}_copy"
        } else {
            "${leaf.substring(0, dot)}_copy${leaf.substring(dot)}"
        }
    }

    private fun uniqueLeafName(dir: DocumentFile, desired: String): String {
        if (dir.findFile(desired) == null) return desired
        val dot = desired.lastIndexOf('.')
        val base = if (dot <= 0) desired else desired.substring(0, dot)
        val extWithDot = if (dot <= 0) "" else desired.substring(dot)
        var n = 2
        while (true) {
            val cand = "${base}_$n$extWithDot"
            if (dir.findFile(cand) == null) return cand
            n++
        }
    }

    private fun deleteTree(doc: DocumentFile): Boolean {
        if (doc.isDirectory) {
            val kids = doc.listFiles()
            kids.forEach { child ->
                if (!deleteTree(child)) return false
            }
        }
        return doc.delete()
    }

    private fun buildTreeUri(doc: DocumentFile): Uri? {
        val authority = doc.uri.authority ?: return null
        val documentId = DocumentsContract.getDocumentId(doc.uri)
        return DocumentsContract.buildTreeDocumentUri(authority, documentId)
    }

    private fun ensureChildDirectory(root: DocumentFile, relativePath: String): DocumentFile? {
        var current = root
        for (segment in relativePath.split('/').filter { it.isNotBlank() }) {
            val existing = current.findFile(segment)
            current = when {
                existing != null && existing.isDirectory -> existing
                existing != null -> return null
                else -> current.createDirectory(segment) ?: return null
            }
        }
        return current
    }

    private fun seedStarterProject(
        projectRoot: DocumentFile,
        projectName: String,
        template: StarterProjectTemplate,
    ) {
        starterProjectFiles(projectName, template).forEach { (relativePath, content) ->
            val parentPath = relativePath.substringBeforeLast('/', "")
            val leafName = relativePath.substringAfterLast('/')
            val parent = if (parentPath.isEmpty()) {
                projectRoot
            } else {
                ensureChildDirectory(projectRoot, parentPath)
                    ?: throw IllegalStateException("Cannot create folder: $parentPath")
            }
            val file = parent.findFile(leafName) ?: parent.createFile(mimeTypeFor(leafName), leafName)
            ?: throw IllegalStateException("Cannot create file: $relativePath")
            appContext.contentResolver.openOutputStream(file.uri, "wt")?.use { out ->
                OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                    writer.write(content)
                }
            } ?: throw IllegalStateException("Cannot write file: $relativePath")
        }
    }

    private fun starterProjectFiles(
        projectName: String,
        template: StarterProjectTemplate,
    ): Map<String, String> = when (template) {
        StarterProjectTemplate.BLANK -> linkedMapOf(
            "README.md" to """
                # $projectName

                Created with Kinetic.

                ## Structure

                - `src/` for source files
                - `notes/` for docs and planning
            """.trimIndent(),
            ".gitignore" to """
                .DS_Store
                *.log
                node_modules/
                dist/
                build/
            """.trimIndent(),
            "src/.gitkeep" to "",
            "notes/.gitkeep" to "",
        )
        StarterProjectTemplate.KOTLIN_CONSOLE -> linkedMapOf(
            "README.md" to """
                # $projectName

                Simple Kotlin console starter created with Kinetic.

                ## Entry point

                `src/main/kotlin/Main.kt`
            """.trimIndent(),
            ".gitignore" to """
                .DS_Store
                *.log
                build/
                out/
            """.trimIndent(),
            "src/main/kotlin/Main.kt" to """
                fun main() {
                    println("Hello from $projectName")
                }
            """.trimIndent(),
        )
        StarterProjectTemplate.WEB_APP -> linkedMapOf(
            "README.md" to """
                # $projectName

                Small web starter created with Kinetic.

                Open `src/index.html` in a browser to begin iterating.
            """.trimIndent(),
            ".gitignore" to """
                .DS_Store
                *.log
                node_modules/
                dist/
            """.trimIndent(),
            "src/index.html" to """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="utf-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                    <title>$projectName</title>
                    <link rel="stylesheet" href="./styles.css" />
                  </head>
                  <body>
                    <main class="app-shell">
                      <h1>$projectName</h1>
                      <p>Start editing <code>src/index.html</code> and <code>src/main.js</code>.</p>
                      <button id="hello-button">Say hello</button>
                      <pre id="output"></pre>
                    </main>
                    <script src="./main.js"></script>
                  </body>
                </html>
            """.trimIndent(),
            "src/styles.css" to """
                :root {
                  color-scheme: dark;
                  font-family: Inter, Arial, sans-serif;
                  background: #0b1020;
                  color: #ecf2ff;
                }

                body {
                  margin: 0;
                  min-height: 100vh;
                  display: grid;
                  place-items: center;
                  background: radial-gradient(circle at top, #1e2a52, #0b1020 60%);
                }

                .app-shell {
                  width: min(560px, calc(100vw - 32px));
                  padding: 32px;
                  border-radius: 24px;
                  background: rgba(11, 18, 32, 0.88);
                  border: 1px solid rgba(129, 236, 255, 0.25);
                  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.35);
                }

                button {
                  margin-top: 16px;
                  border: 0;
                  border-radius: 999px;
                  padding: 10px 16px;
                  background: #81ecff;
                  color: #06242d;
                  font-weight: 700;
                  cursor: pointer;
                }

                pre {
                  margin-top: 16px;
                  white-space: pre-wrap;
                }
            """.trimIndent(),
            "src/main.js" to """
                const button = document.getElementById("hello-button");
                const output = document.getElementById("output");

                if (button && output) {
                  button.addEventListener("click", () => {
                    output.textContent = "Hello from $projectName";
                  });
                }
            """.trimIndent(),
        )
    }

    private fun mimeTypeFor(name: String): String = when (name.substringAfterLast('.', "")) {
        "html" -> "text/html"
        "css" -> "text/css"
        "js" -> "application/javascript"
        "json" -> "application/json"
        "md", "txt", "kt" -> "text/plain"
        else -> "text/plain"
    }
}

data class TreeRow(
    val path: String,
    val uri: Uri,
    val displayName: String,
    val isDirectory: Boolean,
    val depth: Int,
)
