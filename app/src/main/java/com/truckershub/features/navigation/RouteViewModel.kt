package com.truckershub.features.navigation

import android.app.Application
import android.content.Context
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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import java.util.Locale

class RouteViewModel(application: Application) : AndroidViewModel(application) {

    private val routingClient = OrsClient()
    private val auth = FirebaseAuth.getInstance()
    private val context = application.applicationContext

    // EINGABE FELDER
    var startText by mutableStateOf("Aktueller Standort")
    var destinationText by mutableStateOf("")

    // ROUTE & STATUS
    var currentRoute by mutableStateOf<Route?>(null)
    var isCalculating by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    var selectedTruckProfile by mutableStateOf<TruckProfile?>(DefaultTruckProfiles.STANDARD_EU_TRUCK)

    // BORDER ALERT
    var borderLocation by mutableStateOf<GeoPoint?>(null)
    var nextBorderCountry by mutableStateOf<CountryInfo?>(null)

    fun updateStartPoint(text: String) { startText = text }
    fun updateDestinationPoint(text: String) { destinationText = text }

    // RESET
    fun resetRoute() {
        currentRoute = null
        isCalculating = false
        errorMessage = null
        destinationText = ""
        borderLocation = null
        nextBorderCountry = null
    }

    // BERECHNEN
    fun calculateRoute(context: Context, currentLocation: GeoPoint?, onSuccess: () -> Unit) {
        if (destinationText.isBlank()) {
            errorMessage = "Bitte Ziel eingeben"
            return
        }

        isCalculating = true
        errorMessage = null
        currentRoute = null
        borderLocation = null
        nextBorderCountry = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. GEOCODING (Deutsch bevorzugt)
                val startGeo = if (startText == "Aktueller Standort" || startText.isBlank()) {
                    currentLocation
                } else {
                    getAddressLocation(startText)
                }
                val destGeo = getAddressLocation(destinationText)

                if (startGeo == null || destGeo == null) {
                    errorMessage = "Adresse nicht gefunden. Tipp: 'Ort, Straße' eingeben."
                    isCalculating = false
                    return@launch
                }

                // 2. PROFIL
                val profile = selectedTruckProfile ?: DefaultTruckProfiles.STANDARD_EU_TRUCK
                val totalWeight = profile.truck.weight + profile.trailer.weight

                // 3. API REQUEST (ORS/OSRM)
                val routeUrl = "https://router.project-osrm.org/route/v1/driving/" +
                        "${startGeo.longitude},${startGeo.latitude};" +
                        "${destGeo.longitude},${destGeo.latitude}?overview=full&steps=true"

                val client = OkHttpClient()
                val request = Request.Builder().url(routeUrl).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val json = response.body?.string() ?: ""
                    val jsonObj = JSONObject(json)

                    if (jsonObj.optString("code") == "Ok") {
                        val routes = jsonObj.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val routeJson = routes.getJSONObject(0)

                            // Route Details
                            val geometry = routeJson.getString("geometry")
                            val duration = routeJson.getDouble("duration")
                            val distance = routeJson.getDouble("distance")

                            // ANWEISUNGEN PARSEN & ÜBERSETZEN 🇩🇪
                            val instructionsList = mutableListOf<RouteInstruction>()
                            val legs = routeJson.getJSONArray("legs")
                            if (legs.length() > 0) {
                                val steps = legs.getJSONObject(0).getJSONArray("steps")
                                for (i in 0 until steps.length()) {
                                    val step = steps.getJSONObject(i)

                                    val maneuver = step.getJSONObject("maneuver")
                                    val type = maneuver.optString("type")
                                    val modifier = maneuver.optString("modifier")
                                    val exit = maneuver.optInt("exit", 0)
                                    val streetName = step.optString("name", "")

                                    val germanText = getGermanInstruction(type, modifier, streetName, exit)

                                    // HIER WAR DER FEHLER: 'duration' statt 'time' und Typen angepasst!
                                    instructionsList.add(RouteInstruction(
                                        text = germanText,
                                        distance = step.getDouble("distance"),
                                        duration = step.getDouble("duration"), // duration ist Double
                                        type = 0 // Standard-Typ, falls benötigt
                                    ))
                                }
                            }

                            // Route bauen
                            val newRoute = Route(
                                id = "route_${System.currentTimeMillis()}",
                                userId = auth.currentUser?.uid ?: "guest",
                                name = "$startText → $destinationText",
                                startPoint = RoutePoint(startText, startGeo, startText),
                                endPoint = RoutePoint(destinationText, destGeo, destinationText),
                                routeDetails = RouteDetails(
                                    distance = distance,
                                    duration = duration.toLong(), // HIER WAR DER FEHLER: Double zu Long konvertieren!
                                    points = geometry,
                                    instructions = instructionsList
                                ),
                                truckProfile = profile,
                                estimatedFuelCost = 0.0,
                                estimatedTollCost = 0.0
                            )

                            withContext(Dispatchers.Main) {
                                currentRoute = newRoute
                                isCalculating = false
                                onSuccess()
                            }
                        }
                    } else {
                        throw Exception("Keine Route gefunden.")
                    }
                } else {
                    throw Exception("Server-Fehler: ${response.code}")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMessage = "Fehler: ${e.message}"
                    isCalculating = false
                }
            }
        }
    }

    /**
     * DER DOLMETSCHER 🇩🇪
     */
    private fun getGermanInstruction(type: String, modifier: String, streetName: String, exit: Int): String {
        val street = if (streetName.isNotBlank()) "auf $streetName" else ""

        return when (type) {
            "turn" -> when (modifier) {
                "left" -> "Links abbiegen $street"
                "right" -> "Rechts abbiegen $street"
                "slight left" -> "Halb links halten $street"
                "slight right" -> "Halb rechts halten $street"
                "sharp left" -> "Scharf links abbiegen"
                "sharp right" -> "Scharf rechts abbiegen"
                "uturn" -> "Bitte wenden"
                else -> "Abbiegen $street"
            }
            "new name" -> "Weiterfahren $street"
            "depart" -> "Starten Sie Richtung $street"
            "arrive" -> "Sie haben Ihr Ziel erreicht 🏁"
            "merge" -> "Auffahren $street"
            "on ramp" -> "Auf die Auffahrt $street"
            "off ramp" -> "Ausfahrt nehmen $street"
            "fork" -> when (modifier) {
                "left" -> "Links halten $street"
                "right" -> "Rechts halten $street"
                else -> "Gabelung: $street"
            }
            "end of road" -> when (modifier) {
                "left" -> "Am Ende links"
                "right" -> "Am Ende rechts"
                else -> "Ende der Straße"
            }
            "roundabout" -> "Kreisverkehr: $exit. Ausfahrt nehmen"
            "rotary" -> "Kreisverkehr: $exit. Ausfahrt nehmen"
            "roundabout turn" -> "Im Kreisverkehr wenden"
            "notification" -> "Hinweis: $street"
            else -> if (streetName.isNotBlank()) "Weiter auf $streetName" else "Dem Straßenverlauf folgen"
        }
    }

    private fun getAddressLocation(address: String): GeoPoint? {
        return try {
            val geocoder = Geocoder(context, Locale.GERMANY)
            val res = geocoder.getFromLocationName(address, 1)
            if (!res.isNullOrEmpty()) GeoPoint(res[0].latitude, res[0].longitude) else null
        } catch (e: Exception) { null }
    }
}