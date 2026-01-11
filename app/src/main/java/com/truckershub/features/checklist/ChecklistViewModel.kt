package com.truckershub.features.checklist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class ChecklistViewModel : ViewModel() {

    // Verbindung zur Datenbank und Auth
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Unsere Liste im ViewModel (damit sie beim Drehen nicht verloren geht)
    // Wir initialisieren sie hier direkt
    var checklist = mutableStateListOf(
        CheckPoint(1, "Beleuchtung & Blinker", false),
        CheckPoint(2, "Reifen (Druck & Zustand)", false),
        CheckPoint(3, "Bremsen & Druckluft", false),
        CheckPoint(4, "Spiegel & Scheiben", false),
        CheckPoint(5, "Motoröl & Kühlwasser", false),
        CheckPoint(6, "Ladungssicherung", false),
        CheckPoint(7, "Digitaler Tacho & Maut", false),
        CheckPoint(8, "Warndreieck & Weste", false)
    )
        private set

    // Status für den Button (Ladekreis anzeigen?)
    var isSaving by mutableStateOf(false)
        private set

    // Funktion zum Umschalten der Haken
    fun toggleCheck(index: Int, isChecked: Boolean) {
        val item = checklist[index]
        checklist[index] = item.copy(isChecked = isChecked)
    }

    // Funktion: Ab an die Cloud damit!
    fun saveReport(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onError("Nicht eingeloggt!")
            return
        }

        // Wir senden nur das Nötigste: Was wurde gecheckt?
        val reportData = hashMapOf(
            "userId" to user.uid,
            "userEmail" to user.email,
            "timestamp" to FieldValue.serverTimestamp(), // Die Server-Zeit (Beweis!)
            "checks" to checklist.map { "${it.title}: ${it.isChecked}" },
            "allClear" to checklist.all { it.isChecked } // War alles grün?
        )

        isSaving = true

        db.collection("departure_checks") // So heißt der Ordner in der DB
            .add(reportData)
            .addOnSuccessListener {
                isSaving = false
                onSuccess() // Sag dem Screen: "Alles erledigt"
            }
            .addOnFailureListener { e ->
                isSaving = false
                onError(e.localizedMessage ?: "Fehler beim Speichern")
            }
    }
}