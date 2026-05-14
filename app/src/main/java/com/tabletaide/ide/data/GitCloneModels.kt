package com.tabletaide.ide.data

import android.net.Uri
import java.io.File
import java.net.URI

data class GitRemoteSpec(
    val normalizedUrl: String,
    val host: String,
    val repoName: String,
) {
    companion object {
        fun parseHttps(rawUrl: String): GitRemoteSpec? {
            val trimmed = rawUrl.trim()
            if (!trimmed.startsWith("https://", ignoreCase = true)) return null
            val parsed = try {
                URI(trimmed)
            } catch (_: Exception) {
                return null
            }
            if (!parsed.userInfo.isNullOrBlank()) return null
            val host = parsed.host?.trim()?.lowercase().orEmpty()
            if (host.isEmpty()) return null
            if (!parsed.query.isNullOrBlank() || !parsed.fragment.isNullOrBlank()) return null
            val rawPath = parsed.path?.trim().orEmpty()
            val pathSegments = rawPath.split('/').filter { it.isNotBlank() }
            if (pathSegments.size < 2) return null
            val repoLeaf = pathSegments.last().removeSuffix(".git")
            val repoName = sanitizeRepoName(repoLeaf) ?: return null
            val normalizedPath = "/" + pathSegments.joinToString("/")
            val normalized = URI(
                "https",
                null,
                host,
                parsed.port,
                normalizedPath,
                null,
                null,
            ).toString()
            return GitRemoteSpec(
                normalizedUrl = normalized,
                host = host,
                repoName = repoName,
            )
        }

        private fun sanitizeRepoName(name: String): String? {
            val trimmed = name.trim().trimEnd('.')
            if (trimmed.isEmpty()) return null
            if (trimmed == "." || trimmed == "..") return null
            if (trimmed.any { it == '/' || it == '\\' || it == ':' }) return null
            return trimmed
        }
    }
}

data class GitAuthEntry(
    val host: String,
    val username: String,
    val token: String,
)

data class GitSavedAuthState(
    val host: String,
    val suggestedUsername: String,
    val hasSavedToken: Boolean,
)

data class GitCloneRequest(
    val remote: GitRemoteSpec,
    val target: SupportedCloneTarget,
    val username: String,
    val token: String,
    val saveAuth: Boolean,
)

sealed interface CloneTargetResolution

data class SupportedCloneTarget(
    val parentTreeUri: Uri,
    val parentDirectory: File,
    val repoDirectory: File,
    val repoName: String,
    val destinationLabel: String,
) : CloneTargetResolution

sealed interface UnsupportedCloneTarget : CloneTargetResolution {
    val userMessage: String
}

data class AllFilesAccessRequired(
    override val userMessage: String,
) : UnsupportedCloneTarget

data class UnsupportedTreeProvider(
    override val userMessage: String,
) : UnsupportedCloneTarget

data class CloneTargetConflict(
    override val userMessage: String,
) : UnsupportedCloneTarget

sealed interface GitCloneResult

data class GitCloneSuccess(
    val remote: GitRemoteSpec,
    val repoDirectory: File,
) : GitCloneResult

data class GitCloneFailure(
    val error: GitCloneError,
) : GitCloneResult

sealed interface GitCloneError {
    val userMessage: String
}

data class GitCloneValidationError(
    override val userMessage: String,
) : GitCloneError

data class GitCloneAuthError(
    override val userMessage: String,
) : GitCloneError

data class GitCloneNetworkError(
    override val userMessage: String,
) : GitCloneError

data class GitCloneStorageError(
    override val userMessage: String,
) : GitCloneError

data class GitCloneUnknownError(
    override val userMessage: String,
) : GitCloneError

fun defaultGitUsernameForHost(host: String): String = when {
    host.contains("gitlab") -> "oauth2"
    else -> "git"
}

fun gitHostHelpText(host: String): String = when {
    host.contains("github") ->
        "Use an HTTPS personal access token. If the default username fails, enter your GitHub username."
    host.contains("gitlab") ->
        "Use an HTTPS personal access token. GitLab often works with username oauth2."
    else ->
        "Use an HTTPS personal access token for this host."
}
