package com.tabletaide.ide.data

data class GitHubUser(
    val login: String,
    val name: String?,
    val avatarUrl: String?,
)

data class GitHubRepo(
    val id: Long,
    val name: String,
    val fullName: String,
    val cloneUrl: String,
    val isPrivate: Boolean,
    val updatedAt: String,
    val description: String?,
)

data class GitHubSessionState(
    val signedIn: Boolean = false,
    val login: String? = null,
    val displayName: String? = null,
)

data class GitHubRepoListState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val repos: List<GitHubRepo> = emptyList(),
    val errorMessage: String? = null,
)

data class GitHubOAuthUiState(
    val oauthConfigured: Boolean = false,
    val authorizing: Boolean = false,
    val session: GitHubSessionState = GitHubSessionState(),
    val repoList: GitHubRepoListState = GitHubRepoListState(),
    val errorMessage: String? = null,
)
