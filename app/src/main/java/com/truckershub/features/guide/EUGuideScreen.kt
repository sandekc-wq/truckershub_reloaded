package com.truckershub.features.guide

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.EuroSymbol
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckershub.core.data.model.CountryInfo
import com.truckershub.core.data.model.PredefinedCountries
import com.truckershub.core.data.model.RequirementType
import com.truckershub.core.data.model.TollSystem
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EUGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // Lade die Liste aller Länder aus deiner neuen Datei
    val countries = remember { PredefinedCountries.getAll() }

    // State: Welches Land ist gerade ausgewählt? (Null = Liste anzeigen)
    var selectedCountry by remember { mutableStateOf<CountryInfo?>(null) }
    // State: Zeigen wir die Lenkzeiten an?
    var showCompliance by remember { mutableStateOf(false) }

    val title = when {
        showCompliance -> "EU REGELN 👮‍♂️"
        selectedCountry != null -> selectedCountry!!.name.uppercase()
        else -> "EU GUIDE 🇪🇺"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(title, color = TextWhite, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showCompliance) showCompliance = false
                        else if (selectedCountry != null) selectedCountry = null
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = ThubNeonBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThubDarkGray)
            )
        },
        containerColor = ThubBlack
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (showCompliance) {
                // 1. DER NEUE SPICKZETTEL (Lenkzeiten)
                ComplianceView(
                    onOpenBalm = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.balm.bund.de/SharedDocs/Downloads/DE/Lkw-Maut/Flyer_Lenk_und_Ruhezeiten.pdf?__blob=publicationFile&v=4"))
                        context.startActivity(browserIntent)
                    }
                )
            } else if (selectedCountry == null) {
                // 2. LISTE ALLER LÄNDER (+ Button für Regeln ganz oben)
                Column {
                    // Der "Spickzettel"-Button ganz oben
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.dp, ThubNeonBlue, RoundedCornerShape(12.dp))
                            .clickable { showCompliance = true }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = ThubNeonBlue)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("EU LENK- & RUHEZEITEN", color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color.Gray, thickness = 0.5.dp)

                    CountryList(countries) { clickedCountry ->
                        selectedCountry = clickedCountry
                    }
                }
            } else {
                // 3. DETAIL ANSICHT (Länder-Infos)
                CountryDetailView(selectedCountry!!)
            }
        }
    }
}

@Composable
fun ComplianceView(onOpenBalm: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            NeonSection("LENKZEITEN (Täglich)", Icons.Default.Timer) {
                InfoRow("Standard", "9 Stunden", true)
                InfoRow("Verlängerung", "10 Stunden (max. 2x / Woche)", false)
            }
        }
        item {
            NeonSection("PAUSEN (Unterbrechung)", Icons.Default.Timer) {
                InfoRow("Nach 4,5 Std.", "45 Minuten Pause", true)
                InfoRow("Aufteilung", "Erst 15 Min, dann 30 Min", false)
            }
        }
        item {
            NeonSection("RUHEZEITEN (Täglich)", Icons.Default.Timer) {
                InfoRow("Regulär", "11 Stunden", true)
                InfoRow("Reduziert", "9 Stunden (max. 3x / Woche)", false)
                InfoRow("Splitting", "3 Std + 9 Std (=12 Std)", false)
            }
        }
        item {
            NeonSection("WOCHENLENKZEIT", Icons.Default.Timer) {
                InfoRow("Eine Woche", "Max. 56 Stunden", true)
                InfoRow("Doppelwoche", "Max. 90 Stunden", false)
            }
        }

        item {
            Button(
                onClick = onOpenBalm,
                colors = ButtonDefaults.buttonColors(containerColor = ThubNeonBlue),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Link, null, tint = ThubBlack)
                Spacer(modifier = Modifier.width(8.dp))
                Text("OFFIZIELLES BALM PDF ÖFFNEN", color = ThubBlack, fontWeight = FontWeight.Bold)
            }
            Text(
                "Der Link führt zur offiziellen Seite des Bundesamtes für Logistik und Mobilität.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top=8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun CountryList(countries: List<CountryInfo>, onClick: (CountryInfo) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(countries) { country ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().clickable { onClick(country) }
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(country.flag ?: "🏳️", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(country.name, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Vorwahl: ${country.callingCode} • Währung: ${country.currency}", color = Color.Gray, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Info, null, tint = ThubNeonBlue)
                }
            }
        }
    }
}

@Composable
fun CountryDetailView(country: CountryInfo) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // 1. MAUT INFO
        item {
            NeonSection("MAUT & VIGNETTE", Icons.Default.EuroSymbol) {
                val mautText = when(country.tollSystem) {
                    TollSystem.ELECTRONIC -> "Elektronische Mautpflicht!"
                    TollSystem.VIGNETTE -> "Vignettenpflicht!"
                    TollSystem.MIXED -> "Vignette + Box (Mix)"
                    else -> "Keine generelle Maut"
                }

                InfoRow("System", mautText, true)
                if (country.tollSystemName != null) {
                    InfoRow("Name", country.tollSystemName, false)
                }

                // Warnung bei hohen Strafen
                val tollFine = country.requirements.find { it.type == RequirementType.TOLL }?.fine
                if (tollFine != null) {
                    Text("⚠️ Strafe bei Verstoß: $tollFine", color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=8.dp))
                }
            }
        }

        // 2. TEMPOLIMITS (Jetzt mit echten Daten!)
        item {
            NeonSection("TEMPOLIMITS (LKW > 3.5t)", Icons.Default.Speed) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    SpeedSign("50", "Stadt")
                    // HIER KOMMEN DIE ECHTEN DATEN REIN 👇
                    SpeedSign(country.speedLimitHighway.toString(), "Autobahn")
                    SpeedSign(country.speedLimitCountry.toString(), "Landstraße")
                }
            }
        }

        // 3. EQUIPMENT & REGELN (Neu: Dynamisch aus der Liste)
        if (country.requirements.isNotEmpty()) {
            item {
                NeonSection("PFLICHT-AUSRÜSTUNG", Icons.Default.Info) {
                    country.requirements.forEach { req ->
                        // Einfache Icon-Wahl basierend auf Typ
                        val icon = when(req.type) {
                            RequirementType.DOCUMENT -> Icons.AutoMirrored.Filled.MenuBook
                            RequirementType.EQUIPMENT -> Icons.Default.Warning
                            RequirementType.TOLL -> Icons.Default.EuroSymbol
                            else -> Icons.Default.Info
                        }
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(icon, null, tint = ThubNeonBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(req.title, color = TextWhite, fontWeight = FontWeight.Bold)
                                Text(req.description, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        HorizontalDivider(color = Color.Black, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }

        // 4. WINTERAUSRÜSTUNG
        if (country.winterTiresRequired || country.snowChainsRequired) {
            item {
                NeonSection("WINTER-REGELN", Icons.Default.AcUnit) {
                    if (country.winterPeriod != null) {
                        Text("📅 Zeitraum: ${country.winterPeriod.startDate} bis ${country.winterPeriod.endDate}", color = TextWhite)
                        Text("ℹ️ ${country.winterPeriod.condition}", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (country.snowChainsRequired) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AcUnit, null, tint = ThubNeonBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Schneeketten-Mitführpflicht!", color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. FAHRVERBOTE
        if (country.truckDrivingBanInfo != null) {
            item {
                NeonSection("FAHRVERBOTE", Icons.Default.Timer) {
                    Text(country.truckDrivingBanInfo, color = Color.Red)
                }
            }
        }

        // 6. NOTRUF
        item {
            NeonSection("NOTRUF", Icons.Default.Warning) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Allgemein", color = Color.Gray, fontSize = 12.sp)
                        Text(country.emergencyNumber, color = ThubNeonBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    if (country.policeNumber != null) {
                        Column {
                            Text("Polizei", color = Color.Gray, fontSize = 12.sp)
                            Text(country.policeNumber, color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 7. TIPPS
        if (country.tips.isNotEmpty()) {
            item {
                Text("PROFI-TIPPS:", color = Color.Gray, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                country.tips.forEach { tip ->
                    Row(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text("💡", modifier = Modifier.padding(end = 8.dp))
                        Text(tip, color = TextWhite, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// Hilfs-Komponente für die Sektionen
@Composable
fun NeonSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = ThubNeonBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = ThubNeonBlue, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Black)
            content()
        }
    }
}

// Hilfs-Komponente für Zeilen
@Composable
fun InfoRow(label: String, value: String, highlight: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray)
        Text(value, color = if (highlight) TextWhite else Color.Gray, fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal)
    }
}

// Hilfs-Komponente für Verkehrszeichen
@Composable
fun SpeedSign(speed: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            // Weißer Kreis
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(50),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(4.dp, Color.Red)
            ) {}
            // Zahl
            Text(speed, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.Gray, fontSize = 10.sp)
    }
}