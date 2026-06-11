package com.tabletaide.ide.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubOAuthService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    private val gitAuthStore: GitAuthStore,
    private val sessionStore: GitHubSessionStore,
    private val oauthSettings: GitHubOAuthSettingsStore,
) {
    private val pendingPrefs get() =
        context.getSharedPreferences(PENDING_PREFS, Context.MODE_PRIVATE)

    @Volatile
    private var pendingVerifier: String? = null

    @Volatile
    private var pendingState: String? = null

    fun beginAuthorization(): Result<Uri> {
        if (!oauthSettings.isConfigured()) {
            return Result.failure(
                IllegalStateException(
                    "GitHub OAuth client ID is not set. Paste your OAuth App Client ID below.",
                ),
            )
        }
        val verifier = GitHubPkce.generateVerifier()
        val state = GitHubPkce.generateState()
        val challenge = GitHubPkce.challengeForVerifier(verifier)
        storePendingAuthorization(verifier, state)
        val uri = Uri.parse("https://github.com/login/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", oauthSettings.getClientId())
            .appendQueryParameter("redirect_uri", GitHubOAuthConfig.REDIRECT_URI)
            .appendQueryParameter("scope", GitHubOAuthConfig.SCOPE)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
        return Result.success(uri)
    }

    suspend fun completeAuthorization(callbackUri: Uri): Result<GitHubUser> = withContext(Dispatchers.IO) {
        if (callbackUri.scheme != GitHubOAuthConfig.REDIRECT_SCHEME ||
            callbackUri.host != GitHubOAuthConfig.REDIRECT_HOST
        ) {
            return@withContext Result.failure(IllegalArgumentException("Not a GitHub OAuth callback."))
        }
        val error = callbackUri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            val description = callbackUri.getQueryParameter("error_description").orEmpty()
            return@withContext Result.failure(
                IllegalStateException(description.ifBlank { "GitHub sign-in was denied ($error)." }),
            )
        }
        val code = callbackUri.getQueryParameter("code")?.trim().orEmpty()
        val state = callbackUri.getQueryParameter("state")?.trim().orEmpty()
        val expectedState = pendingState ?: loadPendingState()
        val verifier = pendingVerifier ?: loadPendingVerifier()
        clearPendingAuthorization()
        if (code.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("GitHub OAuth callback missing code."))
        }
        if (expectedState.isNullOrBlank() || state != expectedState) {
            return@withContext Result.failure(IllegalStateException("GitHub OAuth state mismatch."))
        }
        if (verifier.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("GitHub OAuth session expired. Try again."))
        }
        val token = exchangeCodeForToken(code, verifier).getOrElse { return@withContext Result.failure(it) }
        val user = fetchCurrentUser(token).getOrElse { return@withContext Result.failure(it) }
        gitAuthStore.save(
            GitAuthEntry(
                host = GitHubOAuthConfig.HOST,
                username = user.login,
                token = token,
            ),
        )
        sessionStore.saveSession(user)
        Result.success(user)
    }

    fun currentSession(): GitHubSessionState = sessionStore.loadSession(gitAuthStore)

    fun signOut() {
        gitAuthStore.clear(GitHubOAuthConfig.HOST)
        sessionStore.clear()
        clearPendingAuthorization()
    }

    fun cancelPendingAuthorization() {
        clearPendingAuthorization()
    }

    private fun storePendingAuthorization(verifier: String, state: String) {
        pendingVerifier = verifier
        pendingState = state
        pendingPrefs.edit()
            .putString(KEY_PENDING_VERIFIER, verifier)
            .putString(KEY_PENDING_STATE, state)
            .apply()
    }

    private fun loadPendingVerifier(): String? =
        pendingPrefs.getString(KEY_PENDING_VERIFIER, null)?.trim()?.takeIf { it.isNotEmpty() }

    private fun loadPendingState(): String? =
        pendingPrefs.getString(KEY_PENDING_STATE, null)?.trim()?.takeIf { it.isNotEmpty() }

    private fun clearPendingAuthorization() {
        pendingState = null
        pendingVerifier = null
        pendingPrefs.edit()
            .remove(KEY_PENDING_VERIFIER)
            .remove(KEY_PENDING_STATE)
            .apply()
    }

    companion object {
        private const val PENDING_PREFS = "kinetic_github_oauth_pending"
        private const val KEY_PENDING_VERIFIER = "verifier"
        private const val KEY_PENDING_STATE = "state"
    }

    private fun exchangeCodeForToken(code: String, verifier: String): Result<String> {
        val clientId = oauthSettings.getClientId()
        if (clientId.isBlank()) {
            return Result.failure(IllegalStateException("GitHub OAuth client ID is not configured."))
        }
        val body = FormBody.Builder()
            .add("client_id", clientId)
            .add("redirect_uri", GitHubOAuthConfig.REDIRECT_URI)
            .add("code", code)
            .add("code_verifier", verifier)
            .build()
        val request = Request.Builder()
            .url("https://github.com/login/oauth/access_token")
            .addHeader("Accept", "application/json")
            .post(body)
            .build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.failure(
                        IllegalStateException("GitHub token exchange failed (HTTP ${response.code})."),
                    )
                }
                val json = JSONObject(responseBody)
                val error = json.optString("error").trim()
                if (error.isNotEmpty()) {
                    val description = json.optString("error_description", error)
                    val message = if (description.contains("client_id", ignoreCase = true)) {
                        "$description Check GitHub → Settings → Developer settings → OAuth Apps " +
                            "(not GitHub Apps) and confirm the Client ID matches this build."
                    } else {
                        description
                    }
                    return Result.failure(IllegalStateException(message))
                }
                val token = json.optString("access_token").trim()
                if (token.isEmpty()) {
                    return Result.failure(IllegalStateException("GitHub token response missing access_token."))
                }
                Result.success(token)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchCurrentUser(accessToken: String): Result<GitHubUser> {
        val request = Request.Builder()
            .url("https://api.github.com/user")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Accept", "application/vnd.github+json")
            .get()
            .build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.failure(
                        IllegalStateException("GitHub user profile failed (HTTP ${response.code})."),
                    )
                }
                Result.success(GitHubApiParser.parseUser(body))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
