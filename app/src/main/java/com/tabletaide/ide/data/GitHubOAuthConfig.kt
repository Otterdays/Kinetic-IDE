package com.tabletaide.ide.data

import com.tabletaide.ide.BuildConfig

object GitHubOAuthConfig {
    const val HOST = "github.com"
    const val REDIRECT_SCHEME = "com.tabletaide.ide"
    const val REDIRECT_HOST = "oauth"
    const val REDIRECT_PATH = "/github"
    const val REDIRECT_URI = "$REDIRECT_SCHEME://$REDIRECT_HOST$REDIRECT_PATH"
    const val SCOPE = "repo"

    val clientId: String get() = BuildConfig.GITHUB_OAUTH_CLIENT_ID.trim()

    fun isConfigured(): Boolean = clientId.isNotBlank()
}
