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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha // <--- WICHTIG
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle // <--- DER HAT GEFEHLT!
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.ImageLoader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.truckershub.R
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.data.model.User
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

// --- HELPER ---
private const val UPLOAD_URL = "https://inetfacts.de/thub_api/upload_thub.php"
private const val SECRET_KEY = "LKW_V8_POWER"

private fun Uri.toScaledBitmap(context: Context): Bitmap {
    val contentResolver = context.contentResolver
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(this)?.use { stream -> BitmapFactory.decodeStream(stream, null, options) }
    var inSampleSize = 1
    while (options.outHeight / inSampleSize > 800 || options.outWidth / inSampleSize > 800) { inSampleSize *= 2 }
    val finalOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
    return contentResolver.openInputStream(this)?.use { stream -> BitmapFactory.decodeStream(stream, null, finalOptions) } ?: throw Exception("Bild Fehler")
}

// ================================================================
// DESIGN COMPONENTS
// ================================================================

@Composable
fun StatusButtonBig(icon: ImageVector, text: String, color: Color, isSelected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) color else Color(0xFF1E1E1E)
    val contentColor = if (isSelected) Color.Black else TextWhite.copy(alpha = 0.7f)
    val borderStroke = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = borderStroke,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(105.dp)
            .height(90.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .shadow(if (isSelected) 10.dp else 0.dp, RoundedCornerShape(16.dp), spotColor = color)
            .alpha(if (enabled) 1f else 0.6f) // <--- HIER REPARIERT (Einfacher & Sauberer)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = text, tint = contentColor, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = text, color = contentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ThubStatRow(icon: ImageVector, label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
            .border(1.dp, ThubDarkGray, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(value, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

// ================================================================
// MAIN SCREEN
// ================================================================

@Composable
fun ProfileScreen(
    targetUserId: String? = null,
    onBackClick: () -> Unit,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val myUid = auth.currentUser?.uid
    val userIdToLoad = targetUserId ?: myUid
    val isOwnProfile = (userIdToLoad == myUid)

    var userProfile by remember { mutableStateOf<User?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(userIdToLoad) {
        if (userIdToLoad != null) {
            firestore.collection("users").document(userIdToLoad).addSnapshotListener { document, _ ->
                if (document != null && document.exists()) userProfile = document.toObject(User::class.java)
            }
        }
    }

    if (isEditing && isOwnProfile) {
        ProfileEditScreen(user = userProfile, userId = myUid, onBack = { isEditing = false }, onSave = { isEditing = false })
    } else {
        ProfileViewScreen(
            user = userProfile,
            userId = userIdToLoad,
            isOwnProfile = isOwnProfile,
            onBackClick = onBackClick,
            onEditClick = { isEditing = true },
            firestore = firestore
        )
    }
}

// ================================================================
// VIEW MODE
// ================================================================

@Composable
fun ProfileViewScreen(
    user: User?,
    userId: String?,
    isOwnProfile: Boolean,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    firestore: FirebaseFirestore
) {
    val context = LocalContext.current
    val imageLoader = remember { ImageLoader.Builder(context).okHttpClient { SecureHttpClient.createImageLoadingClient(context) }.crossfade(true).build() }

    val funkName = user?.funkName?.ifBlank { "Unbekannt" } ?: "Laden..."
    val currentStatus = user?.status ?: "Fahrbereit"
    val statusColor = when (currentStatus) {
        "Fahrbereit" -> Color.Green
        "Laden/Entl." -> Color(0xFFFFA500)
        "Pause" -> ThubNeonBlue
        else -> ThubNeonBlue
    }

    fun updateStatus(newStatus: String) {
        if (isOwnProfile && userId != null) {
            firestore.collection("users").document(userId).update("status", newStatus)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.thub_background), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(8.dp))
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)))

        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = TextWhite) }
                Spacer(modifier = Modifier.weight(1f))
                Text(if(isOwnProfile) "MEIN PROFIL" else "PROFIL ANSICHT", color = ThubNeonBlue, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(contentAlignment = Alignment.TopCenter) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 50.dp).border(1.dp, ThubNeonBlue.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(top = 60.dp, start = 16.dp, end = 16.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(funkName, style = MaterialTheme.typography.headlineMedium, color = ThubNeonBlue, fontWeight = FontWeight.Black)
                        Text(user?.company ?: "Keine Firma", color = Color.Gray, fontSize = 14.sp)

                        if (!user?.bio.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("\"${user?.bio}\"", color = Color.White.copy(alpha = 0.8f), fontStyle = FontStyle.Italic, fontSize = 14.sp) // <--- JETZT GEHT DAS!
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.background(ThubDarkGray.copy(alpha = 0.5f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Icon(Icons.Filled.LocalShipping, null, tint = TextWhite, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("${user?.truckBrand ?: "-"} • ${user?.trailerType ?: "-"}", color = TextWhite, fontSize = 14.sp)
                        }
                    }
                }
                AsyncImage(
                    model = user?.profileImageUrl?.ifEmpty { R.drawable.thub_background }, imageLoader = imageLoader, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(100.dp).clip(CircleShape).border(3.dp, statusColor, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("AKTUELLER STATUS", color = ThubNeonBlue, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusButtonBig(Icons.Filled.DriveEta, "Fahrbereit", Color.Green, currentStatus == "Fahrbereit", enabled = isOwnProfile) { updateStatus("Fahrbereit") }
                StatusButtonBig(Icons.Filled.Work, "Laden/Entl.", Color(0xFFFFA500), currentStatus == "Laden/Entl.", enabled = isOwnProfile) { updateStatus("Laden/Entl.") }
                StatusButtonBig(Icons.Filled.Coffee, "Pause", ThubNeonBlue, currentStatus == "Pause", enabled = isOwnProfile) { updateStatus("Pause") }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("STATISTIKEN 📊", color = ThubNeonBlue, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))
            ThubStatRow(Icons.Filled.LocalParking, "Parkplätze genutzt", (user?.stats?.totalParkings ?: 0).toString(), Color(0xFFFFA500))
            ThubStatRow(Icons.Filled.Star, "Bewertungen", (user?.stats?.totalRatings ?: 0).toString(), Color.Yellow)
            ThubStatRow(Icons.Filled.Group, "Freunde", (user?.stats?.totalFriends ?: 0).toString(), ThubNeonBlue)
            ThubStatRow(Icons.Filled.Traffic, "Ampel-Updates", (user?.stats?.ampelUpdates ?: 0).toString(), Color.Green)

            Spacer(modifier = Modifier.height(32.dp))

            if (isOwnProfile) {
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Edit, null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Profil & Einstellungen bearbeiten", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
            } else {
                Text("Profilansicht (Nur Lesen)", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ================================================================
// EDIT MODE
// ================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    user: User?,
    userId: String?,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    val snackbarHostState = remember { SnackbarHostState() }

    var funkName by remember { mutableStateOf(user?.funkName ?: "") }
    var company by remember { mutableStateOf(user?.company ?: "") }
    var truckBrand by remember { mutableStateOf(user?.truckBrand ?: "") }
    var trailerType by remember { mutableStateOf(user?.trailerType ?: "Planenauflieger") }
    var bio by remember { mutableStateOf(user?.bio ?: "") }
    var truckLength by remember { mutableStateOf(user?.truckLength ?: "") }
    var truckType by remember { mutableStateOf(user?.truckType ?: "") }

    var shareLocation by remember { mutableStateOf(user?.preferences?.shareLocation ?: false) }
    var notificationsEnabled by remember { mutableStateOf(user?.preferences?.notifications ?: true) }
    var darkMode by remember { mutableStateOf(user?.preferences?.darkMode ?: false) }
    var language by remember { mutableStateOf(user?.preferences?.language ?: "de") }

    var profileImageUrl by remember { mutableStateOf(user?.profileImageUrl ?: "") }
    var isUploading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            coroutineScope.launch {
                isUploading = true
                withContext(Dispatchers.IO) {
                    try {
                        val bitmap = it.toScaledBitmap(context)
                        val outputStream = ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                        val client = SecureHttpClient.createSecureClient(context, true)
                        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("key", SECRET_KEY).addFormDataPart("image", "profile_${userId}.jpg", outputStream.toByteArray().toRequestBody("image/jpeg".toMediaTypeOrNull())).build()
                        val request = Request.Builder().url(UPLOAD_URL).post(requestBody).build()
                        val json = JSONObject(client.newCall(request).execute().body?.string() ?: "")
                        if (json.getString("status") == "success") profileImageUrl = json.getString("url")
                    } catch (e: Exception) { /* Err */ } finally { isUploading = false }
                }
            }
        }
    }

    Scaffold(containerColor = Color.Black, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {

            Text("PROFIL EDITIEREN", color = ThubNeonBlue, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    AsyncImage(model = profileImageUrl.ifEmpty { R.drawable.thub_background }, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(80.dp).clip(CircleShape).border(2.dp, ThubNeonBlue, CircleShape).clickable { imagePickerLauncher.launch("image/*") })
                    if (isUploading) CircularProgressIndicator(modifier = Modifier.size(30.dp), color = ThubNeonBlue)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Tippe für neues Bild", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("IDENTITÄT", color = ThubNeonBlue, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = user?.firstName ?: "", onValueChange = {}, enabled = false, label = { Text("Vorname") }, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Gray, disabledBorderColor = ThubDarkGray), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = user?.lastName ?: "", onValueChange = {}, enabled = false, label = { Text("Nachname") }, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Gray, disabledBorderColor = ThubDarkGray), modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            ThubEditField(funkName, { funkName = it }, "Funkname")
            ThubEditField(company, { company = it }, "Firma")
            ThubEditField(bio, { bio = it }, "Über mich", singleLine = false)

            Spacer(modifier = Modifier.height(16.dp))

            Text("MEIN TRUCK 🚛", color = ThubNeonBlue, fontWeight = FontWeight.Bold)
            ThubEditField(truckBrand, { truckBrand = it }, "LKW Marke")

            var expanded by remember { mutableStateOf(false) }
            val options = listOf("Planenauflieger", "Kofferauflieger", "Tankwagen", "Kipper", "Autotransporter", "Sattel", "Schwerlast")

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = trailerType, onValueChange = {}, readOnly = true, label = { Text("Auflieger / Aufbau", color = ThubNeonBlue) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThubNeonBlue, unfocusedBorderColor = Color.Gray, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { opt -> DropdownMenuItem(text = { Text(opt) }, onClick = { trailerType = opt; expanded = false }) }
                }
            }

            ThubEditField(truckLength, { truckLength = it }, "LKW Gesamtlänge (m)", keyboardType = KeyboardType.Number)
            ThubEditField(truckType, { truckType = it }, "LKW Typ (z.B. Sattelzug)")

            Spacer(modifier = Modifier.height(16.dp))

            Text("EINSTELLUNGEN ⚙️", color = ThubNeonBlue, fontWeight = FontWeight.Bold)

            var langExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = langExpanded, onExpandedChange = { langExpanded = !langExpanded }) {
                OutlinedTextField(
                    value = if(language == "de") "Deutsch" else "English", onValueChange = {}, readOnly = true, label = { Text("Sprache / Language", color = ThubNeonBlue) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThubNeonBlue, unfocusedBorderColor = Color.Gray, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                    DropdownMenuItem(text = { Text("Deutsch") }, onClick = { language = "de"; langExpanded = false })
                    DropdownMenuItem(text = { Text("English") }, onClick = { language = "en"; langExpanded = false })
                }
            }

            ThubSwitch(shareLocation, { shareLocation = it }, "Standort freigeben", "Zeige deinen Standort Freunden auf der Karte")
            ThubSwitch(notificationsEnabled, { notificationsEnabled = it }, "Benachrichtigungen", "Erhalte Toast-Nachrichten")
            ThubSwitch(darkMode, { darkMode = it }, "Dunkler Modus", "Nutze dunkles Theme")

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (userId != null) {
                        val safeUser = user?.copy()
                        val updatedPrefs = safeUser?.preferences?.copy(shareLocation = shareLocation, notifications = notificationsEnabled, darkMode = darkMode, language = language)

                        val updates = hashMapOf<String, Any>(
                            "funkName" to funkName, "company" to company, "truckBrand" to truckBrand,
                            "trailerType" to trailerType, "bio" to bio, "truckLength" to truckLength,
                            "truckType" to truckType, "profileImageUrl" to profileImageUrl,
                            "preferences.shareLocation" to shareLocation,
                            "preferences.notifications" to notificationsEnabled,
                            "preferences.darkMode" to darkMode,
                            "preferences.language" to language
                        )
                        firestore.collection("users").document(userId).update(updates).addOnSuccessListener { onSave() }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Änderungen speichern", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Abbrechen", color = Color.Gray) }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun ThubEditField(value: String, onValueChange: (String) -> Unit, label: String, singleLine: Boolean = true, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = ThubNeonBlue) },
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThubNeonBlue, unfocusedBorderColor = Color.Gray, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = singleLine,
        maxLines = if(singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
fun ThubSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, subLabel: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextWhite, fontWeight = FontWeight.Bold)
            Text(subLabel, color = Color.Gray, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = ThubNeonBlue, checkedTrackColor = ThubNeonBlue.copy(alpha = 0.5f)))
    }
}