package com.truckershub.core.data.model

import androidx.annotation.DrawableRes

/**
 * LAND INFOS 🌍🚛
 * Wichtige Informationen für LKW-Fahrer bei Grenzüberquerung.
 * Stand: 2025
 */
data class CountryInfo(
    val id: String = "",                 // Ländercode (z.B. "DE", "AT", "CH")
    val name: String = "",               // Name ("Deutschland", "Österreich")
    val flag: String? = null,            // Flaggen-Emoji 🇩🇪
    val callingCode: String = "",        // Vorwahl (+49)

    // TEMPOLIMITS (Neu hinzugefügt für das UI!) 🛑
    val speedLimitHighway: Int = 80,     // Standard LKW > 3.5t/7.5t
    val speedLimitCountry: Int = 60,     // Landstraße

    // Vignetten- und Maut-Systeme
    val tollSystem: TollSystem = TollSystem.NONE,
    val vignetteRequired: Boolean = false,
    val vignetteTypes: List<VignetteType> = emptyList(),
    val electronicTollRequired: Boolean = false,
    val tollSystemName: String? = null,  // z.B. "Go-Box", "Toll Collect"

    // Wichtige Regeln
    val requirements: List<Requirement> = emptyList(),
    val restrictions: List<Restriction> = emptyList(),

    // Winter-Ausrüstung
    val winterTiresRequired: Boolean = false,
    val snowChainsRequired: Boolean = false,
    val winterPeriod: WinterPeriod? = null,

    // Öffnungszeiten / Fahrverbote
    val truckDrivingBanInfo: String? = null,

    // Kontakt & Notfall
    val emergencyNumber: String = "112",
    val truckBreakdownNumber: String? = null,
    val policeNumber: String? = null,

    // Währungen
    val currency: String = "EUR",
    val language: String = "de",

    // Nützliche Infos
    val fuelPrices: FuelPrices? = null,
    val avgParkingCost: Double? = null,

    // Tipps
    val tips: List<String> = emptyList(),
    val commonIssues: List<String> = emptyList()
)

/**
 * MAUT-SYSTEME
 */
enum class TollSystem {
    NONE, VIGNETTE, ELECTRONIC, TOLL_BOOTH, MIXED
}

/**
 * VIGNETTEN-TYPEN
 */
data class VignetteType(
    val name: String = "",
    val price: Double = 0.0,
    val duration: String = "",
    val vehicleClasses: List<String> = emptyList(),
    val purchaseUrl: String? = null
)

/**
 * PFLICHTEN/ANFORDERUNGEN
 */
data class Requirement(
    val type: RequirementType = RequirementType.OTHER,
    val title: String = "",
    val description: String = "",
    val mandatory: Boolean = true,
    val fine: String? = null
)

enum class RequirementType {
    DOCUMENT, EQUIPMENT, TOLL, PERMIT, OTHER
}

/**
 * BESCHRÄNKUNGEN
 */
data class Restriction(
    val type: RestrictionType = RestrictionType.OTHER,
    val title: String = "",
    val description: String = "",
    val whenToApply: String = ""
)

enum class RestrictionType {
    WEIGHT, DIMENSION, TIME, WEATHER, ROAD, OTHER
}

/**
 * WINTERPERIODE
 */
data class WinterPeriod(
    val startDate: String = "",
    val endDate: String = "",
    val condition: String = ""
)

/**
 * KRAFTSTOFFPREISE
 */
data class FuelPrices(
    val diesel: Double = 0.0,
    val updated: Long = System.currentTimeMillis()
)

/**
 * 🇪🇺 VORDEFINIERTE LÄNDER 🇪🇺
 * Wichtige Transit-Länder für LKW-Fahrer in Europa
 */
object PredefinedCountries {

    // 🇩🇪 DEUTSCHLAND
    val GERMANY = CountryInfo(
        id = "DE",
        name = "Deutschland",
        flag = "🇩🇪",
        callingCode = "+49",
        speedLimitHighway = 80,
        speedLimitCountry = 60, // > 7.5t
        tollSystem = TollSystem.ELECTRONIC,
        electronicTollRequired = true,
        tollSystemName = "Toll Collect",
        requirements = listOf(
            Requirement(RequirementType.DOCUMENT, "Papiere", "Führerschein, Fahrzeugschein, Ladebegleitpapiere"),
            Requirement(RequirementType.EQUIPMENT, "Warnweste", "Pflicht für Fahrer")
        ),
        winterTiresRequired = true,
        winterPeriod = WinterPeriod("01.10.", "31.03.", "bei winterlichen Verhältnissen"),
        truckDrivingBanInfo = "Sonn- und Feiertage: 0-22 Uhr (Ferienfahrverbote beachten!)",
        tips = listOf("Toll Collect OBU immer checken!", "Rettungsgasse bilden bei Stau!", "Hupverbot in Ortschaften beachten"),
        commonIssues = listOf("Stau im Ruhrgebiet", "Parkplatzmangel ab 17 Uhr")
    )

    // 🇦🇹 ÖSTERREICH
    val AUSTRIA = CountryInfo(
        id = "AT",
        name = "Österreich",
        flag = "🇦🇹",
        callingCode = "+43",
        speedLimitHighway = 80, // Nachts oft 60 (Lärmschutz)!
        speedLimitCountry = 70, // > 7.5t
        tollSystem = TollSystem.MIXED,
        vignetteRequired = true,
        electronicTollRequired = true,
        tollSystemName = "Go-Box",
        requirements = listOf(
            Requirement(RequirementType.TOLL, "Go-Box", "Pflicht > 3.5t. Vorher aufladen!", true, "ab €220 Strafe"),
            Requirement(RequirementType.EQUIPMENT, "Schneeketten", "Mitführpflicht 1.11.-15.4.")
        ),
        winterTiresRequired = true,
        snowChainsRequired = true,
        winterPeriod = WinterPeriod("01.11.", "15.04.", "Winterreifenpflicht an Antriebsachse"),
        truckDrivingBanInfo = "Nachtfahrverbot 22-5 Uhr (außer 'Lärmarme' LKW). Wochenendfahrverbot Sa 15 Uhr - So 22 Uhr.",
        tips = listOf("IG-L (Immissionsschutz) Tempo beachten!", "Go-Box Piepser zählen (1x=OK, 2x=Guthaben niedrig, 0x/4x=FEHLER)"),
        commonIssues = listOf("Hohe Strafen bei Go-Box Fehlern", "Inntalautobahn Blockabfertigung")
    )

    // 🇨🇭 SCHWEIZ
    val SWITZERLAND = CountryInfo(
        id = "CH",
        name = "Schweiz",
        flag = "🇨🇭",
        callingCode = "+41",
        currency = "CHF",
        speedLimitHighway = 80,
        speedLimitCountry = 80,
        tollSystem = TollSystem.ELECTRONIC,
        electronicTollRequired = true,
        tollSystemName = "LSVA",
        requirements = listOf(
            Requirement(RequirementType.TOLL, "LSVA / ID Card", "Leistungsabhängige Abgabe", true, "Sehr hohe Strafen!"),
            Requirement(RequirementType.DOCUMENT, "Zollpapiere", "Vor Einfahrt bereit haben (e-dec)")
        ),
        winterTiresRequired = true,
        snowChainsRequired = true,
        winterPeriod = WinterPeriod("01.10.", "30.04.", "Schneeketten obligatorisch bei Signal"),
        truckDrivingBanInfo = "Nachtfahrverbot 22-5 Uhr strikt! Sonntagsfahrverbot.",
        tips = listOf("Kein Navi-Radarwarner erlaubt!", "Zollzeiten genau beachten", "Teure Bußgelder"),
        commonIssues = listOf("Lange Wartezeiten am Zoll (Basel/Chiasso)")
    )

    // 🇵🇱 POLEN
    val POLAND = CountryInfo(
        id = "PL",
        name = "Polen",
        flag = "🇵🇱",
        callingCode = "+48",
        currency = "PLN",
        speedLimitHighway = 80,
        speedLimitCountry = 70,
        tollSystem = TollSystem.ELECTRONIC,
        electronicTollRequired = true,
        tollSystemName = "e-TOLL",
        requirements = listOf(
            Requirement(RequirementType.TOLL, "e-TOLL App/OBU", "Pflicht > 3.5t auf Nationalstraßen"),
            Requirement(RequirementType.EQUIPMENT, "Feuerlöscher", "Zusätzlich vorgeschrieben!")
        ),
        winterTiresRequired = false, // Aber empfohlen
        winterPeriod = WinterPeriod("01.11.", "31.03.", "Empfohlen"),
        truckDrivingBanInfo = "Feiertage & Ferienzeiten beachten (oft Fr-So im Sommer)",
        tips = listOf("e-TOLL Konto vor Grenzübertritt laden", "Warnweste für JEDEN Insassen"),
        commonIssues = listOf("Schlechte Straßen im Osten", "Sprachbarriere bei Kontrollen")
    )

    // 🇨🇿 TSCHECHIEN
    val CZECH = CountryInfo(
        id = "CZ",
        name = "Tschechien",
        flag = "🇨🇿",
        callingCode = "+420",
        currency = "CZK",
        speedLimitHighway = 80,
        speedLimitCountry = 70, // Offiziell oft, LKW fahren meist 80
        tollSystem = TollSystem.ELECTRONIC,
        electronicTollRequired = true,
        tollSystemName = "Myto CZ",
        requirements = listOf(
            Requirement(RequirementType.TOLL, "Myto Box", "Pflicht > 3.5t"),
            Requirement(RequirementType.EQUIPMENT, "Ersatzlampen-Set", "Mitführpflicht!")
        ),
        winterTiresRequired = true,
        winterPeriod = WinterPeriod("01.11.", "31.03.", "Pflicht auf gekennzeichneten Strecken"),
        truckDrivingBanInfo = "So 13-22 Uhr. Ferien: Fr 17-21, Sa 7-13 Uhr.",
        tips = listOf("Licht am Tag Pflicht!", "0.0 Promille Grenze strikt!"),
        commonIssues = listOf("D1 Autobahn (Prag-Brünn) oft Baustellen")
    )

    // 🇫🇷 FRANKREICH
    val FRANCE = CountryInfo(
        id = "FR",
        name = "Frankreich",
        flag = "🇫🇷",
        callingCode = "+33",
        speedLimitHighway = 90, // LKW dürfen hier schneller!
        speedLimitCountry = 80, // Auf Priority Roads, sonst 60
        tollSystem = TollSystem.ELECTRONIC,
        electronicTollRequired = true,
        tollSystemName = "TIS-PL (Télépéage)",
        requirements = listOf(
            Requirement(RequirementType.TOLL, "Télépéage Box", "Für fast alle Autobahnen"),
            Requirement(RequirementType.EQUIPMENT, "Toter-Winkel-Aufkleber", "Pflicht 'Angles Morts'!")
        ),
        winterTiresRequired = true,
        winterPeriod = WinterPeriod("01.11.", "31.03.", "In Gebirgsregionen"),
        truckDrivingBanInfo = "Sa 22 Uhr - So 22 Uhr. Ferienfahrverbote beachten.",
        tips = listOf("Angles Morts Aufkleber an Zugmaschine UND Auflieger!", "Alkoholtester mitführen"),
        commonIssues = listOf("Teure Autobahnmaut", "Flüchtlinge in Calais")
    )

    // 🇧🇪 BELGIEN
    val BELGIUM = CountryInfo(
        id = "BE",
        name = "Belgien",
        flag = "🇧🇪",
        callingCode = "+32",
        speedLimitHighway = 90,
        speedLimitCountry = 60, // Flandern! Wallonie 90. Wir warnen lieber vor 60.
        tollSystem = TollSystem.ELECTRONIC,
        electronicTollRequired = true,
        tollSystemName = "Viapass (Satellic)",
        requirements = listOf(
            Requirement(RequirementType.TOLL, "OBU (Satellic)", "Muss IMMER grün leuchten!"),
            Requirement(RequirementType.EQUIPMENT, "Feuerlöscher", "Griffbereit im Fahrerhaus")
        ),
        truckDrivingBanInfo = "Kein generelles Sonntagsfahrverbot (außer Gefahrgut/Übergröße)",
        tips = listOf("OBU darf nie rot sein (hohe Strafe!)", "Unterschiedliche Tempolimits Flandern/Wallonie beachten"),
        commonIssues = listOf("Ring Antwerpen Stau")
    )

    // 🇳🇱 NIEDERLANDE
    val NETHERLANDS = CountryInfo(
        id = "NL",
        name = "Niederlande",
        flag = "🇳🇱",
        callingCode = "+31",
        speedLimitHighway = 80,
        speedLimitCountry = 80,
        tollSystem = TollSystem.VIGNETTE, // Eurovignette (noch bis 2026)
        tollSystemName = "Eurovignette",
        requirements = listOf(
            Requirement(RequirementType.TOLL, "Eurovignette", "Online buchen vor Einfahrt")
        ),
        truckDrivingBanInfo = "Kein generelles Sonntagsfahrverbot.",
        tips = listOf("Viele Blitzer von hinten!", "Parken nur auf ausgewiesenen Plätzen"),
        commonIssues = listOf("Dichte Verkehrslage Randstad")
    )

    /**
     * Alle vordefinierten Länder abrufen
     */
    fun getAll(): List<CountryInfo> {
        return listOf(GERMANY, AUSTRIA, SWITZERLAND, POLAND, CZECH, FRANCE, BELGIUM, NETHERLANDS)
    }

    /**
     * Lädt Land nach Code
     */
    fun getByCode(code: String): CountryInfo? {
        return getAll().find { it.id.equals(code, ignoreCase = true) }
    }
}