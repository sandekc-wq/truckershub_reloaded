package com.truckershub.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.truckershub.core.data.model.Route
import com.truckershub.core.data.model.RouteRequest
import com.truckershub.core.data.model.RouteDetails
import com.truckershub.core.data.model.RoutePoint
import com.truckershub.core.data.model.TruckProfile
import com.truckershub.core.data.network.GraphHopperClient
import com.truckershub.core.data.network.RouteResponse
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * ROUTE REPOSITORY
 *
 * Verwaltet Routing-Operationen
 * - Routenberechnung mit GraphHopper
 * - Routen speichern in Firebase
 * - Gespeicherte Routen laden
 */
class RouteRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val graphHopperClient: GraphHopperClient = GraphHopperClient()
) {

    private val _isCalculating = MutableStateFlow(false)
    val isCalculating: Flow<Boolean> = _isCalculating.asStateFlow()

    private val _lastRoute = MutableStateFlow<Route?>(null)
    val lastRoute: Flow<Route?> = _lastRoute.asStateFlow()

    /**
     * Berechnet eine neue Route
     *
     * @param request Die Routenanfrage
     * @return RouteResponse oder null bei Fehler
     */
    suspend fun calculateRoute(request: RouteRequest): RouteResponse? {
        _isCalculating.value = true

        return try {
            val response = graphHopperClient.calculateRoute(request)
            response
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            _isCalculating.value = false
        }
    }

    /**
     * Speichert eine Route in Firebase
     *
     * @param route Die Route die gespeichert werden soll
     * @return true bei Erfolg
     */
    suspend fun saveRoute(route: Route): Boolean {
        return try {
            firestore.collection("routes").document(route.id).set(route)
                .await()
            _lastRoute.value = route
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Läd eine gespeicherte Route anhand der ID
     *
     * @param routeId Die Route ID
     * @return Route oder null bei Fehler
     */
    suspend fun getRouteById(routeId: String): Route? {
        return try {
            val doc = firestore.collection("routes").document(routeId).get().await()
            if (doc.exists()) {
                doc.toObject(Route::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Läd alle gespeicherten Routen eines Users
     *
     * @param userId Die User ID
     * @return Flow mit Routen
     */
    fun getSavedRoutes(userId: String): Flow<List<Route>> {
        return callbackFlow {
            val listener: ListenerRegistration = firestore.collection("routes")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isSaved", true)
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        close(exception)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val routes = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Route::class.java)
                        }
                        trySend(routes)
                    }
                }

            awaitClose { listener.remove() }
        }
    }

    /**
     * Läd zuletzt verwendete Routen
     *
     * @param userId Die User ID
     * @param limit Maximale Anzahl
     * @return Flow mit Routen
     */
    fun getRecentRoutes(userId: String, limit: Int = 10): Flow<List<Route>> {
        return callbackFlow {
            val listener: ListenerRegistration = firestore.collection("recent_routes")
                .whereEqualTo("userId", userId)
                .orderBy("lastUsed", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        close(exception)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val routes = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Route::class.java)
                        }
                        trySend(routes)
                    }
                }

            awaitClose { listener.remove() }
        }
    }

    /**
     * Löscht eine gespeicherte Route
     *
     * @param routeId Die Route ID
     * @return true bei Erfolg
     */
    suspend fun deleteRoute(routeId: String): Boolean {
        return try {
            firestore.collection("routes").document(routeId).delete().await()
            if (_lastRoute.value?.id == routeId) {
                _lastRoute.value = null
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Aktualisiert das letzte verwendete Datum einer Route
     *
     * @param routeId Die Route ID
     * @return true bei Erfolg
     */
    suspend fun updateLastUsed(routeId: String): Boolean {
        return try {
            firestore.collection("recent_routes").document(routeId)
                .update("lastUsed", System.currentTimeMillis())
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Erstellt eine Route aus GraphHopper Response
     *
     * @param response Die GraphHopper Antwort
     * @param request Die ursprüngliche Anfrage
     * @param userId Die User ID
     * @return Route oder null
     */
    fun createRouteFromResponse(
        response: RouteResponse,
        request: RouteRequest,
        userId: String
    ): Route? {
        val path = response.getBestPath() ?: return null

        return Route(
            id = java.util.UUID.randomUUID().toString(),
            userId = userId,
            name = generateRouteName(request),
            startPoint = com.truckershub.core.data.model.RoutePoint(
                name = "Start",
                location = request.startPoint
            ),
            endPoint = com.truckershub.core.data.model.RoutePoint(
                name = "Ziel",
                location = request.endPoint
            ),
            truckProfile = null,  // TODO: TruckProfile aus Request erstellen
            routeDetails = com.truckershub.core.data.model.RouteDetails(
                distance = path.distance,
                duration = path.time,
                points = path.points
            ),
            waypoints = request.waypoints.map { point ->
                com.truckershub.core.data.model.RoutePoint(location = point)
            },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Generiert einen Route-Namen
     */
    private fun generateRouteName(request: RouteRequest): String {
        return "Route ${System.currentTimeMillis()}"
    }

    /**
     * Speichert eine Route in den "zuletzt verwendet" Bereich
     */
    suspend fun addToRecentRoutes(route: Route): Boolean {
        return try {
            // Erstelle ein Map um lastUsed hinzuzufügen
            val routeData = mapOf(
                "id" to route.id,
                "userId" to route.userId,
                "name" to route.name,
                "startPoint" to route.startPoint,
                "endPoint" to route.endPoint,
                "truckProfile" to route.truckProfile,
                "routeDetails" to route.routeDetails,
                "waypoints" to route.waypoints,
                "createdAt" to route.createdAt,
                "updatedAt" to route.updatedAt,
                "isSaved" to route.isSaved,
                "estimatedFuelCost" to route.estimatedFuelCost,
                "estimatedTollCost" to route.estimatedTollCost,
                "lastUsed" to System.currentTimeMillis()
            )

            firestore.collection("recent_routes")
                .document(route.id)
                .set(routeData)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}