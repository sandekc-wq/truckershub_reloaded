package com.truckershub.features.checklist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.truckershub.core.design.TextWhite

@Composable
fun ChecklistScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Hier kommt die Abfahrtskontrolle hin ✅", color = TextWhite)
    }
}