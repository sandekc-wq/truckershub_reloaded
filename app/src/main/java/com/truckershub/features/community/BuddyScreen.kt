package com.truckershub.features.community

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.automirrored.filled.Message // <--- Neu für Chat Icon
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.truckershub.R
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.data.model.User

// Datenmodell für eine Anfrage
data class FriendRequest(
    val id: String,
    val fromId: String,
    val toId: String,
    val status: String,
    val timestamp: Any?
)

@Composable
fun BuddyScreen(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val userId = auth.currentUser?.uid ?: return
    var selectedTab by remember { mutableIntStateOf(0) }

    // NEU: State, um zu merken, mit wem wir chatten wollen
    var chatPartnerId by remember { mutableStateOf<String?>(null) }

    // WENN WIR EINEN CHAT-PARTNER HABEN, ZEIGEN WIR DEN CHAT SCREEN
    if (chatPartnerId != null) {
        ChatScreen(
            friendId = chatPartnerId!!,
            onBack = { chatPartnerId = null } // Zurück zur Liste
        )
    } else {
        // SONST ZEIGEN WIR DIE NORMALE LISTE
        Column(modifier = Modifier.fillMaxSize().background(ThubBlack)) {

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ThubBlack,
                contentColor = ThubNeonBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = ThubNeonBlue
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Postkasten 📬", fontWeight = FontWeight.Bold) },
                    selectedContentColor = ThubNeonBlue,
                    unselectedContentColor = Color.Gray
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Meine Crew 🚛", fontWeight = FontWeight.Bold) },
                    selectedContentColor = ThubNeonBlue,
                    unselectedContentColor = Color.Gray
                )
            }

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (selectedTab == 0) {
                    PostboxList(userId, firestore)
                } else {
                    // Wir übergeben jetzt die Funktion zum Chatten
                    CrewList(userId, firestore, onChatClick = { friendId ->
                        chatPartnerId = friendId
                    })
                }
            }
        }
    }
}

@Composable
fun PostboxList(myUserId: String, firestore: FirebaseFirestore) {
    var requests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        firestore.collection("friend_requests")
            .whereEqualTo("toId", myUserId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    requests = snap.documents.map { doc ->
                        FriendRequest(
                            id = doc.id,
                            fromId = doc.getString("fromId") ?: "",
                            toId = doc.getString("toId") ?: "",
                            status = doc.getString("status") ?: "",
                            timestamp = doc.get("timestamp")
                        )
                    }
                    isLoading = false
                }
            }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ThubNeonBlue)
        }
    } else if (requests.isEmpty()) {
        EmptyState(Icons.Default.MarkEmailUnread, "Keine neuen Anfragen.\nAlles ruhig im Funk!")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(requests) { request ->
                RequestCard(request, firestore)
            }
        }
    }
}

@Composable
fun CrewList(myUserId: String, firestore: FirebaseFirestore, onChatClick: (String) -> Unit) {
    var friends by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val list = mutableListOf<String>()
        firestore.collection("friend_requests")
            .whereEqualTo("toId", myUserId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { snap1 ->
                snap1.documents.forEach { list.add(it.getString("fromId") ?: "") }
                firestore.collection("friend_requests")
                    .whereEqualTo("fromId", myUserId)
                    .whereEqualTo("status", "accepted")
                    .get()
                    .addOnSuccessListener { snap2 ->
                        snap2.documents.forEach { list.add(it.getString("toId") ?: "") }
                        friends = list.distinct()
                        isLoading = false
                    }
            }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ThubNeonBlue)
        }
    } else if (friends.isEmpty()) {
        EmptyState(Icons.Default.Group, "Deine Crew ist noch leer.\nSuch dir Buddies auf der Karte!")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(friends) { friendId ->
                FriendCard(friendId, firestore, onClick = { onChatClick(friendId) })
            }
        }
    }
}

@Composable
fun RequestCard(request: FriendRequest, firestore: FirebaseFirestore) {
    var senderUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(request.fromId) {
        firestore.collection("users").document(request.fromId).get().addOnSuccessListener {
            senderUser = it.toObject(User::class.java)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, ThubNeonBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = senderUser?.profileImageUrl?.ifEmpty { R.drawable.thub_logo_bg },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(60.dp).clip(CircleShape).border(2.dp, ThubNeonBlue, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(senderUser?.funkName ?: "Lädt...", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("möchte dein Buddy sein!", color = ThubNeonBlue, fontSize = 12.sp)
            }
            Row {
                IconButton(onClick = { firestore.collection("friend_requests").document(request.id).delete() }) {
                    Icon(Icons.Default.Close, null, tint = Color.Red)
                }
                IconButton(onClick = { firestore.collection("friend_requests").document(request.id).update("status", "accepted") }) {
                    Icon(Icons.Default.Check, null, tint = Color.Green)
                }
            }
        }
    }
}

@Composable
fun FriendCard(friendId: String, firestore: FirebaseFirestore, onClick: () -> Unit) {
    var friendUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(friendId) {
        firestore.collection("users").document(friendId).get().addOnSuccessListener {
            friendUser = it.toObject(User::class.java)
        }
    }

    // Die ganze Karte ist jetzt klickbar -> Öffnet Chat
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // <--- HIER PASSIERT DIE MAGIE
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = friendUser?.profileImageUrl?.ifEmpty { R.drawable.thub_logo_bg },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(50.dp).clip(CircleShape).border(1.dp, Color.Gray, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(friendUser?.funkName ?: "Laden...", color = TextWhite, fontWeight = FontWeight.Bold)

                val status = friendUser?.status ?: "Unbekannt"
                val statusColor = when(status) {
                    "Fahrbereit" -> Color.Green
                    "Pause" -> ThubNeonBlue
                    else -> Color.Gray
                }
                Text("• $status", color = statusColor, fontSize = 12.sp)
            }
            // Kleines Chat Icon rechts
            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Chat", tint = ThubNeonBlue)
        }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = ThubDarkGray, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}