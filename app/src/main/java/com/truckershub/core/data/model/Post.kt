package com.truckershub.core.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Der Bauplan für einen Feed-Beitrag (Diesel-Feed) ⛽
 */
data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "Unbekannt",
    val userAvatarUrl: String = "",

    // Das Herzstück: Das Bild der Karre (oder vom Stau)
    val imageUrl: String = "",

    // Der dumme Spruch dazu ;-)
    val text: String = "",

    // Statistik
    val likes: List<String> = emptyList(), // Liste der User-IDs, die geliked haben
    val commentCount: Int = 0,

    // Wann gepostet? (Wichtig für die 24h Löschung!)
    @ServerTimestamp
    val timestamp: Date? = null
)