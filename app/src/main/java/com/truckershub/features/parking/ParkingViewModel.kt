package com.truckershub.features.parking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.truckershub.core.data.model.AmpelReport
import com.truckershub.core.data.model.AmpelStatus
import com.truckershub.core.data.model.ParkingReview
import com.truckershub.core.data.model.ParkingSpot
import com.truckershub.core.data.repository.ParkingRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * PARKPLATZ VIEW-MODEL
 * 
 * ⚡ WICHTIG: Was macht ein ViewModel?
 * -------------------------------------
 * 1. Hält den Zustand der UI (welche Parkplätze zeigen wir an?)
 * 2. Kommuniziert mit dem Repository (Daten holen/speichern)
 * 3. Überlebt Screen-Rotation (Daten gehen nicht verloren)
 * 
 * Schema:
 * Screen → ViewModel → Repository → Firebase
 */
class ParkingViewModel(
    private val repository: ParkingRepository = ParkingRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {
    
    // ==========================================
    // ZUSTAND (STATE)
    // ==========================================
    
    // Liste aller Parkplätze in der Nähe
    var parkingSpots by mutableStateOf<List<ParkingSpot>>(emptyList())
        private set
    
    // Ausgewählter Parkplatz (für Detail-Ansicht)
    var selectedParking by mutableStateOf<ParkingSpot?>(null)
        private set
    
    // Bewertungen des ausgewählten Parkplatzes
    var reviews by mutableStateOf<List<ParkingReview>>(emptyList())
        private set
    
    // Ampel-Meldungen
    var ampelReports by mutableStateOf<List<AmpelReport>>(emptyList())
        private set
    
    // Ladezustand
    var isLoading by mutableStateOf(false)
        private set
    
    // Fehlermeldung
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    // Aktueller Benutzer
    private val currentUserId = auth.currentUser?.uid
    private val currentUserName = auth.currentUser?.email?.substringBefore("@") ?: "Fahrer"
    
    // ==========================================
    // FUNKTIONEN
    // ==========================================
    
    /**
     * Parkplätze in der Nähe laden
     * 
     * @param myLocation Meine aktuelle Position
     * @param radiusKm Suchradius in Kilometern
     */
    fun loadParkingSpotsNearby(myLocation: GeoPoint, radiusKm: Double = 50.0) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            repository.getParkingSpotsNearby(myLocation, radiusKm)
                .catch { e ->
                    errorMessage = "Fehler beim Laden: ${e.localizedMessage}"
                    isLoading = false
                }
                .collect { spots ->
                    parkingSpots = spots
                    isLoading = false
                }
        }
    }
    
    /**
     * Einzelnen Parkplatz auswählen (für Detail-Screen)
     */
    fun selectParking(parkingId: String) {
        viewModelScope.launch {
            val parking = repository.getParkingSpot(parkingId)
            selectedParking = parking
            
            // Bewertungen laden
            if (parking != null) {
                loadReviews(parkingId)
                loadAmpelReports(parkingId)
            }
        }
    }
    
    /**
     * Parkplatz-Auswahl zurücksetzen
     */
    fun clearSelection() {
        selectedParking = null
        reviews = emptyList()
        ampelReports = emptyList()
    }
    
    /**
     * Ampel-Status melden
     * 
     * @param parkingId ID des Parkplatzes
     * @param status Grün, Gelb oder Rot
     * @param comment Optionaler Kommentar
     */
    fun reportAmpelStatus(
        parkingId: String,
        status: AmpelStatus,
        comment: String = ""
    ) {
        if (currentUserId == null) {
            errorMessage = "Nicht eingeloggt!"
            return
        }
        
        viewModelScope.launch {
            val success = repository.reportAmpelStatus(
                parkingId = parkingId,
                userId = currentUserId,
                userName = currentUserName,
                status = status,
                comment = comment
            )
            
            if (success) {
                errorMessage = null
                // Parkplatz-Liste neu laden, um aktuellen Status zu zeigen
                selectedParking?.let {
                    selectParking(parkingId)
                }
            } else {
                errorMessage = "Meldung konnte nicht gespeichert werden"
            }
        }
    }
    
    /**
     * Bewertung abgeben
     */
    fun submitReview(review: ParkingReview) {
        viewModelScope.launch {
            val success = repository.submitReview(review)
            
            if (success) {
                errorMessage = null
                // Bewertungen neu laden
                review.parkingId.let { loadReviews(it) }
            } else {
                errorMessage = "Bewertung konnte nicht gespeichert werden"
            }
        }
    }
    
    /**
     * Bewertungen für einen Parkplatz laden
     */
    private fun loadReviews(parkingId: String) {
        viewModelScope.launch {
            repository.getReviews(parkingId)
                .catch { e ->
                    errorMessage = "Bewertungen konnten nicht geladen werden"
                }
                .collect { loadedReviews ->
                    reviews = loadedReviews
                }
        }
    }
    
    /**
     * Ampel-Meldungen laden
     */
    private fun loadAmpelReports(parkingId: String) {
        viewModelScope.launch {
            repository.getAmpelReports(parkingId)
                .catch { e ->
                    // Ignorieren, nicht kritisch
                }
                .collect { reports ->
                    ampelReports = reports
                }
        }
    }
}
