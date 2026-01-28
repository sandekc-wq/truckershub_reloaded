package com.truckershub.features.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckershub.core.data.model.Location
import com.truckershub.core.data.model.LocationType
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedLocationsScreen(
    onClose: () -> Unit,
    onJumpToLocation: (Location) -> Unit, // Callback: Wenn man auf "Hinfahren" klickt
    viewModel: LocationViewModel = viewModel()
) {
    // Wir beobachten die Live-Daten aus der Datenbank
    val locations by viewModel.savedLocations.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Filtern der Liste: Wenn Suchtext da ist, nur passende anzeigen
    val filteredLocations = locations.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThubBlack)
            .padding(16.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Firmen-Wiki 📂",
                color = ThubNeonBlue,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SUCHLEISTE ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Suchen (Name, Info...)") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = ThubNeonBlue) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThubNeonBlue,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = ThubNeonBlue,
                unfocusedLabelColor = Color.Gray,
                cursorColor = ThubNeonBlue,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = ThubBlack,
                unfocusedContainerColor = ThubBlack
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- LISTE ---
        if (filteredLocations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (searchQuery.isEmpty()) {
                    Text("Noch keine Orte gespeichert.\nDrücke lange auf die Karte! 📍", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                } else {
                    Text("Nichts gefunden für '$searchQuery' 🤷‍♂️", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(filteredLocations) { location ->
                    LocationItem(
                        location = location,
                        onClick = { onJumpToLocation(location) },
                        onDelete = { viewModel.deleteLocation(location) }
                    )
                }
            }
        }
    }
}

@Composable
fun LocationItem(
    location: Location,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // Klick auf die Karte springt zum Ort
            .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ICON (Je nach Typ: Firma, Tanke, Privat)
            val icon = when(location.type) {
                LocationType.COMPANY -> Icons.Default.Business
                LocationType.FUEL -> Icons.Default.LocalGasStation
                LocationType.PRIVATE -> Icons.Default.Home
                else -> Icons.Default.Place
            }

            // Icon links
            Icon(icon, null, tint = ThubNeonBlue, modifier = Modifier.size(32.dp))

            Spacer(modifier = Modifier.width(16.dp))

            // TEXT INFOS (Mitte)
            Column(modifier = Modifier.weight(1f)) {
                Text(location.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                // Nur anzeigen, wenn Text vorhanden ist
                if (location.description.isNotEmpty()) {
                    Text("ℹ️ ${location.description}", color = Color.LightGray, fontSize = 14.sp)
                }
                if (location.requirements.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("⚠️ PSA: ${location.requirements}", color = Color.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // LÖSCHEN BUTTON (Rechts)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}