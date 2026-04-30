package com.tabletaide.ide.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class KineticThemeMode(val id: String, val displayName: String) {
    DARK("dark", "Dark"),
    LIGHT("light", "Light"),
    HIGH_CONTRAST("high_contrast", "High Contrast"),
}

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

private val KineticLightScheme = lightColorScheme(
    primary = Color(0xFF006A77),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF00E3FD),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF53634A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E8C6),
    onSecondaryContainer = Color(0xFF121F0B),
    tertiary = Color(0xFF5E5288),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE5DEFF),
    onTertiaryContainer = Color(0xFF1A133C),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF101417),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101417),
    surfaceVariant = Color(0xFFE0E6EA),
    onSurfaceVariant = Color(0xFF3D4850),
    outline = Color(0xFF6B7780),
    outlineVariant = Color(0xFFBCC5CB),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private val KineticHighContrastScheme = darkColorScheme(
    primary = Color(0xFF00F0FF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF00C4D1),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFFEFFFBC),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFFB9CE85),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFFE9E2FF),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFFBFB1FF),
    onTertiaryContainer = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF101417),
    onSurfaceVariant = Color(0xFFE7EEF5),
    outline = Color(0xFFB4C9D9),
    outlineVariant = Color(0xFF5E7482),
    error = Color(0xFFFF5C6A),
    onError = Color(0xFF000000),
)

@Composable
fun KineticTheme(
    mode: KineticThemeMode = KineticThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val scheme = when (mode) {
        KineticThemeMode.DARK -> KineticScheme
        KineticThemeMode.LIGHT -> KineticLightScheme
        KineticThemeMode.HIGH_CONTRAST -> KineticHighContrastScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = IdeTypography,
        content = content,
    )
}
