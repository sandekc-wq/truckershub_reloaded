package com.truckershub.core.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckershub.R
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.TextWhite

@Suppress("DEPRECATION")
@Composable
fun ThubRegisterScreen(
    onRegisterClick: (String, String, String, String, String) -> Unit,
    onGoogleClick: () -> Unit,
    onFacebookClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Hintergrund-Box (Schwarz + Logo)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Grundfarbe Schwarz
    ) {

        // 1. DAS RUNDE LOGO (Zentriert im Hintergrund)
        Image(
            painter = painterResource(id = R.drawable.thub_logo_bg),
            contentDescription = "Logo Background",
            contentScale = ContentScale.Fit, // Logo soll komplett sichtbar sein, nicht abgeschnitten
            alpha = 0.5f, // Ein bisschen transparent, damit der Text lesbar bleibt
            modifier = Modifier
                .align(Alignment.Center) // Genau in die Mitte
                .fillMaxWidth(0.8f) // Nicht ganz so breit wie der Bildschirm
        )

        // 2. DER INHALT (Formular)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "WERDE PARTNER 🚛",
                style = MaterialTheme.typography.headlineMedium,
                color = ThubNeonBlue,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Erstelle dein Truckers Hub Konto.",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- FELDER ---
            ThubTextField(value = firstName, onValueChange = { firstName = it }, label = "Vorname")
            Spacer(modifier = Modifier.height(12.dp))

            ThubTextField(value = lastName, onValueChange = { lastName = it }, label = "Nachname")
            Spacer(modifier = Modifier.height(12.dp))

            ThubTextField(value = birthDate, onValueChange = { birthDate = it }, label = "Geburtsdatum (TT.MM.JJJJ)")
            Spacer(modifier = Modifier.height(12.dp))

            ThubTextField(value = email, onValueChange = { email = it }, label = "E-Mail Adresse")
            Spacer(modifier = Modifier.height(12.dp))

            ThubTextField(value = password, onValueChange = { password = it }, label = "Passwort")
            Spacer(modifier = Modifier.height(12.dp))

            ThubTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Passwort wiederholen")

            Spacer(modifier = Modifier.height(32.dp))

            // REGISTER BUTTON
            Button(
                onClick = {
                    onRegisterClick(firstName, lastName, birthDate, email, password)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(text = "KONTO ERSTELLEN", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ODER ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Divider(color = Color.Gray, modifier = Modifier.weight(1f))
                Text(text = " ODER ", color = Color.LightGray, modifier = Modifier.padding(horizontal = 8.dp))
                Divider(color = Color.Gray, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SOCIAL BUTTONS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = onGoogleClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).padding(end = 8.dp).height(50.dp)
                ) { Text("Google", color = Color.Black) }

                Button(
                    onClick = onFacebookClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).padding(start = 8.dp).height(50.dp)
                ) { Text("Facebook", color = Color.White) }
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = onLoginClick) {
                Text(text = "Zurück zum Login", color = ThubNeonBlue)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}