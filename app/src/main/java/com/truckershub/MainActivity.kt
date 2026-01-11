package com.truckershub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckershub.core.design.ThubTheme
import com.truckershub.features.auth.LoginScreen
import com.truckershub.features.auth.AuthViewModel
import com.truckershub.features.home.HomeScreen // Hier importieren wir das neue Dashboard

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThubTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Wir holen uns das ViewModel HIER oben, damit wir entscheiden können: Login oder Home?
                    val authViewModel: AuthViewModel = viewModel()

                    if (authViewModel.loginSuccess) {
                        // WENN eingeloggt -> Zeige Dashboard
                        HomeScreen(
                            onLogoutClick = {
                                authViewModel.logout()
                            }
                        )
                    } else {
                        // SONST -> Zeige LoginScreen
                        LoginScreen(viewModel = authViewModel)
                    }
                }
            }
        }
    }
}