package com.tabletaide.ide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tabletaide.ide.data.GitHubOAuthUiState
import com.tabletaide.ide.data.GitHubRepo

@Composable
fun GitHubCloneSection(
    oauthState: GitHubOAuthUiState,
    cloneBusy: Boolean,
    selectedDestinationLabel: String?,
    hasAllFilesAccess: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onLoadRepos: () -> Unit,
    onPickDestination: () -> Unit,
    onCloneRepo: (GitHubRepo) -> Unit,
    onSaveOAuthClientId: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var oauthClientIdDraft by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("") }
    var selectedRepo by remember { mutableStateOf<GitHubRepo?>(null) }
    val session = oauthState.session
    val repoList = oauthState.repoList

    LaunchedEffect(session.signedIn) {
        if (session.signedIn && !repoList.loaded && !repoList.loading) {
            onLoadRepos()
        }
    }

    val filteredRepos = remember(filter, repoList.repos) {
        val normalized = filter.trim().lowercase()
        if (normalized.isEmpty()) {
            repoList.repos
        } else {
            repoList.repos.filter { repo ->
                repo.fullName.lowercase().contains(normalized) ||
                    repo.name.lowercase().contains(normalized) ||
                    repo.description.orEmpty().lowercase().contains(normalized)
            }
        }
    }

    Column(modifier = modifier) {
        Text(
            text = "Sign in with GitHub, pick a repository, choose a destination folder, then clone and open it.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!oauthState.oauthConfigured) {
            Text(
                text = "Create a GitHub OAuth App at github.com/settings/developers. Set the callback URL to:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = GITHUB_CALLBACK_HINT,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            OutlinedTextField(
                value = oauthClientIdDraft,
                onValueChange = { oauthClientIdDraft = it },
                label = { Text("OAuth App Client ID") },
                placeholder = { Text("Ov23li…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            )
            TextButton(
                onClick = { onSaveOAuthClientId(oauthClientIdDraft) },
                enabled = oauthClientIdDraft.trim().isNotEmpty(),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text("Save Client ID")
            }
            oauthState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            return@Column
        }
        if (!hasAllFilesAccess) {
            Text(
                text = "All files access is required so git can write the cloned repository.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (!session.signedIn) {
            TextButton(
                onClick = onSignIn,
                enabled = oauthState.oauthConfigured && !oauthState.authorizing,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(if (oauthState.authorizing) "Waiting for GitHub…" else "Sign in with GitHub")
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Signed in as ${session.displayName ?: session.login ?: "GitHub user"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = onSignOut, enabled = !cloneBusy) {
                    Text("Sign out")
                }
            }
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                label = { Text("Search repositories") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            if (repoList.loading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    Text(
                        text = "Loading repositories…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            repoList.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                TextButton(onClick = onLoadRepos, enabled = !repoList.loading) {
                    Text("Retry")
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (!repoList.loading && repoList.loaded && filteredRepos.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = if (filter.isBlank()) "No repositories found." else "No repositories match \"$filter\".",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
                items(filteredRepos, key = { it.id }) { repo ->
                    val selected = selectedRepo?.id == repo.id
                    TextButton(
                        onClick = { selectedRepo = repo },
                        enabled = !cloneBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = buildString {
                                    append(repo.fullName)
                                    if (repo.isPrivate) append(" · private")
                                    if (selected) append(" ✓")
                                },
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            repo.description?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = "Destination",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
            Text(
                text = selectedDestinationLabel
                    ?: "Choose a shared device folder (Downloads or Documents).",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = onPickDestination,
                enabled = !cloneBusy,
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(if (selectedDestinationLabel == null) "Choose destination" else "Change destination")
            }
            val canClone = selectedRepo != null &&
                selectedDestinationLabel != null &&
                hasAllFilesAccess &&
                !cloneBusy
            TextButton(
                onClick = { selectedRepo?.let(onCloneRepo) },
                enabled = canClone,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(if (cloneBusy) "Cloning…" else "Clone and open")
            }
        }
        oauthState.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (cloneBusy) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            )
        }
    }
}

private const val GITHUB_CALLBACK_HINT = "com.tabletaide.ide://oauth/github"
