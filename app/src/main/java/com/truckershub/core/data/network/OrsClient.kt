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
 * Angepasst für das neue RouteInstruction Format.
 */
class OrsClient {

    companion object {
        // Dein Key (lass ich so stehen!)
        private const val API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjQxZGJiYjhiMjRhNjQ3YjBhNjFkNDE1MDZmMzUwZjgxIiwiaCI6Im11cm11cjY0In0="

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

            // Die Geometrie (die blaue Linie)
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

                        // HIER WAR DER FEHLER: Wir passen das jetzt an die neue Klasse an!

                        // Index finden (das ist wichtig für den Grenz-Alarm)
                        val wayPointsJson = step.optJSONArray("way_points")
                        val wayPointsList = mutableListOf<Int>()
                        var firstIndex = 0

                        if (wayPointsJson != null && wayPointsJson.length() > 0) {
                            // Der erste Punkt sagt uns, WO auf der Linie das passiert
                            firstIndex = wayPointsJson.optInt(0)
                            for (k in 0 until wayPointsJson.length()) {
                                wayPointsList.add(wayPointsJson.optInt(k))
                            }
                        }

                        instructionsList.add(
                            RouteInstruction(
                                text = step.optString("instruction", ""),
                                distance = step.optDouble("distance", 0.0),
                                duration = step.optDouble("duration", 0.0), // Jetzt Double!
                                type = step.optInt("type", 0),              // Direkt den ORS Typ nutzen
                                index = firstIndex,                         // <--- DAS BRAUCHEN WIR!
                                wayPoints = wayPointsList
                            )
                        )
                    }
                }
            }

            val path = RoutePath(
                distance = distance,
                time = duration * 1000,
                points = geometryString,
                // encoded_polyline gibt's in deiner RoutePath Klasse evtl nicht,
                // aber points reicht uns.
                instructions = instructionsList
            )

            RouteResponse(paths = listOf(path))

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}