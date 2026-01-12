package com.truckershub.core.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.truckershub.R

@Composable
fun ThubBackground(
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Das Hintergrundbild
        // ContentScale.Crop sorgt dafür, dass der ganze Bildschirm gefüllt ist,
        // ohne dass der LKW verzerrt wird.
        Image(
            painter = painterResource(id = R.drawable.thub_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Die Abdunklung (Sonnenbrille)
        // WIR HABEN HIER GEÄNDERT: Von 0.85f auf 0.6f
        // Das bedeutet: Nur noch 60% Schwarz, 40% LKW scheint durch.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        // 3. Der Inhalt kommt oben drauf
        content()
    }
}