package com.tabletaide.ide.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tabletaide.ide.data.TreeRow
import com.tabletaide.ide.ui.theme.KineticColors

@Composable
fun FileTreePane(
    rows: List<TreeRow>,
    selectedPath: String?,
    onOpenWorkspace: () -> Unit,
    onSelectFile: (TreeRow) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        Text(
            text = "Open folder",
            color = KineticColors.primary,
            fontSize = 13.sp,
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .clickable(onClick = onOpenWorkspace),
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            items(rows, key = { it.path }) { row ->
                val selected = row.path == selectedPath
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !row.isDirectory) { onSelectFile(row) }
                        .padding(
                            start = (10 + row.depth * 10).dp,
                            top = 5.dp,
                            bottom = 5.dp,
                            end = 8.dp,
                        ),
                ) {
                    Icon(
                        imageVector = if (row.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = if (selected) KineticColors.primary
                        else KineticColors.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                    Text(
                        text = row.displayName,
                        color = if (selected) KineticColors.primary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
