package com.truckershub.core.data.network

import com.google.firebase.firestore.GeoPoint
import com.truckershub.core.data.model.Route
import com.truckershub.core.data.model.RoutePath
import com.truckershub.core.data.model.RouteRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * GRAPHHOPPER API CLIENT
 *
 * Client für Truck-Routing mit GraphHopper API
 * Kostenloser Plan: 2000 Anfragen/Tag (Standard)
 *
 * API Key: Bekommen von https://www.graphhopper.com/
 */
class GraphHopperClient {

    companion object {
        const val BASE_URL = "https://graphhopper.com/api/1/route"
        // TODO: API Key in secure Speicher (z.B. local.properties oder Firebase Remote Config)
        private const val DEFAULT_API_KEY = "YOUR_GRAPHHOPPER_API_KEY_HERE"

        /**
         * HTTP Client mit Logging
         */
        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private val apiKey = DEFAULT_API_KEY

    /**
     * Bereitet die URL für Routenanfrage vor
     */
    private fun buildRouteUrl(request: RouteRequest): String {
        val sb = StringBuilder("$BASE_URL?")

        // Start & End Punkte
        sb.append("point=${request.startPoint.latitude},${request.startPoint.longitude}&")
        sb.append("point=${request.endPoint.latitude},${request.endPoint.longitude}&")

        // Waypoints (optional)
        request.waypoints.forEach { waypoint ->
            sb.append("point=${waypoint.latitude},${waypoint.longitude}&")
        }

        // Fahrzeug-Typ
        sb.append("vehicle=${request.vehicle}&")

        // Profile
        sb.append("profile=${request.profile}&")

        // Sprache
        sb.append("locale=${request.locale}&")

        // Optionen
        if (!request.calcPoints) sb.append("calc_points=false&")
        if (!request.instructions) sb.append("instructions=false&")
        if (!request.elevation) sb.append("elevation=false&")
        if (request.avoidFerries) sb.append("avoid_ferries=true&")
        if (request.avoidForLoad) sb.append("avoid_for_load=true&")
        if (request.avoidTollRoads) sb.append("avoid_toll_roads=true&")

        // API Key
        sb.append("key=$apiKey")

        return sb.toString()
    }

    /**
     * Sendet Routenanfrage an GraphHopper
     *
     * @param request Die Routenanfrage mit allen Parametern
     * @return Route-Response oder null bei Fehler
     */
    suspend fun calculateRoute(request: RouteRequest): RouteResponse? {
        return try {
            val url = buildRouteUrl(request)
            val httpRequest = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(httpRequest).execute()

            if (!response.isSuccessful) {
                return null
            }

            val responseBody = response.body?.string() ?: return null
            parseRouteResponse(responseBody)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parst GraphHopper JSON Antwort
     */
    private fun parseRouteResponse(json: String): RouteResponse? {
        return try {
            val jsonObject = JSONObject(json)

            val message = jsonObject.optString("message", "")
            val pathsJson = jsonObject.optJSONArray("paths")

            val paths = if (pathsJson != null && pathsJson.length() > 0) {
                val pathList = mutableListOf<RoutePath>()

                for (i in 0 until pathsJson.length()) {
                    val pathObj = pathsJson.getJSONObject(i)

                    val path = RoutePath(
                        distance = pathObj.optDouble("distance", 0.0),
                        time = pathObj.optLong("time", 0L),
                        points = pathObj.optString("points", ""),
                        points_encoded = pathObj.optBoolean("points_encoded", true),
                        ascend = pathObj.optDouble("ascend", 0.0),
                        descend = pathObj.optDouble("descend", 0.0),
                        bbox = parseBoundingBox(pathObj.optJSONArray("bbox"))
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

    /**
     * Parst Bounding Box Array
     */
    private fun parseBoundingBox(bbox: org.json.JSONArray?): List<Double>? {
        if (bbox == null || bbox.length() != 4) return null

        return try {
            listOf(
                bbox.getDouble(0),
                bbox.getDouble(1),
                bbox.getDouble(2),
                bbox.getDouble(3)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * EINFACHE ROUTENANFRAGE (Ohne komplexe Parameter)
     *
     * Für schnelle Tests oder einfache Routen
     */
    suspend fun calculateSimpleRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): RouteResponse? {
        // Standard Truck Profile (EU Standard)
        val simpleRequest = RouteRequest(
            startPoint = GeoPoint(startLat, startLng),
            endPoint = GeoPoint(endLat, endLng),
            vehicleLength = 16.5,
            vehicleHeight = 4.0,
            vehicleWidth = 2.55,
            vehicleWeight = 40.0,
            hazmat = false
        )

        return calculateRoute(simpleRequest)
    }

    /**
     * Update API Key
     */
    fun setApiKey(key: String) {
        // TODO: Secure speichern statt Feld-Update
    }

    /**
     * Setzt API Key für Tests (NICHT für Production!)
     */
    public fun setTestApiKey(key: String) {
        // Für Testing
    }
}

/**
 * ROUTE RESPONSE
 *
 * Antwort von GraphHopper API
 */
data class RouteResponse(
    val paths: List<RoutePath>?,
    val message: String?,
    val hints: Map<String, Any>? = null
) {
    /**
     * Holt die beste Route (erster Pfad)
     */
    fun getBestPath(): RoutePath? {
        return paths?.firstOrNull()
    }

    /**
     * Gesamtstrecke aller Wege
     */
    fun getTotalDistance(): Double {
        return paths?.sumOf { it.distance } ?: 0.0
    }

    /**
     * Gesamtdauer aller Wege
     */
    fun getTotalDuration(): Long {
        return paths?.sumOf { it.time } ?: 0L
    }
}