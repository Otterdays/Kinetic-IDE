package com.tabletaide.ide.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tabletaide.ide.GitHubOAuthActivity
import com.tabletaide.ide.data.GitHubOAuthUiState
import com.tabletaide.ide.data.GitHubRepo
import com.tabletaide.ide.data.GitSavedAuthState
import com.tabletaide.ide.data.RecentWorkspaceEntry
import com.tabletaide.ide.data.StarterProjectTemplate
import com.tabletaide.ide.ui.theme.KineticColors

@Composable
fun StartupGatewayScreen(
    recentWorkspaces: List<RecentWorkspaceEntry>,
    statusMessage: String?,
    cloneUiState: GitCloneUiState,
    githubOAuthState: GitHubOAuthUiState,
    checkAllFilesAccess: () -> Boolean,
    onPeekSavedGitAuth: (String) -> GitSavedAuthState?,
    onClearSavedGitAuth: (String) -> Unit,
    onClearCloneFeedback: () -> Unit,
    onOpenWorkspace: (Uri) -> Unit,
    onOpenRecentWorkspace: (String) -> Unit,
    onCreateStarterProject: (Uri, String, StarterProjectTemplate) -> Unit,
    onCloneRepository: (Uri, String, String, String, Boolean, Boolean) -> Unit,
    onBeginGitHubSignIn: () -> Uri?,
    onCompleteGitHubSignIn: (Uri) -> Unit,
    onCancelGitHubSignIn: () -> Unit,
    onGitHubSignOut: () -> Unit,
    onLoadGitHubRepos: () -> Unit,
    onCloneGitHubRepository: (Uri, GitHubRepo) -> Unit,
    onSaveGitHubOAuthClientId: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val openWorkspacePicker = rememberWorkspaceTreePicker(onTreePicked = onOpenWorkspace)
    var hasAllFilesAccess by remember { mutableStateOf(checkAllFilesAccess()) }

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showCloneDialog by remember { mutableStateOf(false) }

    var newProjectParentUri by remember { mutableStateOf<Uri?>(null) }
    var newProjectParentLabel by remember { mutableStateOf<String?>(null) }
    val pickProjectLocation = rememberWorkspaceTreePicker { treeUri ->
        newProjectParentUri = treeUri
        newProjectParentLabel = DocumentFile.fromTreeUri(context, treeUri)?.name ?: "Selected folder"
    }

    var cloneDestinationUri by remember { mutableStateOf<Uri?>(null) }
    var cloneDestinationLabel by remember { mutableStateOf<String?>(null) }
    val pickCloneDestination = rememberWorkspaceTreePicker { treeUri ->
        cloneDestinationUri = treeUri
        cloneDestinationLabel = DocumentFile.fromTreeUri(context, treeUri)?.name ?: "Selected folder"
    }

    val githubOAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val callbackUri = result.data?.data
                if (callbackUri != null) {
                    onCompleteGitHubSignIn(callbackUri)
                } else {
                    onCancelGitHubSignIn()
                }
            }
            else -> onCancelGitHubSignIn()
        }
    }

    LaunchedEffect(statusMessage) {
        val message = statusMessage?.trim().orEmpty()
        if (message.isNotEmpty()) {
            snackbarHostState.showSnackbar(message)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAllFilesAccess = checkAllFilesAccess()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            selectedParentLabel = newProjectParentLabel,
            onPickLocation = pickProjectLocation,
            onDismiss = {
                showNewProjectDialog = false
                newProjectParentUri = null
                newProjectParentLabel = null
            },
            onCreate = { projectName, template ->
                val parentUri = newProjectParentUri ?: return@NewProjectDialog
                onCreateStarterProject(parentUri, projectName, template)
                showNewProjectDialog = false
                newProjectParentUri = null
                newProjectParentLabel = null
            },
        )
    }

    if (showCloneDialog) {
        CloneRepositoryDialog(
            selectedDestinationLabel = cloneDestinationLabel,
            hasAllFilesAccess = hasAllFilesAccess,
            cloneUiState = cloneUiState,
            githubOAuthState = githubOAuthState,
            lookupSavedAuth = onPeekSavedGitAuth,
            onPickDestination = pickCloneDestination,
            onOpenManageFilesAccess = {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    )
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                }
                context.startActivity(intent)
            },
            onClearSavedAuth = onClearSavedGitAuth,
            onGitHubSignIn = {
                val authUri = onBeginGitHubSignIn() ?: return@CloneRepositoryDialog
                githubOAuthLauncher.launch(
                    GitHubOAuthActivity.createIntent(context, authUri.toString()),
                )
            },
            onGitHubSignOut = onGitHubSignOut,
            onLoadGitHubRepos = onLoadGitHubRepos,
            onCloneGitHubRepo = { repo ->
                val targetUri = cloneDestinationUri ?: return@CloneRepositoryDialog
                onCloneGitHubRepository(targetUri, repo)
            },
            onSaveGitHubOAuthClientId = onSaveGitHubOAuthClientId,
            onDismiss = {
                showCloneDialog = false
                cloneDestinationUri = null
                cloneDestinationLabel = null
                onClearCloneFeedback()
            },
            onClone = { repoUrl, username, token, useSavedToken, saveToken ->
                val targetUri = cloneDestinationUri ?: return@CloneRepositoryDialog
                onCloneRepository(
                    targetUri,
                    repoUrl,
                    username,
                    token,
                    useSavedToken,
                    saveToken,
                )
            },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KineticColors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                KineticColors.surfaceContainerHigh,
                                KineticColors.surface.copy(alpha = 0.96f),
                            ),
                        ),
                        shape = RoundedCornerShape(28.dp),
                    )
                    .padding(28.dp),
            ) {
                Column {
                    Text(
                        text = "Welcome back, Architect.",
                        style = MaterialTheme.typography.headlineLarge,
                        color = KineticColors.onSurface,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Start clean, reopen a workspace, or stage a repository import.",
                        color = KineticColors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = "The IDE shell will still resume directly whenever a prior session"
                            + " can be restored.",
                        color = KineticColors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Text(
                        text = if (hasAllFilesAccess) {
                            "Tip: clone or open repos under Downloads or Documents for git, push, and shell support."
                        } else {
                            "Git clone, push, and shell commands need All files access plus a shared-storage folder (Downloads or Documents)."
                        },
                        color = if (hasAllFilesAccess) {
                            KineticColors.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    if (!hasAllFilesAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    },
                                )
                            },
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Text("Grant All files access")
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GatewayActionCard(
                    title = "New Project",
                    body = "Create a starter workspace with a proper structure and open it immediately.",
                    icon = Icons.Default.AddBox,
                    accent = KineticColors.tertiary,
                    modifier = Modifier.weight(1f),
                    onClick = { showNewProjectDialog = true },
                )
                GatewayActionCard(
                    title = "Open Folder",
                    body = "Jump into an existing workspace using Android's document tree picker.",
                    icon = Icons.Default.FolderOpen,
                    accent = KineticColors.primary,
                    modifier = Modifier.weight(1f),
                    onClick = openWorkspacePicker,
                )
                GatewayActionCard(
                    title = "Clone Repository",
                    body = "Sign in with GitHub to browse repos, or paste an HTTPS URL and clone to device storage.",
                    icon = Icons.Default.Link,
                    accent = KineticColors.railAccent,
                    modifier = Modifier.weight(1f),
                    onClick = { showCloneDialog = true },
                )
            }

            Spacer(Modifier.height(22.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = KineticColors.primary,
                        )
                        Text(
                            text = "Recent Workspaces",
                            style = MaterialTheme.typography.titleMedium,
                            color = KineticColors.onSurface,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                    Text(
                        text = "Reopen something recent without dropping straight into an empty shell.",
                        color = KineticColors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
                    )
                    if (recentWorkspaces.isEmpty()) {
                        Text(
                            text = "No recent workspaces yet. Open a folder or create a project to"
                                + " start building your history.",
                            color = KineticColors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        recentWorkspaces.forEachIndexed { index, entry ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                                )
                            }
                            RecentWorkspaceRow(
                                entry = entry,
                                onOpen = { onOpenRecentWorkspace(entry.uriString) },
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
        )
    }
}

@Composable
private fun GatewayActionCard(
    title: String,
    body: String,
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(accent.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = KineticColors.onSurface,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = KineticColors.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            TextButton(
                onClick = onClick,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Open")
            }
        }
    }
}

@Composable
private fun RecentWorkspaceRow(
    entry: RecentWorkspaceEntry,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    KineticColors.primary.copy(alpha = 0.12f),
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = KineticColors.primary,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = entry.displayName,
                color = KineticColors.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = DateUtils.getRelativeTimeSpanString(
                    entry.lastOpenedAtMs,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                ).toString(),
                color = KineticColors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.width(10.dp))
        TextButton(onClick = onOpen) {
            Icon(Icons.Default.NorthEast, contentDescription = null)
            Text("Open", modifier = Modifier.padding(start = 4.dp))
        }
    }
}

