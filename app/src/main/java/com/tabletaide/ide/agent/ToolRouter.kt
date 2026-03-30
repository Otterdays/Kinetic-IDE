package com.tabletaide.ide.agent

import com.tabletaide.ide.data.WorkspaceMutationBus
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
    private val mutationBus: WorkspaceMutationBus,
) {

    fun toolDefinitions(): JSONArray {
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
            put("description", "Write UTF-8 text to a file under the workspace root. Creates parent folders if needed.")
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
        return JSONArray().put(readFile).put(writeFile)
    }

    suspend fun dispatch(name: String, input: JSONObject): String = withContext(Dispatchers.IO) {
        when (name) {
            "read_file" -> {
                val path = input.optString("path")
                if (path.isBlank()) return@withContext "Error: missing path"
                if (!workspace.hasRoot()) return@withContext "Error: no workspace folder opened"
                workspace.readText(path).fold(
                    onSuccess = { it },
                    onFailure = { "Error: ${it.message}" },
                )
            }
            "write_file" -> {
                val path = input.optString("path")
                val content = input.optString("content")
                if (path.isBlank()) return@withContext "Error: missing path"
                if (!workspace.hasRoot()) return@withContext "Error: no workspace folder opened"
                workspace.writeText(path, content).fold(
                    onSuccess = {
                        mutationBus.notifyFileWritten(path)
                        "Wrote file successfully: $path"
                    },
                    onFailure = { "Error: ${it.message}" },
                )
            }
            else -> "Error: unknown tool $name"
        }
    }
}
