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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    selectedPath: String?,
    hasWorkspaceRoot: Boolean,
    onOpenWorkspace: () -> Unit,
    onSelectFile: (TreeRow) -> Unit,
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
    var treeFilterQuery by remember { mutableStateOf("") }
    val displayedRows = remember(rows, treeFilterQuery) { filterTreeRows(rows, treeFilterQuery) }

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
            onValueChange = { treeFilterQuery = it },
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
            items(displayedRows, key = { it.path }) { row ->
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
