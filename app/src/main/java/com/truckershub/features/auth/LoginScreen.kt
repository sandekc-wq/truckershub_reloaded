package com.truckershub.features.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckershub.R
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.StatusRed

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(true) }

    if (viewModel.loginSuccess) {
        Box(modifier = Modifier.fillMaxSize().background(ThubBlack), contentAlignment = Alignment.Center) {
            Text("LOGIN ERFOLGREICH!", color = ThubNeonBlue, style = MaterialTheme.typography.headlineLarge)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Hintergrund
        Image(
            painter = painterResource(id = R.drawable.thub_background),
            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
        )
        // Dimmer
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)))

        // 2. Inhalt
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isRegister) "NEUES KONTO" else "WILLKOMMEN",
                style = MaterialTheme.typography.headlineLarge,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Fehleranzeige
            if (viewModel.errorMessage != null) {
                Text(
                    text = viewModel.errorMessage!!,
                    color = StatusRed,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-Mail Adresse") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThubNeonBlue, unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = ThubNeonBlue, unfocusedLabelColor = Color.Gray,
                    cursorColor = ThubNeonBlue, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Passwort") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThubNeonBlue, unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = ThubNeonBlue, unfocusedLabelColor = Color.Gray,
                    cursorColor = ThubNeonBlue, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isRegister) {
                        viewModel.register(email, password)
                    } else {
                        viewModel.login(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThubNeonBlue, // Hintergrund: Neon Blau
                    contentColor = ThubBlack       // Text: Schwarz (Damit man es sieht!)
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = !viewModel.isLoading
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(color = ThubBlack, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (isRegister) "KONTO ERSTELLEN" else "ANMELDEN",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { isRegister = !isRegister }) {
                Text(
                    text = if (isRegister) "Bereits ein Konto? Zum Login" else "Noch kein Konto? Hier registrieren",
                    color = TextWhite
                )
            }
        }
    }
}