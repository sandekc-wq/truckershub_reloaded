package com.truckershub.core.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckershub.core.data.model.Location
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    // Alle Orte laden (für die Liste)
    @Query("SELECT * FROM locations ORDER BY name ASC")
    fun getAllLocations(): Flow<List<Location>>

    // Neuen Ort speichern (oder überschreiben)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: Location)

    // Ort löschen
    @Delete
    suspend fun deleteLocation(location: Location)

    // Ort anhand der ID finden
    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getLocationById(id: Long): Location?
}