package com.tabletaide.ide.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitPushService @Inject constructor(
    private val authStore: GitAuthStore,
) {
    suspend fun push(
        request: GitPushRequest,
        onProgress: (String) -> Unit = {},
    ): Result<GitPushResult> = withContext(Dispatchers.IO) {
        val remoteSpec = GitRemoteSpec.parseHttps(request.remoteUrl)
            ?: return@withContext Result.failure(
                IllegalStateException("Only HTTPS remotes are supported for push in this version."),
            )
        val auth = authStore.load(remoteSpec.host)
            ?: return@withContext Result.failure(
                IllegalStateException("No saved git auth for ${remoteSpec.host}. Clone or save auth for this host first."),
            )
        try {
            Git.open(request.repository.workTree).use { git ->
                onProgress("Pushing ${request.branchName}…")
                val localRef = "refs/heads/${request.branchName}"
                val remoteRef = "refs/heads/${request.upstreamBranch}"
                val results = git.push()
                    .setRemote(request.remoteName)
                    .setCredentialsProvider(
                        UsernamePasswordCredentialsProvider(auth.username, auth.token),
                    )
                    .setRefSpecs(RefSpec("$localRef:$remoteRef"))
                    .call()
                val failingUpdate = results
                    .flatMap { it.remoteUpdates }
                    .firstOrNull { it.status !in successfulStatuses }
                if (failingUpdate != null) {
                    return@withContext Result.failure(
                        IllegalStateException(mapPushFailure(failingUpdate)),
                    )
                }
                Result.success(
                    GitPushResult(
                        branchName = request.branchName,
                        upstreamBranch = request.upstreamBranch,
                        remoteName = request.remoteName,
                        message = "Pushed ${request.branchName} to ${request.remoteName}/${request.upstreamBranch}.",
                    ),
                )
            }
        } catch (e: TransportException) {
            Result.failure(
                IllegalStateException(mapTransportError(e)),
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message?.takeIf { it.isNotBlank() }
                        ?: "Push failed. Check network access, auth, and upstream tracking.",
                ),
            )
        }
    }

    private fun mapTransportError(error: TransportException): String {
        val message = error.message?.lowercase().orEmpty()
        return when {
            "not authorized" in message || "auth" in message || "401" in message || "403" in message ->
                "Push authentication failed. Check the saved token for this git host."
            "unknownhost" in message || "timeout" in message || "timed out" in message ->
                "Network error while pushing to the git host. Check connectivity and try again."
            else ->
                "Push transport failed. Check the remote, network, and saved auth, then try again."
        }
    }

    private fun mapPushFailure(update: RemoteRefUpdate): String {
        return when (update.status) {
            RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD ->
                "Push was rejected because the remote branch moved ahead. Pull or sync first."
            RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED ->
                "Push was rejected because the remote ref changed. Refresh the branch and try again."
            RemoteRefUpdate.Status.REJECTED_NODELETE ->
                "Push was rejected by the remote server."
            RemoteRefUpdate.Status.REJECTED_OTHER_REASON ->
                update.message?.takeIf { it.isNotBlank() }
                    ?: "Push was rejected by the remote server."
            else ->
                "Push failed with status ${update.status}."
        }
    }

    private companion object {
        val successfulStatuses = setOf(
            RemoteRefUpdate.Status.OK,
            RemoteRefUpdate.Status.UP_TO_DATE,
        )
    }
}
