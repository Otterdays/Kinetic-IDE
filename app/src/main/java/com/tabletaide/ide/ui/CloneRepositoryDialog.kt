package com.tabletaide.ide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.tabletaide.ide.data.GitRemoteSpec
import com.tabletaide.ide.data.GitSavedAuthState
import com.tabletaide.ide.data.gitHostHelpText

@Composable
fun CloneRepositoryDialog(
    selectedDestinationLabel: String?,
    hasAllFilesAccess: Boolean,
    cloneUiState: GitCloneUiState,
    lookupSavedAuth: (String) -> GitSavedAuthState?,
    onPickDestination: () -> Unit,
    onOpenManageFilesAccess: () -> Unit,
    onClearSavedAuth: (String) -> Unit,
    onDismiss: () -> Unit,
    onClone: (repoUrl: String, username: String, token: String, useSavedToken: Boolean, saveToken: Boolean) -> Unit,
) {
    var repoUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var useSavedToken by remember { mutableStateOf(false) }
    var saveToken by remember { mutableStateOf(true) }

    val remote = remember(repoUrl) { GitRemoteSpec.parseHttps(repoUrl) }
    val savedAuth = lookupSavedAuth(repoUrl)
    val hostHelp = remote?.host?.let(::gitHostHelpText)
    val repoIsValid = remote != null
    val tokenRequired = !useSavedToken
    val canSubmit = !cloneUiState.busy &&
        repoIsValid &&
        selectedDestinationLabel != null &&
        hasAllFilesAccess &&
        (!tokenRequired || token.trim().isNotEmpty())

    LaunchedEffect(savedAuth?.host) {
        if (savedAuth != null && token.isBlank()) {
            useSavedToken = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clone repository") },
        text = {
            Column(modifier = Modifier.widthIn(min = 320.dp, max = 560.dp)) {
                Text(
                    text = "Clone a repository into a shared device folder using HTTPS token auth.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!hasAllFilesAccess) {
                    Text(
                        text = "Shared-folder clone needs All files access so the git runtime can write the repo.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    TextButton(
                        onClick = onOpenManageFilesAccess,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Text("Grant file access")
                    }
                }
                OutlinedTextField(
                    value = repoUrl,
                    onValueChange = { repoUrl = it },
                    label = { Text("Repository URL") },
                    placeholder = { Text("https://github.com/owner/repo.git") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    isError = repoUrl.isNotBlank() && !repoIsValid,
                )
                if (repoUrl.isNotBlank() && !repoIsValid) {
                    Text(
                        text = "Enter a valid HTTPS repository URL. Embedded credentials are not allowed.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (!hostHelp.isNullOrBlank()) {
                    Text(
                        text = hostHelp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (optional)") },
                    placeholder = {
                        Text(savedAuth?.suggestedUsername ?: remote?.host?.let { "Default: git" } ?: "Default: git")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                if (savedAuth != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = useSavedToken,
                            onCheckedChange = { useSavedToken = it },
                        )
                        Text(
                            text = "Use saved token for ${savedAuth.host}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(
                            onClick = {
                                useSavedToken = false
                                onClearSavedAuth(repoUrl)
                            },
                        ) {
                            Text("Clear saved")
                        }
                    }
                }
                if (!useSavedToken) {
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("Personal access token") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = saveToken,
                            onCheckedChange = { saveToken = it },
                        )
                        Text(
                            text = "Save token securely on this device",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Text(
                    text = "Destination",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                Text(
                    text = selectedDestinationLabel
                        ?: "Choose a shared device folder for the cloned repo.",
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
                if (cloneUiState.busy) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
                if (!cloneUiState.progressMessage.isNullOrBlank()) {
                    Text(
                        text = cloneUiState.progressMessage,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (!cloneUiState.errorMessage.isNullOrBlank()) {
                    Text(
                        text = cloneUiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !cloneUiState.busy) {
                Text("Close")
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    enabled = canSubmit,
                    onClick = {
                        onClone(
                            repoUrl.trim(),
                            username.trim(),
                            token.trim(),
                            useSavedToken,
                            saveToken,
                        )
                    },
                ) {
                    Text(if (cloneUiState.busy) "Cloning…" else "Clone now")
                }
            }
        },
    )
}
