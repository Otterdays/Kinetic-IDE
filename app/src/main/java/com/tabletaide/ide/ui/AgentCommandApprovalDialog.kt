package com.tabletaide.ide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tabletaide.ide.data.AgentToolApprovalState
import com.tabletaide.ide.data.CommandRunnerUiState
import com.tabletaide.ide.ui.theme.KineticColors

@Composable
fun AgentToolApprovalDialog(
    approvalState: AgentToolApprovalState,
    runnerState: CommandRunnerUiState,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
) {
    if (!approvalState.visible) return

    val targetText = approvalState.target.ifBlank { "Workspace" }
    val confirmLabel = when (approvalState.toolName) {
        "run_command" -> "Run command"
        "delete_path" -> "Delete"
        else -> "Approve"
    }

    AlertDialog(
        onDismissRequest = onDeny,
        title = { Text("Approve tool action") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "The AI agent wants to perform a ${approvalState.riskClass.displayName.lowercase()} action.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${approvalState.toolName} -> $targetText",
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = runnerState.workspacePath.ifBlank {
                        approvalState.workspacePath.ifBlank { "Workspace path unavailable" }
                    },
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp),
                    color = KineticColors.onSurfaceVariant,
                )
                Text(
                    text = "Policy: ${approvalState.policyMode.displayName}",
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                approvalState.commandPreview?.let { preview ->
                    Text(
                        text = "Risk: ${preview.level.displayName}",
                        modifier = Modifier.padding(top = 10.dp),
                        color = when (preview.level) {
                            com.tabletaide.ide.data.CommandRiskLevel.HIGH -> MaterialTheme.colorScheme.error
                            com.tabletaide.ide.data.CommandRiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
                            com.tabletaide.ide.data.CommandRiskLevel.LOW -> MaterialTheme.colorScheme.primary
                        },
                    )
                    preview.reasons.forEach { reason ->
                        Text(
                            text = "• $reason",
                            color = KineticColors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    if (preview.likelyTargets.isNotEmpty()) {
                        Text(
                            text = "Likely targets:",
                            color = KineticColors.outline,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        preview.likelyTargets.take(6).forEach { target ->
                            Text(
                                text = target,
                                fontFamily = FontFamily.Monospace,
                                color = KineticColors.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = approvalState.assistantExplanation,
                    color = KineticColors.onSurface,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    text = "Request JSON",
                    color = KineticColors.outline,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = approvalState.inputJson,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .background(KineticColors.surfaceContainerHighest.copy(alpha = 0.45f))
                        .border(
                            1.dp,
                            KineticColors.outlineVariant.copy(alpha = 0.35f),
                            androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        )
                        .padding(10.dp),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text("Deny")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onApprove,
                enabled = if (approvalState.toolName == "run_command") {
                    runnerState.available && !runnerState.busy
                } else {
                    true
                },
            ) {
                Text(confirmLabel)
            }
        },
    )
}
