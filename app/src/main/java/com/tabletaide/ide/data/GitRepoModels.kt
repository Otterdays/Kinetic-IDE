package com.tabletaide.ide.data

import java.io.File

sealed interface GitRepositoryResolution

data class GitRepositoryReady(
    val workTree: File,
    val gitDir: File,
    val repoName: String,
) : GitRepositoryResolution

data class GitRepositoryUnavailable(
    val userMessage: String,
) : GitRepositoryResolution

data class GitIdentity(
    val name: String,
    val email: String,
)

data class GitStatusSnapshot(
    val repoName: String,
    val branchName: String,
    val upstreamBranch: String?,
    val upstreamRemoteName: String?,
    val upstreamRemoteUrl: String?,
    val aheadCount: Int,
    val behindCount: Int,
    val stagedPaths: List<String>,
    val unstagedPaths: List<String>,
    val untrackedPaths: List<String>,
    val identity: GitIdentity,
    val clean: Boolean,
) {
    val hasTrackedUpstream: Boolean =
        !upstreamBranch.isNullOrBlank() &&
            !upstreamRemoteName.isNullOrBlank() &&
            !upstreamRemoteUrl.isNullOrBlank()
    val changedFileCount: Int = (stagedPaths + unstagedPaths + untrackedPaths).distinct().size
    val canCommit: Boolean = !clean
    val canPush: Boolean = hasTrackedUpstream
}

data class GitRepoUiState(
    val available: Boolean = false,
    val repoName: String = "",
    val branchName: String = "main",
    val upstreamBranch: String? = null,
    val aheadCount: Int = 0,
    val behindCount: Int = 0,
    val stagedCount: Int = 0,
    val unstagedCount: Int = 0,
    val untrackedCount: Int = 0,
    val clean: Boolean = true,
    val canCommit: Boolean = false,
    val canPush: Boolean = false,
    val message: String? = null,
) {
    companion object {
        fun fromSnapshot(snapshot: GitStatusSnapshot): GitRepoUiState {
            return GitRepoUiState(
                available = true,
                repoName = snapshot.repoName,
                branchName = snapshot.branchName,
                upstreamBranch = snapshot.upstreamBranch,
                aheadCount = snapshot.aheadCount,
                behindCount = snapshot.behindCount,
                stagedCount = snapshot.stagedPaths.size,
                unstagedCount = snapshot.unstagedPaths.size,
                untrackedCount = snapshot.untrackedPaths.size,
                clean = snapshot.clean,
                canCommit = snapshot.canCommit,
                canPush = snapshot.canPush,
            )
        }
    }
}

data class GitCommitRequest(
    val repository: GitRepositoryReady,
    val message: String,
    val author: GitIdentity,
    val stageUntracked: Boolean,
)

data class GitCommitResult(
    val commitId: String,
    val shortCommitId: String,
    val branchName: String,
    val message: String,
)

data class GitPushRequest(
    val repository: GitRepositoryReady,
    val branchName: String,
    val upstreamBranch: String,
    val remoteName: String,
    val remoteUrl: String,
)

data class GitPushResult(
    val branchName: String,
    val upstreamBranch: String,
    val remoteName: String,
    val message: String,
)

data class GitCommitDialogState(
    val visible: Boolean = false,
    val draftMessage: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val stageUntracked: Boolean = true,
    val busy: Boolean = false,
    val generatingMessage: Boolean = false,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
)
