package com.truckershub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckershub.core.design.ThubTheme
import com.truckershub.core.ui.screens.ThubLoginScreen
import com.truckershub.core.ui.screens.ThubRegisterScreen
import com.truckershub.features.auth.AuthViewModel
import com.truckershub.features.home.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThubTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val authViewModel: AuthViewModel = viewModel()

                    // Wir merken uns hier lokal, ob der User zum Registrierungs-Screen will
                    var showRegisterScreen by remember { mutableStateOf(false) }

                    // 1. Sind wir schon eingeloggt? -> Ab nach Hause (Karte)
                    if (authViewModel.loginSuccess) {
                        HomeScreen(
                            onLogoutClick = {
                                authViewModel.logout()
                            }
                        )
                    } else {
                        // 2. Sind wir NICHT eingeloggt...
                        if (showRegisterScreen) {
                            // ... und wollen uns registrieren? -> Zeige Registrierungs-Screen
                            ThubRegisterScreen(
                                onRegisterClick = { vorname, nachname, geb, email, pass ->
                                    // Wir übergeben jetzt alle 5 Werte an das ViewModel
                                    authViewModel.register(vorname, nachname, geb, email, pass)
                                },
                                onGoogleClick = { /* TODO: Google Logik später */ },
                                onFacebookClick = { /* TODO: Facebook Logik später */ },
                                onLoginClick = {
                                    // Der User hat schon ein Konto -> Zurück zum Login
                                    showRegisterScreen = false
                                }
                            )
                        } else {
                            // ... sonst zeigen wir den normalen LOGIN-SCREEN
                            // (Hier war vorher der Fehler, da stand der falsche Screen)
                            ThubLoginScreen(
                                onLoginClick = { email, password ->
                                    authViewModel.login(email, password)
                                },
                                onRegisterClick = {
                                    // Der User will ein neues Konto -> Schalter umlegen
                                    showRegisterScreen = true
                                },
                                onGoogleClick = { /* TODO: Google Logik später */ },
                                onFacebookClick = { /* TODO: Facebook Logik später */ }
                            )
                        }
                    }
                }
            }
        }
    }
}