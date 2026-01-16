package com.truckershub.core.data.model

/**
 * FREUNDSCHAFT DATENMODELL
 *
 * Verwaltet Freundschaftsbeziehungen zwischen Fahrern
 */
data class Friendship(
    val id: String = "",                  // z.B. "user1_user2" (alphabetisch sortiert)
    val userId1: String = "",             // Teilnehmer 1
    val userId2: String = "",             // Teilnehmer 2
    val status: FriendshipStatus = FriendshipStatus.PENDING,
    val requestedBy: String = "",         // Wer hat die Anfrage geschickt?
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null,
    val messagesCount: Int = 0            // Wie viele Nachrichten insgesamt?
) {
    /**
     * Prüft ob ein bestimmter User Teil der Freundschaft ist
     */
    fun involves(userId: String): Boolean {
        return userId1 == userId || userId2 == userId
    }

    /**
     * Gibt die User-ID des Partners zurück
     */
    fun getPartnerId(myUserId: String): String {
        return if (userId1 == myUserId) userId2 else userId1
    }

    /**
     * Prüft ob der User der Anfrager ist
     */
    fun amIRequester(myUserId: String): Boolean {
        return requestedBy == myUserId
    }

    /**
     * Prüft ob die Freundschaft aktiv ist
     */
    fun isActive(): Boolean {
        return status == FriendshipStatus.ACCEPTED
    }
}

/**
 * FREUNDSCHAFTS-STATUS
 */
enum class FriendshipStatus {
    PENDING,       // Warte auf Antwort
    ACCEPTED,      // Freunde
    REJECTED,      // Abgelehnt
    BLOCKED        // Blockiert
}

/**
 * FREUNDSCHAFTS-ANFRAGE
 *
 * Vereinfachtes Model für Anfragen-Listen
 */
data class FriendshipRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserPhoto: String? = null,
    val fromUserTruck: String? = null,   // z.B. "Scania R450"
    val toUserId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val message: String = ""              // Persönliche Nachricht zur Anfrage
)

/**
 * NACHRICHT DATENMODELL
 *
 * Email-artige Kommunikation zwischen Freunden
 */
data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val senderName: String = "",
    val senderPhoto: String? = null,

    // Nachricht
    val subject: String = "",             // Betreff
    val content: String = "",             // Nachrichtentext
    val attachments: List<String> = emptyList(),  // URLs zu Anhängen

    // Status
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false,            // Gelesen?
    val readAt: Long? = null,             // Wann gelesen?
    val folder: MessageFolder = MessageFolder.INBOX,  // Postfach-Ordner

    // Optional
    val replyToId: String? = null,        // Wenn Antwort auf eine Nachricht
    val forwarded: Boolean = false        // Weitergeleitet?
) {
    /**
     * Prüft ob Nachricht vom User selbst ist (gesendet)
     */
    fun isSentBy(myUserId: String): Boolean {
        return senderId == myUserId
    }

    /**
     * Prüft ob Nachricht für den User ist (empfangen)
     */
    fun isReceivedBy(myUserId: String): Boolean {
        return receiverId == myUserId
    }
}

/**
 * NACHRICHTEN-ORDNER
 */
enum class MessageFolder {
    INBOX,         // Eingang
    SENT,          // Gesendet
    ARCHIVE,       // Archiv
    TRASH          // Papierkorb
}

/**
 * NACHRICHTEN-KURZINFO
 *
 * Leichtgewichtig für Listen
 */
data class MessageSummary(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhoto: String? = null,
    val subject: String = "",
    val preview: String = "",             // Inhalt vorschneiden
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val folder: MessageFolder = MessageFolder.INBOX,
    val hasAttachments: Boolean = false
) {
    companion object {
        /**
         * Erstellt Summary aus vollständiger Message
         */
        fun fromMessage(message: Message): MessageSummary {
            val maxLength = 50
            val preview = if (message.content.length > maxLength) {
                message.content.substring(0, maxLength) + "..."
            } else {
                message.content
            }

            return MessageSummary(
                id = message.id,
                senderId = message.senderId,
                senderName = message.senderName,
                senderPhoto = message.senderPhoto,
                subject = message.subject,
                preview = preview,
                timestamp = message.timestamp,
                read = message.read,
                folder = message.folder,
                hasAttachments = message.attachments.isNotEmpty()
            )
        }
    }
}