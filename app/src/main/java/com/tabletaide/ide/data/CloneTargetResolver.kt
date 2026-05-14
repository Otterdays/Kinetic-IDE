package com.tabletaide.ide.data

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

@Singleton
class CloneTargetResolver @Inject constructor() {
    fun resolve(parentTreeUri: Uri, remote: GitRemoteSpec): CloneTargetResolution {
        if (!Environment.isExternalStorageManager()) {
            return AllFilesAccessRequired(
                userMessage = "Shared-folder git clone needs All files access so JGit can write the repo on device.",
            )
        }
        if (parentTreeUri.authority != EXTERNAL_STORAGE_AUTHORITY) {
            return UnsupportedTreeProvider(
                userMessage = "Pick a folder from Internal shared storage. This provider is not supported yet.",
            )
        }
        val documentId = try {
            DocumentsContract.getTreeDocumentId(parentTreeUri)
        } catch (_: Exception) {
            return UnsupportedTreeProvider(
                userMessage = "Could not resolve the selected folder. Pick another shared-storage folder.",
            )
        }
        val parentDirectory = resolveExternalStorageDirectory(documentId)
            ?: return UnsupportedTreeProvider(
                userMessage = "Only primary shared storage folders are supported for the first clone MVP.",
            )
        if (!parentDirectory.exists() || !parentDirectory.isDirectory) {
            return UnsupportedTreeProvider(
                userMessage = "The selected folder is not available as a writable filesystem path.",
            )
        }
        val repoDirectory = File(parentDirectory, remote.repoName)
        if (repoDirectory.exists()) {
            return CloneTargetConflict(
                userMessage = "A folder named ${remote.repoName} already exists in the selected location.",
            )
        }
        return SupportedCloneTarget(
            parentTreeUri = parentTreeUri,
            parentDirectory = parentDirectory,
            repoDirectory = repoDirectory,
            repoName = remote.repoName,
            destinationLabel = buildDestinationLabel(parentDirectory),
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

    private fun buildDestinationLabel(directory: File): String {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        return when {
            directory == docsDir -> "Documents"
            else -> directory.name.ifBlank { directory.absolutePath }
        }
    }
}
