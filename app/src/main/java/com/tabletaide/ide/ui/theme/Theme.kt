package com.tabletaide.ide.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KineticScheme = darkColorScheme(
    primary = KineticColors.primary,
    onPrimary = KineticColors.onPrimaryFixed,
    primaryContainer = Color(0xFF00E3FD),
    onPrimaryContainer = Color(0xFF004D57),
    secondary = KineticColors.secondary,
    onSecondary = Color(0xFF455900),
    secondaryContainer = Color(0xFF506600),
    onSecondaryContainer = Color(0xFFEFFFBC),
    tertiary = KineticColors.tertiary,
    onTertiary = Color(0xFF290067),
    tertiaryContainer = Color(0xFF7000FF),
    onTertiaryContainer = Color(0xFFF8F1FF),
    error = KineticColors.error,
    onError = Color(0xFF490006),
    background = KineticColors.background,
    onBackground = KineticColors.onSurface,
    surface = KineticColors.surface,
    onSurface = KineticColors.onSurface,
    surfaceVariant = KineticColors.surfaceVariant,
    onSurfaceVariant = KineticColors.onSurfaceVariant,
    outline = KineticColors.outline,
    outlineVariant = KineticColors.outlineVariant,
    surfaceContainerLowest = KineticColors.surfaceContainerLowest,
    surfaceContainerLow = KineticColors.surfaceContainerLow,
    surfaceContainer = KineticColors.surface,
    surfaceContainerHigh = KineticColors.surfaceContainerHigh,
    surfaceContainerHighest = KineticColors.surfaceContainerHighest,
)

@Composable
fun TabletAiIdeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KineticScheme,
        typography = IdeTypography,
        content = content,
    )
}
