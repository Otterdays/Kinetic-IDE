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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tabletaide.ide.ui.theme.KineticColors

@Composable
fun CodeEditorPane(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scroll = rememberScrollState()
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
    val lineCount = remember(text) {
        if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
    }
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .verticalScroll(scroll)
            .padding(vertical = 36.dp),
    ) {
        ColumnGutter(lineCount, gutterStyle, Modifier.width(48.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 18.dp, end = 16.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                readOnly = !enabled,
                textStyle = style.copy(color = Color.Transparent),
                cursorBrush = SolidColor(KineticColors.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box {
                        Text(
                            text = highlightCode(text, MaterialTheme.colorScheme.onSurface),
                            style = style,
                        )
                        innerTextField()
                    }
                },
            )
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
