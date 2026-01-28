package com.truckershub.features.map

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.truckershub.core.data.model.Location
import com.truckershub.core.data.model.LocationType
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubNeonBlue

// Das ist der NEUE Dialog für "Favoriten / Firmen-Wiki"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveLocationDialog(
    latitude: Double,
    longitude: Double,
    onDismiss: () -> Unit,
    onSave: (Location) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var requirements by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(LocationType.COMPANY) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ThubBlack),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThubNeonBlue, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ort speichern 📍",
                        color = ThubNeonBlue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name Eingabe
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (z.B. Zentrallager)") },
                    // HIER WAR DER FEHLER: Jetzt nutzen wir OutlinedTextFieldDefaults
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThubNeonBlue,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = ThubNeonBlue,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = ThubNeonBlue,
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Typ Auswahl
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TypeChip("Firma", LocationType.COMPANY, selectedType) { selectedType = it }
                    TypeChip("Privat", LocationType.PRIVATE, selectedType) { selectedType = it }
                    TypeChip("Tanke", LocationType.FUEL, selectedType) { selectedType = it }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Wiki Infos
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Info (z.B. Einfahrt hinten)") },
                    // HIER AUCH KORRIGIERT
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThubNeonBlue,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = ThubNeonBlue,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = ThubNeonBlue,
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // PSA Infos
                OutlinedTextField(
                    value = requirements,
                    onValueChange = { requirements = it },
                    label = { Text("PSA (z.B. Helm, Brille)") },
                    // UND HIER AUCH KORRIGIERT
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThubNeonBlue,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = ThubNeonBlue,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = ThubNeonBlue,
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Speichern Button
                Button(
                    onClick = {
                        val newLocation = Location(
                            name = name,
                            latitude = latitude,
                            longitude = longitude,
                            type = selectedType,
                            description = description,
                            requirements = requirements
                        )
                        onSave(newLocation)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = ThubBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SPEICHERN", color = ThubBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, type: LocationType, selected: LocationType, onSelect: (LocationType) -> Unit) {
    val isSelected = type == selected
    Surface(
        color = if (isSelected) ThubNeonBlue else Color.DarkGray,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .clickable { onSelect(type) }
            .padding(4.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) ThubBlack else Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}