package com.truckershub.core.data.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * PARKPLATZ DATENMODELL
 * 
 * Repräsentiert einen LKW-Parkplatz (Autohof, Raststätte, etc.)
 * 
 * @property id Eindeutige ID (Firestore Document ID)
 * @property name Name des Parkplatzes (z.B. "Autohof Geiselwind")
 * @property location GPS-Koordinaten
 * @property type Art des Parkplatzes
 * @property facilities Ausstattung (Duschen, Restaurant, etc.)
 * @property isPaid Kostenpflichtig?
 * @property pricePerNight Preis pro Nacht (falls kostenpflichtig)
 * @property truckCapacity Maximale LKW-Anzahl
 * @property currentAmpel Aktueller Status (green/yellow/red)
 * @property lastAmpelUpdate Wann wurde der Status zuletzt aktualisiert?
 * @property ratings Durchschnittsbewertungen
 */
data class ParkingSpot(
    val id: String = "",
    val name: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val type: ParkingType = ParkingType.AUTOHOF,
    val facilities: List<String> = emptyList(),
    val isPaid: Boolean = false,
    val pricePerNight: Double = 0.0,
    val truckCapacity: Int = 0,
    val currentAmpel: AmpelStatus = AmpelStatus.UNKNOWN,
    val lastAmpelUpdate: Long = 0L,
    val ratings: ParkingRatings = ParkingRatings(),
    val address: String = "",
    val country: String = "DE",
    val description: String = "",
    val reportedBy: String = ""
)

/**
 * AMPEL-STATUS
 * 
 * Zeigt an, wie voll der Parkplatz ist:
 * - GREEN: Viele freie Plätze (>30% frei)
 * - YELLOW: Teilweise belegt (10-30% frei)
 * - RED: Voll / Fast voll (<10% frei)
 * - UNKNOWN: Keine aktuellen Meldungen
 */
enum class AmpelStatus {
    GREEN,    // Grün: Plätze frei
    YELLOW,   // Gelb: Wird voll
    RED,      // Rot: Voll
    UNKNOWN   // Grau: Keine Info
}

/**
 * PARKPLATZ-TYP
 */
enum class ParkingType {
    AUTOHOF,           // Autohof (Tank & Rast, etc.)
    RASTSTAETTE,       // Autobahnraststätte
    INDUSTRIEGEBIET,   // Industriegebiet / Gewerbegebiet
    PARKPLATZ,         // Einfacher Parkplatz
    PRIVAT,            // Privater Stellplatz
    UNKNOWN            // Nicht Gelistet
}

/**
 * BEWERTUNGEN
 * 
 * Durchschnittliche Bewertungen (1-5 Sterne)
 */
data class ParkingRatings(
    val overall: Double = 0.0,        // Gesamt-Bewertung
    val cleanliness: Double = 0.0,    // Sauberkeit
    val safety: Double = 0.0,         // Sicherheit
    val facilities: Double = 0.0,     // Ausstattung
    val foodQuality: Double = 0.0,    // Essen
    val priceValue: Double = 0.0,     // Preis-Leistung
    val totalReviews: Int = 0         // Anzahl Bewertungen
)

/**
 * AMPEL-MELDUNG
 * 
 * Wenn ein Fahrer den Status eines Parkplatzes meldet
 */
data class AmpelReport(
    val id: String = "",
    val parkingId: String = "",
    val userId: String = "",
    val userName: String = "",
    val status: AmpelStatus = AmpelStatus.UNKNOWN,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (30 * 60 * 1000), // 30 Minuten
    val comment: String = ""
)

/**
 * PARKPLATZ-BEWERTUNG
 * 
 * Vollständige Bewertung eines Fahrers
 */
data class ParkingReview(
    val id: String = "",
    val parkingId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userFunkName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    
    // Bewertungen (1-5)
    val overall: Int = 5,
    val cleanliness: Int = 5,
    val safety: Int = 5,
    val facilities: Int = 5,
    val foodQuality: Int = 5,
    val priceValue: Int = 5,
    
    // Freitext
    val comment: String = "",
    
    // Checkboxen (Was gibt es dort?)
    val hasShower: Boolean = false,
    val hasRestaurant: Boolean = false,
    val hasShop: Boolean = false,
    val hasFuelStation: Boolean = false,
    val hasWifi: Boolean = false,
    val hasWC: Boolean = false
)
