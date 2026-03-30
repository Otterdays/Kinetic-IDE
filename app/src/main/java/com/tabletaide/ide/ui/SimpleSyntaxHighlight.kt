package com.tabletaide.ide.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.tabletaide.ide.ui.theme.KineticColors

private val KEYWORDS = setOf(
    "package", "import", "class", "interface", "object", "fun", "val", "var",
    "if", "else", "when", "for", "while", "return", "true", "false", "null",
    "as", "is", "in", "try", "catch", "finally", "throw",
    "private", "internal", "open", "abstract", "override", "public", "protected",
    "suspend", "data", "sealed", "enum", "const", "lateinit", "init",
    "def", "self", "from", "with", "pass", "lambda", "yield",
)

private val tokenRegex = Regex("""("[^"]*"|'[^']*'|\w+)""")

fun highlightCode(text: String, baseColor: Color = KineticColors.onSurface): AnnotatedString = buildAnnotatedString {
    if (text.isEmpty()) return@buildAnnotatedString
    val lines = text.split('\n')
    lines.forEachIndexed { lineIndex, line ->
        if (lineIndex > 0) append('\n')
        val commentStart = line.indexOf("//")
        val codeEnd = if (commentStart >= 0) commentStart else line.length
        append(highlightPlainSegment(line.substring(0, codeEnd), baseColor))
        if (commentStart >= 0) {
            withStyle(SpanStyle(KineticColors.outline)) {
                append(line.substring(commentStart))
            }
        }
    }
}

private fun highlightPlainSegment(segment: String, baseColor: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    for (m in tokenRegex.findAll(segment)) {
        if (m.range.first > i) {
            withStyle(SpanStyle(baseColor)) {
                append(segment.substring(i, m.range.first))
            }
        }
        val token = m.value
        val style = when {
            token.startsWith('"') || token.startsWith('\'') -> SpanStyle(KineticColors.tertiary)
            KEYWORDS.contains(token) -> SpanStyle(KineticColors.secondary, fontWeight = FontWeight.SemiBold)
            token.firstOrNull()?.isUpperCase() == true && token.length > 1 -> SpanStyle(KineticColors.primary)
            else -> SpanStyle(baseColor)
        }
        withStyle(style) { append(token) }
        i = m.range.last + 1
    }
    if (i < segment.length) {
        withStyle(SpanStyle(baseColor)) {
            append(segment.substring(i))
        }
    }
}
