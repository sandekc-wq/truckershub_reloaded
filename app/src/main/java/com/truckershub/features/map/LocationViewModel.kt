package com.truckershub.features.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truckershub.core.data.local.TruckersHubDatabase
import com.truckershub.core.data.model.Location
import com.truckershub.core.data.repository.LocationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LocationRepository

    // Live-Liste aller gespeicherten Orte (für die Karte)
    val savedLocations: StateFlow<List<Location>>

    init {
        // 1. Datenbank holen
        val db = TruckersHubDatabase.getDatabase(application, viewModelScope)
        // 2. Repository erstellen
        repository = LocationRepository(db.locationDao())

        // 3. Datenstrom starten (wandelt Flow in StateFlow um, ideal für Compose)
        savedLocations = repository.getAllLocations()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    // Funktion zum Speichern (wird vom UI aufgerufen)
    fun saveLocation(location: Location) {
        viewModelScope.launch {
            repository.saveLocation(location)
        }
    }

    // Funktion zum Löschen
    fun deleteLocation(location: Location) {
        viewModelScope.launch {
            repository.deleteLocation(location)
        }
    }
}