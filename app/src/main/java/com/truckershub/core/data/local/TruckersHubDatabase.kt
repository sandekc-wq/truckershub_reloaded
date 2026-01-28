package com.truckershub.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters // <--- WICHTIG: Import für Converters
import com.truckershub.core.data.model.Location // <--- WICHTIG: Import für Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * TRUCKERSHUB DATABASE (Room)
 *
 * Die Room Database für Offline-Cache und lokale Daten.
 * Enthält: Parkplätze (Cache) und Eigene Orte (Favoriten/Wiki).
 */
@Database(
    entities = [ParkingSpotEntity::class, Location::class], // <--- Location HINZUGEFÜGT
    version = 2, // <--- VERSION AUF 2 ERHÖHT
    exportSchema = false
)
@TypeConverters(Converters::class) // <--- CONVERTERS AKTIVIERT (für LocationType & Datum)
abstract class TruckersHubDatabase : RoomDatabase() {

    /**
     * DAO für Parkplätze
     */
    abstract fun parkingSpotDao(): ParkingSpotDao

    /**
     * DAO für Eigene Orte / Firmen-Wiki
     */
    abstract fun locationDao(): LocationDao // <--- NEUES DAO HINZUGEFÜGT

    companion object {
        // Singleton Pattern
        @Volatile
        private var INSTANCE: TruckersHubDatabase? = null

        /**
         * Ruft die Datenbank-Instanz ab
         */
        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): TruckersHubDatabase {
            // Wenn Instanz bereits existiert, zurückgeben
            // Andernfalls neu erstellen
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TruckersHubDatabase::class.java,
                    "truckershub_database"
                )
                    // Fallback wenn Migration fehlt (löscht Daten bei Versions-Wechsel -> OK für Dev)
                    .fallbackToDestructiveMigration()
                    // Callback für Initialisierung
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Callback für Datenbank-Initialisierung
         */
        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                scope.launch {
                    // Hier können Beispieldaten eingefügt werden
                }
            }
        }
    }
}

/**
 * DATENBASIS-HELPER
 *
 * Hilfsfunktionen für die Datenbank-Verwaltung
 */
object DatabaseHelper {

    /**
     * Prüft ob Daten Datenbank leer ist (bezogen auf Parkplätze)
     */
    suspend fun isEmpty(database: TruckersHubDatabase): Boolean {
        return database.parkingSpotDao().getCount() == 0
    }

    /**
     * Löscht alle veralteten Daten (älter als X Stunden)
     */
    suspend fun clearStaleData(
        database: TruckersHubDatabase,
        maxAgeHours: Int = 24
    ) {
        val maxAge = System.currentTimeMillis() - (maxAgeHours * 60 * 60 * 1000)
        val staleSpots = database.parkingSpotDao().getStaleParkingSpots(maxAge)
        staleSpots.forEach { spot ->
            database.parkingSpotDao().deleteById(spot.id)
        }
    }

    /**
     * Berechnet Cache-Größe in Bytes (schätzung)
     */
    fun estimateCacheSize(database: TruckersHubDatabase): Long {
        // Einfache Schätzung: Anzahl Einträge × ca. 500 Bytes
        return 0L
    }

    /**
     * Konvertiert alle Entity-Objekte zu Data Classes (wenn nötig)
     */
    suspend fun getAllAsDataClasses(database: TruckersHubDatabase): List<ParkingSpotEntity> {
        return database.parkingSpotDao().getAllParkingSpotsOnce()
    }
}