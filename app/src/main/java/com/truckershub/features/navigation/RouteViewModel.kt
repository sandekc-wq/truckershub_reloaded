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
import com.truckershub.core.data.model.CountryInfo
import com.truckershub.core.data.model.DefaultTruckProfiles
import com.truckershub.core.data.model.PredefinedCountries // <--- WICHTIG: Importieren
import com.truckershub.core.data.model.Route
import com.truckershub.core.data.model.RouteDetails
import com.truckershub.core.data.model.RouteInstruction
import com.truckershub.core.data.model.RoutePoint
import com.truckershub.core.data.model.RouteRequest
import com.truckershub.core.data.model.TruckProfile
import com.truckershub.core.data.network.OrsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint as OsmGeoPoint // Für den Decoder
import java.util.Locale

class RouteViewModel(application: Application) : AndroidViewModel(application) {

    private val routingClient = OrsClient()
    private val auth = FirebaseAuth.getInstance()
    private val context = application.applicationContext

    // ==========================================
    // ZUSTAND (STATE)
    // ==========================================

    var startPoint by mutableStateOf("")
    var destinationPoint by mutableStateOf("")
    var waypoints by mutableStateOf<List<RoutePoint>>(emptyList())

    var selectedTruckProfile by mutableStateOf<TruckProfile?>(
        DefaultTruckProfiles.STANDARD_EU_TRUCK
    )

    var currentRoute by mutableStateOf<Route?>(null)
    var isCalculating by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    var instructions by mutableStateOf<List<RouteInstruction>>(emptyList())
        private set

    // NEU: Das Gehirn für den BorderAlert 🧠🇪🇺
    // Hier speichern wir, WO die Grenze ist und WELCHES Land kommt
    var borderLocation by mutableStateOf<GeoPoint?>(null)
    var nextBorderCountry by mutableStateOf<CountryInfo?>(null)

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
        // Reset Border-Info bei neuer Berechnung
        borderLocation = null
        nextBorderCountry = null

        viewModelScope.launch {
            try {
                // 1. Geocoding
                val startGeo = getGeoPointFromAddress(startPoint)
                val destGeo = getGeoPointFromAddress(destinationPoint)

                if (startGeo == null || destGeo == null) {
                    errorMessage = "Adresse nicht gefunden"
                    isCalculating = false
                    return@launch
                }

                // 2. Profil prüfen
                val profile = selectedTruckProfile ?: DefaultTruckProfiles.STANDARD_EU_TRUCK
                val totalWeight = profile.truck.weight + profile.trailer.weight

                // 3. Anfrage bauen
                val request = RouteRequest(
                    startPoint = startGeo,
                    endPoint = destGeo,
                    waypoints = emptyList(),
                    vehicleHeight = profile.truck.height,
                    vehicleWeight = totalWeight,
                    vehicleWidth = profile.truck.width,
                    hazmat = profile.hazmat,
                    vehicle = "truck",
                    profile = "driving-hgv"
                )

                // 4. API Aufruf
                val response = withContext(Dispatchers.IO) {
                    routingClient.calculateRoute(request)
                }

                if (response != null && !response.paths.isNullOrEmpty()) {
                    val path = response.paths[0]
                    instructions = path.instructions ?: emptyList()

                    // NEU: Sofort nach Grenzen scannen! 🕵️‍♂️
                    // Wir brauchen die decodierten Punkte, um die Koordinate zu finden
                    val decodedGeoPoints = decodePolyline(path.points)
                    checkForBorderCrossing(instructions, decodedGeoPoints)

                    currentRoute = Route(
                        id = "route_${System.currentTimeMillis()}",
                        userId = auth.currentUser?.uid ?: "guest",
                        name = "$startPoint → $destinationPoint",
                        startPoint = RoutePoint(startPoint, startGeo, startPoint),
                        endPoint = RoutePoint(destinationPoint, destGeo, destinationPoint),
                        routeDetails = RouteDetails(
                            distance = path.distance,
                            duration = path.time,
                            points = path.points,
                            instructions = path.instructions ?: emptyList()
                        ),
                        truckProfile = profile,
                        estimatedFuelCost = (path.distance / 1000) / 100 * 30.0 * 1.65,
                        estimatedTollCost = 0.0
                    )
                } else {
                    errorMessage = "Keine Route gefunden (Prüfe API-Key / Limits)"
                }

            } catch (e: Exception) {
                errorMessage = "Fehler: ${e.message}"
                e.printStackTrace()
            } finally {
                isCalculating = false
            }
        }
    }

    /**
     * DER GRENZ-SPÜRHUND 🐕
     * Sucht in den Navigationsanweisungen nach "Enter Country"
     */
    private fun checkForBorderCrossing(instrList: List<RouteInstruction>, points: List<OsmGeoPoint>) {
        // Mapping: Welches Wort steht für welches Land? (Englisch & Deutsch)
        val countryKeywords = mapOf(
            "Austria" to "AT", "Österreich" to "AT",
            "Germany" to "DE", "Deutschland" to "DE",
            "Switzerland" to "CH", "Schweiz" to "CH",
            "Poland" to "PL", "Polen" to "PL",
            "Czech" to "CZ", "Tschechien" to "CZ",
            "France" to "FR", "Frankreich" to "FR",
            "Belgium" to "BE", "Belgien" to "BE",
            "Netherlands" to "NL", "Niederlande" to "NL"
        )

        for (instr in instrList) {
            val text = instr.text ?: continue

            // Wir suchen im Text nach einem der Ländernamen
            for ((keyword, code) in countryKeywords) {
                if (text.contains(keyword, ignoreCase = true) && text.contains("Enter", ignoreCase = true) ||
                    text.contains("Grenze", ignoreCase = true) && text.contains(keyword, ignoreCase = true)) {

                    // TREFFER! Wir haben eine Grenze gefunden.
                    val country = PredefinedCountries.getByCode(code)

                    // Jetzt brauchen wir die Koordinate.
                    // 'instr.index' sagt uns, der wievielte Punkt auf der Linie das ist.
                    // (Wir prüfen sicherheitshalber die Bounds)
                    val idx = instr.index
                    if (country != null && idx >= 0 && idx < points.size) {
                        val point = points[idx]

                        // Wir speichern das Ergebnis im State
                        nextBorderCountry = country
                        borderLocation = GeoPoint(point.latitude, point.longitude)

                        println("Border gefunden! 🚨 ${country.name} bei Index $idx")
                        return // Wir nehmen nur die ERSTE Grenze auf der Route
                    }
                }
            }
        }
    }

    // Hilfsfunktion: Adresse -> Koordinaten
    private suspend fun getGeoPointFromAddress(address: String): GeoPoint? {
        return withContext(Dispatchers.IO) {
            try {
                if (address.contains(",")) {
                    val parts = address.split(",")
                    if (parts.size == 2) {
                        val latStr = parts[0].trim()
                        val lonStr = parts[1].trim()
                        if (latStr.matches(Regex("-?\\d+(\\.\\d+)?")) && lonStr.matches(Regex("-?\\d+(\\.\\d+)?"))) {
                            return@withContext GeoPoint(latStr.toDouble(), lonStr.toDouble())
                        }
                    }
                }
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

    // Polyline Decoder (jetzt direkt hier, damit wir die Punkte analysieren können)
    private fun decodePolyline(encoded: String): List<OsmGeoPoint> {
        val poly = ArrayList<OsmGeoPoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = OsmGeoPoint(lat / 1E5, lng / 1E5)
            poly.add(p)
        }
        return poly
    }

    fun clearRoute() {
        currentRoute = null
        startPoint = ""
        destinationPoint = ""
        errorMessage = null
        instructions = emptyList()
        borderLocation = null
        nextBorderCountry = null
    }
    // Einfach unten in RouteViewModel einfügen:
    fun saveRoute() {
        // TODO: Route speichern (Datenbank) kommt später
    }
}