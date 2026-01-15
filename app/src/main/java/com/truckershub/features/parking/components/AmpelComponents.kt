package com.truckershub.features.parking.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.truckershub.core.data.model.AmpelStatus
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue

/**
 * AMPEL-MELDE-DIALOG
 * 
 * Erlaubt dem Fahrer, den Status eines Parkplatzes zu melden:
 * - Grün: Viele freie Plätze
 * - Gelb: Wird voll
 * - Rot: Komplett voll
 */
@Composable
fun AmpelReportDialog(
    parkingName: String,
    onDismiss: () -> Unit,
    onReport: (AmpelStatus, String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf<AmpelStatus?>(null) }
    var comment by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "PARKPLATZ-STATUS MELDEN",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThubNeonBlue,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = parkingName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Ampel-Auswahl
                Text(
                    text = "Wie ist die aktuelle Situation?",
                    color = TextWhite,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Grün - Plätze frei
                    AmpelButton(
                        status = AmpelStatus.GREEN,
                        label = "Plätze frei",
                        icon = Icons.Filled.CheckCircle,
                        color = Color(0xFF00C853),
                        isSelected = selectedStatus == AmpelStatus.GREEN,
                        onClick = { selectedStatus = AmpelStatus.GREEN }
                    )
                    
                    // Gelb - Wird voll
                    AmpelButton(
                        status = AmpelStatus.YELLOW,
                        label = "Wird voll",
                        icon = Icons.Filled.Warning,
                        color = Color(0xFFFFA000),
                        isSelected = selectedStatus == AmpelStatus.YELLOW,
                        onClick = { selectedStatus = AmpelStatus.YELLOW }
                    )
                    
                    // Rot - Voll
                    AmpelButton(
                        status = AmpelStatus.RED,
                        label = "Voll",
                        icon = Icons.Filled.Cancel,
                        color = Color(0xFFD32F2F),
                        isSelected = selectedStatus == AmpelStatus.RED,
                        onClick = { selectedStatus = AmpelStatus.RED }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Optionaler Kommentar
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Kommentar (optional)", color = ThubNeonBlue) },
                    placeholder = { Text("z.B. 'Nur noch 5 Plätze frei'", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThubNeonBlue,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = ThubNeonBlue
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Abbrechen
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Abbrechen", color = Color.Gray)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Melden
                    Button(
                        onClick = {
                            selectedStatus?.let { status ->
                                onReport(status, comment)
                                onDismiss()
                            }
                        },
                        enabled = selectedStatus != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThubNeonBlue,
                            disabledContainerColor = Color.Gray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Melden", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * AMPEL-BUTTON (Grün/Gelb/Rot)
 */
@Composable
fun AmpelButton(
    status: AmpelStatus,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) color else Color.Gray,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
            .width(85.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = if (isSelected) color else TextWhite,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * AMPEL-INDIKATOR (Mini-Anzeige)
 * 
 * Zeigt den aktuellen Status als farbigen Punkt mit Text
 */
@Composable
fun AmpelIndicator(
    status: AmpelStatus,
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    val (color, text) = when (status) {
        AmpelStatus.GREEN -> Color(0xFF00C853) to "Frei"
        AmpelStatus.YELLOW -> Color(0xFFFFA000) to "Teilweise belegt"
        AmpelStatus.RED -> Color(0xFFD32F2F) to "Voll"
        AmpelStatus.UNKNOWN -> Color.Gray to "Keine Info"
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
        )
        
        if (showText) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * STERNE-BEWERTUNG (Anzeige)
 */
@Composable
fun StarRating(
    rating: Double,
    maxStars: Int = 5,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        repeat(maxStars) { index ->
            Icon(
                imageVector = if (index < rating.toInt()) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = Color(0xFFFFA000),
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = String.format("%.1f", rating),
            color = Color(0xFFFFA000),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
