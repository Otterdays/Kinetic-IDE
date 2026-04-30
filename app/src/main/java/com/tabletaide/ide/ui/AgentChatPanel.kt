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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tabletaide.ide.data.LlmCredentialState
import com.tabletaide.ide.data.LlmProvider
import com.tabletaide.ide.ui.theme.KineticColors

@Composable
fun AgentChatPanel(
    lines: List<AgentViewModel.ChatLine>,
    busy: Boolean,
    error: String?,
    currentProvider: LlmProvider,
    credentials: LlmCredentialState,
    onSend: (String) -> Unit,
    onClear: () -> Unit,
    onProviderChange: (LlmProvider) -> Unit,
    onOpenApiKeys: () -> Unit,
    /** Workspace-relative revert for captured write/edit tool rows ([TRACE: DOCS/ROADMAP.md] Epic 2.2). */
    onRevertToolMutation: (String) -> Unit,
    onApplyToolMutation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    var providerMenuOpen by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
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
                    Box {
                        Text(
                            text = currentProvider.displayName,
                            fontSize = 9.sp,
                            color = KineticColors.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { providerMenuOpen = true }
                        )
                        DropdownMenu(
                            expanded = providerMenuOpen,
                            onDismissRequest = { providerMenuOpen = false },
                        ) {
                            LlmProvider.entries.forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider.displayName) },
                                    onClick = {
                                        onProviderChange(provider)
                                        providerMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
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
                    "ASSISTANT STATUS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = KineticColors.outline,
                )
                Text(
                    when {
                        busy -> "WORKING"
                        credentials.hasKey(currentProvider) -> "READY"
                        else -> "NEEDS KEY"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = KineticColors.primary,
                )
            }
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
                TextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
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
                IconButton(
                    onClick = {
                        onSend(draft)
                        draft = ""
                    },
                    enabled = !busy && draft.isNotBlank(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(KineticColors.primary, KineticColors.primaryDim),
                            ),
                        ),
                ) {
                    Icon(Icons.Default.Send, null, tint = KineticColors.onPrimaryFixed)
                }
            }
        }
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
    }
}

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
