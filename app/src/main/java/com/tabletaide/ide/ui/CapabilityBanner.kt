package com.tabletaide.ide.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tabletaide.ide.data.CommandRunnerUiState
import com.tabletaide.ide.data.GitRepoUiState

@Composable
fun CapabilityBanner(
    hasWorkspaceRoot: Boolean,
    hasAllFilesAccess: Boolean,
    gitState: GitRepoUiState,
    runnerState: CommandRunnerUiState,
    onOpenAllFilesAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val notices = remember(
        hasWorkspaceRoot,
        gitState.available,
        gitState.message,
        runnerState.available,
        runnerState.availabilityMessage,
    ) {
        when {
            !hasWorkspaceRoot -> listOf(
                "Open a workspace to enable editor saves, AI file tools, git, and command execution.",
            )
            else -> buildList {
                gitState.message
                    ?.trim()
                    ?.takeIf { !gitState.available && it.isNotEmpty() }
                    ?.let { add("Git: $it") }
                runnerState.availabilityMessage
                    ?.trim()
                    ?.takeIf { !runnerState.available && it.isNotEmpty() }
                    ?.let { add("Run: $it") }
            }
        }
    }
    val needsAllFilesAccess = hasWorkspaceRoot &&
        !hasAllFilesAccess &&
        notices.any { it.contains("All files access", ignoreCase = true) }
    if (notices.isEmpty() && !needsAllFilesAccess) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (hasWorkspaceRoot) {
                    "Workspace capability check"
                } else {
                    "Workspace required"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (hasWorkspaceRoot) {
            Text(
                text = "Use a shared-storage folder (Downloads/Documents) for git, push, and shell. Grant All files access when git or run is blocked.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        notices.forEach { notice ->
            Text(
                text = "• $notice",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (needsAllFilesAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            TextButton(onClick = onOpenAllFilesAccess) {
                Text("Grant All files access")
            }
        }
    }
}
