package com.tabletaide.ide.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubOAuthService @Inject constructor(
    private val httpClient: OkHttpClient,
    private val gitAuthStore: GitAuthStore,
    private val sessionStore: GitHubSessionStore,
) {
    @Volatile
    private var pendingVerifier: String? = null

    @Volatile
    private var pendingState: String? = null

    fun beginAuthorization(): Result<Uri> {
        if (!GitHubOAuthConfig.isConfigured()) {
            return Result.failure(
                IllegalStateException(
                    "GitHub OAuth is not configured. Add githubOAuthClientId to local.properties.",
                ),
            )
        }
        val verifier = GitHubPkce.generateVerifier()
        val state = GitHubPkce.generateState()
        val challenge = GitHubPkce.challengeForVerifier(verifier)
        pendingVerifier = verifier
        pendingState = state
        val uri = Uri.parse("https://github.com/login/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", GitHubOAuthConfig.clientId)
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
        val expectedState = pendingState
        val verifier = pendingVerifier
        pendingState = null
        pendingVerifier = null
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
        pendingState = null
        pendingVerifier = null
    }

    private fun exchangeCodeForToken(code: String, verifier: String): Result<String> {
        val body = JSONObject()
            .put("client_id", GitHubOAuthConfig.clientId)
            .put("redirect_uri", GitHubOAuthConfig.REDIRECT_URI)
            .put("code", code)
            .put("code_verifier", verifier)
            .toString()
        val request = Request.Builder()
            .url("https://github.com/login/oauth/access_token")
            .addHeader("Accept", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
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
                    return Result.failure(IllegalStateException(description))
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
