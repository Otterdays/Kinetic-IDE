package com.tabletaide.ide.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tabletaide.ide.ui.theme.KineticColors

private enum class TerminalTab { Terminal, Output, Debug }

@Composable
fun TerminalPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
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
        ) {
            tabs.forEachIndexed { index, t ->
                val sel = index == tab
                Text(
                    text = t.name.uppercase().replace('_', ' '),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (sel) KineticColors.primary else KineticColors.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { tab = index }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            when (tabs[tab]) {
                TerminalTab.Terminal -> {
                    Text(
                        text = "kinetic-syntax",
                        color = KineticColors.secondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = "~ % Shell not connected (use Termux for a real PTY).",
                        color = KineticColors.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = "[INFO] Workspace SAF I/O is active — agent can read/write files.",
                        color = KineticColors.onSurface.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    TextButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://termux.com/")),
                            )
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("Termux setup", color = KineticColors.primary, fontSize = 12.sp)
                    }
                }
                TerminalTab.Output -> {
                    Text(
                        "Build output will appear here.",
                        color = KineticColors.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                TerminalTab.Debug -> {
                    Text(
                        "Debug console — connect in Phase 2.",
                        color = KineticColors.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}
