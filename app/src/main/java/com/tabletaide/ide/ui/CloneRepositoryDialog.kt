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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tabletaide.ide.data.GitHubOAuthUiState
import com.tabletaide.ide.data.GitHubRepo
import com.tabletaide.ide.data.GitRemoteSpec
import com.tabletaide.ide.data.GitSavedAuthState
import com.tabletaide.ide.data.gitHostHelpText

private enum class CloneDialogTab { GitHub, Manual }

@Composable
fun CloneRepositoryDialog(
    selectedDestinationLabel: String?,
    hasAllFilesAccess: Boolean,
    cloneUiState: GitCloneUiState,
    githubOAuthState: GitHubOAuthUiState,
    lookupSavedAuth: (String) -> GitSavedAuthState?,
    onPickDestination: () -> Unit,
    onOpenManageFilesAccess: () -> Unit,
    onClearSavedAuth: (String) -> Unit,
    onGitHubSignIn: () -> Unit,
    onGitHubSignOut: () -> Unit,
    onLoadGitHubRepos: () -> Unit,
    onCloneGitHubRepo: (GitHubRepo) -> Unit,
    onSaveGitHubOAuthClientId: (String) -> Unit,
    onDismiss: () -> Unit,
    onClone: (repoUrl: String, username: String, token: String, useSavedToken: Boolean, saveToken: Boolean) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(CloneDialogTab.GitHub) }
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
                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    Tab(
                        selected = selectedTab == CloneDialogTab.GitHub,
                        onClick = { selectedTab = CloneDialogTab.GitHub },
                        text = { Text("GitHub") },
                    )
                    Tab(
                        selected = selectedTab == CloneDialogTab.Manual,
                        onClick = { selectedTab = CloneDialogTab.Manual },
                        text = { Text("Manual URL") },
                    )
                }
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
                when (selectedTab) {
                    CloneDialogTab.GitHub -> {
                        GitHubCloneSection(
                            oauthState = githubOAuthState,
                            cloneBusy = cloneUiState.busy,
                            selectedDestinationLabel = selectedDestinationLabel,
                            hasAllFilesAccess = hasAllFilesAccess,
                            onSignIn = onGitHubSignIn,
                            onSignOut = onGitHubSignOut,
                            onLoadRepos = onLoadGitHubRepos,
                            onPickDestination = onPickDestination,
                            onCloneRepo = onCloneGitHubRepo,
                            onSaveOAuthClientId = onSaveGitHubOAuthClientId,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        if (!cloneUiState.errorMessage.isNullOrBlank()) {
                            Text(
                                text = cloneUiState.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                    CloneDialogTab.Manual -> ManualCloneFields(
                        repoUrl = repoUrl,
                        onRepoUrlChange = { repoUrl = it },
                        username = username,
                        onUsernameChange = { username = it },
                        token = token,
                        onTokenChange = { token = it },
                        useSavedToken = useSavedToken,
                        onUseSavedTokenChange = { useSavedToken = it },
                        saveToken = saveToken,
                        onSaveTokenChange = { saveToken = it },
                        remote = remote,
                        savedAuth = savedAuth,
                        hostHelp = hostHelp,
                        repoIsValid = repoIsValid,
                        selectedDestinationLabel = selectedDestinationLabel,
                        cloneUiState = cloneUiState,
                        onPickDestination = onPickDestination,
                        onClearSavedAuth = { onClearSavedAuth(repoUrl) },
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
            if (selectedTab == CloneDialogTab.Manual) {
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
            }
        },
    )
}

@Composable
private fun ManualCloneFields(
    repoUrl: String,
    onRepoUrlChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    token: String,
    onTokenChange: (String) -> Unit,
    useSavedToken: Boolean,
    onUseSavedTokenChange: (Boolean) -> Unit,
    saveToken: Boolean,
    onSaveTokenChange: (Boolean) -> Unit,
    remote: GitRemoteSpec?,
    savedAuth: GitSavedAuthState?,
    hostHelp: String?,
    repoIsValid: Boolean,
    selectedDestinationLabel: String?,
    cloneUiState: GitCloneUiState,
    onPickDestination: () -> Unit,
    onClearSavedAuth: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Text(
            text = "Clone a repository into a shared device folder using HTTPS token auth.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = repoUrl,
            onValueChange = onRepoUrlChange,
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
            onValueChange = onUsernameChange,
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
                    onCheckedChange = onUseSavedTokenChange,
                )
                Text(
                    text = "Use saved token for ${savedAuth.host}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = {
                        onUseSavedTokenChange(false)
                        onClearSavedAuth()
                    },
                ) {
                    Text("Clear saved")
                }
            }
        }
        if (!useSavedToken) {
            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
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
                    onCheckedChange = onSaveTokenChange,
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
}
