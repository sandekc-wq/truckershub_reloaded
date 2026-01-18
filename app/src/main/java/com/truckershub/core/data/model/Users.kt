package com.truckershub.core.data.model

import com.google.firebase.firestore.GeoPoint

/**
 * USER DATENMODELL (MASTER)
 *
 * Die zentrale Definition eines Benutzers.
 * Vereint Profil-Daten, Einstellungen und Statistiken.
 */
data class User(
    val id: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,

    // --- PERSÖNLICHE DATEN (Aus ProfileScreen) ---
    val firstName: String = "",
    val lastName: String = "",
    val funkName: String = "",
    val bio: String = "",
    val status: String = "Fahrbereit", // Fahrbereit, Pause, Laden/Entl.

    // --- TRUCK DATEN ---
    val company: String = "",
    val truckBrand: String = "",
    val trailerType: String = "",
    val truckLength: String = "",
    val truckType: String = "",

    // --- NESTED OBJECTS (Die "Schubladen") ---

    // Standort
    val location: UserLocation = UserLocation(),

    // Einstellungen (Sprache, DarkMode, etc.)
    val preferences: UserPreferences = UserPreferences(),

    // Statistiken (Parkplätze, Likes, etc.)
    val stats: UserStats = UserStats()
)

data class UserLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toGeoPoint() = GeoPoint(latitude, longitude)
}

data class UserPreferences(
    val shareLocation: Boolean = false,
    val language: String = "de",
    val notifications: Boolean = true,
    val darkMode: Boolean = false
)

data class UserStats(
    val totalParkings: Int = 0,
    val totalRatings: Int = 0,
    val totalFriends: Int = 0,
    val totalMessages: Int = 0,
    val ampelUpdates: Int = 0
)

data class UserSummary(
    val id: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val isOnline: Boolean = false,
    val isFriend: Boolean = false
)