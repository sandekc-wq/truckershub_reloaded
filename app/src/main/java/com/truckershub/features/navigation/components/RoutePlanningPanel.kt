package com.truckershub.features.navigation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.ThubRed

@Composable
fun RoutePlanningPanel(
    modifier: Modifier = Modifier,
    startText: String,
    destinationText: String,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onStartChanged: (String) -> Unit,
    onDestinationChanged: (String) -> Unit,
    onCalculateRoute: () -> Unit,
    onMinimize: () -> Unit,
    onSosClick: () -> Unit,
    onWaypointAdded: () -> Unit,
    currentLocation: Any? = null
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = ThubBlack.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onSosClick, modifier = Modifier.size(36.dp).background(ThubRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))) {
                    Icon(Icons.Filled.Warning, "SOS", tint = ThubRed, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Row(modifier = Modifier.weight(1f).clickable { onExpandToggle() }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Navigation, null, tint = ThubNeonBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ROUTE PLANEN", color = ThubNeonBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                IconButton(onClick = onExpandToggle) {
                    Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, "Toggle", tint = Color.Gray)
                }
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedTextField(
                        value = startText,
                        onValueChange = onStartChanged,
                        label = { Text("Start", color = Color.Gray) },
                        placeholder = { Text("Aktueller Standort", color = Color.DarkGray) },
                        leadingIcon = { Icon(Icons.Filled.MyLocation, null, tint = if(startText == "Aktueller Standort") ThubNeonBlue else Color.Gray) },
                        trailingIcon = {
                            if (startText != "Aktueller Standort" && startText.isNotEmpty()) {
                                IconButton(onClick = { onStartChanged("") }) { Icon(Icons.Filled.Close, null, tint = Color.Gray) }
                            } else if (startText.isEmpty()) {
                                IconButton(onClick = { onStartChanged("Aktueller Standort") }) { Icon(Icons.Filled.MyLocation, null, tint = ThubNeonBlue) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThubNeonBlue, unfocusedBorderColor = ThubDarkGray, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, cursorColor = ThubNeonBlue),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = destinationText,
                        onValueChange = onDestinationChanged,
                        label = { Text("Zielort", color = Color.Gray) },
                        placeholder = { Text("z.B. Berlin", color = Color.Gray.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Filled.Flag, null, tint = ThubRed) },
                        trailingIcon = {
                            if (destinationText.isNotEmpty()) {
                                IconButton(onClick = { onDestinationChanged("") }) { Icon(Icons.Filled.Close, null, tint = Color.Gray) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThubNeonBlue, unfocusedBorderColor = ThubDarkGray, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, cursorColor = ThubNeonBlue),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onMinimize, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, ThubDarkGray)) {
                            Text("Später", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = onCalculateRoute, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue)) {
                            Text("LOS! 🚛", color = ThubBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}