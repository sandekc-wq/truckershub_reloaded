package com.truckershub.features.navigation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.GeoPoint
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.TextWhite

/**
 * ROUTEN-PLANUNGS PANEL (Thub Style)
 *
 * Erscheint OBEN auf der Karte
 * Mit Start/Ziel/Zwischenziel Inputs
 */
@Suppress("DEPRECATION")
@Composable
fun RoutePlanningPanel(
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onExpandToggle: () -> Unit = {},
    onStartChanged: (String) -> Unit = {},
    onDestinationChanged: (String) -> Unit = {},
    onWaypointAdded: () -> Unit = {},
    onCalculateRoute: () -> Unit = {},
    onMinimize: () -> Unit = {},
    currentLocation: GeoPoint? = null
) {
    var startInput by remember { mutableStateOf("") }
    var destinationInput by remember { mutableStateOf("") }
    var waypointsList by remember { mutableStateOf<List<String>>(emptyList()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThubDarkGray.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            // Header - CLICKABLE zum Expandieren
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Filled.Route,
                        contentDescription = null,
                        tint = ThubNeonBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ROUTE PLANEN",
                            style = MaterialTheme.typography.titleMedium,
                            color = ThubNeonBlue,
                            fontWeight = FontWeight.Bold
                        )
                        if (!expanded && destinationInput.isNotEmpty()) {
                            Text(
                                text = "📍 $destinationInput",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextWhite.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }
                }
                IconButton(onClick = onExpandToggle) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Einklappen" else "Ausklappen",
                        tint = ThubNeonBlue
                    )
                }
            }

            if (expanded) {
                Divider(color = ThubNeonBlue.copy(alpha = 0.3f), thickness = 0.5.dp)

                Column(modifier = Modifier.padding(12.dp)) {
                    RouteInputField(
                        label = "START",
                        hint = "z.B. München, Berlin...",
                        value = startInput,
                        icon = Icons.Filled.MyLocation,
                        iconColor = Color.Green,
                        onValueChange = { newValue ->
                            startInput = newValue
                            onStartChanged(newValue)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    WaypointsSection(
                        waypoints = waypointsList,
                        onAddWaypoint = {
                            waypointsList = waypointsList + ""
                            onWaypointAdded()
                        },
                        onRemoveWaypoint = { index ->
                            waypointsList = waypointsList.toMutableList().apply { removeAt(index) }
                        },
                        onWaypointChanged = { index, value ->
                            waypointsList = waypointsList.toMutableList().apply {
                                if (index < size) set(index, value)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    RouteInputField(
                        label = "ZIEL",
                        hint = "z.B. Hamburg, Frankfurt...",
                        value = destinationInput,
                        icon = Icons.Filled.LocationOn,
                        iconColor = Color(0xFFFF0000),
                        onValueChange = { newValue ->
                            destinationInput = newValue
                            onDestinationChanged(newValue)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onCalculateRoute,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThubNeonBlue
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = destinationInput.isNotEmpty()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Directions,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Route berechnen",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ROUTEN INPUT FELD
 */
@Composable
fun RouteInputField(
    label: String,
    hint: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.width(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextWhite.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(hint, color = TextWhite.copy(alpha = 0.5f)) },
            readOnly = readOnly,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextWhite),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = iconColor,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = ThubNeonBlue
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
    }
}

/**
 * ZWISCHENZIELE SECTION
 */
@Composable
fun WaypointsSection(
    waypoints: List<String> = emptyList(),
    onAddWaypoint: () -> Unit,
    onRemoveWaypoint: (Int) -> Unit = {},
    onWaypointChanged: (Int, String) -> Unit = { _, _ -> }
) {
    Column {
        waypoints.forEachIndexed { index, waypoint ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RouteInputField(
                    label = "STOP ${index + 1}",
                    hint = "Zwischenhalt eingeben...",
                    value = waypoint,
                    icon = Icons.Filled.AddLocation,
                    iconColor = Color(0xFFFFA500),
                    onValueChange = { newValue ->
                        onWaypointChanged(index, newValue)
                    },
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { onRemoveWaypoint(index) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Entfernen",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Button(
            onClick = onAddWaypoint,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThubDarkGray.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                ThubNeonBlue.copy(alpha = 0.5f)
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = ThubNeonBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Zwischenziel hinzufügen",
                    color = ThubNeonBlue,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/**
 * MINI ROUTEN PANEL
 */
@Composable
fun MiniRoutePanel(
    modifier: Modifier = Modifier,
    destination: String = "",
    onExpand: () -> Unit = {},
    onClearRoute: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ThubNeonBlue
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Route,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (destination.isNotEmpty()) destination else "Route planen...",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }

            Row {
                IconButton(onClick = onExpand) {
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = "Erweitern",
                        tint = Color.Black
                    )
                }
                IconButton(onClick = onClearRoute) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Route löschen",
                        tint = Color.Black
                    )
                }
            }
        }
    }
}
