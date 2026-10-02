package com.rameshai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OrbIdle = Color(0xFF5B8CFF)
val OrbListening = Color(0xFF4CD9C0)
val OrbThinking = Color(0xFFB37CFF)
val OrbSpeaking = Color(0xFFFFC24C)
val OrbError = Color(0xFFFF5C5C)
val BackgroundDark = Color(0xFF0B0D14)
val SurfaceDark = Color(0xFF13161F)

private val RameshDarkColors = darkColorScheme(
    primary = OrbIdle,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = Color(0xFFE6E8F0),
    onSurface = Color(0xFFE6E8F0)
)

@Composable
fun RameshAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RameshDarkColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
