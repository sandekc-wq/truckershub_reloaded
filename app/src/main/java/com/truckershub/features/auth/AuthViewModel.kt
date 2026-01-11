package com.truckershub.features.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // HIER IST DAS KEYLESS GO:
    // Beim Starten wird sofort geprüft: Gibt es schon einen User?
    // Wenn ja (auth.currentUser != null), ist loginSuccess sofort TRUE.
    var loginSuccess by mutableStateOf(auth.currentUser != null)
        private set

    fun register(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            errorMessage = "Bitte E-Mail und Passwort ausfüllen."
            return
        }

        isLoading = true
        errorMessage = null

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    loginSuccess = true
                } else {
                    val exception = task.exception
                    errorMessage = if (exception is FirebaseAuthUserCollisionException) {
                        "Diese E-Mail wird schon verwendet."
                    } else {
                        exception?.localizedMessage ?: "Fehler bei der Registrierung."
                    }
                }
            }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            errorMessage = "Bitte E-Mail und Passwort ausfüllen."
            return
        }

        isLoading = true
        errorMessage = null

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    loginSuccess = true
                } else {
                    errorMessage = task.exception?.localizedMessage ?: "Login fehlgeschlagen."
                }
            }
    }

    fun logout() {
        auth.signOut()
        loginSuccess = false
        isLoading = false
        errorMessage = null
    }
}