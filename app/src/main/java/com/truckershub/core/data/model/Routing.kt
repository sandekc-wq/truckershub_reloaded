package com.truckershub.core.data.model

import com.google.firebase.firestore.GeoPoint

/**
 * ROUTE DATENMODELL 🗺️
 * Die Haupt-Klasse für eine gespeicherte oder aktive Route.
 */
data class Route(
    val id: String = "",
    val userId: String = "",
    val name: String = "",

    // Start & End
    val startPoint: RoutePoint = RoutePoint(),
    val endPoint: RoutePoint = RoutePoint(),

    // Truck configuration
    val truckProfile: TruckProfile? = null,

    // Route details
    val routeDetails: RouteDetails = RouteDetails(),

    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val estimatedFuelCost: Double = 0.0,
    val estimatedTollCost: Double = 0.0
)

/**
 * ROUTENPUNKT (Start/Ziel)
 */
data class RoutePoint(
    val name: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val address: String = ""
)

/**
 * ROUTEN-DETAILS
 */
data class RouteDetails(
    val distance: Double = 0.0,           // Meter
    val duration: Long = 0L,              // Sekunden
    val points: String = "",              // Encoded Polyline String

    // WICHTIG: Hier nutzen wir jetzt die Klasse aus der neuen Datei!
    val instructions: List<RouteInstruction> = emptyList()
)

/**
 * ROUTING-ANFRAGE (für OpenRouteService)
 */
data class RouteRequest(
    val startPoint: GeoPoint,
    val endPoint: GeoPoint,
    val waypoints: List<GeoPoint> = emptyList(),

    // Truck Maße
    val vehicleLength: Double = 16.5,
    val vehicleHeight: Double = 4.0,
    val vehicleWidth: Double = 2.55,
    val vehicleWeight: Double = 40.0,
    val hazmat: Boolean = false,

    // API Parameter
    val vehicle: String = "truck",
    val profile: String = "driving-hgv"
)

/**
 * API ANTWORTEN (Interne Hilfsklassen für den JSON-Parser)
 */
data class RouteResponse(
    val paths: List<RoutePath>? = null
)

data class RoutePath(
    val distance: Double = 0.0,
    val time: Long = 0L,
    val points: String = "",
    val instructions: List<RouteInstruction>? = null // Nutzt auch die neue Klasse!
)