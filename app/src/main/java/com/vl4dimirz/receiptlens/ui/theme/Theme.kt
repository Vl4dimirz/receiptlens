package com.vl4dimirz.receiptlens.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// แบรนด์ = monochrome คงที่ (ไม่ใช้ dynamic color) เพื่อเอกลักษณ์ quiet-luxury
private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    secondary = InkSoft,
    onSecondary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperDim,
    onSurfaceVariant = InkSoft,
    outline = Line,
    outlineVariant = Line,
    error = Danger,
    onError = Paper,
)

private val DarkColors = darkColorScheme(
    primary = PaperOnDark,
    onPrimary = InkDark,
    secondary = MistDark,
    onSecondary = InkDark,
    background = InkDark,
    onBackground = PaperOnDark,
    surface = SurfaceDark,
    onSurface = PaperOnDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = MistDark,
    outline = LineDark,
    outlineVariant = LineDark,
    error = Danger,
    onError = Paper,
)

@Composable
fun ReceiptLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
