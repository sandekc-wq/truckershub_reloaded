package com.truckershub.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class Location(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,           // z.B. "Zentrallager Müller" oder "Zuhause"
    val latitude: Double,
    val longitude: Double,
    val type: LocationType,     // FIRMA, PRIVAT, TANKSTELLE, SONSTIGES
    val description: String = "", // Für das "Firmen-Wiki" (z.B. "Einfahrt hinten nehmen")
    val requirements: String = "" // Für PSA (z.B. "Helm, Warnweste")
)

enum class LocationType {
    COMPANY, // Firma
    PRIVATE, // Zuhause/Privat
    FUEL,    // Tankstelle
    OTHER    // Sonstiges
}