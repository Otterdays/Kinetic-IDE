package com.tabletaide.ide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tabletaide.ide.IdeConstants
import com.tabletaide.ide.ui.theme.KineticColors
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun CodeEditorPane(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    tabPath: String?,
    scrollInitialPx: Int,
    onScrollPxCommitted: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val text = value.text
    val lineCount = remember(text) {
        if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
    }
    val useLightChrome = text.length >= IdeConstants.LARGE_FILE_CHAR_THRESHOLD ||
        lineCount >= IdeConstants.LARGE_FILE_LINE_SOFT_THRESHOLD
    val showLargeBanner = text.length >= IdeConstants.LARGE_FILE_CHAR_THRESHOLD

    val scroll = remember(tabPath) { ScrollState(scrollInitialPx) }

    LaunchedEffect(tabPath, scroll) {
        if (tabPath == null) return@LaunchedEffect
        snapshotFlow { scroll.value }
            .distinctUntilChanged()
            .collect { px -> onScrollPxCommitted(tabPath, px) }
    }

    val lineHeight = 20.sp
    val fontSize = 14.sp
    val style = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontFamily = FontFamily.Monospace,
    )
    val gutterStyle = TextStyle(
        color = KineticColors.outline,
        fontSize = 12.sp,
        lineHeight = lineHeight,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.End,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        if (showLargeBanner && text.isNotEmpty()) {
            Surface(
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 0.dp,
            ) {
                Text(
                    text = "Large buffer (>${IdeConstants.LARGE_FILE_CHAR_THRESHOLD / 1_000_000}M chars) — " +
                        "syntax chrome reduced.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(vertical = 36.dp),
        ) {
            if (!useLightChrome) {
                ColumnGutter(lineCount, gutterStyle, Modifier.width(48.dp))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 18.dp, end = 16.dp),
            ) {
                val displayColor = MaterialTheme.colorScheme.onSurface
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    readOnly = !enabled,
                    textStyle = style.copy(color = Color.Transparent),
                    cursorBrush = SolidColor(KineticColors.primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box {
                            if (useLightChrome && text.isNotEmpty()) {
                                Text(text = text, style = style, color = displayColor)
                            } else if (text.isEmpty()) {
                                Text("", style = style)
                            } else {
                                Text(
                                    text = highlightCode(text, displayColor),
                                    style = style,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ColumnGutter(
    lineCount: Int,
    gutterStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(end = 8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        repeat(lineCount) { i ->
            Text(
                text = "${i + 1}",
                style = gutterStyle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
