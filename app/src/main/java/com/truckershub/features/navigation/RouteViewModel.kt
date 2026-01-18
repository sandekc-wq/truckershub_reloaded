package com.truckershub.features.navigation

import android.app.Application
import android.location.Geocoder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.truckershub.core.data.model.DefaultTruckProfiles
import com.truckershub.core.data.model.Route
import com.truckershub.core.data.model.RouteDetails
import com.truckershub.core.data.model.RoutePoint
import com.truckershub.core.data.model.RouteRequest
import com.truckershub.core.data.model.TruckProfile
// WICHTIG: Wir nutzen jetzt den neuen LKW-Client
import com.truckershub.core.data.network.OrsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class RouteViewModel(application: Application) : AndroidViewModel(application) {

    // Hier läuft jetzt der neue OpenRouteService Client (LKW)
    private val routingClient = OrsClient()

    private val auth = FirebaseAuth.getInstance()
    private val context = application.applicationContext

    // ==========================================
    // ZUSTAND (STATE)
    // ==========================================

    var startPoint by mutableStateOf("")
    var destinationPoint by mutableStateOf("")
    var waypoints by mutableStateOf<List<RoutePoint>>(emptyList())

    // Standard-LKW als Startwert
    var selectedTruckProfile by mutableStateOf<TruckProfile?>(
        DefaultTruckProfiles.STANDARD_EU_TRUCK
    )

    var currentRoute by mutableStateOf<Route?>(null)
    var isCalculating by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // UI-Zustand für die Navigationsanweisungen
    var instructions by mutableStateOf<List<com.truckershub.core.data.model.RouteInstruction>>(emptyList())
        private set

    // ==========================================
    // FUNKTIONEN
    // ==========================================

    fun updateStartPoint(point: String) { startPoint = point }
    fun updateDestinationPoint(point: String) { destinationPoint = point }

    fun calculateRoute() {
        if (startPoint.isBlank() || destinationPoint.isBlank()) {
            errorMessage = "Start und Ziel eingeben"
            return
        }

        isCalculating = true
        errorMessage = null
        currentRoute = null

        viewModelScope.launch {
            try {
                // 1. Geocoding (Adresse zu Koordinaten)
                val startGeo = getGeoPointFromAddress(startPoint)
                val destGeo = getGeoPointFromAddress(destinationPoint)

                if (startGeo == null || destGeo == null) {
                    errorMessage = "Adresse nicht gefunden"
                    isCalculating = false
                    return@launch
                }

                // 2. Profil prüfen
                val profile = selectedTruckProfile ?: DefaultTruckProfiles.STANDARD_EU_TRUCK

                // Gesamtgewicht berechnen
                val totalWeight = profile.truck.weight + profile.trailer.weight

                // 3. Anfrage bauen
                // Der OrsClient braucht GeoPoints und Maße
                val request = RouteRequest(
                    startPoint = startGeo,
                    endPoint = destGeo,
                    waypoints = emptyList(), // Waypoints später einbauen

                    // WICHTIG: Diese Daten gehen an OpenRouteService für das LKW-Routing
                    vehicleHeight = profile.truck.height,
                    vehicleWeight = totalWeight,
                    vehicleWidth = profile.truck.width,
                    hazmat = profile.hazmat,

                    // Dummy-Werte für Kompatibilität (falls deine Klasse die noch verlangt)
                    vehicle = "truck",
                    profile = "driving-hgv"
                )

                println("Sende Anfrage an ORS: Start=${startGeo.latitude},${startGeo.longitude} Ziel=${destGeo.latitude},${destGeo.longitude}")

                // 4. API Aufruf (Motor starten!)
                val response = withContext(Dispatchers.IO) {
                    routingClient.calculateRoute(request)
                }

                if (response != null && !response.paths.isNullOrEmpty()) {
                    val path = response.paths[0]

                    // Anweisungen für die UI speichern
                    instructions = path.instructions ?: emptyList()

                    // Route Objekt für die Anzeige erstellen
                    currentRoute = Route(
                        id = "route_${System.currentTimeMillis()}",
                        userId = auth.currentUser?.uid ?: "guest",
                        name = "$startPoint → $destinationPoint",
                        startPoint = RoutePoint(startPoint, startGeo, startPoint),
                        endPoint = RoutePoint(destinationPoint, destGeo, destinationPoint),
                        routeDetails = RouteDetails(
                            distance = path.distance,
                            duration = path.time,
                            points = path.points, // Das ist die codierte blaue Linie
                            instructions = path.instructions ?: emptyList()
                        ),
                        truckProfile = profile,
                        // Grobe Schätzung: 30L/100km * 1.65€
                        estimatedFuelCost = (path.distance / 1000) / 100 * 30.0 * 1.65,
                        estimatedTollCost = 0.0 // Maut kommt später
                    )
                } else {
                    errorMessage = "Keine Route gefunden (Key prüfen oder Strecke zu lang für Free-Tier?)"
                }

            } catch (e: Exception) {
                errorMessage = "Fehler: ${e.message}"
                e.printStackTrace()
            } finally {
                isCalculating = false
            }
        }
    }

    // Später implementieren
    fun saveRoute() {
        // TODO: Route in Firestore speichern
    }

    // Hilfsfunktion: Adresse -> Koordinaten
    private suspend fun getGeoPointFromAddress(address: String): GeoPoint? {
        return withContext(Dispatchers.IO) {
            try {
                // Check auf Koordinaten-Eingabe "53.5, 8.5"
                if (address.contains(",")) {
                    val parts = address.split(",")
                    if (parts.size == 2) {
                        val latStr = parts[0].trim()
                        val lonStr = parts[1].trim()
                        // Prüfen ob es Zahlen sind
                        if (latStr.matches(Regex("-?\\d+(\\.\\d+)?")) && lonStr.matches(Regex("-?\\d+(\\.\\d+)?"))) {
                            return@withContext GeoPoint(latStr.toDouble(), lonStr.toDouble())
                        }
                    }
                }

                // Echte Adress-Suche
                val geocoder = Geocoder(context, Locale.GERMANY)
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(address, 1)
                if (!results.isNullOrEmpty()) {
                    GeoPoint(results[0].latitude, results[0].longitude)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun clearRoute() {
        currentRoute = null
        startPoint = ""
        destinationPoint = ""
        errorMessage = null
        instructions = emptyList()
    }
}