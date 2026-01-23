package com.truckershub.features.feed

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubNeonBlue

@Composable
fun AddPostDialog(
    onDismiss: () -> Unit,
    // WICHTIG: Das hier hat gefehlt! Jetzt nehmen wir Text UND Bild entgegen 👇
    onSend: (String, Uri?) -> Unit,
    // WICHTIG: Das hier hat auch gefehlt! 👇
    isUploading: Boolean
) {
    var text by remember { mutableStateOf("") }
    // Hier merken wir uns das ausgewählte Bild
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Der Foto-Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    Dialog(onDismissRequest = { if (!isUploading) onDismiss() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ThubBlack),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThubNeonBlue, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("NEUER POST ⛽", color = ThubNeonBlue, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                // Textfeld
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Was gibts Neues?", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThubNeonBlue,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    enabled = !isUploading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // BILD VORSCHAU (Wenn eins ausgewählt ist)
                if (selectedImageUri != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Vorschau",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )
                        // Button zum Entfernen des Bildes
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // BUTTONS UNTEN
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // FOTO BUTTON 📸
                    TextButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isUploading && selectedImageUri == null
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = ThubNeonBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Foto", color = ThubNeonBlue)
                    }

                    Row {
                        TextButton(onClick = onDismiss, enabled = !isUploading) {
                            Text("Abbrechen", color = Color.Gray)
                        }

                        // SENDEN BUTTON
                        Button(
                            onClick = { onSend(text, selectedImageUri) }, // Gibt beides zurück!
                            colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue),
                            enabled = (text.isNotBlank() || selectedImageUri != null) && !isUploading
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = ThubBlack,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Text("SENDEN", color = ThubBlack)
                            }
                        }
                    }
                }
            }
        }
    }
}