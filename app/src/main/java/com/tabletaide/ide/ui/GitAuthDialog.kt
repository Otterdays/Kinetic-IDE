package com.tabletaide.ide.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tabletaide.ide.data.GitAuthDialogState
import com.tabletaide.ide.data.gitHostHelpText

@Composable
fun GitAuthDialog(
    dialogState: GitAuthDialogState,
    onDismiss: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    if (!dialogState.visible) return
    val hostHelp = dialogState.host.takeIf { it.isNotBlank() }?.let(::gitHostHelpText)

    AlertDialog(
        onDismissRequest = {
            if (!dialogState.busy) onDismiss()
        },
        title = { Text("Save git credentials") },
        text = {
            Column {
                Text(
                    text = buildString {
                        append("HTTPS token for ")
                        append(dialogState.host.ifBlank { "this git host" })
                        append(". Used for push and future clones to the same host.")
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                hostHelp?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                OutlinedTextField(
                    value = dialogState.username,
                    onValueChange = onUsernameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text("Username") },
                    singleLine = true,
                    enabled = !dialogState.busy,
                )
                OutlinedTextField(
                    value = dialogState.token,
                    onValueChange = onTokenChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text("Personal access token") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !dialogState.busy,
                )
                if (dialogState.busy) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
                dialogState.errorMessage?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !dialogState.busy) {
                Text("Close")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = !dialogState.busy &&
                    dialogState.host.isNotBlank() &&
                    dialogState.token.isNotBlank(),
            ) {
                Text("Save")
            }
        },
    )
}
