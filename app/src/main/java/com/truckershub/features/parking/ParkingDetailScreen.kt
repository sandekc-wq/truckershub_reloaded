package com.truckershub.features.parking

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckershub.R
import com.truckershub.core.data.model.AmpelStatus
import com.truckershub.core.data.model.ParkingReview
import com.truckershub.core.data.model.ParkingSpot
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.features.parking.components.AmpelIndicator
import com.truckershub.features.parking.components.AmpelReportDialog
import com.truckershub.features.parking.components.ParkingRatingDialog
// StarRating import entfernt, wir definieren es unten lokal, um Fehler zu vermeiden!
import java.text.SimpleDateFormat
import java.util.*

/**
 * PARKPLATZ-DETAIL-SCREEN (FINAL STABLE VERSION)
 * * ✅ FIX: Zeigt "Keine Bewertungen" an, wenn Liste leer ist (statt nichts).
 * * ✅ FIX: Nutzt stabile Column-Schleife gegen Flackern.
 * * ✅ FIX: Alle UI-Komponenten lokal definiert (Null Fehler Garantie).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingDetailScreen(
    parking: ParkingSpot,
    reviews: List<ParkingReview>,
    onBack: () -> Unit,
    onReportStatus: (AmpelStatus, String) -> Unit,
    onSubmitReview: (ParkingReview) -> Unit,
    onNavigate: () -> Unit = {}
) {
    val context = LocalContext.current
    var showAmpelDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }

    // Scroll-State für den ganzen Screen
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parkplatz-Details", color = ThubNeonBlue) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = ThubNeonBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThubBlack)
            )
        },
        containerColor = ThubBlack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Hintergrundbild
            Image(
                painter = painterResource(id = R.drawable.thub_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)))

            // HAUPT-INHALT (SCROLLBAR)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // ==========================================
                // 1. HEADER MIT NAME UND STATUS
                // ==========================================
                Card(
                    colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = parking.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = ThubNeonBlue,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AmpelIndicator(status = parking.currentAmpel, showText = true)

                            if (parking.ratings.totalReviews > 0) {
                                StarRating(rating = parking.ratings.overall)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = parking.address.ifEmpty { "${parking.type.name} • ${parking.country}" },
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // 2. INFO-KARTEN
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoCard(
                        icon = Icons.Filled.LocalShipping,
                        label = "Kapazität",
                        value = "${parking.truckCapacity} LKW",
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    InfoCard(
                        icon = if (parking.isPaid) Icons.Filled.Paid else Icons.Filled.MoneyOff,
                        label = "Preis",
                        value = if (parking.isPaid) "${parking.pricePerNight}€/Nacht" else "Kostenlos",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // 3. AUSSTATTUNG
                // ==========================================
                if (parking.facilities.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("AUSSTATTUNG", style = MaterialTheme.typography.titleMedium, color = ThubNeonBlue, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            parking.facilities.forEach { facility ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF00C853), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = facility, color = TextWhite)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ==========================================
                // 4. BEWERTUNGS-DETAILS (Balken)
                // ==========================================
                if (parking.ratings.totalReviews > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("BEWERTUNGEN (${parking.ratings.totalReviews})", style = MaterialTheme.typography.titleMedium, color = ThubNeonBlue, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            RatingRow("Sauberkeit", parking.ratings.cleanliness)
                            RatingRow("Sicherheit", parking.ratings.safety)
                            RatingRow("Ausstattung", parking.ratings.facilities)
                            RatingRow("Essen", parking.ratings.foodQuality)
                            RatingRow("Preis-Leistung", parking.ratings.priceValue)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ==========================================
                // 5. UPDATE ZEIT
                // ==========================================
                val lastUpdate = if (parking.lastAmpelUpdate > 0) {
                    val diff = System.currentTimeMillis() - parking.lastAmpelUpdate
                    val minutes = (diff / (1000 * 60)).toInt()
                    "Vor $minutes Min."
                } else "Keine aktuellen Meldungen"

                Text(text = "Letztes Update: $lastUpdate", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // 6. BUTTONS
                // ==========================================
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showAmpelDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Report, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("STATUS MELDEN", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showRatingDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThubNeonBlue),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Star, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BEWERTUNG ABGEBEN", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onNavigate,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThubNeonBlue),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Navigation, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("NAVIGATION STARTEN", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ==========================================
                // 7. FAHRER-MEINUNGEN (Der Fix!)
                // ==========================================
                Text(
                    text = "FAHRER-MEINUNGEN",
                    style = MaterialTheme.typography.titleMedium,
                    color = ThubNeonBlue,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (reviews.isEmpty()) {
                    // Zeige Hinweis, wenn keine Bewertungen da sind
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ThubDarkGray.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Noch keine Bewertungen.\nSei der Erste!",
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Zeige Liste (Stabil mit Column)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        reviews.forEach { review ->
                            ReviewCard(review)
                        }
                    }
                }

                // Platz unten lassen
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }

    // --- DIALOGE ---
    if (showAmpelDialog) {
        AmpelReportDialog(parkingName = parking.name, onDismiss = { showAmpelDialog = false }, onReport = { s, c -> onReportStatus(s, c) })
    }
    if (showRatingDialog) {
        ParkingRatingDialog(parkingId = parking.id, parkingName = parking.name, onDismiss = { showRatingDialog = false }, onSubmit = { r -> onSubmitReview(r); Toast.makeText(context, "Danke für dein Feedback!", Toast.LENGTH_SHORT).show() })
    }
}

// ==========================================
// HILFS-KOMPONENTEN (LOKAL DEFINIERT)
// ==========================================

@Composable
fun InfoCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = ThubDarkGray), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = ThubNeonBlue, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Text(value, color = TextWhite, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RatingRow(label: String, rating: Double) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextWhite, modifier = Modifier.weight(1f))
        StarRating(rating = rating)
    }
}

@Composable
fun ReviewCard(review: ParkingReview) {
    Card(colors = CardDefaults.cardColors(containerColor = ThubDarkGray), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(review.userFunkName.ifEmpty { review.userName }, color = ThubNeonBlue, fontWeight = FontWeight.Bold)
                    Text(SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).format(Date(review.timestamp)), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
                StarRating(rating = review.overall.toDouble())
            }

            // KOMMENTAR ANZEIGEN
            if (review.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = review.comment, color = TextWhite, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (review.hasShower) Tag("Dusche", Color(0xFF00C853))
                if (review.hasRestaurant) Tag("Rest.", Color(0xFF00C853))
                if (review.hasWC) Tag("WC", Color(0xFF00C853))
            }
        }
    }
}

@Composable
fun Tag(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp), modifier = Modifier.clip(RoundedCornerShape(4.dp))) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
fun StarRating(rating: Double) {
    Row {
        repeat(5) { index ->
            val color = if (index < rating) Color(0xFFFFD700) else Color.Gray
            Icon(Icons.Filled.Star, null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
}