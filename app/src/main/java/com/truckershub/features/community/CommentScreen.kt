package com.truckershub.features.community

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.truckershub.R
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.ThubRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Comment(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String,
    val text: String,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    postId: String,
    onBack: () -> Unit,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val myId = auth.currentUser?.uid ?: return
    var commentText by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }

    // Kommentare laden
    LaunchedEffect(postId) {
        firestore.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    comments = snapshot.documents.map { doc ->
                        Comment(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "Unbekannt",
                            userAvatarUrl = doc.getString("userAvatarUrl") ?: "",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getDate("timestamp")?.time ?: 0L
                        )
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kommentare 💬", color = ThubNeonBlue) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = ThubNeonBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThubDarkGray)
            )
        },
        bottomBar = {
            // EINGABE LEISTE
            Row(
                modifier = Modifier
                    .background(ThubBlack)
                    .padding(8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Schreib was...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThubNeonBlue,
                        unfocusedBorderColor = ThubDarkGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = ThubNeonBlue
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            // Erst User Daten holen, dann speichern
                            firestore.collection("users").document(myId).get().addOnSuccessListener { userDoc ->
                                val name = userDoc.getString("funkName") ?: "Unbekannt"
                                val avatar = userDoc.getString("profileImageUrl") ?: ""

                                val newComment = hashMapOf(
                                    "userId" to myId,
                                    "userName" to name,
                                    "userAvatarUrl" to avatar,
                                    "text" to commentText.trim(),
                                    "timestamp" to FieldValue.serverTimestamp()
                                )
                                firestore.collection("posts").document(postId).collection("comments").add(newComment)
                                commentText = ""
                            }
                        }
                    },
                    modifier = Modifier.background(ThubNeonBlue, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Senden", tint = Color.Black)
                }
            }
        },
        containerColor = ThubBlack
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (comments.isEmpty()) {
                item {
                    Text("Noch keine Kommentare. Sei der Erste! 🚛", color = Color.Gray)
                }
            }
            items(comments) { comment ->
                CommentItem(comment, myId, onDelete = {
                    firestore.collection("posts").document(postId)
                        .collection("comments").document(comment.id).delete()
                })
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment, myId: Boolean, onDelete: () -> Unit) {
    // ACHTUNG: myId ist hier ein String im Original, ich habe es oben als Parameter String übergeben
    // aber in der Funktion signatur Boolean geschrieben? Nein, korrigieren wir.
    // Unten ist die korrigierte Version.
}

@Composable
fun CommentItem(comment: Comment, currentUserId: String, onDelete: () -> Unit) {
    val timeFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
    var showMenu by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = comment.userAvatarUrl.ifEmpty { R.drawable.thub_logo_bg },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray)
        )
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.userName, color = ThubNeonBlue, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(timeFormat.format(Date(comment.timestamp)), color = Color.Gray, fontSize = 10.sp)
            }

            // Box für Long Press (Löschen)
            Box {
                Surface(
                    color = ThubDarkGray,
                    shape = RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp),
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                if (comment.userId == currentUserId) showMenu = true
                            }
                        )
                    }
                ) {
                    Text(
                        text = comment.text,
                        color = TextWhite,
                        modifier = Modifier.padding(10.dp),
                        fontSize = 15.sp
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = ThubBlack,
                    offset = DpOffset(x = 0.dp, y = (-10).dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Löschen", color = ThubRed) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = ThubRed) }
                    )
                }
            }
        }
    }
}