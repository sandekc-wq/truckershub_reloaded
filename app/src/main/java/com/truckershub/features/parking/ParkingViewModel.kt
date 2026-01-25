package com.truckershub.features.parking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
 * Verwaltet Parkplätze, Bewertungen und aktualisiert nun auch die User-Statistiken! 📈
 */
class ParkingViewModel(
    private val repository: ParkingRepository = ParkingRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    // ==========================================
    // ZUSTAND (STATE)
    // ==========================================

    var parkingSpots by mutableStateOf<List<ParkingSpot>>(emptyList())
        private set

    var selectedParking by mutableStateOf<ParkingSpot?>(null)
        private set

    var reviews by mutableStateOf<List<ParkingReview>>(emptyList())
        private set

    var ampelReports by mutableStateOf<List<AmpelReport>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val currentUserId = auth.currentUser?.uid
    private val currentUserName = auth.currentUser?.email?.substringBefore("@") ?: "Fahrer"

    // ==========================================
    // FUNKTIONEN
    // ==========================================

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

    fun selectParking(parkingId: String) {
        viewModelScope.launch {
            val parking = repository.getParkingSpot(parkingId)
            selectedParking = parking

            if (parking != null) {
                loadReviews(parkingId)
                loadAmpelReports(parkingId)
            }
        }
    }

    fun clearSelection() {
        selectedParking = null
        reviews = emptyList()
        ampelReports = emptyList()
    }

    // Hilfsfunktion für die Map (damit der neue Marker sofort sichtbar ist)
    fun addTemporarySpot(spot: ParkingSpot) {
        val currentList = parkingSpots.toMutableList()
        currentList.add(spot)
        parkingSpots = currentList
    }

    /**
     * Ampel-Status melden UND Statistik aktualisieren
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

                // --- STATISTIK UPDATE ---
                val userRef = FirebaseFirestore.getInstance().collection("users").document(currentUserId)

                // 1. Ampel-Updates hochzählen
                userRef.update("stats.ampelUpdates", FieldValue.increment(1))

                // 2. "Parkplätze genutzt" hochzählen (Wer meldet, steht meist auch dort)
                userRef.update("stats.totalParkings", FieldValue.increment(1))

                // Liste neu laden, um Änderung sofort zu sehen
                selectedParking?.let {
                    selectParking(parkingId)
                }
            } else {
                errorMessage = "Meldung konnte nicht gespeichert werden"
            }
        }
    }

    /**
     * Bewertung abgeben UND Statistik aktualisieren
     */
    fun submitReview(review: ParkingReview) {
        viewModelScope.launch {
            val success = repository.submitReview(review)

            if (success) {
                errorMessage = null

                // --- STATISTIK UPDATE ---
                // Bewertungen hochzählen
                currentUserId?.let { uid ->
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .update("stats.totalRatings", FieldValue.increment(1))
                }

                review.parkingId.let { loadReviews(it) }
            } else {
                errorMessage = "Bewertung konnte nicht gespeichert werden"
            }
        }
    }

    private fun loadReviews(parkingId: String) {
        viewModelScope.launch {
            repository.getReviews(parkingId)
                .catch { e -> errorMessage = "Bewertungen konnten nicht geladen werden" }
                .collect { loadedReviews -> reviews = loadedReviews }
        }
    }

    private fun loadAmpelReports(parkingId: String) {
        viewModelScope.launch {
            repository.getAmpelReports(parkingId)
                .catch { /* Fehler ignorieren */ }
                .collect { reports -> ampelReports = reports }
        }
    }
}