package com.tabletaide.ide.data

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

@Singleton
class WorkspaceExecutionResolver @Inject constructor() {
    fun resolveWorkspace(treeUri: Uri?): WorkspaceExecutionResolution {
        if (treeUri == null) {
            return WorkspaceExecutionUnavailable("Open a workspace to run commands.")
        }
        if (!Environment.isExternalStorageManager()) {
            return WorkspaceExecutionUnavailable(
                "Running commands needs All files access for workspaces opened from shared storage.",
            )
        }
        if (treeUri.authority != EXTERNAL_STORAGE_AUTHORITY) {
            return WorkspaceExecutionUnavailable(
                "Command execution is only available for shared-storage folders that map to real paths.",
            )
        }
        val documentId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: Exception) {
            return WorkspaceExecutionUnavailable(
                "Could not resolve the current workspace path for command execution.",
            )
        }
        val workTree = resolveExternalStorageDirectory(documentId)
            ?: return WorkspaceExecutionUnavailable(
                "Only primary shared-storage workspaces are supported for in-app command execution right now.",
            )
        if (!workTree.exists() || !workTree.isDirectory) {
            return WorkspaceExecutionUnavailable(
                "The current workspace path is not available for command execution.",
            )
        }
        return WorkspaceExecutionReady(
            workTree = workTree,
            workspaceLabel = workTree.name.ifBlank { "Workspace" },
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
