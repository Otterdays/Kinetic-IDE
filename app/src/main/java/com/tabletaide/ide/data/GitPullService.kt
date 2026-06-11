package com.tabletaide.ide.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitPullService @Inject constructor(
    private val authStore: GitAuthStore,
) {
    suspend fun pull(
        request: GitPullRequest,
        onProgress: (String) -> Unit = {},
    ): Result<GitPullResult> = withContext(Dispatchers.IO) {
        val remoteSpec = GitRemoteSpec.parseHttps(request.remoteUrl)
            ?: return@withContext Result.failure(
                IllegalStateException("Only HTTPS remotes are supported for pull in this version."),
            )
        val auth = authStore.load(remoteSpec.host)
            ?: return@withContext Result.failure(
                IllegalStateException("No saved git auth for ${remoteSpec.host}. Save credentials first."),
            )
        try {
            Git.open(request.repository.workTree).use { git ->
                onProgress("Pulling ${request.upstreamBranch} from ${request.remoteName}…")
                val pullResult = git.pull()
                    .setRemote(request.remoteName)
                    .setCredentialsProvider(
                        UsernamePasswordCredentialsProvider(auth.username, auth.token),
                    )
                    .call()
                if (!pullResult.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Pull did not complete. Check the remote and try again."),
                    )
                }
                val mergeStatus = pullResult.mergeResult?.mergeStatus
                when (mergeStatus) {
                    null,
                    MergeResult.MergeStatus.ALREADY_UP_TO_DATE,
                    ->
                        Result.success(
                            GitPullResult(
                                branchName = request.branchName,
                                message = "Already up to date with ${request.remoteName}/${request.upstreamBranch}.",
                            ),
                        )
                    MergeResult.MergeStatus.FAST_FORWARD,
                    MergeResult.MergeStatus.MERGED,
                    ->
                        Result.success(
                            GitPullResult(
                                branchName = request.branchName,
                                message = "Pulled latest from ${request.remoteName}/${request.upstreamBranch}.",
                            ),
                        )
                    MergeResult.MergeStatus.CONFLICTING ->
                        Result.failure(
                            IllegalStateException(
                                "Pull stopped due to merge conflicts. Resolve conflicts outside Kinetic, then retry.",
                            ),
                        )
                    else ->
                        Result.failure(
                            IllegalStateException(
                                "Pull did not complete ($mergeStatus). Check the repo and try again.",
                            ),
                        )
                }
            }
        } catch (e: TransportException) {
            Result.failure(IllegalStateException(mapTransportError(e)))
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message?.takeIf { it.isNotBlank() }
                        ?: "Pull failed. Check network access, auth, and upstream tracking.",
                ),
            )
        }
    }

    private fun mapTransportError(error: TransportException): String {
        val message = error.message?.lowercase().orEmpty()
        return when {
            "not authorized" in message || "auth" in message || "401" in message || "403" in message ->
                "Pull authentication failed. Check the saved token for this git host."
            "unknownhost" in message || "timeout" in message || "timed out" in message ->
                "Network error while pulling from the git host. Check connectivity and try again."
            else ->
                "Pull transport failed. Check the remote, network, and saved auth, then try again."
        }
    }
}

object GitPushFailures {
    fun isNonFastForward(message: String?): Boolean {
        val normalized = message?.lowercase().orEmpty()
        return "nonfastforward" in normalized ||
            "non-fast-forward" in normalized ||
            "remote branch moved ahead" in normalized
    }
}
