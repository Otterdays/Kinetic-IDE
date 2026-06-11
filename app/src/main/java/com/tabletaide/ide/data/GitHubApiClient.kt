package com.tabletaide.ide.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubApiClient @Inject constructor(
    private val httpClient: OkHttpClient,
    private val gitAuthStore: GitAuthStore,
) {
    suspend fun listAccessibleRepos(): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        val token = gitAuthStore.load(GitHubOAuthConfig.HOST)?.token.orEmpty()
        if (token.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Sign in with GitHub first."))
        }
        val repos = mutableListOf<GitHubRepo>()
        var page = 1
        while (page <= MAX_PAGES) {
            val request = Request.Builder()
                .url(
                    "https://api.github.com/user/repos" +
                        "?per_page=$PAGE_SIZE&page=$page&sort=updated&direction=desc" +
                        "&affiliation=owner,collaborator,organization_member",
                )
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/vnd.github+json")
                .get()
                .build()
            val pageRepos = try {
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("GitHub repo list failed (HTTP ${response.code})."),
                        )
                    }
                    GitHubApiParser.parseRepos(body)
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
            if (pageRepos.isEmpty()) break
            repos += pageRepos
            if (pageRepos.size < PAGE_SIZE) break
            page++
        }
        Result.success(repos.distinctBy { it.id })
    }

    companion object {
        private const val PAGE_SIZE = 100
        private const val MAX_PAGES = 5
    }
}
