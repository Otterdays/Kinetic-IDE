package com.tabletaide.ide.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tabletaide.ide.data.AuditEntry
import com.tabletaide.ide.ui.theme.KineticColors

@Composable
fun AuditTimelinePanel(
    visible: Boolean,
    entries: List<AuditEntry>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "AGENT AUDIT TIMELINE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = KineticColors.outline,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onRefresh) {
                            Text("Refresh", fontSize = 11.sp)
                        }
                        TextButton(onClick = onClear) {
                            Text("Clear all", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Close", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()

                val toolFilter by remember { mutableStateOf("") }
                if (entries.isEmpty() && !loading) {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        "No audit entries yet. Agent tool calls will appear here after they run.",
                        fontSize = 13.sp,
                        color = KineticColors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                } else if (loading) {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        "Loading…",
                        fontSize = 13.sp,
                        color = KineticColors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${entries.size} entries (newest first)",
                        fontSize = 10.sp,
                        color = KineticColors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(entries, key = { it.id }) { entry ->
                            AuditEntryCard(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditEntryCard(entry: AuditEntry) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .background(
                when (entry.status) {
                    "FAILED" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                    "DENIED" -> KineticColors.surfaceContainerHighest.copy(alpha = 0.3f)
                    else -> KineticColors.surfaceContainerHighest.copy(alpha = 0.15f)
                },
                RoundedCornerShape(8.dp),
            )
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(entry.status)
                Text(
                    entry.toolName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = KineticColors.onSurface,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Text(
                    " → ${entry.target}",
                    fontSize = 11.sp,
                    color = KineticColors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp).weight(1f, fill = false),
                )
            }
            Text(
                entry.timestamp,
                fontSize = 10.sp,
                color = KineticColors.outline,
                fontFamily = FontFamily.Monospace,
            )
        }
        Row(
            modifier = Modifier.padding(top = 4.dp, start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Tag(entry.status)
            Tag(entry.riskClass)
            Tag(entry.policyMode)
            if (entry.approvalOutcome.isNotBlank()) {
                Tag(entry.approvalOutcome)
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp, start = 16.dp)) {
                HorizontalDivider(color = KineticColors.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(6.dp))
                DetailRow("Provider", entry.provider)
                DetailRow("Status", entry.status)
                DetailRow("Risk", entry.riskClass)
                DetailRow("Policy", entry.policyMode)
                DetailRow("Approval", entry.approvalOutcome)
                DetailRow("Duration", "${entry.durationMs}ms")
                if (entry.mutationSummary.isNotBlank()) {
                    DetailRow("Mutation", entry.mutationSummary)
                }
            }
        }
    }
}

@Composable
private fun StatusDot(status: String) {
    val color = when (status) {
        "OK" -> KineticColors.primary
        "FAILED" -> MaterialTheme.colorScheme.error
        "DENIED" -> KineticColors.outline
        else -> KineticColors.onSurfaceVariant
    }
    Text(
        "●",
        fontSize = 10.sp,
        color = color,
    )
}

@Composable
private fun Tag(text: String) {
    Text(
        text.uppercase(),
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
        color = KineticColors.onSurfaceVariant,
        modifier = Modifier
            .background(KineticColors.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            "$label:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = KineticColors.outline,
            modifier = Modifier.width(72.dp),
        )
        Text(
            value,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = KineticColors.onSurface,
        )
    }
}
