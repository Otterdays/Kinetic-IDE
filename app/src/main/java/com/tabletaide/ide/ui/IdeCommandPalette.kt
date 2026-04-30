package com.tabletaide.ide.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tabletaide.ide.ui.theme.KineticColors

data class PaletteCommand(
    val title: String,
    val subtitle: String,
    /** Lowercase tokens used for fuzzy-ish filtering */
    val keywords: List<String>,
    val enabled: Boolean = true,
    val onInvoke: () -> Unit,
)

/**
 * Quick-action palette — [TRACE: DOCS/ROADMAP.md] Epic 1.3 command palette MVP.
 */
@Composable
fun IdeCommandPalette(
    visible: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    commands: List<PaletteCommand>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val q = query.trim().lowercase()
    val filtered = remember(q, commands) {
        if (q.isEmpty()) commands
        else {
            commands.filter { cmd ->
                val hay =
                    (cmd.title + " " + cmd.subtitle + " " + cmd.keywords.joinToString(" ")).lowercase()
                q.all { ch -> hay.contains(ch) }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.55f)
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Command palette",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = KineticColors.outline,
                )
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    placeholder = { Text("Filter commands…", fontSize = 13.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = KineticColors.surfaceBright,
                        unfocusedContainerColor = KineticColors.surfaceBright,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = KineticColors.primary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = { "${it.title}|${it.subtitle}" }) { cmd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = cmd.enabled) {
                                    cmd.onInvoke()
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    cmd.title,
                                    fontSize = 14.sp,
                                    color = if (cmd.enabled) KineticColors.onSurface else KineticColors.outline,
                                )
                                Text(
                                    cmd.subtitle,
                                    fontSize = 11.sp,
                                    color = KineticColors.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Shortcuts: Ctrl+P palette · Ctrl+S save · Ctrl+Shift+S save all · Ctrl+W close tab",
                    fontSize = 10.sp,
                    color = KineticColors.onSurfaceVariant,
                )
            }
        }
    }
}
