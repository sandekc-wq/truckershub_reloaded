package com.truckershub.features.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.features.navigation.components.RoutePlanningPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteScreen(
    onBack: () -> Unit,
    viewModel: RouteViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ROUTEN DETAILS", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = ThubNeonBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThubDarkGray)
            )
        },
        containerColor = ThubBlack
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // HIER WAR DER FEHLER: Das Panel brauchte die neuen Parameter! 🛠️
            RoutePlanningPanel(
                modifier = Modifier.fillMaxWidth(),
                expanded = true, // Hier immer ausgeklappt lassen
                onExpandToggle = { }, // Brauchen wir hier nicht
                onStartChanged = { viewModel.updateStartPoint(it) },
                onDestinationChanged = { viewModel.updateDestinationPoint(it) },
                onWaypointAdded = {},
                onCalculateRoute = { viewModel.calculateRoute() },
                onMinimize = { },
                // FIX: Die neuen Parameter bedienen (hier einfach leer lassen)
                onSosClick = { /* Hier passiert nichts, SOS ist auf der Map */ },
                currentLocation = null // Haben wir hier gerade nicht, ist okay
            )

            if (viewModel.isCalculating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ThubNeonBlue)
            }

            if (viewModel.errorMessage != null) {
                Text(
                    text = viewModel.errorMessage!!,
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Die Liste der Anweisungen
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.instructions) { instruction ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            // Einfache Pfeil-Logik (kann man später verschönern)
                            Text(
                                text = when (instruction.type) {
                                    0, 1 -> "⬅️"
                                    2, 3 -> "➡️"
                                    else -> "⬆️"
                                },
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(instruction.text, color = TextWhite, fontWeight = FontWeight.Bold)
                                Text(
                                    "${instruction.distance.toInt()} m",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}