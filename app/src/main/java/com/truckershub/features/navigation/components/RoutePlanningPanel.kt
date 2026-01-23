package com.truckershub.features.navigation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning // SOS Icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.GeoPoint
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.ThubRed // Unser Import!

@Composable
fun RoutePlanningPanel(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onStartChanged: (String) -> Unit,
    onDestinationChanged: (String) -> Unit,
    onWaypointAdded: () -> Unit,
    onCalculateRoute: () -> Unit,
    onMinimize: () -> Unit,
    onSosClick: () -> Unit, // <--- NEU: Der SOS-Trigger
    currentLocation: GeoPoint?
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp), // Etwas Abstand zum Rand
        colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // --- HEADER ZEILE (Hier passiert der Tausch!) ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
            ) {
                // DER NEUE EHRENPLATZ FÜR SOS 🔴
                // Statt dem alten Icon bauen wir hier einen kleinen roten Knopf ein
                IconButton(
                    onClick = onSosClick, // Löst den Alarm aus
                    modifier = Modifier
                        .size(40.dp)
                        .background(ThubRed, CircleShape) // Roter Hintergrund
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "SOS",
                        tint = TextWhite, // Weißes Icon auf rotem Grund
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Titel
                Text(
                    text = "ROUTE PLANEN",
                    color = ThubNeonBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )

                // Pfeil zum Auf/Zuklappen
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = ThubNeonBlue
                )
            }

            // --- AUSKLAPP-BEREICH (Bleibt wie er war) ---
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Start
                    OutlinedTextField(
                        value = "",
                        onValueChange = onStartChanged,
                        label = { Text("Startpunkt", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Navigation, null, tint = ThubNeonBlue) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThubNeonBlue,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Ziel
                    OutlinedTextField(
                        value = "",
                        onValueChange = onDestinationChanged,
                        label = { Text("Zielort", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Flag, null, tint = ThubNeonBlue) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThubNeonBlue,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onCalculateRoute,
                            colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue)
                        ) {
                            Text("ROUTE STARTEN", color = ThubBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}