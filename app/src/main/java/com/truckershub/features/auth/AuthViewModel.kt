package com.truckershub.features.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AuthViewModel : ViewModel() {

    // Verbindung zu Firebase (Türsteher & Aktenschrank)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // --- ZUSTAND (STATE) ---
    // Hat der Login/Registrierung geklappt?
    var loginSuccess by mutableStateOf(false)
        private set

    // Gibt es Fehlermeldungen? (z.B. "Passwort zu kurz")
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Kleiner Check beim App-Start: Ist schon jemand eingeloggt?
    init {
        if (auth.currentUser != null) {
            loginSuccess = true
        }
    }

    // --- FUNKTIONEN ---

    // 1. REGISTRIEREN (Neu!)
    fun register(firstName: String, lastName: String, birthDate: String, email: String, pass: String) {
        // Erstmal aufräumen
        errorMessage = null

        // Validierung: Haben wir alles?
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || pass.isBlank()) {
            errorMessage = "Bitte alle Felder ausfüllen."
            return
        }

        // A) Benutzer bei Firebase Auth erstellen (E-Mail & Passwort)
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { authResult ->
                // Benutzer wurde erstellt! Jetzt holen wir uns seine ID.
                val uid = authResult.user?.uid

                if (uid != null) {
                    // B) Benutzerdaten in Firestore speichern (Die Personalakte)
                    val userProfile = hashMapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "birthDate" to birthDate,
                        "email" to email,
                        "role" to "trucker", // Standard-Rolle
                        "created_at" to System.currentTimeMillis()
                    )

                    db.collection("users").document(uid)
                        .set(userProfile, SetOptions.merge())
                        .addOnSuccessListener {
                            // Alles erledigt: Tür auf!
                            loginSuccess = true
                        }
                        .addOnFailureListener { e ->
                            errorMessage = "Profil konnte nicht gespeichert werden: ${e.localizedMessage}"
                        }
                }
            }
            .addOnFailureListener { e ->
                // Fehler beim Erstellen (z.B. E-Mail schon vergeben)
                errorMessage = e.localizedMessage ?: "Registrierung fehlgeschlagen."
            }
    }

    // 2. EINLOGGEN (Klassisch)
    fun login(email: String, pass: String) {
        errorMessage = null

        if (email.isBlank() || pass.isBlank()) {
            errorMessage = "Bitte E-Mail und Passwort eingeben."
            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                loginSuccess = true
            }
            .addOnFailureListener { e ->
                errorMessage = "Login fehlgeschlagen: ${e.localizedMessage}"
            }
    }

    // 3. LOGOUT
    fun logout() {
        auth.signOut()
        loginSuccess = false
        errorMessage = null
    }

    // --- PLATZHALTER (Damit der Code nicht rot wird) ---
    fun loginWithGoogle() {
        // TODO: Google Sign-In Logik hier einbauen
        errorMessage = "Google Login kommt bald!"
    }

    fun loginWithFacebook() {
        // TODO: Facebook Login Logik hier einbauen
        errorMessage = "Facebook Login kommt bald!"
    }
}