package com.truckershub.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * PARKPLATZ DAO (Data Access Object)
 *
 * Room Database Operationen für Parkplätze
 */
@Dao
interface ParkingSpotDao {

    /**
     * Fügt einen Parkplatz hinzu oder ersetzt wenn vorhanden
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(parkingSpot: ParkingSpotEntity)

    /**
     * Speichert mehrere Parkplätze auf einmal
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(parkingSpots: List<ParkingSpotEntity>)

    /**
     * Aktualisiert einen bestehenden Parkplatz
     */
    @Update
    suspend fun update(parkingSpot: ParkingSpotEntity)

    /**
     * Läd alle Parkplätze (als Flow für Live-Updates)
     */
    @Query("SELECT * FROM parking_spots ORDER BY name ASC")
    fun getAllParkingSpots(): Flow<List<ParkingSpotEntity>>

    /**
     * Läd alle Parkplätze einmalig (nicht als Flow)
     */
    @Query("SELECT * FROM parking_spots ORDER BY name ASC")
    suspend fun getAllParkingSpotsOnce(): List<ParkingSpotEntity>

    /**
     * Läd Parkplatz anhand der ID
     */
    @Query("SELECT * FROM parking_spots WHERE id = :spotId")
    fun getParkingSpotById(spotId: String): Flow<ParkingSpotEntity?>

    /**
     * Läd Parkplätze in einem Radius (einfache Distanz-Berechnung)
     * Dies ist eine vereinfachte Auswahl - bessere Version nutzt Haversine Formula
     */
    @Query("""
        SELECT * FROM parking_spots
        WHERE latitude BETWEEN :minLat AND :maxLat
        AND longitude BETWEEN :minLng AND :maxLng
        ORDER BY name ASC
    """)
    suspend fun getParkingSpotsInRadius(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): List<ParkingSpotEntity>

    /**
     * Läd Parkplätze mit einem bestimmten Ampel-Status
     */
    @Query("SELECT * FROM parking_spots WHERE currentAmpel = :ampelStatus ORDER BY name ASC")
    fun getParkingSpotsByAmpelStatus(ampelStatus: String): Flow<List<ParkingSpotEntity>>

    /**
     * Läd freie Parkplätze (Grün)
     */
    @Query("SELECT * FROM parking_spots WHERE currentAmpel = 'GREEN' ORDER BY lastSyncedAt DESC")
    fun getFreeParkingSpots(): Flow<List<ParkingSpotEntity>>

    /**
     * Aktualisiert den Ampel-Status eines Parkplatzes
     */
    @Query("UPDATE parking_spots SET currentAmpel = :ampelStatus, lastSyncedAt = :timestamp WHERE id = :spotId")
    suspend fun updateAmpelStatus(spotId: String, ampelStatus: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Läd alle veralteten Parkplätze (älter als X Sekunden)
     */
    @Query("SELECT * FROM parking_spots WHERE lastSyncedAt < :maxAge ORDER BY lastSyncedAt ASC")
    suspend fun getStaleParkingSpots(maxAge: Long): List<ParkingSpotEntity>

    /**
     * Löscht alle Parkplätze (für Cache-Reset)
     */
    @Query("DELETE FROM parking_spots")
    suspend fun deleteAll()

    /**
     * Löscht einen spezifischen Parkplatz
     */
    @Query("DELETE FROM parking_spots WHERE id = :spotId")
    suspend fun deleteById(spotId: String)

    /**
     * Zählt die Gesamtzahl der Parkplätze im Cache
     */
    @Query("SELECT COUNT(*) FROM parking_spots")
    suspend fun getCount(): Int

    /**
     * Sucht Parkplätze nach Namen
     */
    @Query("SELECT * FROM parking_spots WHERE name LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    suspend fun searchByName(searchQuery: String): List<ParkingSpotEntity>

    /**
     * Läd Parkplätze mit bestimmter Ausstattung
     */
    @Query("SELECT * FROM parking_spots WHERE hasShower = :hasShower AND hasRestaurant = :hasRestaurant")
    suspend fun getParkingSpotsByFacilities(hasShower: Boolean, hasRestaurant: Boolean): List<ParkingSpotEntity>

    /**
     * Läd kostenlose Parkplätze
     */
    @Query("SELECT * FROM parking_spots WHERE freeParking = 1 ORDER BY name ASC")
    fun getFreeParkingSpotsOnly(): Flow<List<ParkingSpotEntity>>
}