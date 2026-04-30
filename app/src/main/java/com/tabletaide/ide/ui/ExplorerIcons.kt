package com.tabletaide.ide.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.vector.ImageVector
import com.tabletaide.ide.data.TreeRow

/** Picks an icon by name / extension; folders always use [Icons.Filled.Folder]. */
fun iconForTreeRow(row: TreeRow): ImageVector {
    if (row.isDirectory) return Icons.Filled.Folder
    val name = row.displayName.lowercase()
    if (name.endsWith(".gradle.kts") || name.endsWith(".gradle")) return Icons.Filled.Settings
    val dot = name.lastIndexOf('.')
    val ext = if (dot in 1 until name.lastIndex) name.substring(dot + 1) else ""
    return when (ext) {
        "kt", "kts" -> Icons.Filled.Code
        "java", "cs", "go", "rs", "py", "rb", "php", "swift" -> Icons.Filled.Code
        "js", "jsx", "ts", "tsx", "mjs", "cjs" -> Icons.Filled.Code
        "c", "h", "cpp", "hpp", "cc", "cxx" -> Icons.Filled.Code
        "md", "txt", "rst" -> Icons.Filled.Description
        "json", "yaml", "yml", "toml", "xml" -> Icons.Filled.DataObject
        "html", "htm", "css", "scss", "sass", "less" -> Icons.Filled.Code
        "png", "jpg", "jpeg", "gif", "webp", "svg", "bmp" -> Icons.Filled.Image
        "mp3", "wav", "ogg", "flac", "m4a" -> Icons.Filled.AudioFile
        "mp4", "webm", "mkv", "mov" -> Icons.Filled.VideoFile
        "sh", "bash", "zsh", "fish", "bat", "cmd", "ps1" -> Icons.Filled.Terminal
        "properties", "pro", "env" -> Icons.Filled.Settings
        else -> Icons.Filled.InsertDriveFile
    }
}
