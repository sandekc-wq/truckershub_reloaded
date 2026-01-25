package com.truckershub.features.community

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckershub.R
import com.truckershub.core.design.ThubBackground
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.TextWhite

// Kleines Datenmodell nur für den Chat
data class ChatMessage(
    val id: String,
    val text: String,
    val isFromMe: Boolean,
    val time: String
)

@Composable
fun ChatScreen(
    buddyName: String, // Mit wem schreiben wir?
    onBack: () -> Unit
) {
    // Simulierte Nachrichten
    val messages = remember { mutableStateListOf(
        ChatMessage("1", "Hallo K.I.T.T., hörst du mich?", true, "10:00"),
        ChatMessage("2", "Ich höre dich laut und deutlich, Michael.", false, "10:01"),
        ChatMessage("3", "Wir müssen die Ladung pünktlich abliefern.", true, "10:02"),
        ChatMessage("4", "Keine Sorge. Mein Turbo-Boost ist bereit.", false, "10:02")
    ) }

    var inputText by remember { mutableStateOf("") }

    ThubBackground {
        Box(modifier = Modifier.fillMaxSize()) {

            // Branding im Hintergrund
            Image(
                painter = painterResource(id = R.drawable.thub_logo_bg),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(300.dp)
                    .alpha(0.1f)
            )

            Column(modifier = Modifier.fillMaxSize()) {

                // --- HEADER (Wie im Checklist Screen) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ThubBlack.copy(alpha = 0.8f)) // Leicht abgedunkelt oben
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = TextWhite
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Avatar (Klein)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ThubDarkGray, CircleShape)
                            .border(1.dp, Color.Green, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(buddyName.take(1), color = TextWhite, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = buddyName,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Online • Funkkanal 9", // Kleines Detail ;)
                            color = ThubNeonBlue,
                            fontSize = 12.sp
                        )
                    }
                }

                // --- NACHRICHTEN VERLAUF ---
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    reverseLayout = true // Neueste Nachrichten unten (wir drehen die Liste um)
                ) {
                    // Wir drehen die Liste beim Anzeigen um, damit die neueste unten ist
                    items(messages.reversed()) { msg ->
                        MessageBubble(msg)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // --- EINGABE LEISTE ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ThubBlack)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. PUSH-TO-TALK BUTTON (Mikrofon)
                    IconButton(
                        onClick = { /* TODO: Funk starten */ },
                        modifier = Modifier
                            .size(48.dp)
                            .background(ThubDarkGray, CircleShape)
                            .border(1.dp, ThubNeonBlue, CircleShape)
                    ) {
                        Icon(Icons.Default.Mic, "Funk", tint = ThubNeonBlue)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 2. TEXT EINGABE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(ThubDarkGray)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (inputText.isEmpty()) {
                            Text("Nachricht...", color = Color.Gray, fontSize = 14.sp)
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = TextStyle(color = TextWhite, fontSize = 16.sp),
                            cursorBrush = SolidColor(ThubNeonBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 3. SENDEN BUTTON
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                messages.add(ChatMessage("x", inputText, true, "Jetzt"))
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(ThubNeonBlue, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Senden", tint = ThubBlack)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage) {
    val align = if (msg.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (msg.isFromMe) ThubNeonBlue.copy(alpha = 0.2f) else ThubDarkGray
    val borderColor = if (msg.isFromMe) ThubNeonBlue else Color.Transparent
    val shape = if (msg.isFromMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .align(align)
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bgColor)
                .border(1.dp, borderColor, shape)
                .padding(12.dp)
        ) {
            Text(
                text = msg.text,
                color = TextWhite,
                fontSize = 16.sp
            )
            Text(
                text = msg.time,
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}