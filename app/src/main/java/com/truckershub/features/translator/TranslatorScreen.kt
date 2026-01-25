package com.truckershub.features.translator

// WICHTIG: Hier sind die Werkzeuge (Imports)
import androidx.compose.foundation.BorderStroke // <--- Das hatte gefehlt!
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckershub.R
import com.truckershub.core.design.ThubBackground
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.TextWhite

// Datenmodell
data class Phrase(
    val german: String,
    val translation: String
)

@Composable
fun TranslatorScreen(
    onBack: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf("PL") }

    val phrases = remember(selectedLanguage) {
        getPhrases(selectedLanguage)
    }

    ThubBackground {
        Box(modifier = Modifier.fillMaxSize()) {

            // Branding Logo
            Image(
                painter = painterResource(id = R.drawable.thub_logo_bg),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(300.dp)
                    .alpha(0.1f)
            )

            Column(modifier = Modifier.fillMaxSize()) {

                // HEADER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DOLMETSCHER",
                        style = MaterialTheme.typography.headlineSmall,
                        color = ThubNeonBlue,
                        fontWeight = FontWeight.Black
                    )
                }

                // SPRACH-AUSWAHL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LanguageButton("🇵🇱 PL", selectedLanguage == "PL") { selectedLanguage = "PL" }
                    LanguageButton("🇬🇧 EN", selectedLanguage == "EN") { selectedLanguage = "EN" }
                    LanguageButton("🇫🇷 FR", selectedLanguage == "FR") { selectedLanguage = "FR" }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // LISTE
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(phrases) { phrase ->
                        PhraseCard(phrase)
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) ThubNeonBlue else ThubDarkGray
    val textColor = if (isSelected) ThubBlack else TextWhite

    Box(
        modifier = Modifier
            .height(40.dp)
            .width(80.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PhraseCard(phrase: Phrase) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ThubDarkGray.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = phrase.german,
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = phrase.translation,
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun getPhrases(lang: String): List<Phrase> {
    return when(lang) {
        "PL" -> listOf(
            Phrase("Wo ist das Büro?", "Gdzie jest biuro?"),
            Phrase("Wo ist die Toilette?", "Gdzie jest toaleta?"),
            Phrase("Darf ich hier parken?", "Czy mogę tutaj zaparkować?"),
            Phrase("Ich brauche einen Stempel.", "Potrzebuję pieczątkę."),
            Phrase("Wann wird entladen?", "Kiedy rozładunek?"),
            Phrase("Ich habe ein Problem.", "Mam problem."),
            Phrase("Danke!", "Dziękuję!")
        )
        "FR" -> listOf(
            Phrase("Wo ist das Büro?", "Où est le bureau?"),
            Phrase("Wo ist die Toilette?", "Où sont les toilettes?"),
            Phrase("Darf ich hier parken?", "Puis-je me garer ici?"),
            Phrase("Ich brauche einen Stempel.", "J'ai besoin d'un tampon."),
            Phrase("Wann wird entladen?", "Quand est le déchargement?"),
            Phrase("Ich habe ein Problem.", "J'ai un problème."),
            Phrase("Danke!", "Merci!")
        )
        else -> listOf( // EN
            Phrase("Wo ist das Büro?", "Where is the office?"),
            Phrase("Wo ist die Toilette?", "Where is the toilet?"),
            Phrase("Darf ich hier parken?", "Can I park here?"),
            Phrase("Ich brauche einen Stempel.", "I need a stamp."),
            Phrase("Wann wird entladen?", "When is unloading?"),
            Phrase("Ich habe ein Problem.", "I have a problem."),
            Phrase("Danke!", "Thank you!")
        )
    }
}