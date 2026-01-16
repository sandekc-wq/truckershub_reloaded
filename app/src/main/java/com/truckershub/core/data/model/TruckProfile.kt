package com.truckershub.core.data.model

/**
 * TRUCK DATENMODELL
 *
 * Fahrzeug-Daten für GraphHopper Truck-Routing
 * Wichtig: Alle Abmessungen genau angeben für korrekte Routenberechnung!
 */
data class TruckProfile(
    val id: String = "",
    val userId: String = "",               // Gehört welchem User?
    val name: String = "Mein Truck",       // Profilname (z.B. "Scania 450")
    val isDefault: Boolean = true,         // Ist das das Standard-Profil?

    // Truck (Zugmaschine)
    val truck: TruckSpecs = TruckSpecs(),

    // Trailer (Anhänger)
    val trailer: TrailerSpecs = TrailerSpecs(),

    // Gesamt
    val totalLength: Double = 0.0,         // Gesamtgesamtlänge (metrisch)

    // Gefahrgut
    val hazmat: Boolean = false,           // Gefahrguttransport?
    val hazmatClass: String? = null,       // Gefahrgutklasse (z.B. "ADR 2")

    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Überprüft ob das Profil komplett ist
     */
    fun isValid(): Boolean {
        return truck.length > 0 &&
               truck.width > 0 &&
               truck.height > 0 &&
               truck.weight > 0 &&
               trailer.length > 0 &&
               trailer.width > 0 &&
               trailer.height > 0
    }

    /**
     * Berechnet Gesamtlänge automatisch
     */
    fun calculateTotalLength(): Double {
        return truck.length + trailer.length
    }
}

/**
 * TRUCK SPEZIFIKATIONEN
 *
 * Zugmaschine-Daten
 */
data class TruckSpecs(
    val length: Double = 16.5,             // Länge in Metern (Standard EU: 16.5m)
    val width: Double = 2.55,              // Breite in Metern (Standard: 2.55m)
    val height: Double = 4.0,              // Höhe in Metern (variiert)
    val weight: Double = 18.0,             // Leergewicht in Tonnen
    val axleLoad: Double = 12.0,           // Achslast in Tonnen
    val emissionClass: String = "EURO 6",  // EURO 6 / EURO 5 / etc.
    val truckType: TruckType = TruckType.SATTELZUG  // Fahrzeugtyp
)

/**
 * TRAILER SPEZIFIKATIONEN
 *
 * Anhänger-Daten
 */
data class TrailerSpecs(
    val length: Double = 13.6,             // Anhängerlänge in Metern
    val width: Double = 2.55,              // Anhängerbreite in Metern
    val height: Double = 4.0,              // Anhängerhöhe in Metern
    val weight: Double = 22.0,             // Anhängergewicht in Tonnen
    val axleLoad: Double = 12.0,           // Anhänger Achslast in Tonnen
    val trailerType: TrailerType = TrailerType.KOFFER  // Anhängertyp
)

/**
 * TRUCK TYP
 */
enum class TruckType {
    SATTELZUG,          // Sattelzug
    ZUGMASCHINEN,       // Zugmaschinen (nur Motor)
    LASTWAGEN,          // Lastwagen (ohne Anhänger)
    GLIEDERZUG          // Gliederzug
}

/**
 * TRAILER TYP
 */
enum class TrailerType {
    KOFFER,             // Kofferanhänger
    CONTAINER,          // Container
    KUEHL,              // Kühlauflieger
    TANK,               // Tankauflieger
    PLATTEN,            // Platten-/Flachauflieger
    KIPPER,             // Kipper
    SCHNELLEINLADUNG,   // Schnell-Einlade-Koffer
    SICHERHEITS,        // Sicherheitstransport
    ANDERE              // Andere
}

/**
 * DEFAULT PROFILES
 *
 * Vordefinierte Standard-Profile für gängige Trucks
 */
object DefaultTruckProfiles {
    val STANDARD_EU_TRUCK = TruckProfile(
        name = "Standard EU LKW",
        isDefault = true,
        truck = TruckSpecs(
            length = 16.5,
            width = 2.55,
            height = 4.0,
            weight = 18.0,
            axleLoad = 12.0,
            emissionClass = "EURO 6",
            truckType = TruckType.SATTELZUG
        ),
        trailer = TrailerSpecs(
            length = 13.6,
            width = 2.55,
            height = 4.0,
            weight = 22.0,
            axleLoad = 12.0,
            trailerType = TrailerType.KOFFER
        )
    )

    val LARGE_TRUCK = TruckProfile(
        name = "Großer LKW",
        isDefault = false,
        truck = TruckSpecs(
            length = 18.75,
            width = 2.55,
            height = 4.2,
            weight = 20.0,
            axleLoad = 14.0,
            emissionClass = "EURO 6",
            truckType = TruckType.SATTELZUG
        ),
        trailer = TrailerSpecs(
            length = 15.6,
            width = 2.55,
            height = 4.2,
            weight = 24.0,
            axleLoad = 14.0,
            trailerType = TrailerType.KOFFER
        )
    )
}