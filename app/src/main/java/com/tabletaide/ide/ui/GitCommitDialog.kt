package com.tabletaide.ide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tabletaide.ide.data.GitCommitDialogState
import com.tabletaide.ide.data.GitRepoUiState

@Composable
fun GitCommitDialog(
    gitState: GitRepoUiState,
    dialogState: GitCommitDialogState,
    onDismiss: () -> Unit,
    onDraftChange: (String) -> Unit,
    onAuthorNameChange: (String) -> Unit,
    onAuthorEmailChange: (String) -> Unit,
    onStageUntrackedChange: (Boolean) -> Unit,
    onGenerateMessage: () -> Unit,
    onCommit: () -> Unit,
    onCommitAndPush: () -> Unit,
    onPull: () -> Unit,
    onOpenGitAuth: () -> Unit,
) {
    if (!dialogState.visible) return

    AlertDialog(
        onDismissRequest = {
            if (!dialogState.busy && !dialogState.generatingMessage) onDismiss()
        },
        title = { Text("Commit changes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = buildString {
                        append(gitState.repoName.ifBlank { "Repository" })
                        append("  ")
                        append(gitState.branchName)
                    },
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = buildString {
                        append("Staged ${gitState.stagedCount}")
                        append("  ·  Unstaged ${gitState.unstagedCount}")
                        append("  ·  Untracked ${gitState.untrackedCount}")
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!gitState.upstreamBranch.isNullOrBlank()) {
                    Text(
                        text = buildString {
                            append("Tracked upstream: ${gitState.upstreamBranch}")
                            if (gitState.behindCount > 0) {
                                append("  ·  behind ${gitState.behindCount}")
                            }
                            if (gitState.aheadCount > 0) {
                                append("  ·  ahead ${gitState.aheadCount}")
                            }
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Current branch has no tracked upstream yet. Clone from the app or set upstream outside Kinetic.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (gitState.canPush && !gitState.hasSavedAuth) {
                    Text(
                        text = "Push needs a saved HTTPS token for ${gitState.remoteHost ?: "this host"}.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onOpenGitAuth, enabled = !dialogState.busy) {
                        Text("Save git token")
                    }
                }
                HorizontalDivider()
                OutlinedTextField(
                    value = dialogState.draftMessage,
                    onValueChange = onDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Commit message") },
                    minLines = 3,
                    maxLines = 4,
                    enabled = !dialogState.busy,
                )
                OutlinedTextField(
                    value = dialogState.authorName,
                    onValueChange = onAuthorNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Author name") },
                    singleLine = true,
                    enabled = !dialogState.busy,
                )
                OutlinedTextField(
                    value = dialogState.authorEmail,
                    onValueChange = onAuthorEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Author email") },
                    singleLine = true,
                    enabled = !dialogState.busy,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = dialogState.stageUntracked,
                        onCheckedChange = {
                            if (!dialogState.busy) onStageUntrackedChange(it)
                        },
                        enabled = !dialogState.busy,
                    )
                    Text("Stage untracked files too")
                }
                if (dialogState.busy || dialogState.generatingMessage) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                dialogState.progressMessage?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                dialogState.errorMessage?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !dialogState.busy && !dialogState.generatingMessage,
            ) {
                Text("Close")
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onPull,
                    enabled = !dialogState.busy &&
                        !dialogState.generatingMessage &&
                        gitState.pushReady &&
                        gitState.behindCount > 0,
                ) {
                    Text("Pull")
                }
                TextButton(
                    onClick = onGenerateMessage,
                    enabled = !dialogState.busy &&
                        !dialogState.generatingMessage &&
                        gitState.available &&
                        !gitState.clean,
                ) {
                    Text("Generate message")
                }
                TextButton(
                    onClick = onCommit,
                    enabled = !dialogState.busy &&
                        !dialogState.generatingMessage &&
                        gitState.available &&
                        !gitState.clean,
                ) {
                    Text("Commit")
                }
                TextButton(
                    onClick = onCommitAndPush,
                    enabled = !dialogState.busy &&
                        !dialogState.generatingMessage &&
                        gitState.available &&
                        (!gitState.clean || gitState.aheadCount > 0) &&
                        gitState.pushReady,
                ) {
                    Text(if (gitState.clean && gitState.aheadCount > 0) "Push" else "Commit & push")
                }
            }
        },
    )
}
