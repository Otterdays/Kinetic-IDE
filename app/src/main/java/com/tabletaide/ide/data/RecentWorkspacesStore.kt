package com.tabletaide.ide.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS = "kinetic_recent_workspaces"
private const val KEY_ENTRIES = "entries_json"
private const val MAX_RECENT_WORKSPACES = 12

data class RecentWorkspaceEntry(
    val uriString: String,
    val displayName: String,
    val lastOpenedAtMs: Long,
)

@Singleton
class RecentWorkspacesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _entries = MutableStateFlow(loadEntries())
    val entries: StateFlow<List<RecentWorkspaceEntry>> = _entries.asStateFlow()

    fun recordWorkspace(treeUri: Uri, displayNameOverride: String? = null) {
        val uriString = treeUri.toString()
        if (uriString.isBlank()) return
        val displayName = displayNameOverride
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: DocumentFile.fromTreeUri(context, treeUri)?.name?.trim().orEmpty()
                .ifEmpty { "Workspace" }
        val updated = _entries.value
            .filterNot { it.uriString == uriString }
            .toMutableList()
            .apply {
                add(
                    0,
                    RecentWorkspaceEntry(
                        uriString = uriString,
                        displayName = displayName,
                        lastOpenedAtMs = System.currentTimeMillis(),
                    ),
                )
            }
            .take(MAX_RECENT_WORKSPACES)
        saveEntries(updated)
    }

    fun removeWorkspace(uriString: String) {
        val updated = _entries.value.filterNot { it.uriString == uriString }
        saveEntries(updated)
    }

    fun pruneUnavailable(validUriStrings: Set<String>) {
        val updated = _entries.value.filter { it.uriString in validUriStrings }
        if (updated.size == _entries.value.size) return
        saveEntries(updated)
    }

    private fun loadEntries(): List<RecentWorkspaceEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    add(
                        RecentWorkspaceEntry(
                            uriString = item.getString("uriString"),
                            displayName = item.optString("displayName", "Workspace")
                                .ifBlank { "Workspace" },
                            lastOpenedAtMs = item.optLong("lastOpenedAtMs", 0L),
                        ),
                    )
                }
            }.sortedByDescending { it.lastOpenedAtMs }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveEntries(entries: List<RecentWorkspaceEntry>) {
        val arr = JSONArray()
        entries.forEach { entry ->
            arr.put(
                JSONObject().apply {
                    put("uriString", entry.uriString)
                    put("displayName", entry.displayName)
                    put("lastOpenedAtMs", entry.lastOpenedAtMs)
                },
            )
        }
        prefs.edit().putString(KEY_ENTRIES, arr.toString()).apply()
        _entries.value = entries
    }
}
