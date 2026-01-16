package com.truckershub.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue

/**
 * OFFLINE WARNING BANNER (Thub Style)
 *
 * Zeigt Warnung wenn App offline ist
 * Mit Option zum Schließen
 */
@Composable
fun OfflineWarningBanner(
    isVisible: Boolean,
    onDismiss: () -> Unit = {},
    lastUpdated: Long? = null
) {
    var visible by remember { mutableStateOf(isVisible) }

    AnimatedVisibility(
        visible = visible && (!isVisible || (lastUpdated != null && isStale(lastUpdated!!))),
        enter = slideInVertically(
            initialOffsetY = { -it }
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it }
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFF6B6B) // Rot-Warnfarbe
            ),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Warn-Icon
                Icon(
                    imageVector = Icons.Filled.WifiOff,
                    contentDescription = "Offline",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "KEINE INTERNETVERBINDUNG",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )

                    if (lastUpdated != null) {
                        Text(
                            text = "Letzte Aktualisierung: ${formatLastUpdated(lastUpdated)}",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else {
                        Text(
                            text = "Daten möglicherweise veraltet",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Schließen-Button
                IconButton(
                    onClick = {
                        visible = false
                        onDismiss()
                    }
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Schließen",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * OFFLINE TOAST (Thub Style)
 *
 * Kleiner Toast-Nachricht unten im Screen
 */
@Composable
fun OfflineToast(
    isVisible: Boolean,
    onDismiss: () -> Unit = {},
    lastUpdated: Long? = null
) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            TextButton(onClick = onDismiss) {
                Text("OK", style = MaterialTheme.typography.labelSmall)
            }
        },
        containerColor = ThubDarkGray,
        contentColor = TextWhite
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.WifiOff,
                contentDescription = "Offline",
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (lastUpdated != null) {
                    "Offline • Letzte Aktualisierung: ${formatLastUpdated(lastUpdated)}"
                } else {
                    "Keine Internetverbindung"
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * VERALTETE DATEN WARNUNG
 *
 * Zeigt Warnung wenn Daten älter als X Zeit sind
 */
@Composable
fun StaleDataWarningBanner(
    isVisible: Boolean,
    lastUpdated: Long,
    onRefresh: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFA500) // Orange-Warnfarbe
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Update,
                    contentDescription = "Update",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DATEN VERALTET",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Letzte Aktualisierung: ${formatLastUpdated(lastUpdated)}",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Refresh Button
                TextButton(
                    onClick = onRefresh,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("Aktualisieren")
                }

                // Schließen-Button
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Schließen",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Hilfsfunktion: Formatiert Zeitdifferenz
 */
private fun formatLastUpdated(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestamp

    val diffMinutes = diffMs / (1000 * 60)
    val diffHours = diffMs / (1000 * 60 * 60)

    return when {
        diffMinutes < 1L -> "Gerade eben"
        diffMinutes < 60L -> "vor $diffMinutes Min${if (diffMinutes == 1L) "" else "uten"}"
        diffHours < 24L -> "vor $diffHours Std${if (diffHours == 1L) "" else "unden"}"
        else -> "vor ${diffHours / 24} Tagen"
    }
}

/**
 * Prüft ob Daten veraltet sind
 */
private fun isStale(timestamp: Long, maxAgeMinutes: Int = 60): Boolean {
    val maxAgeMs = maxAgeMinutes * 60 * 1000L
    val age = System.currentTimeMillis() - timestamp
    return age > maxAgeMs
}

/**
 * LETZTE AKTUALISIERUNG BADGE
 *
 * Kleiner Badge der die letzte Aktualisierung zeigt
 */
@Composable
fun LastUpdatedBadge(
    lastUpdated: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ThubDarkGray.copy(alpha = 0.8f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Update,
                contentDescription = "Aktualisiert",
                tint = ThubNeonBlue,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formatLastUpdated(lastUpdated),
                color = TextWhite,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}