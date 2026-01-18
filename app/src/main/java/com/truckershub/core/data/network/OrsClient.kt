package com.truckershub.core.data.network

import com.truckershub.core.data.model.RouteInstruction
import com.truckershub.core.data.model.RoutePath
import com.truckershub.core.data.model.RouteRequest
import com.truckershub.core.data.model.RouteResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenRouteService Client (PRO VERSION 🚛)
 * Nutzt den JSON-Endpunkt für fix und fertig codierte Polylines.
 */
class OrsClient {

    companion object {
        // Dein Key (funktioniert ja laut Statistik!)
        private const val API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjQxZGJiYjhiMjRhNjQ3YjBhNjFkNDE1MDZmMzUwZjgxIiwiaCI6Im11cm11cjY0In0="

        // WICHTIG: Hier stand vorher ".../geojson". Jetzt nehmen wir ".../json".
        // Das liefert uns die Geometrie direkt als fertigen String!
        private const val BASE_URL = "https://api.openrouteservice.org/v2/directions/driving-hgv/json"

        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun calculateRoute(request: RouteRequest): RouteResponse? {
        return try {
            val jsonBody = JSONObject()
            val coordsArray = JSONArray()
            // ORS will [Lon, Lat]
            coordsArray.put(JSONArray().put(request.startPoint.longitude).put(request.startPoint.latitude))
            request.waypoints.forEach { wp -> coordsArray.put(JSONArray().put(wp.longitude).put(wp.latitude)) }
            coordsArray.put(JSONArray().put(request.endPoint.longitude).put(request.endPoint.latitude))

            jsonBody.put("coordinates", coordsArray)
            jsonBody.put("instructions", true)
            // Keine "geometry"-Option nötig, standardmäßig gibt er uns den String!

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(httpRequest).execute()

            if (!response.isSuccessful) {
                println("ORS Fehler: ${response.code} - ${response.message}")
                return null
            }

            val responseBody = response.body?.string() ?: return null
            parseOrsResponse(responseBody)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseOrsResponse(json: String): RouteResponse? {
        return try {
            val root = JSONObject(json)
            val routes = root.optJSONArray("routes") ?: return null
            if (routes.length() == 0) return null

            val route = routes.getJSONObject(0)
            val summary = route.optJSONObject("summary")

            // Hier kommt das Goldstück: Die fertige Linie!
            val geometryString = route.optString("geometry", "")

            val distance = summary?.optDouble("distance", 0.0) ?: 0.0
            val duration = summary?.optDouble("duration", 0.0)?.toLong() ?: 0L

            val instructionsList = mutableListOf<RouteInstruction>()
            val segments = route.optJSONArray("segments")
            if (segments != null && segments.length() > 0) {
                val steps = segments.getJSONObject(0).optJSONArray("steps")
                if (steps != null) {
                    for (i in 0 until steps.length()) {
                        val step = steps.getJSONObject(i)
                        instructionsList.add(
                            RouteInstruction(
                                text = step.optString("instruction", ""),
                                distance = step.optDouble("distance", 0.0),
                                time = step.optDouble("duration", 0.0).toLong(),
                                sign = mapOrsSignToGh(step.optInt("type", 0)),
                                street_name = step.optString("name", "")
                            )
                        )
                    }
                }
            }

            val path = RoutePath(
                distance = distance,
                time = duration * 1000,
                points = geometryString, // Einfach durchreichen!
                points_encoded = true,
                bbox = null,
                instructions = instructionsList
            )

            RouteResponse(paths = listOf(path), message = null)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun mapOrsSignToGh(orsType: Int): Int {
        return when (orsType) {
            0, 1 -> 0 // Links
            2, 3 -> 1 // Rechts
            4, 5 -> -1 // Links scharf
            else -> 0
        }
    }
}