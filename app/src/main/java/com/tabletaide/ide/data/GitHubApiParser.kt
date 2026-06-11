package com.tabletaide.ide.data

import org.json.JSONArray
import org.json.JSONObject

internal object GitHubApiParser {
    fun parseUser(json: String): GitHubUser {
        val root = JSONObject(json)
        val login = root.optString("login").trim()
        val name = root.optString("name").trim().ifEmpty { null }
        val avatarUrl = root.optString("avatar_url").trim().ifEmpty { null }
        return GitHubUser(login = login, name = name, avatarUrl = avatarUrl)
    }

    fun parseRepos(json: String): List<GitHubRepo> {
        val array = JSONArray(json)
        val repos = mutableListOf<GitHubRepo>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optLong("id")
            val name = item.optString("name").trim()
            val fullName = item.optString("full_name").trim()
            val cloneUrl = item.optString("clone_url").trim()
            if (name.isEmpty() || fullName.isEmpty() || cloneUrl.isEmpty()) continue
            repos.add(
                GitHubRepo(
                    id = id,
                    name = name,
                    fullName = fullName,
                    cloneUrl = cloneUrl,
                    isPrivate = item.optBoolean("private"),
                    updatedAt = item.optString("updated_at").trim(),
                    description = item.optString("description").trim().ifEmpty { null },
                ),
            )
        }
        return repos.sortedBy { it.fullName.lowercase() }
    }
}
