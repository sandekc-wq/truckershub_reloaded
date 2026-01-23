package com.truckershub.features.map

import android.location.Geocoder
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
// WICHTIG: Dieser Import hat gefehlt! 👇
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.TextWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// Datenklasse für das Ergebnis
data class LocationData(
    val name: String,
    val type: String, // Parkplatz, Raststätte, Autohof
    val description: String,
    val capacity: String,
    val isPaid: Boolean,
    val hasShower: Boolean,
    val hasFood: Boolean,
    val hasWifi: Boolean,
    val address: String
)

@Composable
fun AddLocationDialog(
    lat: Double,
    lng: Double,
    onDismiss: () -> Unit,
    onSave: (LocationData) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // STATES
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Parkplatz") }

    // Adresse (Automatisch ermittelt)
    var addressInfo by remember { mutableStateOf("Standort wird ermittelt... 🛰️") }
    var countryCode by remember { mutableStateOf("EU") }

    // Neue Felder für Bewertung
    var capacity by remember { mutableStateOf("") }
    var isPaid by remember { mutableStateOf(false) } // false = Kostenlos, true = Kostenpflichtig

    // Ausstattung (Checkboxen)
    var hasShower by remember { mutableStateOf(false) }
    var hasFood by remember { mutableStateOf(false) }
    var hasWifi by remember { mutableStateOf(false) }

    // --- GEOCODER LOGIK (Adresse holen) ---
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                // Neue API ab Android 33, aber wir nutzen die kompatible Logik
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)

                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val street = addr.thoroughfare ?: ""
                    val number = addr.featureName ?: ""
                    val city = addr.locality ?: ""
                    val country = addr.countryCode ?: "EU"

                    val fullAddr = if (street.isNotEmpty()) "$street $number, $city ($country)" else "$city ($country)"

                    withContext(Dispatchers.Main) {
                        addressInfo = fullAddr
                        countryCode = country
                        if (street.isEmpty() && city.isEmpty()) {
                            addressInfo = "${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}"
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        addressInfo = "Unbekannte Straße (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addressInfo = "GPS Koordinaten: ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}"
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ThubBlack),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "NEUEN ORT MELDEN 📍",
                        style = MaterialTheme.typography.titleMedium,
                        color = ThubNeonBlue,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.Gray)
                    }
                }

                // Automatische Adresse
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Map, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        addressInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. KATEGORIE WAHL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TypeChip(
                        icon = Icons.Default.LocalParking,
                        label = "Parkplatz",
                        isSelected = selectedType == "Parkplatz",
                        onClick = { selectedType = "Parkplatz" }
                    )
                    TypeChip(
                        icon = Icons.Default.Restaurant,
                        label = "Raststätte",
                        isSelected = selectedType == "Raststätte",
                        onClick = { selectedType = "Raststätte" }
                    )
                    TypeChip(
                        icon = Icons.Default.LocalGasStation,
                        label = "Autohof",
                        isSelected = selectedType == "Autohof",
                        onClick = { selectedType = "Autohof" }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. NAME
                ThubTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Name (z.B. Rasthof Börde)"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. KAPAZITÄT & PREIS
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        ThubTextField(
                            value = capacity,
                            onValueChange = { if (it.all { char -> char.isDigit() }) capacity = it },
                            label = "Anzahl LKW",
                            keyboardType = KeyboardType.Number
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    Box(modifier = Modifier.weight(1f).height(64.dp).padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ThubDarkGray)
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                .clickable { isPaid = !isPaid },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                if (isPaid) Icons.Default.AttachMoney else Icons.Default.MoneyOff,
                                contentDescription = null,
                                tint = if(isPaid) Color.Red else Color.Green,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPaid) "Bezahlt" else "Free",
                                color = if(isPaid) Color.Red else Color.Green,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Was gibt es dort?", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))

                // 4. AUSSTATTUNG
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FeatureToggle(icon = Icons.Default.Shower, label = "Dusche", isChecked = hasShower) { hasShower = !hasShower }
                    FeatureToggle(icon = Icons.Default.Restaurant, label = "Essen", isChecked = hasFood) { hasFood = !hasFood }
                    FeatureToggle(icon = Icons.Default.Wifi, label = "WLAN", isChecked = hasWifi) { hasWifi = !hasWifi }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. BESCHREIBUNG
                ThubTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Kommentar / Besonderheiten",
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 6. DER "MAGIC BUTTON" 3D
                Thub3DButton(
                    text = "STANDORT EINTRAGEN",
                    onClick = {
                        val data = LocationData(
                            name = name.ifEmpty { "Unbenannter Ort" },
                            type = selectedType,
                            description = description,
                            capacity = capacity.ifEmpty { "0" },
                            isPaid = isPaid,
                            hasShower = hasShower,
                            hasFood = hasFood,
                            hasWifi = hasWifi,
                            address = addressInfo
                        )
                        onSave(data)
                    }
                )
            }
        }
    }
}

// === KOMPONENTEN ===

@Composable
fun ThubTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ThubNeonBlue,
            unfocusedBorderColor = Color.Gray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = ThubNeonBlue,
            focusedLabelColor = ThubNeonBlue,
            unfocusedLabelColor = Color.Gray
        ),
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun FeatureToggle(icon: ImageVector, label: String, isChecked: Boolean, onToggle: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onToggle() }) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (isChecked) ThubNeonBlue else ThubDarkGray)
                .border(1.dp, if (isChecked) ThubNeonBlue else Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isChecked) ThubBlack else Color.Gray
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = if (isChecked) ThubNeonBlue else Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun TypeChip(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (isSelected) ThubNeonBlue else ThubDarkGray,
                contentColor = if (isSelected) ThubBlack else Color.White
            ),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) ThubNeonBlue else Color.Gray)
    }
}

// --- DER 3D GRADIENT BUTTON (Thub Style) ---
@Composable
fun Thub3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animation für sanftes "Eindrücken"
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "scale")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .graphicsLayer { // JETZT KENNT ER ES! ✅
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        // Hintergrund mit Verlauf
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ThubNeonBlue, // Oben hell
                            Color(0xFF0088AA) // Unten etwas dunkler für 3D
                        )
                    )
                )
                // SCHATTEN LOGIK
                .then(
                    if (isPressed) {
                        // GEDRÜCKT: Inner Shadow Simulation
                        Modifier.border(
                            width = 2.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        // NORMAL: Schein unten, Schatten unten außen
                        Modifier
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.3f))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = ThubNeonBlue)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = ThubBlack,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        }
    }
}