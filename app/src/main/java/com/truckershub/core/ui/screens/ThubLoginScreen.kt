package com.truckershub.core.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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

// --- THUB NEON STYLE (Lokal definiert für diesen Screen) ---
private val NeonDeepBackground = Color(0xFF050A10)
private val NeonCyan = Color(0xFF00E5FF)
private val NeonTextPrimary = Color(0xFFE0F7FA)
private val NeonTextSecondary = Color(0xFF90A4AE)
private val NeonWarning = Color(0xFFFFAB40)

@Composable
fun ThubLoginScreen(
    onLoginClick: (String, String) -> Unit,
    onGoogleClick: () -> Unit,
    onFacebookClick: () -> Unit,
    onRegisterClick: () -> Unit,
    errorMessage: String? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonDeepBackground)
    ) {
        // 1. HINTERGRUND-BILD (Das Logo)
        Image(
            painter = painterResource(id = R.drawable.thub_logo_bg),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(350.dp)
                .alpha(0.5f), // 50% Sichtbarkeit
            contentScale = ContentScale.Fit
        )

        // 2. VERLAUF (Damit man den Text gut lesen kann)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            NeonDeepBackground.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // 3. INHALT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "TRUCKERS HUB",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan, // NEON!
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Willkommen zurück, Partner.", color = NeonTextSecondary, fontSize = 16.sp)

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                // Fehlermeldung in Orange, damit sie auffällt
                Text(text = errorMessage, color = NeonWarning, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            ThubLoginTextField(
                value = email,
                onValueChange = { email = it },
                label = "E-Mail Adresse",
                icon = Icons.Filled.Email
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Passwort") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = NeonCyan) },
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = null, tint = NeonTextSecondary)
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan, // Neon Rand
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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onLoginClick(email, password) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black), // Neon Button
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text("EINLOGGEN", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = NeonTextSecondary.copy(alpha = 0.5f))
                Text(" ODER ", color = NeonTextSecondary, modifier = Modifier.padding(horizontal = 8.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = NeonTextSecondary.copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            SocialLoginButton("Google", Color.White, Color.Black, onGoogleClick)
            Spacer(modifier = Modifier.height(12.dp))
            SocialLoginButton("Facebook", Color(0xFF1877F2), Color.White, onFacebookClick)

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = onRegisterClick) {
                Text("Noch kein Konto? Jetzt registrieren", color = NeonCyan) // Neon Link
            }
        }
    }
}

// Hilfsfunktion für Textfelder (Lokal für diesen Screen)
@Composable
fun ThubLoginTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector) {
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
fun SocialLoginButton(text: String, color: Color, textColor: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text("Weiter mit $text", color = textColor, fontWeight = FontWeight.Bold)
    }
}