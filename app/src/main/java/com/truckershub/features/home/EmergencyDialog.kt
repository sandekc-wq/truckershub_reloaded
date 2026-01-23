package com.truckershub.features.home

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubRed // Oder Color.Red falls nicht definiert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun EmergencyDialog(
    currentLat: Double,
    currentLon: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // State für die Adresse
    var addressText by remember { mutableStateOf("Standort wird ermittelt...") }

    // Adresse laden beim Start
    LaunchedEffect(Unit) {
        addressText = getAddressFromLocation(context, currentLat, currentLon)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ThubBlack),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, Color.Red, RoundedCornerShape(24.dp))
                .shadow(24.dp, spotColor = Color.Red)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("NOTFALL / EMERGENCY", color = Color.Red, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Bewahren Sie Ruhe!", color = Color.Gray, fontSize = 14.sp)

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.DarkGray)

                // KOORDINATEN (Groß!)
                Text("IHRE POSITION:", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                // Formatiert auf 5 Nachkommastellen für Präzision
                val coordsString = "${String.format("%.5f", currentLat)}, ${String.format("%.5f", currentLon)}"

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .clickable {
                            clipboardManager.setText(AnnotatedString(coordsString))
                            Toast.makeText(context, "Koordinaten kopiert!", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Text(
                        text = coordsString,
                        color = TextWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ContentCopy, null, tint = Color.Gray)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ADRESSE
                Text("UNGEFÄHRE ADRESSE:", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = addressText,
                    color = TextWhite,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ACTION BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EmergencyButton(
                        text = "POLIZEI\n110",
                        icon = Icons.Default.LocalPolice,
                        color = Color.Blue,
                        modifier = Modifier.weight(1f)
                    ) {
                        dialNumber(context, "110")
                    }

                    EmergencyButton(
                        text = "NOTRUF\n112",
                        icon = Icons.Default.Phone,
                        color = Color.Red,
                        modifier = Modifier.weight(1f)
                    ) {
                        dialNumber(context, "112")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Abbrechen Button
                TextButton(onClick = onDismiss) {
                    Text("SCHLIESSEN", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun EmergencyButton(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, color),
        modifier = modifier.height(100.dp),
        contentPadding = PaddingValues(0.dp) // Damit Text nicht abgeschnitten wird
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text, color = TextWhite, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

// Hilfsfunktion: Nummer wählen
private fun dialNumber(context: Context, number: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Fehler beim Wählen", Toast.LENGTH_SHORT).show()
    }
}

// Hilfsfunktion: Geocoding (Adresse holen)
private suspend fun getAddressFromLocation(context: Context, lat: Double, lon: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                // Wir bauen uns einen schönen String: Straße, PLZ Stadt, Land
                val sb = StringBuilder()
                if (address.thoroughfare != null) sb.append(address.thoroughfare).append(" ")
                if (address.subThoroughfare != null) sb.append(address.subThoroughfare).append(", ")
                if (address.postalCode != null) sb.append(address.postalCode).append(" ")
                if (address.locality != null) sb.append(address.locality)

                if (sb.isEmpty()) "Adresse unbekannt" else sb.toString()
            } else {
                "Keine Adresse gefunden (Offroad?)"
            }
        } catch (e: Exception) {
            "Adress-Dienst nicht verfügbar"
        }
    }
}