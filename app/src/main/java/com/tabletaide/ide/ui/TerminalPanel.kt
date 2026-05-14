package com.tabletaide.ide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tabletaide.ide.data.CommandRunnerUiState
import com.tabletaide.ide.ui.theme.KineticColors

private enum class TerminalTab { Terminal, Output, Debug }

@Composable
fun TerminalPanel(
    runnerState: CommandRunnerUiState,
    onRun: () -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = TerminalTab.entries
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KineticColors.surfaceContainerLowest),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, terminalTab ->
                    val selected = index == tab
                    Text(
                        text = terminalTab.name.uppercase().replace('_', ' '),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (selected) {
                            KineticColors.primary
                        } else {
                            KineticColors.onSurfaceVariant
                        },
                        modifier = Modifier
                            .clickable { tab = index }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onRun,
                    enabled = runnerState.available && !runnerState.busy,
                ) {
                    Text("Run", fontSize = 11.sp)
                }
                TextButton(
                    onClick = onCancel,
                    enabled = runnerState.busy,
                ) {
                    Text("Cancel", fontSize = 11.sp)
                }
                TextButton(
                    onClick = onClear,
                    enabled = runnerState.available,
                ) {
                    Text("Clear", fontSize = 11.sp)
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (runnerState.workspaceLabel.isNotBlank()) {
                Text(
                    text = "Workspace: ${runnerState.workspaceLabel}",
                    color = KineticColors.secondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            runnerState.errorMessage?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            val panelText = when (tabs[tab]) {
                TerminalTab.Terminal -> runnerState.terminalText
                TerminalTab.Output -> runnerState.outputText
                TerminalTab.Debug -> runnerState.debugText
            }.ifBlank {
                runnerState.availabilityMessage ?: "No terminal output yet."
            }
            Text(
                text = panelText,
                color = KineticColors.onSurfaceVariant,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
