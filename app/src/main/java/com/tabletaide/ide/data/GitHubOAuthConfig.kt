package com.tabletaide.ide.data

object GitHubOAuthConfig {
    const val HOST = "github.com"
    const val REDIRECT_SCHEME = "com.tabletaide.ide"
    const val REDIRECT_HOST = "oauth"
    const val REDIRECT_PATH = "/github"
    const val REDIRECT_URI = "$REDIRECT_SCHEME://$REDIRECT_HOST$REDIRECT_PATH"
    const val SCOPE = "repo"
}
