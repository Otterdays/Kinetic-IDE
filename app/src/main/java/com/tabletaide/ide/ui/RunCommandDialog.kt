package com.tabletaide.ide.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tabletaide.ide.data.CommandRunnerUiState
import com.tabletaide.ide.data.RunCommandDialogState

@Composable
fun RunCommandDialog(
    dialogState: RunCommandDialogState,
    runnerState: CommandRunnerUiState,
    onDismiss: () -> Unit,
    onCommandChange: (String) -> Unit,
    onRun: () -> Unit,
) {
    if (!dialogState.visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Run command") },
        text = {
            Column {
                Text(
                    text = runnerState.workspacePath.ifBlank {
                        runnerState.availabilityMessage ?: "No workspace is available for command execution."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = dialogState.command,
                    onValueChange = onCommandChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text("Shell command") },
                    minLines = 3,
                    maxLines = 5,
                    enabled = runnerState.available && !runnerState.busy,
                )
                dialogState.errorMessage?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onRun,
                enabled = runnerState.available && !runnerState.busy,
            ) {
                Text("Run")
            }
        },
    )
}
