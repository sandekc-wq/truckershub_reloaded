package com.truckershub.features.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.truckershub.core.data.model.Route
import com.truckershub.core.data.model.RouteDetails
import com.truckershub.core.data.model.RoutePoint
import com.truckershub.core.data.model.RouteRequest
import com.truckershub.core.data.model.RouteInstruction
import com.truckershub.core.data.model.TruckProfile
import com.truckershub.core.data.repository.RouteRepository
import kotlinx.coroutines.launch

/**
 * ROUTE VIEW MODEL
 *
 * Verwaltet den Zustand der Routenplanung
 * Kommuniziert zwischen UI (RouteScreen) und Repository
 *
 * Schema:
 * RouteScreen → RouteViewModel → RouteRepository → GraphHopper API
 */
class RouteViewModel(
    private val repository: RouteRepository = RouteRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    // ==========================================
    // ZUSTAND (STATE)
    // ==========================================

    var startPoint by mutableStateOf("")

    var destinationPoint by mutableStateOf("")

    var waypoints by mutableStateOf<List<RoutePoint>>(emptyList())

    var selectedTruckProfile by mutableStateOf<TruckProfile?>(null)

    var currentRoute by mutableStateOf<Route?>(null)

    var isCalculating by mutableStateOf(false)

    var errorMessage by mutableStateOf<String?>(null)

    private val currentUserId = auth.currentUser?.uid

    // ==========================================
    // INPUT FUNKTIONEN
    // ==========================================

    fun updateStartPoint(point: String) {
        startPoint = point
        errorMessage = null
    }

    fun updateDestinationPoint(point: String) {
        destinationPoint = point
        errorMessage = null
    }

    fun addWaypoint(waypoint: RoutePoint) {
        waypoints = waypoints + waypoint
    }

    fun removeWaypoint(index: Int) {
        if (index in waypoints.indices) {
            waypoints = waypoints.toMutableList().apply { removeAt(index) }.toList()
        }
    }

    fun clearWaypoints() {
        waypoints = emptyList()
    }

    fun selectTruckProfile(profile: TruckProfile) {
        selectedTruckProfile = profile
        errorMessage = null
    }

    // ==========================================
    // ROUTING FUNKTIONEN
    // ==========================================

    fun calculateRoute() {
        if (startPoint.isBlank()) {
            errorMessage = "Start-Punkt erforderlich"
            return
        }
        if (destinationPoint.isBlank()) {
            errorMessage = "Ziel-Punkt erforderlich"
            return
        }
        if (selectedTruckProfile == null) {
            errorMessage = "Truck-Profil erforderlich"
            return
        }
        if (!selectedTruckProfile!!.isValid()) {
            errorMessage = "Truck-Profil unvollständig (Länge, Höhe, Gewicht müssen gesetzt sein)"
            return
        }

        isCalculating = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val startGeo = parseAddressToGeoPoint(startPoint)
                val destGeo = parseAddressToGeoPoint(destinationPoint)

                val request = RouteRequest(
                    startPoint = startGeo,
                    endPoint = destGeo,
                    waypoints = waypoints.map { it.location },
                    vehicle = "truck",
                    profile = "truck",
                    vehicleLength = selectedTruckProfile!!.truck.length,
                    vehicleHeight = selectedTruckProfile!!.truck.height,
                    vehicleWidth = selectedTruckProfile!!.truck.width,
                    vehicleWeight = (selectedTruckProfile!!.truck.weight + selectedTruckProfile!!.trailer.weight),
                    hazmat = selectedTruckProfile!!.hazmat,
                    avoidFerries = true,
                    avoidTollRoads = false,
                    locale = "de"
                )

                val response = repository.calculateRoute(request)

                if (response != null) {
                    val route = Route(
                        id = "route_${System.currentTimeMillis()}",
                        userId = currentUserId ?: "",
                        name = "$startPoint → $destinationPoint",
                        startPoint = RoutePoint(
                            name = startPoint,
                            location = startGeo,
                            address = startPoint
                        ),
                        endPoint = RoutePoint(
                            name = destinationPoint,
                            location = destGeo,
                            address = destinationPoint
                        ),
                        waypoints = waypoints,
                        truckProfile = selectedTruckProfile,
                        routeDetails = RouteDetails(
                            distance = response.paths?.firstOrNull()?.distance ?: 0.0,
                            duration = response.paths?.firstOrNull()?.time ?: 0L,
                            points = response.paths?.firstOrNull()?.points ?: "",
                            instructions = response.paths?.firstOrNull()?.instructions?.map { RouteInstruction(it.text, it.street_name, it.time, it.distance, it.sign) } ?: emptyList()
                        ),
                        estimatedFuelCost = calculateFuelCost(response.paths?.firstOrNull()?.distance ?: 0.0),
                        estimatedTollCost = 0.0  // TODO: Maut-Berechnung integrieren
                    )

                    currentRoute = route
                } else {
                    errorMessage = "Route konnte nicht berechnet werden"
                }
            } catch (e: Exception) {
                errorMessage = "Fehler: ${e.message}"
                e.printStackTrace()
            } finally {
                isCalculating = false
            }
        }
    }

    fun saveRoute() {
        if (currentRoute == null) {
            errorMessage = "Keine Route zum Speichern"
            return
        }

        val routeToSave = currentRoute!!.copy(isSaved = true)

        viewModelScope.launch {
            try {
                val success = repository.saveRoute(routeToSave)
                if (success) {
                    currentRoute = routeToSave
                    errorMessage = null
                } else {
                    errorMessage = "Route konnte nicht gespeichert werden"
                }
            } catch (e: Exception) {
                errorMessage = "Fehler beim Speichern: ${e.message}"
            }
        }
    }

    fun clearRoute() {
        currentRoute = null
        waypoints = emptyList()
        errorMessage = null
    }

    // ==========================================
    // HILFSFUNKTIONEN
    // ==========================================

    private fun parseAddressToGeoPoint(address: String): GeoPoint {
        return when {
            address.contains("München", ignoreCase = true) -> GeoPoint(48.1351, 11.5820)
            address.contains("Hamburg", ignoreCase = true) -> GeoPoint(53.5511, 9.9937)
            address.contains("Berlin", ignoreCase = true) -> GeoPoint(52.5200, 13.4050)
            address.contains("Köln", ignoreCase = true) -> GeoPoint(50.9375, 6.9603)
            address.contains("Frankfurt", ignoreCase = true) -> GeoPoint(50.1109, 8.6821)
            else -> GeoPoint(48.1351, 11.5820)
        }
    }

    private fun calculateFuelCost(distanceMeters: Double): Double {
        val distanceKm = distanceMeters / 1000
        val literPerKm = 0.3  // 30 Liter pro 100km für LKW
        val literNeeded = distanceKm * literPerKm
        val fuelPrice = 1.65  // EUR pro Liter (Durchschnitt Diesel)
        return literNeeded * fuelPrice
    }
}
