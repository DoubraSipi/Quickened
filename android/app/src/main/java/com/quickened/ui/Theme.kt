package com.quickened.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

val WarmPrimary = Color(0xFF0F766E)
val WarmAccent = Color(0xFFD97706)
val CreamBg = Color(0xFFFDF8F1)
val WarmSurface = Color(0xFFFFFFFF)
val WarmInk = Color(0xFF292019)
val WarmMuted = Color(0xFF8A7A68)
val NightBg = Color(0xFF1C1611)
val NightSurface = Color(0xFF2A231C)

val ToneTint = mapOf(
    "gentle" to Color(0xFFDBEAFE),
    "encouraging" to Color(0xFFDCFCE7),
    "contemplative" to Color(0xFFEDE9FE),
    "challenging" to Color(0xFFFEF3C7)
)

private val LightWarm = lightColorScheme(
    primary = WarmPrimary,
    onPrimary = Color.White,
    secondary = WarmAccent,
    background = CreamBg,
    surface = WarmSurface,
    onBackground = WarmInk,
    onSurface = WarmInk
)

private val DarkWarm = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF08332E),
    secondary = Color(0xFFFBBF24),
    background = NightBg,
    surface = NightSurface,
    onBackground = Color(0xFFF5EFE6),
    onSurface = Color(0xFFF5EFE6)
)

val SerifHeadings = FontFamily.Serif

@Composable
fun QuickenedTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkWarm else LightWarm) {
        content()
    }
}
