package com.truckershub.features.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.ImageLoader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.truckershub.R
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.network.SecureHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- HELPER FUNKTIONEN ---

/**
 * Formatiert Timestamp zu lesbares Datum
 */
private fun formatDate(timestamp: Long): String {
    return if (timestamp > 0) {
        val sdf = SimpleDateFormat("MMM yyyy", Locale.GERMAN)
        sdf.format(Date(timestamp))
    } else {
        "Unbekannt"
    }
}

// --- CONFIG ---
private const val UPLOAD_URL = "https://inetfacts.de/thub_api/upload_thub.php"
private const val SECRET_KEY = "LKW_V8_POWER"

// --- DATENMODELL ---
data class UserProfile(
    // Basis-Informationen
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val funkName: String = "",
    val company: String = "",
    val truckBrand: String = "",
    val trailerType: String = "",
    val bio: String = "",
    val status: String = "Fahrbereit",
    val profileImageUrl: String = "",

    // Fahrzeug-Daten
    val truckLength: String = "",
    val truckType: String = "",

    // Standort & Einstellungen (NEU!)
    val shareLocation: Boolean = false,        // Standort freigeben?
    val language: String = "de",               // "de" oder "en"
    val darkMode: Boolean = false,             // Dunkler Modus
    val notificationsEnabled: Boolean = true,  // Benachrichtigungen

    // Statistiken (NEU!)
    val totalParkings: Int = 0,                // Wie oft geparkt
    val totalRatings: Int = 0,                 // Wie viele Bewertungen
    val totalFriends: Int = 0,                 // Freunde-Anzahl
    val ampelUpdates: Int = 0,                 // Ampel-Updates gemacht

    // Online-Status (NEU!)
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,                   // Letzter Login
    val createdAt: Long = 0L                   // Registrierungsdatum
)

// Standort-Informationen (für Firebase)
data class UserLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

// ================================================================
// HILFSFUNKTIONEN
// ================================================================

private fun Uri.toScaledBitmap(context: Context): Bitmap {
    val contentResolver = context.contentResolver
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(this)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }
    var inSampleSize = 1
    while (options.outHeight / inSampleSize > 800 || options.outWidth / inSampleSize > 800) {
        inSampleSize *= 2
    }
    val finalOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
    return contentResolver.openInputStream(this)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, finalOptions)
    } ?: throw Exception("Konnte Bild nicht laden")
}

@Composable
fun StatusButton(icon: ImageVector, text: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) color else ThubDarkGray
    val contentColor = if (isSelected) Color.Black else color
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(vertical = 8.dp, horizontal = 12.dp)
            .width(70.dp)
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = contentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = text, color = contentColor, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(text = title, color = ThubNeonBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
    }
}

// NEU: Thub-Style Toggle Switch
@Composable
fun ThubToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    description: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ThubDarkGray.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = TextWhite,
                fontWeight = FontWeight.Medium
            )
            if (description != null) {
                Text(
                    text = description,
                    color = TextWhite.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ThubNeonBlue,
                checkedTrackColor = ThubNeonBlue.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
            )
        )
    }
}

// NEU: Thub-Style Dropdown
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThubDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = ThubNeonBlue) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThubNeonBlue,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(ThubDarkGray)
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption, color = TextWhite) },
                    onClick = {
                        onValueChange(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

// NEU: Statistik-Item
@Composable
fun StatisticItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ThubDarkGray.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = TextWhite.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = value,
                color = TextWhite,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ReadOnlyField(label: String, value: String) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        enabled = false,
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Gray) },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = TextWhite.copy(alpha = 0.7f),
            disabledBorderColor = Color.Gray.copy(alpha = 0.5f),
            disabledLabelColor = Color.Gray,
            disabledLeadingIconColor = Color.Gray
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun ThubProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = ThubNeonBlue) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ThubNeonBlue,
            unfocusedBorderColor = Color.Gray,
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            cursorColor = ThubNeonBlue
        ),
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        shape = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailerDropdown(selectedValue: String, onValueChange: (String) -> Unit) {
    val trailerOptions = listOf("Planenauflieger", "Kofferauflieger", "Tankwagen", "Kipper", "Autotransporter", "Schwerlast", "Container")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text("Auflieger / Aufbau", color = ThubNeonBlue) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThubNeonBlue,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            trailerOptions.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onValueChange(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ================================================================
// HAUPT-SCREEN
// ================================================================

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val userId = auth.currentUser?.uid

    // Helper: ImageLoader mit permissivem SSL für eingeschränkte Zertifikate
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .okHttpClient { SecureHttpClient.createImageLoadingClient(context) }
            .crossfade(true)
            .build()
    }

    // States
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var funkName by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var truckBrand by remember { mutableStateOf("") }
    var trailerType by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var currentStatus by remember { mutableStateOf("Fahrbereit") }
    var truckLength by remember { mutableStateOf("") }
    var truckType by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }

    // NEU: Einstellungen States
    var shareLocation by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("de") }
    var darkMode by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    var isUploading by remember { mutableStateOf(false) }

    // Ampel-Logik
    val statusColor = when (currentStatus) {
        "Fahrbereit" -> Color.Green
        "Beschäftigt", "Laden/Entl." -> Color(0xFFFFA500)
        "Pause", "Kaffee" -> ThubNeonBlue
        else -> ThubNeonBlue
    }

    // Bild-Upload
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                isUploading = true
                withContext(Dispatchers.IO) {
                    try {
                        val bitmap = it.toScaledBitmap(context)
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                        val imageBytes = outputStream.toByteArray()

                        val client = SecureHttpClient.createSecureClient(context, enableLogging = true)

                        val requestBody = MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("key", SECRET_KEY)
                            .addFormDataPart(
                                "image",
                                "profile_${userId ?: "unknown"}.jpg",
                                imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                            )
                            .build()

                        val request = Request.Builder()
                            .url(UPLOAD_URL)
                            .post(requestBody)
                            .build()

                        val response = client.newCall(request).execute()

                        if (!response.isSuccessful) {
                            throw Exception("Upload-Fehler: ${response.code}")
                        }

                        val responseBody = response.body?.string() ?: throw Exception("Leere Antwort vom Server")
                        val jsonResponse = JSONObject(responseBody)

                        if (jsonResponse.getString("status") == "success") {
                            val newUrl = jsonResponse.getString("url")
                            if (userId != null) {
                                firestore.collection("users").document(userId)
                                    .update("profileImageUrl", newUrl)
                            }
                            profileImageUrl = newUrl
                        } else {
                            throw Exception(jsonResponse.getString("message"))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Upload fehlgeschlagen: ${e.message}")
                        }
                    } finally {
                        isUploading = false
                    }
                }
            }
        }
    }

    // Daten laden
    LaunchedEffect(userId) {
        if (userId != null) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val profile = document.toObject(UserProfile::class.java)
                        userProfile = profile
                        profile?.let {
                            funkName = it.funkName
                            company = it.company
                            truckBrand = it.truckBrand
                            trailerType = it.trailerType
                            bio = it.bio
                            currentStatus = it.status
                            profileImageUrl = it.profileImageUrl
                            truckLength = it.truckLength
                            truckType = it.truckType
                            // NEU: Einstellungen laden
                            shareLocation = it.shareLocation
                            language = it.language
                            darkMode = it.darkMode
                            notificationsEnabled = it.notificationsEnabled
                        }
                    }
                }
        }
    }

    // Speicher-Logik
    val saveChanges: () -> Unit = {
        if (userId != null) {
            val currentProfile = userProfile ?: UserProfile()
            val updatedData = currentProfile.copy(
                funkName = funkName,
                company = company,
                truckBrand = truckBrand,
                trailerType = trailerType,
                bio = bio,
                status = currentStatus,
                truckLength = truckLength,
                truckType = truckType,
                profileImageUrl = profileImageUrl.ifEmpty { currentProfile.profileImageUrl },
                // NEU: Einstellungen speichern
                shareLocation = shareLocation,
                language = language,
                darkMode = darkMode,
                notificationsEnabled = notificationsEnabled
            )

            firestore.collection("users").document(userId).set(updatedData)
                .addOnSuccessListener {
                    userProfile = updatedData
                    coroutineScope.launch { snackbarHostState.showSnackbar("Profil erfolgreich gespeichert! ✅") }
                }
                .addOnFailureListener { e ->
                    coroutineScope.launch { snackbarHostState.showSnackbar("Fehler: ${e.message} ❌") }
                }
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar("Fehler: Nicht eingeloggt!") }
        }
    }

    // UI
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Image(
                painter = painterResource(id = R.drawable.thub_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("MEIN PROFIL", style = MaterialTheme.typography.headlineMedium, color = ThubNeonBlue, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = profileImageUrl.ifEmpty { R.drawable.thub_background },
                        imageLoader = imageLoader,
                        contentDescription = "Profilbild",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(3.dp, statusColor, CircleShape)
                            .clickable(enabled = !isUploading) { imagePickerLauncher.launch("image/*") },
                        error = painterResource(R.drawable.thub_background)
                    )
                    if (isUploading) {
                        CircularProgressIndicator(color = ThubNeonBlue)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${userProfile?.firstName ?: ""} ${userProfile?.lastName ?: ""}",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader("AKTUELLER STATUS")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatusButton(Icons.Filled.DriveEta, "Fahrbereit", Color.Green, currentStatus == "Fahrbereit") { currentStatus = "Fahrbereit" }
                    StatusButton(Icons.Filled.Work, "Laden/Entl.", Color(0xFFFFA500), currentStatus == "Laden/Entl.") { currentStatus = "Laden/Entl." }
                    StatusButton(Icons.Filled.Coffee, "Pause", ThubNeonBlue, currentStatus == "Pause") { currentStatus = "Pause" }
                }
                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader("IDENTITÄT")
                ReadOnlyField("Vorname", userProfile?.firstName ?: "")
                ReadOnlyField("Nachname", userProfile?.lastName ?: "")
                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader("MEIN RIGG & ICH 🛠️")
                ThubProfileTextField(funkName, { funkName = it }, "Funkname")
                Spacer(modifier = Modifier.height(8.dp))
                ThubProfileTextField(company, { company = it }, "Firma")
                Spacer(modifier = Modifier.height(16.dp))
                TrailerDropdown(trailerType) { trailerType = it }
                Spacer(modifier = Modifier.height(8.dp))
                ThubProfileTextField(bio, { bio = it }, "Über mich", singleLine = false)
                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader("MEIN TRUCK 🚚")
                ThubProfileTextField(truckLength, { truckLength = it }, "LKW Gesamtlänge (m)", keyboardType = KeyboardType.Number)
                Spacer(modifier = Modifier.height(8.dp))
                ThubProfileTextField(truckType, { truckType = it }, "LKW Typ (z.B. Sattelzug)")
                Spacer(modifier = Modifier.height(24.dp))

                // NEU: EINSTELLUNGEN ⚙️
                SectionHeader("EINSTELLUNGEN ⚙️")
                ThubDropdown(
                    label = "Sprache / Language",
                    selectedValue = if (language == "de") "Deutsch" else "English",
                    options = listOf("Deutsch", "English"),
                    onValueChange = { newLanguage ->
                        language = if (newLanguage == "Deutsch") "de" else "en"
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ThubToggleSwitch(
                    checked = shareLocation,
                    onCheckedChange = { shareLocation = it },
                    label = "Standort freigeben",
                    description = "Zeige deinen Standort Freunden auf der Karte"
                )
                Spacer(modifier = Modifier.height(8.dp))
                ThubToggleSwitch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                    label = "Benachrichtigungen",
                    description = "Erhalte Toast-Nachrichten bei neuen Ereignissen"
                )
                Spacer(modifier = Modifier.height(8.dp))
                ThubToggleSwitch(
                    checked = darkMode,
                    onCheckedChange = { darkMode = it },
                    label = "Dunkler Modus",
                    description = "Nutze dunkles Theme"
                )
                Spacer(modifier = Modifier.height(24.dp))

                // NEU: STATISTIKEN 📊
                SectionHeader("MEINE STATISTIKEN 📊")
                StatisticItem(
                    icon = Icons.Filled.LocalParking,
                    label = "Parkplätze genutzt",
                    value = (userProfile?.totalParkings ?: 0).toString(),
                    color = Color(0xFFFFA500)
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatisticItem(
                    icon = Icons.Filled.Star,
                    label = "Bewertungen abgegeben",
                    value = (userProfile?.totalRatings ?: 0).toString(),
                    color = Color.Yellow
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatisticItem(
                    icon = Icons.Filled.Group,
                    label = "Freunde",
                    value = (userProfile?.totalFriends ?: 0).toString(),
                    color = ThubNeonBlue
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatisticItem(
                    icon = Icons.Filled.Streetview,
                    label = "Ampel-Updates",
                    value = (userProfile?.ampelUpdates ?: 0).toString(),
                    color = Color.Green
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatisticItem(
                    icon = Icons.Filled.Cake,
                    label = "Mitglied seit",
                    value = formatDate(userProfile?.createdAt ?: 0),
                    color = Color(0xFFFF1493)
                )
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = saveChanges,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Änderungen speichern", fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}