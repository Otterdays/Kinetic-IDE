package com.tabletaide.ide.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tabletaide.ide.data.TreeRow
import com.tabletaide.ide.ui.theme.KineticColors

@OptIn(ExperimentalFoundationApi::class)
@Suppress("LongMethod")
@Composable
fun FileTreePane(
    rows: List<TreeRow>,
    treeFilterQuery: String,
    onTreeFilterQueryChange: (String) -> Unit,
    selectedPath: String?,
    favoritePaths: List<String>,
    recentPaths: List<String>,
    hasWorkspaceRoot: Boolean,
    onOpenWorkspace: () -> Unit,
    onSelectFile: (TreeRow) -> Unit,
    onOpenPinnedPath: (String) -> Unit,
    onToggleFavoritePath: (String) -> Unit,
    onCreateFile: (parentDirRelativeOrEmpty: String, leafName: String) -> Unit,
    onCreateFolderRelative: (relativeFolderPathTrimmed: String) -> Unit,
    onRename: (TreeRow, newLeafName: String) -> Unit,
    onDuplicate: (TreeRow) -> Unit,
    onDelete: (TreeRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuRow by remember { mutableStateOf<TreeRow?>(null) }
    var renameTarget by remember { mutableStateOf<TreeRow?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<TreeRow?>(null) }
    var createFileParent by remember { mutableStateOf<String?>(null) }

    val favShown = remember(favoritePaths) { favoritePaths.take(12) }
    val favSet = remember(favoritePaths) { favoritePaths.toSet() }
    val recentShown = remember(recentPaths, favSet) {
        recentPaths.asSequence().filterNot { it in favSet }.take(14).toList()
    }

    /** Parent directory relative path trimmed, or blank for root (`null` means no dialog showing). */
    var newFolderParent by remember { mutableStateOf<String?>(null) }

    renameTarget?.let { row ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = renameDraft.trim()
                        if (name.isNotEmpty()) {
                            onRename(row, name)
                        }
                        renameTarget = null
                    },
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            },
        )
    }

    deleteTarget?.let { row ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete permanently?") },
            text = { Text(row.path) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(row)
                        deleteTarget = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }

    createFileParent?.let { parentKey ->
        var nameDraft by remember(createFileParent) { mutableStateOf("untitled.txt") }
        AlertDialog(
            onDismissRequest = { createFileParent = null },
            title = { Text("New file") },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("File name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val n = nameDraft.trim()
                        if (n.isNotEmpty()) {
                            onCreateFile(if (parentKey.isEmpty()) "" else parentKey, n)
                        }
                        createFileParent = null
                    },
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { createFileParent = null }) { Text("Cancel") }
            },
        )
    }

    newFolderParent?.let { folderParentTrimmed ->
        var folderDraft by remember(newFolderParent) { mutableStateOf("new-folder") }
        AlertDialog(
            onDismissRequest = { newFolderParent = null },
            title = { Text("New folder") },
            text = {
                OutlinedTextField(
                    value = folderDraft,
                    onValueChange = { folderDraft = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val n = folderDraft.trim()
                        if (n.isNotEmpty()) {
                            val rel = if (folderParentTrimmed.isEmpty()) {
                                n.trim('/')
                            } else {
                                "${folderParentTrimmed.trim('/')}/$n".trim('/')
                            }
                            onCreateFolderRelative(rel)
                        }
                        newFolderParent = null
                    },
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { newFolderParent = null }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = "EXPLORER",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color = KineticColors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
        OutlinedTextField(
            value = treeFilterQuery,
            onValueChange = onTreeFilterQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp),
            placeholder = { Text("Filter tree…", fontSize = 12.sp) },
            singleLine = true,
        )
        Text(
            text = "Open folder",
            color = KineticColors.primary,
            fontSize = 13.sp,
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .clickable(onClick = onOpenWorkspace),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            TextButton(
                enabled = hasWorkspaceRoot,
                onClick = { createFileParent = "" },
            ) {
                Text("New file")
            }
            TextButton(
                enabled = hasWorkspaceRoot,
                onClick = { newFolderParent = "" },
            ) {
                Text("New folder")
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (hasWorkspaceRoot && favShown.isNotEmpty()) {
                item(key = "hdr_starred", contentType = "pin_header") {
                    Text(
                        text = "STARRED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = KineticColors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }
                items(
                    items = favShown,
                    key = { "fav:$it" },
                    contentType = { "pin_row" },
                ) { path ->
                    PinPathRow(
                        path = path,
                        selected = path == selectedPath,
                        icon = Icons.Default.Star,
                        onOpen = { onOpenPinnedPath(path) },
                    )
                }
            }
            if (hasWorkspaceRoot && recentShown.isNotEmpty()) {
                item(key = "hdr_recent", contentType = "pin_header") {
                    Text(
                        text = "RECENT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = KineticColors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }
                items(
                    items = recentShown,
                    key = { "rec:$it" },
                    contentType = { "pin_row" },
                ) { path ->
                    PinPathRow(
                        path = path,
                        selected = path == selectedPath,
                        icon = Icons.Default.History,
                        onOpen = { onOpenPinnedPath(path) },
                    )
                }
            }
            if (hasWorkspaceRoot && (favShown.isNotEmpty() || recentShown.isNotEmpty())) {
                item(key = "pins_divider", contentType = "pin_divider") {
                    HorizontalDivider(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                }
            }
            items(
                items = rows,
                key = { it.path },
                contentType = { "tree_row" },
            ) { row ->
                val menuOpen = menuRow?.path == row.path
                Box(Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (!row.isDirectory) {
                                        onSelectFile(row)
                                    }
                                },
                                onLongClick = { menuRow = row },
                            )
                            .padding(
                                start = (10 + row.depth * 10).dp,
                                top = 5.dp,
                                bottom = 5.dp,
                                end = 8.dp,
                            ),
                    ) {
                        Icon(
                            imageVector = iconForTreeRow(row),
                            contentDescription = null,
                            tint = if (row.path == selectedPath) KineticColors.primary
                            else KineticColors.onSurfaceVariant.copy(alpha = 0.75f),
                        )
                        Text(
                            text = row.displayName,
                            color = if (row.path == selectedPath) KineticColors.primary
                            else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuRow = null },
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        if (row.isDirectory) {
                            DropdownMenuItem(
                                text = { Text("New file here") },
                                onClick = {
                                    menuRow = null
                                    createFileParent = row.path
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("New subfolder…") },
                                onClick = {
                                    menuRow = null
                                    newFolderParent = row.path
                                },
                            )
                        }
                        if (!row.isDirectory) {
                            val isFav = favoritePaths.contains(row.path)
                            DropdownMenuItem(
                                text = {
                                    Text(if (isFav) "Remove favorite" else "Add favorite")
                                },
                                onClick = {
                                    menuRow = null
                                    onToggleFavoritePath(row.path)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    menuRow = null
                                    renameDraft = row.displayName
                                    renameTarget = row
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                onClick = {
                                    menuRow = null
                                    onDuplicate(row)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuRow = null
                                deleteTarget = row
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinPathRow(
    path: String,
    selected: Boolean,
    icon: ImageVector,
    onOpen: () -> Unit,
) {
    val label = path.substringAfterLast('/').ifBlank { path }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, top = 3.dp, bottom = 3.dp, end = 10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (selected) KineticColors.primary
            else KineticColors.onSurfaceVariant.copy(alpha = 0.85f),
        )
        Text(
            text = label,
            color = if (selected) KineticColors.primary else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
