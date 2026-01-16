package com.truckershub.features.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.TextWhite
import com.truckershub.features.navigation.components.RoutePlanningPanel

/**
 * ROUTENSCREEN
 *
 * Hauptscreen für Truck-Routing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteScreen(
    onBackClick: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: RouteViewModel = viewModel()
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ROUTENPLANUNG 🚛",
                        color = ThubNeonBlue,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = ThubNeonBlue)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, "Einstellungen", tint = ThubNeonBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThubBlack
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ThubBlack)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Route Planning Panel
                RoutePlanningPanel(
                    expanded = true,
                    onStartChanged = { viewModel.updateStartPoint(it) },
                    onDestinationChanged = { viewModel.updateDestinationPoint(it) },
                    onWaypointAdded = { /* TODO */ },
                    onCalculateRoute = { viewModel.calculateRoute() },
                    onMinimize = { /* TODO */ }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error Message
                if (viewModel.errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                viewModel.errorMessage!!,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Loading
                if (viewModel.isCalculating) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ThubNeonBlue,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Route Result
                viewModel.currentRoute?.let { route ->
                    RouteResultCard(route = route, onSave = { viewModel.saveRoute() })
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Info Box
                if (viewModel.currentRoute == null && !viewModel.isCalculating) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint = ThubNeonBlue,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Routenplanung für Trucks",
                                color = ThubNeonBlue,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Gib Start- und Zielpunkt ein,\nwähle dein Truck-Profil,\nund berechne die beste Route!",
                                color = TextWhite.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ROUTE RESULT CARD
 */
@Composable
fun RouteResultCard(
    route: com.truckershub.core.data.model.Route,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "✓ ROUTE BERECHNET",
                color = ThubNeonBlue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Route, contentDescription = null, tint = ThubNeonBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(route.name, color = TextWhite, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Straighten, contentDescription = null, tint = Color(0xFF00FF0A), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("${String.format("%.1f", route.routeDetails.distance / 1000)} km", color = TextWhite, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                val hours = route.routeDetails.duration / 3600
                val minutes = (route.routeDetails.duration % 3600) / 60
                Text("${hours}h ${minutes}min", color = TextWhite, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = Color(0xFFFF9100), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("~€${String.format("%.2f", route.estimatedFuelCost)}", color = TextWhite, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Speichern", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = ThubNeonBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Teilen", color = ThubNeonBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
