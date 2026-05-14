package com.tabletaide.ide.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitCloneService @Inject constructor(
    private val authStore: GitAuthStore,
) {
    suspend fun clone(
        request: GitCloneRequest,
        onProgress: (String) -> Unit = {},
    ): GitCloneResult = withContext(Dispatchers.IO) {
        try {
            onProgress("Connecting to ${request.remote.host}…")
            Git.cloneRepository()
                .setURI(request.remote.normalizedUrl)
                .setDirectory(request.target.repoDirectory)
                .setCloneSubmodules(false)
                .setCredentialsProvider(
                    UsernamePasswordCredentialsProvider(
                        request.username.trim(),
                        request.token.trim(),
                    ),
                )
                .setProgressMonitor(CloneProgressMonitor(onProgress))
                .call()
                .use { }
            if (request.saveAuth) {
                authStore.save(
                    GitAuthEntry(
                        host = request.remote.host,
                        username = request.username.trim(),
                        token = request.token.trim(),
                    ),
                )
            }
            GitCloneSuccess(
                remote = request.remote,
                repoDirectory = request.target.repoDirectory,
            )
        } catch (e: InvalidRemoteException) {
            cleanupFailedClone(request.target.repoDirectory)
            GitCloneFailure(
                GitCloneValidationError(
                    "Repository not found or URL is invalid for ${request.remote.host}.",
                ),
            )
        } catch (e: TransportException) {
            cleanupFailedClone(request.target.repoDirectory)
            GitCloneFailure(mapTransportError(e))
        } catch (e: IOException) {
            cleanupFailedClone(request.target.repoDirectory)
            GitCloneFailure(
                GitCloneStorageError(
                    "Clone failed while writing to device storage. Check the destination folder and try again.",
                ),
            )
        } catch (e: GitAPIException) {
            cleanupFailedClone(request.target.repoDirectory)
            GitCloneFailure(
                GitCloneUnknownError(
                    "Git clone failed before the repository could be opened.",
                ),
            )
        } catch (e: Exception) {
            cleanupFailedClone(request.target.repoDirectory)
            GitCloneFailure(
                GitCloneUnknownError(
                    "Git clone failed unexpectedly. Try again or pick a different destination folder.",
                ),
            )
        }
    }

    private fun mapTransportError(error: TransportException): GitCloneError {
        val message = error.message?.lowercase().orEmpty()
        return when {
            "not authorized" in message || "auth" in message || "401" in message || "403" in message ->
                GitCloneAuthError(
                    "Authentication failed. Check the token and username for this repository host.",
                )
            "unknownhost" in message || "timed out" in message || "timeout" in message ->
                GitCloneNetworkError(
                    "Network error while contacting the git host. Check connectivity and try again.",
                )
            "not found" in message ->
                GitCloneValidationError(
                    "Repository not found. Check the HTTPS URL and your access level.",
                )
            else -> GitCloneNetworkError(
                "Transport error during clone. Check connectivity, credentials, and repository access.",
            )
        }
    }

    private fun cleanupFailedClone(repoDirectory: File) {
        if (!repoDirectory.exists()) return
        repoDirectory.deleteRecursively()
    }
}

private class CloneProgressMonitor(
    private val onProgress: (String) -> Unit,
) : ProgressMonitor {
    private var lastTask: String = ""

    override fun start(totalTasks: Int) {
        onProgress("Starting clone…")
    }

    override fun beginTask(title: String?, totalWork: Int) {
        lastTask = title?.trim().orEmpty()
        if (lastTask.isNotEmpty()) {
            onProgress(lastTask)
        }
    }

    override fun update(completed: Int) = Unit

    override fun endTask() {
        if (lastTask.isNotEmpty()) {
            onProgress("$lastTask complete")
        }
    }

    override fun showDuration(enabled: Boolean) = Unit

    override fun isCancelled(): Boolean = false
}
