package com.tabletaide.ide.data

import android.content.Context
import com.tabletaide.ide.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubOAuthSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getClientId(): String {
        val stored = prefs.getString(KEY_CLIENT_ID, "").orEmpty().trim()
        if (stored.isNotBlank()) return stored
        return BuildConfig.GITHUB_OAUTH_CLIENT_ID.trim()
    }

    fun setClientId(clientId: String) {
        prefs.edit().putString(KEY_CLIENT_ID, clientId.trim()).apply()
    }

    fun isConfigured(): Boolean = getClientId().isNotBlank()

    companion object {
        private const val PREFS = "kinetic_github_oauth_settings"
        private const val KEY_CLIENT_ID = "oauth_client_id"
    }
}
