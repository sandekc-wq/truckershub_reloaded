package com.truckershub.core.data.model

import com.google.firebase.firestore.GeoPoint

/**
 * USER DATENMODELL
 *
 * Vollständiges Benutzerprofil für TruckersHub
 */
data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,

    // Standort
    val location: UserLocation = UserLocation(),

    // Einstellungen
    val preferences: UserPreferences = UserPreferences(),

    // Statistiken
    val stats: UserStats = UserStats()
)

/**
 * STANDORT EINES BENUTZERS
 *
 * Wird für Standort-Sharing verwendet
 */
data class UserLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toGeoPoint() = GeoPoint(latitude, longitude)
}

/**
 * BENUTZER-EINSTELLUNGEN
 *
 * Konfiguration des Profils und der App
 */
data class UserPreferences(
    val shareLocation: Boolean = false,     // Standort freigeben?
    val language: String = "de",            // "de" oder "en"
    val notifications: Boolean = true,      // Toast-Benachrichtigungen
    val darkMode: Boolean = false           // Dunkler Modus
)

/**
 * BENUTZER-STATISTIKEN
 *
 * Zeigt Aktivität und Engagierung
 */
data class UserStats(
    val totalParkings: Int = 0,             // Wie oft geparkt
    val totalRatings: Int = 0,              // Wie viele Bewertungen abgegeben
    val totalFriends: Int = 0,              // Anzahl Freunde
    val totalMessages: Int = 0,             // Gesamtzahl Nachrichten
    val ampelUpdates: Int = 0               // Wie viele Ampel-Updates
)

/**
 * BENUTZER-KURZINFO
 *
 * Für Listen und Anzeigen (leichtgewichtig)
 */
data class UserSummary(
    val id: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val isOnline: Boolean = false,
    val isFriend: Boolean = false
)