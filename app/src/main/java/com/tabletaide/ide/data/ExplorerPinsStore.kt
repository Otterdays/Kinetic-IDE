package com.tabletaide.ide.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS = "kinetic_explorer_pins"
private const val MAX_RECENTS = 18

/**
 * Per-workspace recent files + starred favorites for the explorer rail.
 * // [TRACE: DOCS/ROADMAP.md] Epic 1.2 favorites / recents MVP
 */
@Singleton
class ExplorerPinsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private var workspaceKey: String = ""

    private val _recents = MutableStateFlow<List<String>>(emptyList())
    val recents: StateFlow<List<String>> = _recents.asStateFlow()

    private val _favorites = MutableStateFlow<List<String>>(emptyList())
    val favorites: StateFlow<List<String>> = _favorites.asStateFlow()

    fun bindWorkspace(rootUri: Uri?) {
        workspaceKey = rootUri?.toString().orEmpty()
        _recents.value = loadRecents()
        _favorites.value = loadFavorites().sorted()
    }

    fun recordFileOpened(relativePath: String) {
        if (workspaceKey.isEmpty() || relativePath.isBlank()) return
        val path = relativePath.trim('/')
        val list = loadRecents().toMutableList()
        list.remove(path)
        list.add(0, path)
        while (list.size > MAX_RECENTS) list.removeAt(list.lastIndex)
        saveRecents(list)
        _recents.value = list
    }

    fun toggleFavorite(relativePath: String) {
        if (workspaceKey.isEmpty() || relativePath.isBlank()) return
        val path = relativePath.trim('/')
        val set = loadFavorites().toMutableSet()
        if (!set.add(path)) set.remove(path)
        saveFavorites(set)
        _favorites.value = set.toList().sorted()
    }

    fun isFavorite(relativePath: String): Boolean =
        relativePath.trim('/') in loadFavorites()

    fun renameTrackedPath(oldPath: String, newPath: String) {
        if (workspaceKey.isEmpty()) return
        val oldTrim = oldPath.trim('/')
        val newTrim = newPath.trim('/')
        val oldPref = "$oldTrim/"
        fun mapOne(p: String): String = when {
            p == oldTrim -> newTrim
            p.startsWith(oldPref) -> newTrim + "/" + p.removePrefix(oldPref)
            else -> p
        }
        saveRecents(loadRecents().map(::mapOne).distinct())
        saveFavorites(loadFavorites().map(::mapOne).toSet())
        _recents.value = loadRecents()
        _favorites.value = loadFavorites().sorted()
    }

    fun removeTrackedPath(path: String, isDirectory: Boolean) {
        if (workspaceKey.isEmpty()) return
        val trim = path.trim('/')
        val pref = "$trim/"
        fun removeMatch(p: String): Boolean =
            if (isDirectory) p == trim || p.startsWith(pref) else p == trim
        saveRecents(loadRecents().filterNot(::removeMatch))
        saveFavorites(loadFavorites().filterNot(::removeMatch).toSet())
        _recents.value = loadRecents()
        _favorites.value = loadFavorites().sorted()
    }

    private fun keyRecents(): String = "recents_${workspaceKey.hashCode()}"
    private fun keyFavs(): String = "favs_${workspaceKey.hashCode()}"

    private fun loadRecents(): List<String> {
        val raw = prefs.getString(keyRecents(), null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    add(arr.getString(i))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveRecents(paths: List<String>) {
        val arr = JSONArray()
        paths.forEach { arr.put(it) }
        prefs.edit().putString(keyRecents(), arr.toString()).apply()
    }

    private fun loadFavorites(): Set<String> {
        val raw = prefs.getString(keyFavs(), null) ?: return emptySet()
        return try {
            val arr = JSONArray(raw)
            buildSet {
                for (i in 0 until arr.length()) {
                    add(arr.getString(i))
                }
            }
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun saveFavorites(set: Set<String>) {
        val arr = JSONArray()
        set.sorted().forEach { arr.put(it) }
        prefs.edit().putString(keyFavs(), arr.toString()).apply()
    }
}
