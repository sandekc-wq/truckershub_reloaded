package com.truckershub.core.data.model

data class RouteInstruction(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val type: Int = 0,
    val text: String = "",
    val index: Int = 0,      // <--- DAS WAR DAS FEHLENDE FELD! 🧩
    val wayPoints: List<Int> = emptyList() // Falls ORS das Array so liefert
)