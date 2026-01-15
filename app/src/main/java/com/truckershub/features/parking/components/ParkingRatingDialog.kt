package com.truckershub.features.parking.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.truckershub.core.data.model.ParkingReview
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue

/**
 * BEWERTUNGS-DIALOG (KORRIGIERT)
 * 
 * 100% ThubStyle, Material3, Jetpack Compose
 */
@Suppress("DEPRECATION")
@Composable
fun ParkingRatingDialog(
    parkingId: String,
    parkingName: String,
    onDismiss: () -> Unit,
    onSubmit: (ParkingReview) -> Unit,
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    // Bewertungen (1-5)
    var overallRating by remember { mutableFloatStateOf(5f) }
    var cleanlinessRating by remember { mutableFloatStateOf(5f) }
    var safetyRating by remember { mutableFloatStateOf(5f) }
    var facilitiesRating by remember { mutableFloatStateOf(5f) }
    var foodRating by remember { mutableFloatStateOf(5f) }
    var priceRating by remember { mutableFloatStateOf(5f) }
    
    // Checkboxen
    var hasShower by remember { mutableStateOf(false) }
    var hasRestaurant by remember { mutableStateOf(false) }
    var hasShop by remember { mutableStateOf(false) }
    var hasFuelStation by remember { mutableStateOf(false) }
    var hasWifi by remember { mutableStateOf(false) }
    var hasWC by remember { mutableStateOf(false) }
    
    // Kommentar
    var comment by remember { mutableStateOf("") }
    
    // User-Info
    val userId = auth.currentUser?.uid ?: ""
    val userEmail = auth.currentUser?.email ?: "Unbekannt"
    val userName = userEmail.substringBefore("@")
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // HEADER
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFA000),
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "PARKPLATZ BEWERTEN",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThubNeonBlue,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = parkingName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // BEWERTUNGS-SLIDER
                Text(
                    text = "Wie war Ihre Erfahrung?",
                    color = ThubNeonBlue,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                RatingSlider("Gesamt-Bewertung", Icons.Filled.Star, overallRating) { overallRating = it }
                Spacer(modifier = Modifier.height(12.dp))
                
                RatingSlider("Sauberkeit", Icons.Filled.CleaningServices, cleanlinessRating) { cleanlinessRating = it }
                Spacer(modifier = Modifier.height(12.dp))
                
                RatingSlider("Sicherheit", Icons.Filled.Security, safetyRating) { safetyRating = it }
                Spacer(modifier = Modifier.height(12.dp))
                
                RatingSlider("Ausstattung", Icons.Filled.Widgets, facilitiesRating) { facilitiesRating = it }
                Spacer(modifier = Modifier.height(12.dp))
                
                RatingSlider("Essen & Qualität", Icons.Filled.Restaurant, foodRating) { foodRating = it }
                Spacer(modifier = Modifier.height(12.dp))
                
                RatingSlider("Preis-Leistung", Icons.Filled.Euro, priceRating) { priceRating = it }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // AUSSTATTUNGS-CHECKBOXEN
                Text(
                    text = "Was gibt es dort?",
                    color = ThubNeonBlue,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FacilityCheckbox("Dusche", Icons.Filled.Shower, hasShower, { hasShower = it }, Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    FacilityCheckbox("Restaurant", Icons.Filled.Restaurant, hasRestaurant, { hasRestaurant = it }, Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FacilityCheckbox("Shop", Icons.Filled.ShoppingCart, hasShop, { hasShop = it }, Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    FacilityCheckbox("Tankstelle", Icons.Filled.LocalGasStation, hasFuelStation, { hasFuelStation = it }, Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FacilityCheckbox("WiFi", Icons.Filled.Wifi, hasWifi, { hasWifi = it }, Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    FacilityCheckbox("WC", Icons.Filled.Wc, hasWC, { hasWC = it }, Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // KOMMENTAR-FELD
                Text(
                    text = "Ihre Meinung (optional)",
                    color = ThubNeonBlue,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Kommentar", color = Color.Gray) },
                    placeholder = { Text("z.B. 'Sehr saubere Duschen, freundliches Personal'", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThubNeonBlue,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = ThubNeonBlue
                    ),
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 4,
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Abbrechen")
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = {
                            val review = ParkingReview(
                                parkingId = parkingId,
                                userId = userId,
                                userName = userName,
                                userFunkName = "",
                                timestamp = System.currentTimeMillis(),
                                overall = overallRating.toInt(),
                                cleanliness = cleanlinessRating.toInt(),
                                safety = safetyRating.toInt(),
                                facilities = facilitiesRating.toInt(),
                                foodQuality = foodRating.toInt(),
                                priceValue = priceRating.toInt(),
                                comment = comment,
                                hasShower = hasShower,
                                hasRestaurant = hasRestaurant,
                                hasShop = hasShop,
                                hasFuelStation = hasFuelStation,
                                hasWifi = hasWifi,
                                hasWC = hasWC
                            )
                            onSubmit(review)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bewerten", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RatingSlider(
    label: String,
    icon: ImageVector,
    rating: Float,
    onRatingChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ThubNeonBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, color = TextWhite, style = MaterialTheme.typography.bodyMedium)
            }
            
            Row {
                repeat(5) { index ->
                    Icon(
                        imageVector = if (index < rating.toInt()) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = null,
                        tint = Color(0xFFFFA000),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = rating,
            onValueChange = onRatingChange,
            valueRange = 1f..5f,
            steps = 3,
            colors = SliderDefaults.colors(
                thumbColor = ThubNeonBlue,
                activeTrackColor = ThubNeonBlue,
                inactiveTrackColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun FacilityCheckbox(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                color = if (checked) ThubNeonBlue.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = ThubNeonBlue,
                uncheckedColor = Color.Gray,
                checkmarkColor = Color.Black
            )
        )
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) ThubNeonBlue else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = label,
            color = if (checked) ThubNeonBlue else TextWhite,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
        )
    }
}
