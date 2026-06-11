package com.tabletaide.ide.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubSessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveSession(user: GitHubUser) {
        prefs.edit()
            .putString(KEY_LOGIN, user.login)
            .putString(KEY_DISPLAY_NAME, user.name?.trim().orEmpty().ifBlank { user.login })
            .apply()
    }

    fun loadSession(gitAuthStore: GitAuthStore): GitHubSessionState {
        val token = gitAuthStore.load(GitHubOAuthConfig.HOST)?.token.orEmpty()
        if (token.isBlank()) {
            return GitHubSessionState(signedIn = false)
        }
        val login = prefs.getString(KEY_LOGIN, null)?.trim().orEmpty()
        val displayName = prefs.getString(KEY_DISPLAY_NAME, null)?.trim().orEmpty()
        return GitHubSessionState(
            signedIn = true,
            login = login.ifBlank { null },
            displayName = displayName.ifBlank { login.ifBlank { null } },
        )
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_LOGIN)
            .remove(KEY_DISPLAY_NAME)
            .apply()
    }

    companion object {
        private const val PREFS = "kinetic_github_session"
        private const val KEY_LOGIN = "login"
        private const val KEY_DISPLAY_NAME = "display_name"
    }
}
