package com.tabletaide.ide.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Captures and restores text-file snapshots of the workspace around a shell command.
 *
 * Limits enforce a bounded cost on Android: only paths under [SNAPSHOT_FILE_BYTE_LIMIT],
 * up to [SNAPSHOT_FILE_COUNT_LIMIT] files, excluding known build / VCS directories.
 * Binary or oversized files are recorded in `skippedPaths` so the UI can surface that
 * a revert may not be complete.
 */
@Singleton
class WorkspaceSnapshotStore @Inject constructor(
    private val workspace: WorkspaceRepository,
) {

    suspend fun snapshot(): WorkspaceSnapshot = withContext(Dispatchers.IO) {
        val files = workspace.listTreeRows().filter { !it.isDirectory }
        val captured = LinkedHashMap<String, String>()
        val skipped = mutableListOf<String>()
        var counted = 0
        for (row in files) {
            if (isExcludedPath(row.path)) continue
            if (counted >= SNAPSHOT_FILE_COUNT_LIMIT) {
                skipped += row.path
                continue
            }
            val content = workspace.readText(row.path).getOrNull()
            if (content == null) {
                skipped += row.path
                continue
            }
            if (content.length > SNAPSHOT_FILE_BYTE_LIMIT) {
                skipped += row.path
                continue
            }
            captured[row.path] = content
            counted++
        }
        WorkspaceSnapshot(files = captured, skippedPaths = skipped)
    }

    fun diff(pre: WorkspaceSnapshot, post: WorkspaceSnapshot): CommandSnapshotDiff {
        val allPaths = (pre.files.keys + post.files.keys).toSortedSet()
        val added = mutableListOf<String>()
        val modified = mutableListOf<String>()
        val deleted = mutableListOf<String>()
        for (path in allPaths) {
            val before = pre.files[path]
            val after = post.files[path]
            when {
                before == null && after != null -> added += path
                before != null && after == null -> deleted += path
                before != null && after != null && before != after -> modified += path
            }
        }
        return CommandSnapshotDiff(
            added = added,
            modified = modified,
            deleted = deleted,
            skippedPre = pre.skippedPaths,
            skippedPost = post.skippedPaths,
        )
    }

    /**
     * Restore the workspace to [target] for the paths in [diff].
     * For each path, the current on-disk content is compared to [reference] (the snapshot
     * the user "left" the workspace in). If they diverge, the path is skipped and reported
     * in the conflicts list so the user can resolve it manually.
     */
    suspend fun restoreChanged(
        diff: CommandSnapshotDiff,
        reference: WorkspaceSnapshot,
        target: WorkspaceSnapshot,
    ): SnapshotRestoreReport = withContext(Dispatchers.IO) {
        val restored = mutableListOf<String>()
        val conflicts = mutableListOf<SnapshotConflict>()

        suspend fun restoreOne(path: String) {
            val current = workspace.readText(path).getOrNull()
            val expected = reference.files[path]
            if (current != expected) {
                conflicts += SnapshotConflict(
                    path = path,
                    reason = "File changed on disk since the command ran; manual review needed.",
                )
                return
            }
            val want = target.files[path]
            if (want == null) {
                // Target says this file should not exist.
                if (current != null) {
                    workspace.deleteNode(path).fold(
                        onSuccess = { restored += path },
                        onFailure = {
                            conflicts += SnapshotConflict(path, it.message ?: "Delete failed")
                        },
                    )
                }
            } else {
                workspace.writeText(path, want).fold(
                    onSuccess = { restored += path },
                    onFailure = {
                        conflicts += SnapshotConflict(path, it.message ?: "Write failed")
                    },
                )
            }
        }

        for (path in diff.added) restoreOne(path)
        for (path in diff.modified) restoreOne(path)
        for (path in diff.deleted) restoreOne(path)

        SnapshotRestoreReport(
            restoredPaths = restored,
            conflicts = conflicts,
        )
    }

    private fun isExcludedPath(path: String): Boolean {
        val segments = path.split('/')
        return segments.any { seg -> seg in EXCLUDED_SEGMENTS }
    }

    private companion object {
        const val SNAPSHOT_FILE_BYTE_LIMIT = 256 * 1024
        const val SNAPSHOT_FILE_COUNT_LIMIT = 5_000
        val EXCLUDED_SEGMENTS = setOf(
            ".git", "build", ".gradle", ".idea", "node_modules",
            "dist", "out", ".kotlin", ".cxx",
        )
    }
}

data class WorkspaceSnapshot(
    val files: Map<String, String>,
    val skippedPaths: List<String>,
)

data class CommandSnapshotDiff(
    val added: List<String>,
    val modified: List<String>,
    val deleted: List<String>,
    val skippedPre: List<String>,
    val skippedPost: List<String>,
) {
    val isEmpty: Boolean
        get() = added.isEmpty() && modified.isEmpty() && deleted.isEmpty()

    fun summary(): String {
        if (isEmpty) return "No workspace files changed"
        return buildString {
            if (added.isNotEmpty()) append("+${added.size} added ")
            if (modified.isNotEmpty()) append("~${modified.size} modified ")
            if (deleted.isNotEmpty()) append("-${deleted.size} deleted")
        }.trim()
    }
}

data class SnapshotConflict(
    val path: String,
    val reason: String,
)

data class SnapshotRestoreReport(
    val restoredPaths: List<String>,
    val conflicts: List<SnapshotConflict>,
)
