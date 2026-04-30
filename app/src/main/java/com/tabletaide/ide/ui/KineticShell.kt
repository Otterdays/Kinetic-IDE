package com.tabletaide.ide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tabletaide.ide.ui.theme.KineticColors

@Composable
fun KineticNavRail(
    section: RailSection,
    onSection: (RailSection) -> Unit,
    onOpenWorkspace: () -> Unit,
    onSearchStub: () -> Unit,
    onExtensionsStub: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        header = {
            Text(
                text = "KS",
                color = KineticColors.railAccent,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        },
    ) {
        Spacer(Modifier.height(8.dp))
        NavigationRailItem(
            selected = section == RailSection.Projects,
            onClick = {
                onSection(RailSection.Projects)
                onOpenWorkspace()
            },
            icon = { Icon(Icons.Default.FolderOpen, null) },
            label = { RailLabel("Projects") },
            colors = railItemColors(section == RailSection.Projects),
        )
        NavigationRailItem(
            selected = section == RailSection.Explorer,
            onClick = { onSection(RailSection.Explorer) },
            icon = { Icon(Icons.Default.Inventory2, null) },
            label = { RailLabel("Explorer") },
            colors = railItemColors(section == RailSection.Explorer),
        )
        NavigationRailItem(
            selected = section == RailSection.Search,
            onClick = {
                onSection(RailSection.Search)
                onSearchStub()
            },
            icon = { Icon(Icons.Default.Search, null) },
            label = { RailLabel("Search") },
            colors = railItemColors(section == RailSection.Search),
        )
        NavigationRailItem(
            selected = section == RailSection.Extensions,
            onClick = {
                onSection(RailSection.Extensions)
                onExtensionsStub()
            },
            icon = { Icon(Icons.Default.Extension, null) },
            label = { RailLabel("Extensions") },
            colors = railItemColors(section == RailSection.Extensions),
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = { /* AI panel always visible */ },
            modifier = Modifier
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(50))
                .background(KineticColors.onSurface.copy(alpha = 0.06f)),
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = "AI",
                tint = KineticColors.railAccent,
            )
        }
        NavigationRailItem(
            selected = false,
            onClick = { /* settings stub */ },
            icon = { Icon(Icons.Default.Settings, null) },
            label = { RailLabel("Settings") },
            colors = NavigationRailItemDefaults.colors(
                unselectedIconColor = KineticColors.onSurfaceVariant,
                unselectedTextColor = KineticColors.onSurfaceVariant,
            ),
        )
    }
}

@Composable
private fun RailLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 9.sp,
        letterSpacing = 0.08.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun railItemColors(selected: Boolean) = NavigationRailItemDefaults.colors(
    selectedIconColor = KineticColors.railAccent,
    selectedTextColor = KineticColors.railAccent,
    indicatorColor = KineticColors.railAccent.copy(alpha = 0.18f),
    unselectedIconColor = KineticColors.onSurfaceVariant,
    unselectedTextColor = KineticColors.onSurfaceVariant,
)

@Composable
fun KineticTopBar(
    tabs: List<EditorTab>,
    selectedIndex: Int,
    canUndoNow: Boolean,
    canRedoNow: Boolean,
    onSelectTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    onSave: () -> Unit,
    onSaveAll: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onExecute: () -> Unit,
    agentBusy: Boolean,
    modifier: Modifier = Modifier,
) {
    val tabScroll = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Kinetic IDE",
                color = KineticColors.railAccent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 16.dp),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(tabScroll),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val sel = index == selectedIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tab.fileName + if (tab.dirty) " ·" else "",
                                fontSize = 13.sp,
                                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (sel) KineticColors.railAccent else KineticColors.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { onSelectTab(index) }
                                    .padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
                            )
                            IconButton(
                                onClick = { onCloseTab(index) },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close tab",
                                    tint = KineticColors.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        if (sel) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(KineticColors.railAccent),
                            )
                        } else {
                            Spacer(Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (agentBusy) "Agent…" else "Ready",
                fontSize = 11.sp,
                color = KineticColors.onSurfaceVariant,
            )
            IconButton(onClick = onUndo, enabled = canUndoNow) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    "Undo",
                    tint = if (canUndoNow) KineticColors.onSurface else KineticColors.outline.copy(alpha = 0.4f),
                )
            }
            IconButton(onClick = onRedo, enabled = canRedoNow) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    "Redo",
                    tint = if (canRedoNow) KineticColors.onSurface else KineticColors.outline.copy(alpha = 0.4f),
                )
            }
            IconButton(onClick = onSave, enabled = tabs.isNotEmpty()) {
                Icon(Icons.Default.Save, "Save", tint = KineticColors.primary)
            }
            IconButton(onClick = onSaveAll, enabled = tabs.any { it.dirty }) {
                Icon(Icons.Default.DoneAll, "Save all", tint = KineticColors.primary)
            }
            Text(
                text = "Debug",
                fontSize = 12.sp,
                color = KineticColors.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable { }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(KineticColors.primary, KineticColors.primaryDim),
                        ),
                    )
                    .clickable(onClick = onExecute)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    null,
                    tint = KineticColors.onPrimaryFixed,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Execute",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KineticColors.onPrimaryFixed,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

@Composable
fun BreadcrumbBar(
    path: String?,
    modifier: Modifier = Modifier,
) {
    val parts = path?.split('/')?.filter { it.isNotEmpty() } ?: emptyList()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(KineticColors.surfaceContainerLowest.copy(alpha = 0.35f))
            .padding(horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (parts.isEmpty()) {
            Text(
                text = "NO FILE OPEN",
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = KineticColors.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
        } else {
            parts.forEachIndexed { i, seg ->
                if (i > 0) {
                    Text(
                        "/",
                        fontSize = 10.sp,
                        color = KineticColors.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
                Text(
                    text = seg.uppercase(),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = if (i == parts.lastIndex) KineticColors.onSurface else KineticColors.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
fun KineticStatusBar(
    activePath: String?,
    agentBusy: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(KineticColors.background)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Kinetic IDE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = KineticColors.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Text(
            "main",
            fontSize = 10.sp,
            color = KineticColors.railAccent,
            modifier = Modifier.padding(start = 16.dp),
            fontFamily = FontFamily.Monospace,
        )
        Text(
            activePath?.substringAfterLast('/') ?: "—",
            fontSize = 10.sp,
            color = KineticColors.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp),
            fontFamily = FontFamily.Monospace,
        )
        Text(
            "UTF-8",
            fontSize = 10.sp,
            color = KineticColors.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp),
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (agentBusy) KineticColors.primary else KineticColors.onSurfaceVariant.copy(alpha = 0.4f),
                    ),
            )
            Text(
                text = if (agentBusy) "AI LIVE" else "AI READY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = KineticColors.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
