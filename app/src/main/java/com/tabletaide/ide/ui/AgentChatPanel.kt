package com.tabletaide.ide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tabletaide.ide.data.LlmCredentialState
import com.tabletaide.ide.data.LlmProvider
import com.tabletaide.ide.data.LlmSelectionState
import com.tabletaide.ide.data.TelemetrySummary
import com.tabletaide.ide.ui.theme.KineticColors
import java.util.Locale

@Composable
fun AgentChatPanel(
    lines: List<AgentViewModel.ChatLine>,
    busy: Boolean,
    enhancingPrompt: Boolean,
    error: String?,
    selection: LlmSelectionState,
    credentials: LlmCredentialState,
    telemetrySummary: TelemetrySummary,
    composerDraft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onEnhancePrompt: () -> Unit,
    onClear: () -> Unit,
    onModelSelect: (LlmProvider, String) -> Unit,
    onOpenApiKeys: () -> Unit,
    /** Workspace-relative revert for captured write/edit tool rows ([TRACE: DOCS/ROADMAP.md] Epic 2.2). */
    onRevertToolMutation: (String) -> Unit,
    onApplyToolMutation: (String) -> Unit,
    /** Restore workspace state from before a `run_command` execution. */
    onRevertCommandMutation: (String) -> Unit,
    onReapplyCommandMutation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var modelPickerVisible by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val currentProvider = selection.provider
    val hasSelectedProviderKey = credentials.hasKey(currentProvider)
    val sendEnabled = !busy && !enhancingPrompt && composerDraft.isNotBlank() && hasSelectedProviderKey
    val enhanceEnabled =
        !busy && !enhancingPrompt && composerDraft.isNotBlank() && hasSelectedProviderKey
    val modelLabel = if (hasSelectedProviderKey) {
        "${selection.provider.displayName} · ${selection.label}"
    } else {
        "Add API key"
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(KineticColors.primary),
                )
                Column {
                    Text(
                        text = "AI ARCHITECT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = KineticColors.onSurface,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                    Text(
                        text = modelLabel,
                        fontSize = 9.sp,
                        color = KineticColors.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { modelPickerVisible = true },
                        maxLines = 1,
                    )
                    ModelPickerDialog(
                        visible = modelPickerVisible,
                        currentProvider = selection.provider,
                        currentModelId = selection.modelId,
                        credentials = credentials,
                        onDismiss = { modelPickerVisible = false },
                        onSelect = onModelSelect,
                        onOpenApiKeys = {
                            modelPickerVisible = false
                            onOpenApiKeys()
                        },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenApiKeys, enabled = !busy) {
                    Icon(Icons.Default.Key, contentDescription = "API keys", tint = KineticColors.onSurfaceVariant)
                }
                IconButton(onClick = onClear, enabled = !busy) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = KineticColors.onSurfaceVariant)
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KineticColors.surfaceVariant.copy(alpha = 0.4f))
                .border(1.dp, KineticColors.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "ASSISTANT STATUS · ${modelLabel.uppercase()}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = KineticColors.outline,
                )
                Text(
                    when {
                        enhancingPrompt -> "ENHANCING"
                        busy -> "WORKING"
                        hasSelectedProviderKey -> "READY"
                        else -> "NEEDS KEY"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = KineticColors.primary,
                )
            }
            if (!hasSelectedProviderKey) {
                Text(
                    "Add an API key with the key icon to start chatting.",
                    fontSize = 10.sp,
                    color = KineticColors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            TelemetrySummaryStrip(
                summary = telemetrySummary,
                modifier = Modifier.padding(top = 10.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(KineticColors.surfaceContainerLowest),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(if (busy) 0.92f else 0.45f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(KineticColors.primary.copy(alpha = if (busy) 0.85f else 0.35f)),
                )
            }
        }
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(lines, key = { it.id }) { line ->
                when (line) {
                    is AgentViewModel.ChatLine.User -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                text = line.text,
                                fontSize = 12.sp,
                                color = KineticColors.onSurfaceVariant,
                            )
                        }
                    }
                    is AgentViewModel.ChatLine.AssistantStreaming -> {
                        AssistantBubble(line.text.ifEmpty { "…" })
                    }
                    is AgentViewModel.ChatLine.AssistantDone -> {
                        AssistantBubble(line.text)
                    }
                    is AgentViewModel.ChatLine.Tool -> {
                        var expanded by remember(line.id) { mutableStateOf(false) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(KineticColors.primary.copy(alpha = 0.08f))
                                .border(
                                    1.dp,
                                    KineticColors.primary.copy(alpha = 0.25f),
                                    RoundedCornerShape(12.dp),
                                )
                                .padding(12.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "TOOL · ${line.name} · ${line.receipt.status}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (line.receipt.status == "OK") {
                                        KineticColors.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                )
                                Text(
                                    if (expanded) "▼" else "▶",
                                    fontSize = 10.sp,
                                    color = KineticColors.outline,
                                )
                            }
                            Text(
                                text = if (expanded) line.resultFull else line.preview,
                                fontSize = 11.sp,
                                color = KineticColors.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .clickable { expanded = true },
                            )
                            if (expanded) {
                                ToolReceiptView(line.receipt)
                                val cmd = line.commandMutation
                                if (cmd != null && line.receipt.status == "OK") {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        Text(
                                            "WORKSPACE CHANGES · ${cmd.diff.summary()}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = KineticColors.outline,
                                        )
                                        val changedPreview = (cmd.diff.added.map { "+ $it" } +
                                            cmd.diff.modified.map { "~ $it" } +
                                            cmd.diff.deleted.map { "- $it" }).take(8)
                                        changedPreview.forEach { lineText ->
                                            Text(
                                                lineText,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = KineticColors.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp),
                                            )
                                        }
                                        val total = cmd.diff.added.size + cmd.diff.modified.size + cmd.diff.deleted.size
                                        if (total > changedPreview.size) {
                                            Text(
                                                "… +${total - changedPreview.size} more",
                                                fontSize = 10.sp,
                                                color = KineticColors.outline,
                                                modifier = Modifier.padding(top = 2.dp),
                                            )
                                        }
                                        when (cmd.phase) {
                                            AgentViewModel.CommandMutationPhase.APPLIED_ON_DISK -> {
                                                TextButton(
                                                    onClick = { onRevertCommandMutation(line.id) },
                                                    enabled = !busy,
                                                ) {
                                                    Text("Revert command changes", fontSize = 11.sp)
                                                }
                                            }
                                            AgentViewModel.CommandMutationPhase.REVERTED -> {
                                                TextButton(
                                                    onClick = { onReapplyCommandMutation(line.id) },
                                                    enabled = !busy,
                                                ) {
                                                    Text("Reapply command changes", fontSize = 11.sp)
                                                }
                                            }
                                            AgentViewModel.CommandMutationPhase.CONFLICT -> {
                                                Text(
                                                    "Conflicts blocked some restores. Manual review needed.",
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(top = 6.dp),
                                                )
                                            }
                                        }
                                        cmd.conflicts.take(4).forEach { c ->
                                            Text(
                                                "• ${c.path}: ${c.reason}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(top = 2.dp),
                                            )
                                        }
                                    }
                                }
                                val mut = line.mutation
                                if (mut != null && line.receipt.status == "OK") {
                                    when (mut.phase) {
                                        AgentViewModel.ToolMutationPhase.APPLIED_ON_DISK -> {
                                            Row(
                                                modifier = Modifier.padding(top = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                TextButton(
                                                    onClick = { onRevertToolMutation(line.id) },
                                                    enabled = !busy,
                                                ) {
                                                    Text("Revert file", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                        AgentViewModel.ToolMutationPhase.REVERTED -> {
                                            Row(
                                                modifier = Modifier.padding(top = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                TextButton(
                                                    onClick = { onApplyToolMutation(line.id) },
                                                    enabled = !busy,
                                                ) {
                                                    Text("Apply again", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                        AgentViewModel.ToolMutationPhase.CONFLICT -> {
                                            Text(
                                                mut.conflictDetail ?: "Conflict",
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(top = 8.dp),
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Request (JSON)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KineticColors.outline,
                                    )
                                    TextButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(line.inputJson))
                                        },
                                        modifier = Modifier.height(28.dp),
                                    ) {
                                        Text(
                                            "Copy JSON",
                                            fontSize = 10.sp,
                                            color = KineticColors.primary,
                                        )
                                    }
                                }
                                Text(
                                    line.inputJson,
                                    fontSize = 10.sp,
                                    color = KineticColors.onSurfaceVariant,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(16.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Column {
                    TextField(
                        value = composerDraft,
                        onValueChange = onDraftChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy && !enhancingPrompt,
                        minLines = 3,
                        maxLines = 5,
                        placeholder = {
                            Text("Ask the Architect…", color = KineticColors.outline, fontSize = 12.sp)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = KineticColors.surfaceBright,
                            unfocusedContainerColor = KineticColors.surfaceBright,
                            disabledContainerColor = KineticColors.surfaceBright.copy(alpha = 0.5f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = KineticColors.primary,
                            focusedTextColor = KineticColors.onSurface,
                            unfocusedTextColor = KineticColors.onSurface,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                if (!hasSelectedProviderKey) {
                                    onOpenApiKeys()
                                } else {
                                    onEnhancePrompt()
                                }
                            },
                            enabled = composerDraft.isNotBlank() && !busy && !enhancingPrompt,
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = KineticColors.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (enhancingPrompt) "Enhancing…" else "Enhance prompt",
                                color = KineticColors.primary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                if (!hasSelectedProviderKey) {
                                    onOpenApiKeys()
                                    return@IconButton
                                }
                                onSend()
                            },
                            enabled = sendEnabled,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(KineticColors.primary, KineticColors.primaryDim),
                                    ),
                                ),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                null,
                                tint = KineticColors.onPrimaryFixed,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetrySummaryStrip(
    summary: TelemetrySummary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KineticColors.surfaceContainerHighest.copy(alpha = 0.35f))
            .border(1.dp, KineticColors.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "TELEMETRY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = KineticColors.outline,
            )
            Text(
                summary.lastEventLabel,
                fontSize = 9.sp,
                color = KineticColors.onSurfaceVariant,
            )
        }
        Text(
            "${summary.turnCount} turns · ${summary.totalTokens} est tokens · ${summary.eventCount} events · ${formatUsd(summary.estimatedCostUsd)} est",
            fontSize = 10.sp,
            color = KineticColors.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            "First token avg ${summary.averageFirstTokenMs?.let { "${it}ms" } ?: "—"} · tool p95 ${summary.p95ToolMs?.let { "${it}ms" } ?: "—"} · failures ${summary.failureCount}",
            fontSize = 10.sp,
            color = KineticColors.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun ToolReceiptView(receipt: AgentViewModel.ToolReceipt) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(KineticColors.surfaceContainerHighest.copy(alpha = 0.45f))
            .border(1.dp, KineticColors.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Text(
            "RECEIPT",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = KineticColors.outline,
        )
        Text(
            "${receipt.startedAt} · ${receipt.durationMs}ms · ${receipt.providerName}",
            fontSize = 10.sp,
            color = KineticColors.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            "${receipt.toolName} → ${receipt.target}",
            fontSize = 11.sp,
            color = KineticColors.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            "Risk: ${receipt.riskClass} · Policy: ${receipt.policyMode} · Decision: ${receipt.approvalOutcome}",
            fontSize = 10.sp,
            color = KineticColors.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun formatUsd(value: Double): String =
    "$" + String.format(Locale.US, "%.4f", value)

@Composable
private fun AssistantBubble(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KineticColors.surfaceContainerHighest.copy(alpha = 0.5f))
            .border(1.dp, KineticColors.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = KineticColors.onSurface,
            lineHeight = 18.sp,
        )
    }
}
