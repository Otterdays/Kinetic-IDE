package com.tabletaide.ide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.tabletaide.ide.data.LlmCredentialState
import com.tabletaide.ide.data.LlmModelCatalog
import com.tabletaide.ide.data.LlmModelOption
import com.tabletaide.ide.data.LlmProvider
import com.tabletaide.ide.data.ModelPickerUiState
import com.tabletaide.ide.data.ProviderModelSection
import com.tabletaide.ide.data.ProviderModelStatus

@Composable
fun ModelPickerDialog(
    visible: Boolean,
    currentProvider: LlmProvider,
    currentModelId: String,
    credentials: LlmCredentialState,
    pickerState: ModelPickerUiState,
    onDismiss: () -> Unit,
    onSelect: (LlmProvider, String) -> Unit,
    onOpenApiKeys: () -> Unit,
    onLoadModels: () -> Unit,
) {
    if (!visible) return

    LaunchedEffect(visible) {
        if (visible) onLoadModels()
    }

    var filter by remember { mutableStateOf("") }
    val filtered = remember(filter, pickerState.models) {
        LlmModelCatalog.filter(pickerState.models, filter)
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
                    placeholder = { Text("Search loaded models…") },
                    singleLine = true,
                )
                Text(
                    text = "Gemini and OpenRouter load live. Other providers need a manual model ID later.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                if (pickerState.loading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading models…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    LlmProvider.entries.forEach { provider ->
                        val section = pickerState.sectionFor(provider)
                        val hasKey = credentials.hasKey(provider)
                        val providerModels = filtered.filter { it.provider == provider }
                        item(key = "header-${provider.id}") {
                            ProviderSectionHeader(
                                provider = provider,
                                hasKey = hasKey,
                                section = section,
                            )
                        }
                        when (section.status) {
                            ProviderModelStatus.NoKey -> Unit
                            ProviderModelStatus.Loading -> {
                                item(key = "loading-${provider.id}") {
                                    Text(
                                        text = "Loading…",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                                    )
                                }
                            }
                            ProviderModelStatus.NotListed -> {
                                item(key = "notlisted-${provider.id}") {
                                    Text(
                                        text = "Model list not available yet",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    )
                                }
                            }
                            ProviderModelStatus.Failed -> {
                                item(key = "failed-${provider.id}") {
                                    Column(modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
                                        Text(
                                            text = section.errorMessage ?: "Could not load models.",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        TextButton(onClick = onLoadModels) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                            ProviderModelStatus.Ready -> {
                                if (provider == LlmProvider.OPENROUTER) {
                                    item(key = "or-note-${provider.id}") {
                                        Text(
                                            text = "Showing tool-capable models for agent use.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                                        )
                                    }
                                }
                                if (providerModels.isEmpty() && filter.isNotBlank()) {
                                    item(key = "empty-filter-${provider.id}") {
                                        Text(
                                            text = "No models match \"$filter\".",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                        )
                                    }
                                } else {
                                    items(providerModels, key = { it.id }) { option ->
                                        ModelPickerRow(
                                            option = option,
                                            selected = option.provider == currentProvider &&
                                                option.id == currentModelId,
                                            enabled = true,
                                            onClick = {
                                                onSelect(option.provider, option.id)
                                                onDismiss()
                                            },
                                        )
                                    }
                                }
                            }
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
    section: ProviderModelSection,
) {
    Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
        HorizontalDivider()
        Text(
            text = buildString {
                append(provider.displayName)
                when {
                    !hasKey -> append(" · needs API key")
                    section.status == ProviderModelStatus.NotListed -> append(" · list not wired")
                }
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
