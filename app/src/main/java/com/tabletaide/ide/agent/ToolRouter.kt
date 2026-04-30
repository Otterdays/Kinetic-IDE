package com.tabletaide.ide.agent

import com.tabletaide.ide.data.WorkspaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRouter @Inject constructor(
    private val workspace: WorkspaceRepository,
) {

    fun toolDefinitions(): JSONArray {
        val listFiles = JSONObject().apply {
            put("name", "list_files")
            put("description", "List files and folders under the opened workspace root.")
            put("input_schema", JSONObject().apply {
                put("type", "object")
                put(
                    "properties",
                    JSONObject().put(
                        "max_entries",
                        JSONObject()
                            .put("type", "integer")
                            .put("description", "Optional result cap. Defaults to 200, max 500."),
                    ),
                )
            })
        }
        val readFile = JSONObject().apply {
            put("name", "read_file")
            put("description", "Read UTF-8 text from a file path relative to the opened workspace root.")
            put("input_schema", JSONObject().apply {
                put("type", "object")
                put(
                    "properties",
                    JSONObject().put(
                        "path",
                        JSONObject().put("type", "string").put("description", "Relative path, use / separators"),
                    ),
                )
                put("required", JSONArray().put("path"))
            })
        }
        val writeFile = JSONObject().apply {
            put("name", "write_file")
            put("description", "Write UTF-8 text to a file under the workspace root. Overwrites existing. Creates parent folders if needed.")
            put("input_schema", JSONObject().apply {
                put("type", "object")
                put(
                    "properties",
                    JSONObject()
                        .put("path", JSONObject().put("type", "string"))
                        .put("content", JSONObject().put("type", "string")),
                )
                put("required", JSONArray().put("path").put("content"))
            })
        }
        val editFile = JSONObject().apply {
            put("name", "edit_file")
            put("description", "Replace a unique substring inside an existing file. Fails if old_string is missing or appears more than once. Prefer this over write_file when changing only part of a file.")
            put("input_schema", JSONObject().apply {
                put("type", "object")
                put(
                    "properties",
                    JSONObject()
                        .put("path", JSONObject().put("type", "string"))
                        .put("old_string", JSONObject().put("type", "string").put("description", "Exact text to find. Must be unique in file."))
                        .put("new_string", JSONObject().put("type", "string").put("description", "Replacement text.")),
                )
                put("required", JSONArray().put("path").put("old_string").put("new_string"))
            })
        }
        val searchFiles = JSONObject().apply {
            put("name", "search_files")
            put("description", "Grep regex across workspace text files. Returns matching lines with path:line.")
            put("input_schema", JSONObject().apply {
                put("type", "object")
                put(
                    "properties",
                    JSONObject()
                        .put("pattern", JSONObject().put("type", "string").put("description", "Java regex"))
                        .put("path_glob", JSONObject().put("type", "string").put("description", "Optional glob filter against relative path, e.g. '*.kt'"))
                        .put("max_matches", JSONObject().put("type", "integer").put("description", "Default 100, max 500.")),
                )
                put("required", JSONArray().put("pattern"))
            })
        }
        val createDirectory = JSONObject().apply {
            put("name", "create_directory")
            put("description", "Create a folder (and any missing parents) under the workspace root.")
            put("input_schema", JSONObject().apply {
                put("type", "object")
                put(
                    "properties",
                    JSONObject().put("path", JSONObject().put("type", "string")),
                )
                put("required", JSONArray().put("path"))
            })
        }
        val deletePath = JSONObject().apply {
            put("name", "delete_path")
            put("description", "Delete a file or folder (recursive) under the workspace root. Irreversible.")
            put("input_schema", JSONObject().apply {
                put("type", "object")
                put(
                    "properties",
                    JSONObject().put("path", JSONObject().put("type", "string")),
                )
                put("required", JSONArray().put("path"))
            })
        }
        val renamePath = JSONObject().apply {
            put("name", "rename_path")
            put("description", "Rename leaf of a file or folder in place (does not move across folders).")
            put("input_schema", JSONObject().apply {
                put("type", "object")
                put(
                    "properties",
                    JSONObject()
                        .put("path", JSONObject().put("type", "string"))
                        .put("new_name", JSONObject().put("type", "string").put("description", "New leaf name only, no slashes.")),
                )
                put("required", JSONArray().put("path").put("new_name"))
            })
        }
        return JSONArray()
            .put(listFiles)
            .put(readFile)
            .put(writeFile)
            .put(editFile)
            .put(searchFiles)
            .put(createDirectory)
            .put(deletePath)
            .put(renamePath)
    }

    suspend fun dispatch(name: String, input: JSONObject): String = withContext(Dispatchers.IO) {
        if (!workspace.hasRoot() && name != "_noop") return@withContext "Error: no workspace folder opened"
        when (name) {
            "list_files" -> {
                val requestedLimit = input.optInt("max_entries", DEFAULT_LIST_LIMIT)
                val limit = requestedLimit.coerceIn(1, MAX_LIST_LIMIT)
                val rows = workspace.listTreeRows()
                if (rows.isEmpty()) return@withContext "(workspace is empty)"

                buildString {
                    rows.take(limit).forEach { row ->
                        append(if (row.isDirectory) "dir  " else "file ")
                        append(row.path)
                        append('\n')
                    }
                    if (rows.size > limit) {
                        append("... truncated ")
                        append(rows.size - limit)
                        append(" more entries")
                    }
                }.trimEnd()
            }
            "read_file" -> {
                val path = input.optString("path")
                if (path.isBlank()) return@withContext "Error: missing path"
                workspace.readText(path).fold(
                    onSuccess = { it },
                    onFailure = { "Error: ${it.message}" },
                )
            }
            "write_file" -> {
                val path = input.optString("path")
                val content = input.optString("content")
                if (path.isBlank()) return@withContext "Error: missing path"
                workspace.writeText(path, content).fold(
                    onSuccess = { "Wrote file successfully: $path" },
                    onFailure = { "Error: ${it.message}" },
                )
            }
            "edit_file" -> {
                val path = input.optString("path")
                val oldStr = input.optString("old_string")
                val newStr = input.optString("new_string")
                if (path.isBlank()) return@withContext "Error: missing path"
                if (oldStr.isEmpty()) return@withContext "Error: old_string must be non-empty"
                val readRes = workspace.readText(path)
                val current = readRes.getOrElse {
                    return@withContext "Error: ${it.message}"
                }
                val firstIdx = current.indexOf(oldStr)
                if (firstIdx < 0) return@withContext "Error: old_string not found in $path"
                val secondIdx = current.indexOf(oldStr, firstIdx + 1)
                if (secondIdx >= 0) return@withContext "Error: old_string is not unique in $path (appears multiple times); add more context"
                val updated = current.substring(0, firstIdx) + newStr + current.substring(firstIdx + oldStr.length)
                workspace.writeText(path, updated).fold(
                    onSuccess = { "Edited $path (1 replacement)" },
                    onFailure = { "Error: ${it.message}" },
                )
            }
            "search_files" -> {
                val patternStr = input.optString("pattern")
                if (patternStr.isBlank()) return@withContext "Error: missing pattern"
                val regex = try {
                    Regex(patternStr)
                } catch (e: Exception) {
                    return@withContext "Error: invalid regex: ${e.message}"
                }
                val glob = input.optString("path_glob").takeIf { it.isNotBlank() }
                val globRegex = glob?.let { globToRegex(it) }
                val maxMatches = input.optInt("max_matches", DEFAULT_SEARCH_LIMIT)
                    .coerceIn(1, MAX_SEARCH_LIMIT)

                val rows = workspace.listTreeRows().filter { !it.isDirectory }
                val out = StringBuilder()
                var matchCount = 0
                var filesScanned = 0
                outer@ for (row in rows) {
                    if (globRegex != null && !globRegex.matches(row.path)) continue
                    if (filesScanned >= MAX_FILES_SCANNED) {
                        out.append("... file scan cap reached (").append(MAX_FILES_SCANNED).append(")\n")
                        break
                    }
                    val text = workspace.readText(row.path).getOrNull() ?: continue
                    filesScanned++
                    text.lineSequence().forEachIndexed { idx, line ->
                        if (regex.containsMatchIn(line)) {
                            out.append(row.path).append(':').append(idx + 1).append(": ")
                                .append(line.trimEnd()).append('\n')
                            matchCount++
                            if (matchCount >= maxMatches) {
                                out.append("... match cap reached (").append(maxMatches).append(")\n")
                                break@outer
                            }
                        }
                    }
                }
                if (matchCount == 0) "(no matches in $filesScanned files)" else out.toString().trimEnd()
            }
            "create_directory" -> {
                val path = input.optString("path")
                if (path.isBlank()) return@withContext "Error: missing path"
                workspace.createDirectory(path).fold(
                    onSuccess = { "Created directory: $it" },
                    onFailure = { "Error: ${it.message}" },
                )
            }
            "delete_path" -> {
                val path = input.optString("path")
                if (path.isBlank()) return@withContext "Error: missing path"
                workspace.deleteNode(path).fold(
                    onSuccess = { "Deleted: $path" },
                    onFailure = { "Error: ${it.message}" },
                )
            }
            "rename_path" -> {
                val path = input.optString("path")
                val newName = input.optString("new_name")
                if (path.isBlank() || newName.isBlank()) return@withContext "Error: missing path or new_name"
                workspace.renameLeaf(path, newName).fold(
                    onSuccess = { "Renamed to: $it" },
                    onFailure = { "Error: ${it.message}" },
                )
            }
            else -> "Error: unknown tool $name"
        }
    }

    private fun globToRegex(glob: String): Regex {
        val sb = StringBuilder("^")
        for (c in glob) {
            when (c) {
                '*' -> sb.append(".*")
                '?' -> sb.append('.')
                '.', '(', ')', '+', '|', '^', '$', '@', '%', '{', '}', '[', ']', '\\' ->
                    sb.append('\\').append(c)
                else -> sb.append(c)
            }
        }
        sb.append('$')
        return Regex(sb.toString())
    }

    private companion object {
        const val DEFAULT_LIST_LIMIT = 200
        const val MAX_LIST_LIMIT = 500
        const val DEFAULT_SEARCH_LIMIT = 100
        const val MAX_SEARCH_LIMIT = 500
        const val MAX_FILES_SCANNED = 2000
    }
}
