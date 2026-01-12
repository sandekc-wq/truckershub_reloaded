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
                            // ... und wollen uns registrieren? -> Zeige Registrierung
                            ThubRegisterScreen(
                                onRegisterClick = { vorname, nachname, geb, email, pass ->
                                    // HIER RUFEN WIR JETZT DIE REGISTRIERUNG AUF:
                                    authViewModel.register(vorname, nachname, geb, email, pass)
                                },
                                onBackClick = {
                                    // Zurück zum Login
                                    showRegisterScreen = false
                                }
                            )
                        } else {
                            // ... sonst zeigen wir den normalen Login
                            ThubLoginScreen(
                                onLoginClick = { email, pass ->
                                    authViewModel.login(email, pass)
                                },
                                onGoogleClick = {
                                    authViewModel.loginWithGoogle()
                                },
                                onFacebookClick = {
                                    authViewModel.loginWithFacebook()
                                },
                                onRegisterClick = {
                                    // Weiche stellen: Auf zur Registrierung!
                                    showRegisterScreen = true
                                },
                                errorMessage = authViewModel.errorMessage
                            )
                        }
                    }
                }
            }

        }
    }
}