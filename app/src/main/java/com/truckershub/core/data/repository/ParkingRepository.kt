package com.truckershub.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.truckershub.core.data.model.AmpelReport
import com.truckershub.core.data.model.AmpelStatus
import com.truckershub.core.data.model.ParkingReview
import com.truckershub.core.data.model.ParkingSpot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ParkingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // --- PARKPLÄTZE ---
    fun getParkingSpotsNearby(centerLocation: GeoPoint, radiusKm: Double = 50.0): Flow<List<ParkingSpot>> = callbackFlow {
        val listener = firestore.collection("parkingSpots").addSnapshotListener { snapshot, _ ->
            val spots = snapshot?.documents?.mapNotNull { doc ->
                try { doc.toObject(ParkingSpot::class.java)?.copy(id = doc.id) } catch (e: Exception) { null }
            } ?: emptyList()
            trySend(spots) // Filterung vereinfacht für Test
        }
        awaitClose { listener.remove() }
    }

    suspend fun getParkingSpot(parkingId: String): ParkingSpot? {
        return try {
            val doc = firestore.collection("parkingSpots").document(parkingId).get().await()
            doc.toObject(ParkingSpot::class.java)?.copy(id = doc.id)
        } catch (e: Exception) { null }
    }

    // --- BEWERTUNGEN (DER FIX) ---
    suspend fun submitReview(review: ParkingReview): Boolean {
        return try {
            val docRef = firestore.collection("parkingReviews").document()

            // WICHTIG: Manuelles Mapping, damit keine leeren Dokumente entstehen!
            val data = hashMapOf(
                "id" to docRef.id,
                "parkingId" to review.parkingId,
                "userId" to review.userId,
                "userName" to review.userName,
                "timestamp" to System.currentTimeMillis(),
                "overall" to review.overall,
                "cleanliness" to review.cleanliness,
                "safety" to review.safety,
                "facilities" to review.facilities,
                "foodQuality" to review.foodQuality,
                "priceValue" to review.priceValue,
                "comment" to review.comment,
                "hasShower" to review.hasShower,
                "hasRestaurant" to review.hasRestaurant,
                "hasShop" to review.hasShop,
                "hasFuelStation" to review.hasFuelStation,
                "hasWifi" to review.hasWifi,
                "hasWC" to review.hasWC
            )

            docRef.set(data).await()
            updateRatings(review.parkingId) // Durchschnitt neu berechnen
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getReviews(parkingId: String): Flow<List<ParkingReview>> = callbackFlow {
        val listener = firestore.collection("parkingReviews")
            .whereEqualTo("parkingId", parkingId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ParkingReview::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(reviews)
            }
        awaitClose { listener.remove() }
    }

    private suspend fun updateRatings(parkingId: String) {
        try {
            val reviews = firestore.collection("parkingReviews").whereEqualTo("parkingId", parkingId).get().await()
            if (!reviews.isEmpty) {
                val list = reviews.toObjects(ParkingReview::class.java)
                val updates = mapOf(
                    "ratings.overall" to list.map { it.overall.toDouble() }.average(),
                    "ratings.totalReviews" to list.size
                )
                firestore.collection("parkingSpots").document(parkingId).update(updates)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // --- AMPEL ---
    suspend fun reportAmpelStatus(parkingId: String, userId: String, userName: String, status: AmpelStatus, comment: String): Boolean {
        return try {
            val data = hashMapOf(
                "parkingId" to parkingId, "status" to status.name, "comment" to comment,
                "timestamp" to System.currentTimeMillis(), "expiresAt" to System.currentTimeMillis() + 1800000
            )
            firestore.collection("ampelReports").add(data).await()
            firestore.collection("parkingSpots").document(parkingId).update("currentAmpel", status.name).await()
            true
        } catch (e: Exception) { false }
    }

    fun getAmpelReports(parkingId: String): Flow<List<AmpelReport>> = callbackFlow {
        trySend(emptyList()) // Platzhalter, um Fehler zu vermeiden
        awaitClose { }
    }
}