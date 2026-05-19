package com.weargluco.watch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val GlucoseHigh = Color(0xFFFF4444)
val GlucoseNormal = Color(0xFF4CAF50)
val GlucoseLow = Color(0xFFFF9800)
val GlucoseCritical = Color(0xFFFF0000)
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)

private val wearColorPalette = Colors(
    primary = Color(0xFF4FC3F7),
    primaryVariant = Color(0xFF0288D1),
    secondary = Color(0xFF81C784),
    secondaryVariant = Color(0xFF388E3C),
    error = Color(0xFFEF5350),
    surface = SurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onSurface = Color.White,
    onError = Color.White,
    background = BackgroundDark,
    onBackground = Color.White
)

@Composable
fun GlucoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = wearColorPalette,
        content = content
    )
}

fun glucoseColor(value: Double, targetLow: Int = 70, targetHigh: Int = 180): Color {
    return when {
        value < targetLow -> GlucoseLow
        value > targetHigh -> GlucoseHigh
        else -> GlucoseNormal
    }
}
