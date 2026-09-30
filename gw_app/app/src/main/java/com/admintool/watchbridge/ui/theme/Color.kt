package com.admintool.watchbridge.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

val TealPrimary = Color(0xFF14B8A6)
val TealDark = Color(0xFF042F2E)
val TealContainer = Color(0xFF115E59)
val OnTealContainer = Color(0xFFCCFBF1)

val WarningRed = Color(0xFFEF4444)
val SuccessGreen = Color(0xFF22C55E)
val SurfaceDark = Color(0xFF18181B)
val BackgroundDark = Color(0xFF000000)

val WatchColorScheme = ColorScheme(
    primary = TealPrimary,
    primaryContainer = TealContainer,
    onPrimary = Color.Black,
    onPrimaryContainer = OnTealContainer,
    surfaceContainer = SurfaceDark,
    background = BackgroundDark,
    onBackground = Color.White,
    onSurface = Color.White
)
