package com.truckershub.core.design

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Wir definieren hier unser dunkles "Thub Style" Design
private val ThubColorScheme = darkColorScheme(
    primary = ThubNeonBlue,
    onPrimary = ThubBlack,
    secondary = ThubNeonBlue,
    onSecondary = ThubBlack,
    background = ThubBlack,
    onBackground = TextWhite,
    surface = ThubDarkGray,
    onSurface = TextWhite,
    error = StatusRed
)

@Composable
fun ThubTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = ThubColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // HIER ist der Stempel: Wir sagen dem Compiler, er soll schweigen,
            // weil wir die schwarze Leiste absichtlich wollen.
            @Suppress("DEPRECATION")
            window.statusBarColor = ThubBlack.toArgb()

            // Helle Icons in der Statusleiste (Uhrzeit etc.), damit man sie auf Schwarz sieht
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ThubTypography,
        content = content
    )
}