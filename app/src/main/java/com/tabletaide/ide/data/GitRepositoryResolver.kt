package com.tabletaide.ide.data

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

@Singleton
class GitRepositoryResolver @Inject constructor() {
    fun resolveWorkspace(treeUri: Uri?): GitRepositoryResolution {
        if (treeUri == null) {
            return GitRepositoryUnavailable("Open a workspace to use git features.")
        }
        if (!Environment.isExternalStorageManager()) {
            return GitRepositoryUnavailable(
                "Git workflow needs All files access for repos opened from shared storage.",
            )
        }
        if (treeUri.authority != EXTERNAL_STORAGE_AUTHORITY) {
            return GitRepositoryUnavailable(
                "Git workflow is only available for shared-storage folders that map to real paths.",
            )
        }
        val documentId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: Exception) {
            return GitRepositoryUnavailable(
                "Could not resolve the current workspace path for git operations.",
            )
        }
        val workTree = resolveExternalStorageDirectory(documentId)
            ?: return GitRepositoryUnavailable(
                "Only primary shared-storage workspaces are supported for git workflow right now.",
            )
        if (!workTree.exists() || !workTree.isDirectory) {
            return GitRepositoryUnavailable(
                "The current workspace path is not available for git operations.",
            )
        }
        val gitDir = File(workTree, ".git")
        if (!gitDir.exists() || !gitDir.isDirectory) {
            return GitRepositoryUnavailable("Open a git repository root to use commit and push.")
        }
        return GitRepositoryReady(
            workTree = workTree,
            gitDir = gitDir,
            repoName = workTree.name.ifBlank { "Repository" },
        )
    }

    private fun resolveExternalStorageDirectory(documentId: String): File? {
        val volume = documentId.substringBefore(':')
        val relativePath = documentId.substringAfter(':', "")
        val baseDirectory = when (volume.lowercase()) {
            "primary" -> Environment.getExternalStorageDirectory()
            "home" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            else -> return null
        }
        return if (relativePath.isBlank()) {
            baseDirectory
        } else {
            File(baseDirectory, relativePath)
        }
    }
}
