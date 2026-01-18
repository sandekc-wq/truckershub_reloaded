package com.truckershub.core.data.network

import com.google.firebase.firestore.GeoPoint
// Wir importieren die existierenden Modelle aus deinem Ordner
import com.truckershub.core.data.model.RoutePath
import com.truckershub.core.data.model.RouteRequest
import com.truckershub.core.data.model.RouteInstruction
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GraphHopperClient {

    companion object {
        const val BASE_URL = "https://graphhopper.com/api/1/route"
        private const val DEFAULT_API_KEY = "c443f66c-b513-4491-9b6f-50a35c263d9e"

        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private val apiKey = DEFAULT_API_KEY

    // === DIÄT-MODUS: Erstmal nur einfache Parameter senden ===
    private fun buildRouteUrl(request: RouteRequest): String {
        val sb = StringBuilder("$BASE_URL?")

        // Start & Ziel
        sb.append("point=${request.startPoint.latitude},${request.startPoint.longitude}&")
        sb.append("point=${request.endPoint.latitude},${request.endPoint.longitude}&")

        // Waypoints
        request.waypoints.forEach { waypoint ->
            sb.append("point=${waypoint.latitude},${waypoint.longitude}&")
        }

        // Profil
        sb.append("profile=car&")
        sb.append("locale=de&")

        // --- MAßE VORÜBERGEHEND DEAKTIVIERT FÜR TEST ---
        /*
        if (request.vehicleLength > 0) sb.append("vehicle_length=${request.vehicleLength}&")
        if (request.vehicleWidth > 0) sb.append("vehicle_width=${request.vehicleWidth}&")
        if (request.vehicleHeight > 0) sb.append("vehicle_height=${request.vehicleHeight}&")
        if (request.vehicleWeight > 0) sb.append("vehicle_weight=${request.vehicleWeight}&")
        if (request.hazmat) sb.append("hazmat=true&")
        */

        // Standard Optionen
        sb.append("calc_points=true&")
        sb.append("instructions=true&")

        sb.append("key=$apiKey")
        return sb.toString()
    }

    suspend fun calculateRoute(request: RouteRequest): RouteResponse? {
        return try {
            val url = buildRouteUrl(request)
            println("GraphHopper URL: $url")

            val httpRequest = Request.Builder().url(url).get().build()
            val response = client.newCall(httpRequest).execute()

            if (!response.isSuccessful) {
                println("GraphHopper Fehler: ${response.code} - ${response.message}")
                return null
            }

            val responseBody = response.body?.string() ?: return null
            parseRouteResponse(responseBody)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseRouteResponse(json: String): RouteResponse? {
        return try {
            val jsonObject = JSONObject(json)
            val message = jsonObject.optString("message", "")
            val pathsJson = jsonObject.optJSONArray("paths")

            val paths = if (pathsJson != null && pathsJson.length() > 0) {
                val pathList = mutableListOf<RoutePath>()

                for (i in 0 until pathsJson.length()) {
                    val pathObj = pathsJson.getJSONObject(i)

                    // Instructions parsen
                    val instructionsList = mutableListOf<RouteInstruction>()
                    val instructionsJson = pathObj.optJSONArray("instructions")
                    if (instructionsJson != null) {
                        for (j in 0 until instructionsJson.length()) {
                            val instrObj = instructionsJson.getJSONObject(j)
                            instructionsList.add(
                                RouteInstruction(
                                    text = instrObj.optString("text", ""),
                                    street_name = instrObj.optString("street_name", ""),
                                    distance = instrObj.optDouble("distance", 0.0),
                                    time = instrObj.optLong("time", 0),
                                    sign = instrObj.optInt("sign", 0)
                                )
                            )
                        }
                    }

                    // WICHTIG: Hier benutzen wir jetzt Named Arguments (Name = Wert)
                    // Damit ist die Reihenfolge egal und die Fehler verschwinden!
                    val path = RoutePath(
                        distance = pathObj.optDouble("distance", 0.0),
                        time = pathObj.optLong("time", 0L),
                        points = pathObj.optString("points", ""),
                        points_encoded = pathObj.optBoolean("points_encoded", true),
                        ascend = pathObj.optDouble("ascend", 0.0),
                        descend = pathObj.optDouble("descend", 0.0),
                        bbox = parseBoundingBox(pathObj.optJSONArray("bbox")),
                        instructions = instructionsList
                    )
                    pathList.add(path)
                }
                pathList
            } else {
                null
            }

            RouteResponse(
                paths = paths,
                message = if (message.isNotBlank()) message else null
            )

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseBoundingBox(bbox: org.json.JSONArray?): List<Double>? {
        if (bbox == null || bbox.length() != 4) return null
        return try {
            listOf(bbox.getDouble(0), bbox.getDouble(1), bbox.getDouble(2), bbox.getDouble(3))
        } catch (e: Exception) { null }
    }

    suspend fun calculateSimpleRoute(startLat: Double, startLng: Double, endLat: Double, endLng: Double): RouteResponse? {
        // Auch hier vereinfacht für den Test
        val simpleRequest = RouteRequest(
            startPoint = GeoPoint(startLat, startLng),
            endPoint = GeoPoint(endLat, endLng),
            vehicleLength = 0.0, vehicleHeight = 0.0, vehicleWidth = 0.0, vehicleWeight = 0.0, hazmat = false
        )
        return calculateRoute(simpleRequest)
    }

    fun setApiKey(key: String) {}
    fun setTestApiKey(key: String) {}
}

// Diese Klasse muss hier definiert sein, damit der Client sie zurückgeben kann
data class RouteResponse(
    val paths: List<RoutePath>?,
    val message: String?
)