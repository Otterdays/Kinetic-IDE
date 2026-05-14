package com.tabletaide.ide.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository.shortenRefName
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitCommitService @Inject constructor(
    private val identityStore: GitIdentityStore,
) {
    suspend fun commit(
        request: GitCommitRequest,
        onProgress: (String) -> Unit = {},
    ): Result<GitCommitResult> = withContext(Dispatchers.IO) {
        val message = request.message.trim()
        val authorName = request.author.name.trim()
        val authorEmail = request.author.email.trim()
        if (message.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Enter a commit message first."))
        }
        if (authorName.isBlank() || authorEmail.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Enter an author name and email before committing."),
            )
        }
        try {
            Git.open(request.repository.workTree).use { git ->
                onProgress("Staging changes…")
                stageAllChanges(git, request.stageUntracked)
                val status = git.status().call()
                val hasStagedChanges = listOf(
                    status.added,
                    status.changed,
                    status.removed,
                ).any { it.isNotEmpty() }
                if (!hasStagedChanges) {
                    return@withContext Result.failure(
                        IllegalStateException("No staged changes are available to commit."),
                    )
                }
                onProgress("Writing commit…")
                val commit = git.commit()
                    .setMessage(message)
                    .setAuthor(authorName, authorEmail)
                    .setCommitter(authorName, authorEmail)
                    .call()
                git.repository.config.apply {
                    setString("user", null, "name", authorName)
                    setString("user", null, "email", authorEmail)
                    save()
                }
                identityStore.save(GitIdentity(authorName, authorEmail))
                val branchName = git.repository.fullBranch?.let(::shortenRefName).orEmpty().ifBlank { "HEAD" }
                Result.success(
                    GitCommitResult(
                        commitId = commit.name,
                        shortCommitId = commit.name.take(7),
                        branchName = branchName,
                        message = message,
                    ),
                )
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message?.takeIf { it.isNotBlank() }
                        ?: "Commit failed. Check git identity and repository state, then try again.",
                ),
            )
        }
    }

    private fun stageAllChanges(git: Git, includeUntracked: Boolean) {
        if (includeUntracked) {
            git.add().addFilepattern(".").call()
        }
        git.add().setUpdate(true).addFilepattern(".").call()
    }
}
