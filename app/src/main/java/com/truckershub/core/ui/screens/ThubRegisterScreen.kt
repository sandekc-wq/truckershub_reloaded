package com.truckershub.core.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckershub.R

// --- THUB NEON STYLE ---
private val NeonDeepBackground = Color(0xFF050A10)
private val NeonDarkBlue = Color(0xFF0D1B2A)
private val NeonCyan = Color(0xFF00E5FF)
private val NeonTextPrimary = Color(0xFFE0F7FA)
private val NeonTextSecondary = Color(0xFF90A4AE)
private val NeonWarning = Color(0xFFFFAB40)

@Composable
fun ThubRegisterScreen(
    onRegisterClick: (String, String, String, String, String) -> Unit,
    onBackClick: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonDeepBackground)
    ) {
        // HINTERGRUND-BILD
        Image(
            painter = painterResource(id = R.drawable.thub_logo_bg),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(350.dp) // Habe es auch etwas größer gemacht (300 -> 350)
                .alpha(0.6f), // HIER IST DER DIMMER: 0.5f = 50% Sichtbarkeit (vorher 0.15f)
            contentScale = ContentScale.Fit
        )

        // VERLAUF (Damit Text lesbar bleibt)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent, // Oben klar
                            NeonDeepBackground.copy(alpha = 0.8f) // Unten etwas dunkler
                        )
                    )
                )
        )

        // INHALT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "WERDE PARTNER",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Erstelle dein Truckers Hub Konto.", color = NeonTextSecondary, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(32.dp))

            // FELDER
            ThubRegisterTextField(firstName, { firstName = it }, "Vorname", Icons.Filled.Person)
            Spacer(modifier = Modifier.height(16.dp))
            ThubRegisterTextField(lastName, { lastName = it }, "Nachname", Icons.Filled.Person)
            Spacer(modifier = Modifier.height(16.dp))
            ThubRegisterTextField(birthDate, { birthDate = it }, "Geburtsdatum (TT.MM.JJJJ)", Icons.Filled.DateRange)

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Visibility, null, tint = NeonWarning, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Identität ist später nicht mehr änderbar.", color = NeonTextSecondary, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = NeonCyan.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))

            ThubRegisterTextField(email, { email = it }, "E-Mail Adresse", Icons.Filled.Email)
            Spacer(modifier = Modifier.height(16.dp))
            ThubPasswordTextField(password, { password = it }, "Passwort", passwordVisible) { passwordVisible = !passwordVisible }
            Spacer(modifier = Modifier.height(16.dp))
            ThubPasswordTextField(confirmPassword, { confirmPassword = it }, "Passwort wiederholen", confirmPasswordVisible) { confirmPasswordVisible = !confirmPasswordVisible }

            Spacer(modifier = Modifier.height(32.dp))

            // BUTTON
            Button(
                onClick = { onRegisterClick(firstName, lastName, birthDate, email, password) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text("KONTO ERSTELLEN", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onBackClick) {
                Text("Zurück zum Login", color = NeonCyan)
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
fun ThubRegisterTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = NeonCyan) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = NeonTextSecondary,
            focusedLabelColor = NeonCyan,
            unfocusedLabelColor = NeonTextSecondary,
            cursorColor = NeonCyan,
            focusedTextColor = NeonTextPrimary,
            unfocusedTextColor = NeonTextPrimary
        ),
        shape = RoundedCornerShape(8.dp),
        singleLine = true
    )
}

@Composable
fun ThubPasswordTextField(value: String, onValueChange: (String) -> Unit, label: String, isVisible: Boolean, onToggle: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = NeonCyan) },
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = NeonTextSecondary)
            }
        },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = NeonTextSecondary,
            focusedLabelColor = NeonCyan,
            unfocusedLabelColor = NeonTextSecondary,
            cursorColor = NeonCyan,
            focusedTextColor = NeonTextPrimary,
            unfocusedTextColor = NeonTextPrimary
        ),
        shape = RoundedCornerShape(8.dp),
        singleLine = true
    )
}