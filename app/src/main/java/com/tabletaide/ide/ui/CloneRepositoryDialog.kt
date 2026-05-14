package com.tabletaide.ide.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val RepoUrlRegex = Regex(
    pattern = """^(https://|ssh://|git@)[^\s]+(?:\.git)?/?$""",
    option = RegexOption.IGNORE_CASE,
)

@Composable
fun CloneRepositoryDialog(
    selectedDestinationLabel: String?,
    onPickDestination: () -> Unit,
    onDismiss: () -> Unit,
    onStageDraft: (String) -> Unit,
) {
    var repoUrl by remember { mutableStateOf("") }
    val isValidUrl = RepoUrlRegex.matches(repoUrl.trim())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clone repository") },
        text = {
            Column(modifier = Modifier.widthIn(min = 320.dp, max = 520.dp)) {
                Text(
                    text = "Stage the clone flow now; real git cloning lands in a later phase.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = repoUrl,
                    onValueChange = { repoUrl = it },
                    label = { Text("Repository URL") },
                    placeholder = { Text("https://github.com/owner/repo.git") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    isError = repoUrl.isNotBlank() && !isValidUrl,
                )
                if (repoUrl.isNotBlank() && !isValidUrl) {
                    Text(
                        text = "Enter an HTTPS or SSH repository URL.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Text(
                    text = "Destination",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                )
                Text(
                    text = selectedDestinationLabel
                        ?: "Choose the local folder where the repo should land later.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = onPickDestination,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        if (selectedDestinationLabel == null) {
                            "Choose destination"
                        } else {
                            "Change destination"
                        },
                    )
                }
                Text(
                    text = "For this MVP the flow is intentionally non-destructive: it validates the"
                        + " repository target and preserves the intended destination without running git.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValidUrl && selectedDestinationLabel != null,
                onClick = { onStageDraft(repoUrl.trim()) },
            ) {
                Text("Continue later")
            }
        },
    )
}
