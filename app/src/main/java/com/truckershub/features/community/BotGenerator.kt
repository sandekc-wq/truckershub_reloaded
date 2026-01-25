package com.truckershub.features.community

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object BotGenerator {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Die feste ID für unseren Bot
    const val BOT_ID = "thub_bot_kitt_v1"
    const val BOT_NAME = "K.I.T.T."
    // Ein cooles Bild für den Bot
    const val BOT_AVATAR = "https://images.unsplash.com/photo-1599933599878-aec540050868?q=80&w=200&auto=format&fit=crop"

    fun summonBot(onSuccess: () -> Unit) {
        val myId = auth.currentUser?.uid ?: return

        // 1. Den Bot-User anlegen (oder überschreiben)
        val botData = hashMapOf(
            "uid" to BOT_ID,
            "firstName" to "K.I.T.T.",
            "lastName" to "(Bot)",
            "funkName" to "K.I.T.T.",
            "email" to "bot@truckershub.local",
            "profileImageUrl" to BOT_AVATAR,
            "status" to "Online",
            "truckType" to "Pontiac Firebird",
            "currentLocation" to null
        )

        firestore.collection("users").document(BOT_ID).set(botData)
            .addOnSuccessListener {
                // 2. Freundschaft erzwingen (Direkt "accepted")
                createFriendship(myId, BOT_ID)

                // 3. Einen Post absetzen
                createBotPost()

                onSuccess()
            }
    }

    private fun createFriendship(myId: String, botId: String) {
        // Wir erstellen einen Eintrag in 'friend_requests' mit status 'accepted'
        // Damit er sofort in deiner "Crew" auftaucht.

        // Checken wir sicherheitshalber nicht auf Doppelungen für den Test,
        // Firestore verkraftet das.

        val friendship = hashMapOf(
            "fromId" to botId, // Der Bot hat DICH angefragt (oder andersrum)
            "toId" to myId,
            "status" to "accepted", // Direkt angenommen!
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("friend_requests").add(friendship)
    }

    private fun createBotPost() {
        val newPost = hashMapOf(
            "userId" to BOT_ID,
            "userName" to BOT_NAME,
            "userAvatarUrl" to BOT_AVATAR,
            "imageUrl" to "https://images.unsplash.com/photo-1601584115197-04ecc0da31d7?q=80&w=600&auto=format&fit=crop", // Ein Stau-Bild
            "text" to "Hallo Partner! Ich bin bereit für Systemtests. Alle Systeme laufen normal. 🚛🚨",
            "likes" to emptyList<String>(),
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("posts").add(newPost)
    }
}