package com.tabletaide.ide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tabletaide.ide.data.LlmCredentialState
import com.tabletaide.ide.data.LlmModelCatalog
import com.tabletaide.ide.data.LlmModelOption
import com.tabletaide.ide.data.LlmProvider

@Composable
fun ModelPickerDialog(
    visible: Boolean,
    currentProvider: LlmProvider,
    currentModelId: String,
    credentials: LlmCredentialState,
    onDismiss: () -> Unit,
    onSelect: (LlmProvider, String) -> Unit,
    onOpenApiKeys: () -> Unit,
) {
    if (!visible) return
    var filter by remember { mutableStateOf("") }
    val filtered = remember(filter, credentials) {
        LlmModelCatalog.filter(filter, credentials)
    }
    val grouped = remember(filtered) {
        LlmProvider.entries.associateWith { provider ->
            filtered.filter { it.provider == provider }
        }.filterValues { it.isNotEmpty() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Model") },
        text = {
            Column {
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Filter models") },
                    placeholder = { Text("Claude, Gemini, gpt-4o, openrouter…") },
                    singleLine = true,
                )
                Text(
                    text = "Providers without an API key are greyed out. Add keys from the key icon.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    grouped.forEach { (provider, models) ->
                        item(key = "header-${provider.id}") {
                            ProviderSectionHeader(
                                provider = provider,
                                hasKey = credentials.hasKey(provider),
                            )
                        }
                        items(models, key = { it.id }) { option ->
                            ModelPickerRow(
                                option = option,
                                selected = option.provider == currentProvider && option.id == currentModelId,
                                enabled = credentials.hasKey(option.provider),
                                onClick = {
                                    onSelect(option.provider, option.id)
                                    onDismiss()
                                },
                            )
                        }
                    }
                    if (grouped.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = "No models match \"$filter\".",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenApiKeys) {
                Text("API keys")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun ProviderSectionHeader(
    provider: LlmProvider,
    hasKey: Boolean,
) {
    Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
        HorizontalDivider()
        Text(
            text = buildString {
                append(provider.displayName)
                if (!hasKey) append(" · needs API key")
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (hasKey) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            },
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ModelPickerRow(
    option: LlmModelOption,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = buildString {
                    append(option.displayName)
                    if (selected) append(" ✓")
                },
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    selected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = option.id,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (enabled) 1f else 0.38f,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
