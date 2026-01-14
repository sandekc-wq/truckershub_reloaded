package com.truckershub.core.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckershub.core.design.ThubBackground
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.ThubDarkGray

@Composable
fun CompleteProfileScreen(
    // Wenn man auf "Speichern" klickt, geben wir die Daten zurück an die Zentrale
    onSaveProfile: (firstName: String, lastName: String, birthDate: String) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") } // Später als echter Kalender

    ThubBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Identitäts-Check 🛂",
                style = MaterialTheme.typography.headlineLarge,
                color = ThubNeonBlue,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Für die Sicherheit der Community benötigen wir deine echten Daten. Diese können später NICHT mehr geändert werden.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // VORNAME
            ThubTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = "Vorname (wie im Ausweis)"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // NACHNAME
            ThubTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = "Nachname"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // GEBURTSDATUM
            ThubTextField(
                value = birthDate,
                onValueChange = { birthDate = it },
                label = "Geburtsdatum (TT.MM.JJJJ)"
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if(firstName.isNotBlank() && lastName.isNotBlank() && birthDate.isNotBlank()) {
                        onSaveProfile(firstName, lastName, birthDate)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    text = "IDENTITÄT BESTÄTIGEN",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Kleiner Helfer für die Textfelder (damit wir nicht alles doppelt schreiben)
@Composable
fun ThubTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ThubNeonBlue,
            unfocusedBorderColor = ThubDarkGray,
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            cursorColor = ThubNeonBlue,
            focusedLabelColor = ThubNeonBlue,
            unfocusedLabelColor = Color.Gray
        ),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}