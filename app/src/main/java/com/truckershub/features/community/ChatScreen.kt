package com.truckershub.features.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.truckershub.core.data.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Datenmodell für eine Nachricht
data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val timestamp: Long
)

// HIER IST DER MUT-STEMPEL! 👇 Damit erlauben wir die experimentellen Designs
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    friendId: String,
    onBack: () -> Unit,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val myId = auth.currentUser?.uid ?: return

    // Wir erzeugen eine EINDEUTIGE Chat-Raum-ID für euch beide.
    val chatRoomId = if (myId < friendId) "${myId}_${friendId}" else "${friendId}_${myId}"

    var friendUser by remember { mutableStateOf<User?>(null) }
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }

    val listState = rememberLazyListState()

    // 1. Lade Infos über Lisa (oder den Freund)
    LaunchedEffect(friendId) {
        firestore.collection("users").document(friendId).get().addOnSuccessListener {
            friendUser = it.toObject(User::class.java)
        }
    }

    // 2. Lade Nachrichten (Echtzeit!)
    LaunchedEffect(chatRoomId) {
        firestore.collection("chats").document(chatRoomId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    messages = snapshot.documents.map { doc ->
                        ChatMessage(
                            id = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getDate("timestamp")?.time ?: 0L
                        )
                    }
                }
            }
    }

    // Auto-Scroll nach unten, wenn neue Nachricht kommt
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = friendUser?.profileImageUrl?.ifEmpty { R.drawable.thub_logo_bg },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(friendUser?.funkName ?: "Laden...", color = TextWhite, style = MaterialTheme.typography.titleMedium)
                            Text(friendUser?.status ?: "Offline", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
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
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Nachricht an ${friendUser?.funkName}...", color = Color.Gray) },
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
                        if (messageText.isNotBlank()) {
                            val msgData = hashMapOf(
                                "senderId" to myId,
                                "text" to messageText.trim(),
                                "timestamp" to FieldValue.serverTimestamp()
                            )
                            // Ab in die Datenbank damit!
                            firestore.collection("chats").document(chatRoomId).collection("messages").add(msgData)
                            messageText = ""
                        }
                    },
                    modifier = Modifier.background(ThubNeonBlue, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Senden", tint = Color.Black)
                }
            }
        }
    ) { padding ->
        // NACHRICHTEN LISTE
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(ThubBlack)
                .padding(padding)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.senderId == myId
                ChatBubble(message = msg, isMe = isMe)
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, isMe: Boolean) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            ContainerBox(isMe) {
                Text(
                    text = message.text,
                    color = if (isMe) Color.Black else TextWhite,
                    fontSize = 16.sp
                )
            }
            Text(
                text = timeFormat.format(Date(message.timestamp)),
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun ContainerBox(isMe: Boolean, content: @Composable () -> Unit) {
    Surface(
        color = if (isMe) ThubNeonBlue else ThubDarkGray,
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isMe) 16.dp else 0.dp,
            bottomEnd = if (isMe) 0.dp else 16.dp
        ),
        modifier = Modifier.widthIn(max = 280.dp)
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}