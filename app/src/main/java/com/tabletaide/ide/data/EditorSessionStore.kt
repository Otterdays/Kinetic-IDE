package com.tabletaide.ide.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS = "kinetic_editor_session"
private const val KEY_WORKSPACE_URI = "workspace_uri"
private const val KEY_SELECTED_INDEX = "selected_index"
private const val KEY_TABS = "tabs_json"
private const val SCHEMA = 2

/**
 * Crash-safe drafts: open buffers + workspace pointer for restore after process death.
 * // [TRACE: DOCS/ROADMAP.md] M1 Epic 1.1
 */
@Singleton
class EditorSessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveSnapshot(snapshot: PersistedEditorSnapshot) {
        val arr = JSONArray()
        snapshot.tabs.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("path", t.path)
                    put("fileName", t.fileName)
                    put("content", t.content)
                    put("lastPersisted", t.lastPersisted)
                    put("scrollPx", t.scrollPx)
                    put("selectionStart", t.selectionStart)
                    put("selectionEnd", t.selectionEnd)
                },
            )
        }
        prefs.edit()
            .putInt("_v", SCHEMA)
            .putString(KEY_WORKSPACE_URI, snapshot.workspaceUri)
            .putInt(KEY_SELECTED_INDEX, snapshot.selectedIndex)
            .putString(KEY_TABS, arr.toString())
            .apply()
    }

    fun loadSnapshotOrNull(): PersistedEditorSnapshot? {
        val uri = prefs.getString(KEY_WORKSPACE_URI, null) ?: return null
        val raw = prefs.getString(KEY_TABS, null) ?: return null
        return try {
            val arr = JSONArray(raw)
            val tabs = buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        PersistedTab(
                            path = o.getString("path"),
                            fileName = o.getString("fileName"),
                            content = o.getString("content"),
                            lastPersisted = o.optString("lastPersisted").ifEmpty {
                                o.getString("content")
                            },
                            scrollPx = o.optInt("scrollPx", 0),
                            selectionStart = o.optInt("selectionStart", 0),
                            selectionEnd = o.optInt("selectionEnd", 0),
                        ),
                    )
                }
            }
            PersistedEditorSnapshot(
                workspaceUri = uri,
                selectedIndex = prefs.getInt(KEY_SELECTED_INDEX, 0),
                tabs = tabs,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

data class PersistedEditorSnapshot(
    val workspaceUri: String,
    val selectedIndex: Int,
    val tabs: List<PersistedTab>,
)

data class PersistedTab(
    val path: String,
    val fileName: String,
    val content: String,
    val lastPersisted: String,
    val scrollPx: Int = 0,
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
)
