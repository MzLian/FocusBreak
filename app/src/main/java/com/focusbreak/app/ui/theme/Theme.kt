package com.focusbreak.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FocusBreakColors = lightColorScheme(
    primary = ForestDark,
    onPrimary = Color.White,
    primaryContainer = MintSurface,
    onPrimaryContainer = ForestDark,
    secondary = Lime,
    onSecondary = ForestDark,
    tertiary = Amber,
    background = Cream,
    onBackground = ForestDark,
    surface = Color.White,
    onSurface = ForestDark,
    surfaceVariant = Color(0xFFE9EDE3),
    onSurfaceVariant = InkMuted,
    outline = Outline,
)

@Composable
fun FocusBreakTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FocusBreakColors, typography = Typography(), content = content)
}
