package com.truckershub.core.data.repository

import com.truckershub.core.data.local.LocationDao // <--- HIER WAR DER FEHLER (jetzt ohne .dao)
import com.truckershub.core.data.model.Location
import kotlinx.coroutines.flow.Flow

class LocationRepository(private val locationDao: LocationDao) {

    // Alle gespeicherten Orte holen (beobachten)
    fun getAllLocations(): Flow<List<Location>> {
        return locationDao.getAllLocations()
    }

    // Ort speichern (oder überschreiben)
    suspend fun saveLocation(location: Location) {
        locationDao.insertLocation(location)
    }

    // Ort löschen
    suspend fun deleteLocation(location: Location) {
        locationDao.deleteLocation(location)
    }
}