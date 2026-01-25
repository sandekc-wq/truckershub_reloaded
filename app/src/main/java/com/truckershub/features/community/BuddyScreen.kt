package com.truckershub.features.community

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckershub.R
import com.truckershub.core.design.ThubBackground
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.ThubRed
import com.truckershub.core.design.TextWhite

data class BuddyItem(
    val id: String,
    val name: String,
    val statusText: String,
    val isOnline: Boolean,
    val type: BuddyType
)

enum class BuddyType {
    CHAT, REQUEST, FRIEND
}

@Composable
fun BuddyScreen(
    onChatClick: (String) -> Unit = {}
) {
    val context = LocalContext.current // Für die Diagnose-Nachricht
    var selectedTab by remember { mutableIntStateOf(0) }
    val allBuddies = remember { mutableStateListOf<BuddyItem>() }

    ThubBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.thub_logo_bg),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(300.dp).alpha(0.1f)
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // TEST BUTTON
                Button(
                    onClick = {
                        // Verhindern, dass K.I.T.T. doppelt kommt
                        if (allBuddies.none { it.name == "K.I.T.T." }) {
                            allBuddies.add(0, BuddyItem("kitt", "K.I.T.T.", "Ich brauche Hilfe, Michael!", true, BuddyType.REQUEST))
                            selectedTab = 0
                            Toast.makeText(context, "K.I.T.T. ruft an!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThubDarkGray),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(40.dp)
                ) {
                    Text("TEST: Anfrage von K.I.T.T. simulieren", color = ThubNeonBlue)
                }

                // TABS
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = ThubNeonBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ThubNeonBlue
                        )
                    },
                    divider = { HorizontalDivider(color = ThubDarkGray) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("CHATS / FUNK", fontWeight = FontWeight.Bold)
                                val requestCount = allBuddies.count { it.type == BuddyType.REQUEST }
                                if (requestCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(modifier = Modifier.size(8.dp).background(ThubRed, CircleShape))
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("MEINE BUDDYS", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // LISTE
                val filteredList = if (selectedTab == 0) {
                    allBuddies.filter { it.type == BuddyType.REQUEST || it.type == BuddyType.CHAT }
                } else {
                    allBuddies.filter { it.type == BuddyType.FRIEND || it.type == BuddyType.CHAT }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredList.isEmpty()) {
                        item { EmptyStateMessage(selectedTab) }
                    } else {
                        items(filteredList) { buddy ->
                            BuddyCard(
                                buddy = buddy,
                                onAccept = {
                                    val index = allBuddies.indexOf(buddy)
                                    if (index != -1) {
                                        // Status ändern zu CHAT
                                        allBuddies[index] = buddy.copy(type = BuddyType.CHAT, statusText = "Verbunden.")
                                        Toast.makeText(context, "${buddy.name} akzeptiert!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDecline = { allBuddies.remove(buddy) },
                                onClick = {
                                    // HIER IST DIE DIAGNOSE 💡
                                    if (buddy.type == BuddyType.CHAT || buddy.type == BuddyType.FRIEND) {
                                        Toast.makeText(context, "Öffne Chat mit ${buddy.name}...", Toast.LENGTH_SHORT).show()
                                        onChatClick(buddy.name)
                                    } else {
                                        Toast.makeText(context, "Bitte erst Anfrage annehmen!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuddyCard(buddy: BuddyItem, onAccept: () -> Unit, onDecline: () -> Unit, onClick: () -> Unit) {
    val borderColor = when(buddy.type) {
        BuddyType.REQUEST -> Color(0xFFFFA500)
        BuddyType.CHAT -> ThubNeonBlue
        BuddyType.FRIEND -> ThubDarkGray
    }

    // Die Karte ist klickbar, ABER nur wenn es keine Anfrage ist.
    // Wenn es eine Anfrage ist, müssen wir erst die Buttons drücken.
    Card(
        colors = CardDefaults.cardColors(containerColor = ThubDarkGray.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            // Hier passiert die Magie: Der Klick wird weitergeleitet
            .clickable(enabled = buddy.type != BuddyType.REQUEST, onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(modifier = Modifier.size(50.dp).background(ThubBlack, CircleShape).border(1.dp, if(buddy.isOnline) Color.Green else Color.Gray, CircleShape), contentAlignment = Alignment.Center) {
                    Text(text = buddy.name.take(1), color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                if (buddy.isOnline) {
                    Box(modifier = Modifier.size(12.dp).background(Color.Green, CircleShape).border(2.dp, ThubBlack, CircleShape).align(Alignment.BottomEnd))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = buddy.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (buddy.type == BuddyType.REQUEST) {
                    Text(text = "📩 Möchte dich adden!", color = Color(0xFFFFA500), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(text = buddy.statusText, color = Color.Gray, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            // Buttons für Anfrage
            if (buddy.type == BuddyType.REQUEST) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(ThubRed.copy(alpha = 0.2f)).clickable(onClick = onDecline), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, null, tint = ThubRed, modifier = Modifier.size(18.dp))
                    }
                    Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.Green.copy(alpha = 0.2f)).clickable(onClick = onAccept), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, null, tint = Color.Green, modifier = Modifier.size(18.dp))
                    }
                }
            } else if (buddy.type == BuddyType.CHAT) {
                Icon(Icons.Default.ChatBubbleOutline, null, tint = ThubNeonBlue)
            }
        }
    }
}

@Composable
fun EmptyStateMessage(tabIndex: Int) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = if (tabIndex == 0) Icons.Default.Inbox else Icons.Default.GroupOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(60.dp).alpha(0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = if (tabIndex == 0) "Keine aktiven Chats oder Anfragen." else "Deine Freundesliste ist noch leer.", color = Color.Gray, fontSize = 16.sp)
        if (tabIndex == 1) {
            Text(text = "Suche Fahrer auf der Karte!", color = ThubNeonBlue, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}