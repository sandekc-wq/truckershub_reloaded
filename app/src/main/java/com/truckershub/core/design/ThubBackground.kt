package com.truckershub.core.design

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun ThubBackground(
    content: @Composable () -> Unit
) {
    // KEIN BILD MEHR! Nur noch sauberes ThubBlack.
    // Damit ist der Hintergrund neutral und dein Logo im ChecklistScreen
    // hat die volle Aufmerksamkeit.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ThubBlack,
        content = content
    )
}