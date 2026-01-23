package com.truckershub.features.navigation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckershub.core.data.model.CountryInfo
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue

/**
 * BORDER ALERT TOAST 🍞🚨
 * Erscheint unten auf der Karte, wenn eine Grenze naht.
 */
@Composable
fun BorderAlert(
    country: CountryInfo?, // Das Land, vor dem wir warnen (null = ausgeblendet)
    isVisible: Boolean,    // Soll der Toast sichtbar sein?
    distanceKm: Int = 0,   // Wie weit noch weg? (0 = Wir sind schon drin/Route führt durch)
    onOpenInfo: () -> Unit // Was passiert beim Klick?
) {
    // Animation: Von unten reinrutschen
    AnimatedVisibility(
        visible = isVisible && country != null,
        enter = slideInVertically(initialOffsetY = { it }), // Kommt von unten
        exit = slideOutVertically(targetOffsetY = { it }),  // Geht nach unten weg
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp) // Abstand vom Rand
    ) {
        country?.let { targetLand ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ThubBlack),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(ThubNeonBlue, Color.Transparent)
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenInfo() } // Klick auf ganze Karte öffnet Info auch
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Die Flagge (Groß)
                    Text(
                        text = targetLand.flag ?: "🏳️", // Fallback, falls keine Flagge da ist
                        fontSize = 32.sp
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // 2. Die Info (Text)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = ThubNeonBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (distanceKm > 0) "GRENZE IN $distanceKm KM" else "ROUTE FÜHRT DURCH",
                                color = ThubNeonBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = targetLand.name.uppercase(),
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 3. Der Button (Pfeil)
                    IconButton(
                        onClick = onOpenInfo,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = ThubDarkGray,
                            contentColor = ThubNeonBlue
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Details")
                    }
                }
            }
        }
    }
}