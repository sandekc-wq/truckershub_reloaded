package com.truckershub.core.data.model

import com.google.firebase.firestore.GeoPoint

/**
 * ROUTE DATENMODELL
 *
 * Complete routing information for truck navigation using GraphHopper
 */
data class Route(
    val id: String = "",                  // Unique route ID
    val userId: String = "",              // Which user created this route?
    val name: String = "",                // Route name (e.g., "München - Hamburg")

    // Start & End
    val startPoint: RoutePoint = RoutePoint(),
    val endPoint: RoutePoint = RoutePoint(),

    // Truck configuration
    val truckProfile: TruckProfile? = null,

    // Route details
    val routeDetails: RouteDetails = RouteDetails(),

    // Optional waypoints
    val waypoints: List<RoutePoint> = emptyList(),

    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false,         // Saved to favorites?
    val estimatedFuelCost: Double = 0.0,  // Estimated fuel cost in EUR
    val estimatedTollCost: Double = 0.0   // Estimated tolls in EUR
) {
    /**
     * Checks if route is valid
     */
    fun isValid(): Boolean {
        return startPoint.location.latitude != 0.0 &&
               endPoint.location.latitude != 0.0 &&
               truckProfile != null &&
               truckProfile!!.isValid()
    }

    /**
     * Gets total distance string (formatted)
     */
    fun getDistanceDisplay(): String {
        val km = routeDetails.distance / 1000
        return "${String.format("%.1f", km)} km"
    }

    /**
     * Gets total duration string (formatted)
     */
    fun getDurationDisplay(): String {
        val hours = routeDetails.duration / 3600
        val minutes = (routeDetails.duration % 3600) / 60
        return "${hours}h ${minutes}min"
    }
}

/**
 * ROUTENPUNKT
 *
 * Start-, End- oder Wegpunkt
 */
data class RoutePoint(
    val name: String = "",                // z.B. "Tankstelle XY", "Ziel" oder Koordinaten
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val address: String = "",             // Addresse falls bekannt
    val isTruckStop: Boolean = false,     // Ist es ein Parkplatz?
    val parkingInfo: ParkingInfo? = null  // Parkplatz-Info falls vorhanden
)

/**
 * PARKPLATZ-INFO im ROUTENPUNKT
 */
data class ParkingInfo(
    val spotId: String = "",
    val name: String = "",
    val ampelStatus: AmpelStatus = AmpelStatus.UNKNOWN,
    val distanceFromRoute: Int = 0,       // Abweichung von Route in Metern
    val estimatedArrival: Long = 0L       // Voraussichtliche Ankunft
)

/**
 * ROUTEN-DETAILS
 *
 * Berechnete Details der Route
 */
data class RouteDetails(
    // Distance & Duration
    val distance: Double = 0.0,           // Gesamtlänge in Metern
    val duration: Long = 0L,              // Gesamtdauer in Sekunden
    val walkTime: Long = 0L,              // Gehzeit (falls Abweichungen)

    // Points
    val points: String = "",              // Encoded polyline (GraphHopper format)
    val pathPoints: List<GeoPoint> = emptyList(),  // Decode

    // Instructions (Fahranweisungen)
    val instructions: List<RouteInstruction> = emptyList(),

    // Statistics
    val ascent: Double = 0.0,             // Gesamthöhenunterschied in Metern
    val descent: Double = 0.0,            // Gefälle

    // Segments
    val segments: List<RouteSegment> = emptyList(),  // Teilstrecken

    // Warnings
    val warnings: List<RouteWarning> = emptyList(),  // Warnungen für LKW
    val restrictions: List<String> = emptyList(),    // Beschränkungen

    // Borders
    val borderCrossings: List<BorderCrossing> = emptyList()  // Grenzübertritte
)

/**
 * ROUTENSEGMENT
 *
 * Ein Teilstück der Route
 */
data class RouteSegment(
    val startPoint: GeoPoint = GeoPoint(0.0, 0.0),
    val endPoint: GeoPoint = GeoPoint(0.0, 0.0),
    val distance: Double = 0.0,
    val duration: Long = 0L,
    val roadName: String = "",            // z.B. "A1", "B12"
    val roadType: String = "",            // z.B. "motorway", "motorway_link", "primary"
    val instructions: String = "",        // z.B. "Biegen Sie rechts ab auf A1"
    val streetName: String = ""           // Detailierterer Straßenname
)

/**
 * ROUTENWARNUNG
 *
 * Wichtige Warnungen für LKW-Fahrer
 */
data class RouteWarning(
    val type: WarningType = WarningType.OTHER,
    val title: String = "",
    val message: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),  // Wo auf der Route?
    val distanceFromStart: Double = 0.0,          // Abstnd vom Start in Metern
    val severity: WarningSeverity = WarningSeverity.INFO
)

enum class WarningType {
    WEIGHT_LIMIT,       // Gewichtsbeschränkung
    HEIGHT_LIMIT,       // Höhenbeschränkung
    WIDTH_LIMIT,        // Breitenbeschränkung
    WIDTH_RESTRICTION,  // Breitenrestriktion
    ROAD_CLOSED,        // Straße gesperrt
    RESTRICITED_AREA,   // Verbotene Zone
    BORDER_ALERT,       // Grenzwarnung
    WEATHER_ALERT,      // Wetterwarnung
    TOLL_ROAD,          // Mautstraße
    CONGESTION,         // Stauwarnung
    CONSTRUCTION,       // Baustelle
    OTHER               // Sonstiges
}

enum class WarningSeverity {
    INFO,       // Nur Info
    WARNING,    // Warnung
    DANGER,     // Gefahr
    BLOCKING    // Route blockiert
}

/**
 * GRENZÜBERTRITT
 *
 * Info über einen Grenzübertritt auf der Route
 */
data class BorderCrossing(
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val fromCountry: String = "",        // z.B. "DE"
    val toCountry: String = "",          // z.B. "AT"
    val fromCountryName: String = "",    // "Deutschland"
    val toCountryName: String = "",      // "Österreich"
    val distanceFromStart: Double = 0.0, // Abstnd vom Start in Metern
    val estimatedTime: Long = 0L,        // Voraussichtliche Ankunft
    val crossingName: String = "",       // Name des Grenzübergangs
    val borderInfo: CountryInfo? = null  // Komplette Infos zum Zielland
)

/**
 * ROUTING-ANFRAGE
 *
 * Parameter für GraphHopper API
 */
data class RouteRequest(
    // Start & End
    val startPoint: GeoPoint,
    val endPoint: GeoPoint,

    // Waypoints (optional)
    val waypoints: List<GeoPoint> = emptyList(),

    // Truck dimensions (aus TruckProfile)
    val vehicleLength: Double = 16.5,   // Meter
    val vehicleHeight: Double = 4.0,    // Meter
    val vehicleWidth: Double = 2.55,    // Meter
    val vehicleWeight: Double = 40.0,   // Tonnen (Zugmaschine + Anhänger)

    // Routing settings
    val vehicle: String = "truck",               // "truck"
    val elevation: Boolean = false,              // Mit Höhenprofil?
    val locale: String = "de",                   // Sprache für Anweisungen
    val instructions: Boolean = true,            // Anweisungen zurückgeben?
    val calcPoints: Boolean = true,              // Punkte berechnen?
    val pointHint: Boolean = true,               // Punkt-Hints?

    // Avoidance
    val avoidFerries: Boolean = false,           // Fähren vermeiden
    val avoidForLoad: Boolean = false,           // Routen mit Ladungsverboten vermeiden
    val avoidTollRoads: Boolean = false,         // Mautstraßen vermeiden

    // Hazmat (Gefahrgut)
    val hazmat: Boolean = false,                 // Gefahrgut?
    val hazmatClass: String? = null,             // Gefahrgutklasse

    // Fuel & Costs
    val fuelPrice: Double = 1.89,                // Preis pro Liter in EUR
    val truckConsumption: Double = 30.0,         // l/100km für LKW

    // Chosen profile
    val profile: String = "truck"               // GraphHopper profile
)

/**
 * ROUTING-ERGEBNIS
 *
 * GraphHopper API Antwort
 */
data class RouteResponse(
    val paths: List<RoutePath>? = null,
    val message: String? = null,
    val hints: Map<String, Any>? = null
)

data class RoutePath(
    val distance: Double = 0.0,
    val time: Long = 0L,
    val points: String = "",                    // Encoded polyline
    val encoded_polyline: String? = null,       // Alternative format
    val bbox: List<Double>? = null,             // Bounding box [minLon, minLat, maxLon, maxLat]
    val points_order: List<Int>? = null,
    val ascend: Double = 0.0,
    val descend: Double = 0.0,
    val instructions: List<RouteInstruction>? = null,
    val legs: List<RouteLeg>? = null,
    val details: Map<String, Any>? = null,
    val points_encoded: Boolean = true,
    val voice_instructions: List<VoiceInstruction>? = null
)

data class RouteInstruction(
    val text: String = "",
    val street_name: String = "",
    val time: Long = 0L,
    val distance: Double = 0.0,
    val sign: Int = 0,
    val length: Double = 0.0,
    val interval: List<Int> = emptyList(),
    val exit_number: Int? = null
)

data class RouteLeg(
    val distance: Double = 0.0,
    val time: Long = 0L,
    val from: Int = 0,
    val to: Int = 0
)

data class VoiceInstruction(
    val distanceAlongGeometry: Double = 0.0,
    val text: String = "",
    val SSMLphonemes: String? = null,
    val chunkLength: Int = 0,
    val startTime: Double = 0.0
)

/**
 * RECENT ROUTES
 *
 * Recently used/start routes for quick access
 */
data class RecentRoute(
    val id: String = "",
    val startPointName: String = "",
    val endPointName: String = "",
    val startLocation: GeoPoint = GeoPoint(0.0, 0.0),
    val endLocation: GeoPoint = GeoPoint(0.0, 0.0),
    val distance: Double = 0.0,
    val duration: Long = 0L,
    val lastUsed: Long = System.currentTimeMillis()
)