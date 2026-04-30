package com.tabletaide.ide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.tabletaide.ide.ui.theme.KineticColors

/**
 * Draggable vertical splitter between editor stack and agent panel.
 * // [TRACE: DOCS/ROADMAP.md] Epic 1.3 split-pane MVP
 */
@Composable
fun EditorAgentSplitDivider(
    totalWidthPx: Float,
    onDragFractionDelta: (Float) -> Unit,
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val denom = totalWidthPx.coerceAtLeast(1f)
    Box(
        modifier = modifier
            .width(10.dp)
            .fillMaxHeight()
            .pointerInput(denom) {
                detectHorizontalDragGestures(
                    onDragEnd = onDragEnd,
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        onDragFractionDelta(dx / denom)
                    },
                )
            }
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight(0.35f)
                .background(KineticColors.outline.copy(alpha = 0.35f)),
        )
    }
}
