package com.truckershub.core.data.model

import androidx.annotation.DrawableRes

/**
 * LAND INFOS
 *
 * Wichtige Informationen für LKW-Fahrer bei Grenzüberquerung
 */
data class CountryInfo(
    val id: String = "",                 // Ländercode (z.B. "DE", "AT", "CH")
    val name: String = "",               // Name ("Deutschland", "Österreich", "Schweiz")
    val flag: String? = null,            // Flaggen-Emoji oder Ressourcen-ID
    val callingCode: String = "",        // Vorwahl (+49, +43, +41)

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

    // Öffnungszeiten
    val truckDrivingBanInfo: String? = null,  // z.B. "Sonntags- und Feiertagsfahrverbot: 0-22 Uhr"

    // Kontakt & Notfall
    val emergencyNumber: String = "112",  // Europaweit standardmäßig 112
    val truckBreakdownNumber: String? = null,  // Pannenhilfe
    val policeNumber: String? = null,

    // Währungen
    val currency: String = "EUR",
    val language: String = "de",          // Hauptsprache

    // Nützliche Infos
    val fuelPrices: FuelPrices? = null,
    val avgParkingCost: Double? = null,   // Durchschnittliche Parkplatzkosten pro Nacht

    // Tipps
    val tips: List<String> = emptyList(),  // Wichtige Tipps für LKW-Fahrer
    val commonIssues: List<String> = emptyList()  // Häufige Probleme
)

/**
 * MAUT-SYSTEME
 */
enum class TollSystem {
    NONE,           // Keine Maut
    VIGNETTE,       // Vignette
    ELECTRONIC,     // Elektronische Maut (Go-Box etc.)
    TOLL_BOOTH,     // Maut-Station
    MIXED           // Gemischt
}

/**
 * VIGNETTEN-TYPEN
 */
data class VignetteType(
    val name: String = "",                // z.B. "10-Tage", "Monats-", "Jahresvignette"
    val price: Double = 0.0,              // Preis in EUR
    val duration: String = "",            // z.B. "10 Tage", "1 Jahr"
    val vehicleClasses: List<String> = emptyList(),  // Welche Fahrzeugklassen?
    val purchaseUrl: String? = null       // Wo online kaufen?
)

/**
 * PFLICHTEN/ANFORDERUNGEN
 */
data class Requirement(
    val type: RequirementType = RequirementType.OTHER,
    val title: String = "",
    val description: String = "",
    val mandatory: Boolean = true,        // Pflicht oder Empfehlung?
    val fine: String? = null              // Bußgeld bei Nichteinhaltung
)

enum class RequirementType {
    DOCUMENT,          // Dokumente
    EQUIPMENT,         // Ausrüstung
    TOLL,              // Maut/Vignette
    PERMIT,            // Genehmigungen
    OTHER              // Sonstiges
}

/**
 * BESCHRÄNKUNGEN
 */
data class Restriction(
    val type: RestrictionType = RestrictionType.OTHER,
    val title: String = "",
    val description: String = "",
    val whenToApply: String = ""          // Wann gilt das?
)

enum class RestrictionType {
    WEIGHT,            // Gewichtsbeschränkung
    DIMENSION,         // Größenbeschränkung
    TIME,              // Zeitliche Beschränkung
    WEATHER,           // Wetterabhängig
    ROAD,              // Straßenabhängig
    OTHER              // Sonstiges
}

/**
 * WINTERPERIODE
 */
data class WinterPeriod(
    val startDate: String = "",           // z.B. "01.11."
    val endDate: String = "",             // z.B. "15.04."
    val condition: String = ""            // z.B. "bei Schneeglätte oder Glatteis"
)

/**
 * KRAFTSTOFFPREISE
 */
data class FuelPrices(
    val diesel: Double = 0.0,            // Preis pro Liter in EUR
    val updated: Long = System.currentTimeMillis()
)

/**
 * VORDEFINIERTE LÄNDER
 *
 * Wichtige Transit-Länder für LKW-Fahrer in Europa
 */
object PredefinedCountries {
    val GERMANY = CountryInfo(
        id = "DE",
        name = "Deutschland",
        flag = "🇩🇪",
        callingCode = "+49",

        tollSystem = TollSystem.ELECTRONIC,
        vignetteRequired = false,
        electronicTollRequired = true,    // Toll Collect
        tollSystemName = "Toll Collect",

        requirements = listOf(
            Requirement(
                type = RequirementType.DOCUMENT,
                title = "Papiere",
                description = "Führerschein, Fahrzeugschein, Ladebegleitskarte, ADR bei Gefahrgut"
            )
        ),

        winterTiresRequired = true,
        winterPeriod = WinterPeriod(
            startDate = "01.10.",
            endDate = "31.03.",
            condition = "bei winterlichen Verhältnissen"
        ),

        truckDrivingBanInfo = " Sonn- und Feiertagsfahrverbot: 0-22 Uhr. Ausnahmen: z.B. in Bayern von 22-22 Uhr",

        emergencyNumber = "112",
        currency = "EUR",
        language = "de",

        tips = listOf(
            "Toll Collect Box immer eingeschaltet haben!",
            "Sonntagsfahrverbot beachten (Ausnahmen vorhanden)",
            "Pkw-Maut auch auf BAB beachten"
        ),
        commonIssues = listOf(
            "Stau um Ballungsräume (München, Frankfurt, Hamburg)",
            "Baustellen sind häufig"
        )
    )

    val AUSTRIA = CountryInfo(
        id = "AT",
        name = "Österreich",
        flag = "🇦🇹",
        callingCode = "+43",

        tollSystem = TollSystem.MIXED,
        vignetteRequired = true,
        vignetteTypes = listOf(
            VignetteType(name = "Tagesvignette", price = 25.90, duration = "Tages"),
            VignetteType(name = "10-Tage", price = 34.00, duration = "10 Tage"),
            VignetteType(name = "2-Monate", price = 96.70, duration = "2 Monate"),
            VignetteType(name = "Jahresvignette", price = 955.50, duration = "Jahr")
        ),
        electronicTollRequired = true,
        tollSystemName = "Go-Box",

        requirements = listOf(
            Requirement(
                type = RequirementType.TOLL,
                title = "Go-Box",
                description = "Für Fahrzeuge über 3,5t zwingend erforderlich",
                mandatory = true,
                fine = "€2.000-€10.000"
            ),
            Requirement(
                type = RequirementType.DOCUMENT,
                title = "LKW-Maut-Vignette",
                description = "Für Fahrzeuge unter 3,5t",
                mandatory = true
            )
        ),

        winterTiresRequired = true,
        snowChainsRequired = true,
        winterPeriod = WinterPeriod(
            startDate = "01.11.",
            endDate = "15.04.",
            condition = "bei winterlichen Verhältnissen"
        ),

        truckDrivingBanInfo = " Sonntagsfahrverbot: 0-22 Uhr",

        emergencyNumber = "112",
        currency = "EUR",
        language = "de",

        tips = listOf(
            "Schneeketten immer mitführen! Wintersituation ändert sich schnell in den Bergen!",
            "Go-Box vorher beantragen und aufladen!",
            "Ganzjährig bei schwereren LKW über 7,5t sind Winterreifen Pflicht"
        ),
        commonIssues = listOf(
            "Schnelle Wetterumschwünge in den Alpen",
            "Straßensperrungen bei Sturm",
            "Lange Staustrecken bei Grenzübertritt"
        )
    )

    val SWITZERLAND = CountryInfo(
        id = "CH",
        name = "Schweiz",
        flag = "🇨🇭",
        callingCode = "+41",

        tollSystem = TollSystem.ELECTRONIC,
        vignetteRequired = false,
        electronicTollRequired = true,
        tollSystemName = "LSVA",

        requirements = listOf(
            Requirement(
                type = RequirementType.TOLL,
                title = "LSVA-Karte",
                description = "Leistungsabhängige Schwerverkehrsabzahlung",
                mandatory = true,
                fine = "CHF 20.000-30.000"
            ),
            Requirement(
                type = RequirementType.DOCUMENT,
                title = "Fahrzeugzulassung",
                description = "Verschiedene Dokumente für Auslandstransporte",
                mandatory = true
            )
        ),

        winterTiresRequired = true,
        snowChainsRequired = true,
        winterPeriod = WinterPeriod(
            startDate = "01.10.",
            endDate = "30.04.",
            condition = "bei winterlichen Verhältnissen"
        ),

        truckDrivingBanInfo = " Sonntagsfahrverbot: 22-7 Uhr. Ausnahmen: z.B. im Landverkehr 0-24 Uhr",

        emergencyNumber = "112",
        currency = "CHF",
        language = "de",

        tips = listOf(
            "LSVA vorher kaufen! (Brennpass, Internet, oder Tankstellen)",
            "Maximale Geschwindigkeit 80 km/h auf Autobahnen!",
            "Nachtfahrverbot beachten (22-7 Uhr)",
            "Schnelle Wetteränderungen in den Bergen!"
        ),
        commonIssues = listOf(
            "Sehr hohe Mautkosten",
            "Tempo 80 ist streng überwacht!",
            "Straßensperrungen im Winter häufig"
        )
    )

    /**
     * Alle vordefinierten Länder
     */
    fun getAll(): List<CountryInfo> {
        return listOf(GERMANY, AUSTRIA, SWITZERLAND)
    }

    /**
     * Lädt Land nach Code
     */
    fun getByCode(code: String): CountryInfo? {
        return getAll().find { it.id == code }
    }
}