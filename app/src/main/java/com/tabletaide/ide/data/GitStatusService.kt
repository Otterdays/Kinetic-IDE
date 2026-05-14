package com.tabletaide.ide.data

import com.tabletaide.ide.IdeConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.BranchConfig
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.Repository.shortenRefName
import org.eclipse.jgit.lib.TextProgressMonitor
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitStatusService @Inject constructor() {
    suspend fun loadStatus(repository: GitRepositoryReady): Result<GitStatusSnapshot> =
        withContext(Dispatchers.IO) {
            try {
                Git.open(repository.workTree).use { git ->
                    val repo = git.repository
                    val status = git.status().call()
                    val branchName = repo.currentBranchName()
                    val branchConfig = BranchConfig(repo.config, branchName)
                    val upstreamRef = branchConfig.merge
                    val upstreamBranch = upstreamRef?.let(::shortenRefName)
                    val remoteName = branchConfig.remote?.takeIf { it.isNotBlank() }
                    val remoteUrl = remoteName?.let { repo.config.getString("remote", it, "url") }
                    val tracking = org.eclipse.jgit.lib.BranchTrackingStatus.of(repo, branchName)
                    val stagedPaths = (
                        status.added +
                            status.changed +
                            status.removed
                        ).distinct().sorted()
                    val unstagedPaths = (
                        status.modified +
                            status.missing +
                            status.conflicting
                        ).distinct().sorted()
                    val untrackedPaths = status.untracked.distinct().sorted()
                    val identity = GitIdentity(
                        name = repo.config.getString("user", null, "name").orEmpty(),
                        email = repo.config.getString("user", null, "email").orEmpty(),
                    )
                    Result.success(
                        GitStatusSnapshot(
                            repoName = repository.repoName,
                            branchName = branchName,
                            upstreamBranch = upstreamBranch,
                            upstreamRemoteName = remoteName,
                            upstreamRemoteUrl = remoteUrl,
                            aheadCount = tracking?.aheadCount ?: 0,
                            behindCount = tracking?.behindCount ?: 0,
                            stagedPaths = stagedPaths,
                            unstagedPaths = unstagedPaths,
                            untrackedPaths = untrackedPaths,
                            identity = identity,
                            clean = stagedPaths.isEmpty() && unstagedPaths.isEmpty() && untrackedPaths.isEmpty(),
                        ),
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun buildAiContext(
        repository: GitRepositoryReady,
        snapshot: GitStatusSnapshot,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Git.open(repository.workTree).use { git ->
                val repo = git.repository
                val builder = StringBuilder().apply {
                    appendLine("Repository: ${snapshot.repoName}")
                    appendLine("Branch: ${snapshot.branchName}")
                    appendLine("Tracked upstream: ${snapshot.upstreamBranch ?: "none"}")
                    appendLine("Ahead: ${snapshot.aheadCount}")
                    appendLine("Behind: ${snapshot.behindCount}")
                    appendLine()
                    appendLine("Staged files (${snapshot.stagedPaths.size}):")
                    appendLine(formatPathList(snapshot.stagedPaths))
                    appendLine()
                    appendLine("Unstaged files (${snapshot.unstagedPaths.size}):")
                    appendLine(formatPathList(snapshot.unstagedPaths))
                    appendLine()
                    appendLine("Untracked files (${snapshot.untrackedPaths.size}):")
                    appendLine(formatPathList(snapshot.untrackedPaths))
                }
                val stagedDiff = formatDiffEntries(
                    repo = repo,
                    entries = git.diff().setCached(true).call(),
                )
                val unstagedDiff = formatDiffEntries(
                    repo = repo,
                    entries = git.diff().call(),
                )
                if (stagedDiff.isNotBlank()) {
                    builder.appendLine()
                    builder.appendLine("Staged diff excerpt:")
                    builder.append(stagedDiff)
                }
                if (unstagedDiff.isNotBlank()) {
                    builder.appendLine()
                    builder.appendLine()
                    builder.appendLine("Unstaged diff excerpt:")
                    builder.append(unstagedDiff)
                }
                Result.success(builder.toString().take(IdeConstants.GIT_COMMIT_PROMPT_MAX_CHARS))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Repository.currentBranchName(): String {
        val fullBranch = fullBranch
        return if (fullBranch.isNullOrBlank()) {
            "HEAD"
        } else {
            shortenRefName(fullBranch)
        }
    }

    private fun formatPathList(paths: List<String>): String {
        if (paths.isEmpty()) return "- none"
        return paths.take(IdeConstants.GIT_COMMIT_PATH_LIMIT).joinToString(separator = "\n") { "- $it" }
    }

    private fun formatDiffEntries(
        repo: Repository,
        entries: List<DiffEntry>,
    ): String {
        if (entries.isEmpty()) return ""
        val output = StringBuilder()
        entries.take(IdeConstants.GIT_COMMIT_DIFF_FILE_LIMIT).forEachIndexed { index, entry ->
            val rendered = formatSingleDiff(repo, entry) ?: return@forEachIndexed
            if (index > 0) output.appendLine()
            val remaining = IdeConstants.GIT_COMMIT_DIFF_CHAR_LIMIT - output.length
            if (remaining <= 0) return@forEachIndexed
            output.append(rendered.take(remaining))
        }
        return output.toString().take(IdeConstants.GIT_COMMIT_DIFF_CHAR_LIMIT)
    }

    private fun formatSingleDiff(repo: Repository, entry: DiffEntry): String? {
        val bytes = ByteArrayOutputStream()
        return try {
            DiffFormatter(bytes).use { formatter ->
                formatter.setRepository(repo)
                formatter.setDiffComparator(RawTextComparator.DEFAULT)
                formatter.isDetectRenames = true
                formatter.format(entry)
            }
            bytes.toString(Charsets.UTF_8.name())
        } catch (_: Exception) {
            null
        }
    }
}
