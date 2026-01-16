package com.truckershub.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PARKPLATZ ENTITY (Room Database)
 *
 * Lokale Speicherung von Parkplätzen für Offline-Unterstützung
 * Spiegelt die ParkingSpot Data Class aus ParkingSpot.kt
 */
@Entity(tableName = "parking_spots")
data class ParkingSpotEntity(
    @PrimaryKey
    val id: String,                          // Firestore Document ID

    // Basis-Informationen
    val name: String,                        // Name des Parkplatzes
    val address: String,                     // Addresse
    val latitude: Double,                    // GPS Breitengrad
    val longitude: Double,                   // GPS Längengrad
    val type: String,                        // ParkingType (AUTOHOF, RASTSTAETTE, etc.)
    val country: String = "DE",              // Ländercode

    // Ausstattung
    val hasToilet: Boolean = false,
    val hasShower: Boolean = false,
    val hasRestaurant: Boolean = false,
    val hasShop: Boolean = false,
    val hasWifi: Boolean = false,
    val hasFuelStation: Boolean = false,
    val freeParking: Boolean = false,

    // Status & Bewertung
    val isPaid: Boolean = false,             // Kostenpflichtig?
    val pricePerNight: Double = 0.0,         // Preis bei kostenpflichtig
    val truckCapacity: Int = 0,              // Maximale LKW-Anzahl
    val currentAmpel: String = "UNKNOWN",    // Ampel-Status (GREEN, YELLOW, RED, UNKNOWN)

    // Bewertungen (Durchschnitte)
    val overallRating: Double = 0.0,
    val cleanlinessRating: Double = 0.0,
    val safetyRating: Double = 0.0,
    val facilitiesRating: Double = 0.0,
    val foodQualityRating: Double = 0.0,
    val priceValueRating: Double = 0.0,
    val totalReviews: Int = 0,

    // Cache-Metadaten
    val lastSyncedAt: Long = 0L,             // Wann zuletzt aus Firebase geladen?
    val createdAt: Long = 0L                 // Wann erstellt?
) {
    /**
     * Konvertiert zu GeoPoint für Karte
     */
    fun toGeoPoint(): com.google.firebase.firestore.GeoPoint {
        return com.google.firebase.firestore.GeoPoint(latitude, longitude)
    }

    /**
     * Prüft ob Daten veraltet sind (älter als 1 Stunde)
     */
    fun isStale(): Boolean {
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        return lastSyncedAt < oneHourAgo
    }

    companion object {
        /**
         * Konvertiert von ParkingSpot Data Class
         */
        fun fromParkingSpot(
            id: String,
            name: String,
            address: String,
            latitude: Double,
            longitude: Double,
            type: String,
            facilities: List<String>,
            isPaid: Boolean,
            pricePerNight: Double,
            truckCapacity: Int,
            currentAmpel: String,
            overallRating: Double,
            cleanlinessRating: Double,
            safetyRating: Double,
            facilitiesRating: Double,
            foodQualityRating: Double,
            priceValueRating: Double,
            totalReviews: Int,
            country: String = "DE"
        ): ParkingSpotEntity {
            return ParkingSpotEntity(
                id = id,
                name = name,
                address = address,
                latitude = latitude,
                longitude = longitude,
                type = type,
                country = country,
                hasToilet = facilities.contains("toilet"),
                hasShower = facilities.contains("shower"),
                hasRestaurant = facilities.contains("restaurant"),
                hasShop = facilities.contains("shop"),
                hasWifi = facilities.contains("wifi"),
                hasFuelStation = facilities.contains("fuelStation"),
                freeParking = !isPaid,
                isPaid = isPaid,
                pricePerNight = pricePerNight,
                truckCapacity = truckCapacity,
                currentAmpel = currentAmpel,
                overallRating = overallRating,
                cleanlinessRating = cleanlinessRating,
                safetyRating = safetyRating,
                facilitiesRating = facilitiesRating,
                foodQualityRating = foodQualityRating,
                priceValueRating = priceValueRating,
                totalReviews = totalReviews,
                lastSyncedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
        }
    }
}