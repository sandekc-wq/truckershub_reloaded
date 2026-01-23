package com.truckershub.features.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.EuroSymbol
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    // Lade die Liste aller Länder aus deiner Datei
    val countries = remember { PredefinedCountries.getAll() }

    // State: Welches Land ist gerade ausgewählt? (Null = Liste anzeigen)
    var selectedCountry by remember { mutableStateOf<CountryInfo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedCountry == null) "EU GUIDE 🇪🇺" else selectedCountry!!.name.uppercase(),
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedCountry != null) selectedCountry = null else onBack()
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
            if (selectedCountry == null) {
                // LISTE ALLER LÄNDER
                CountryList(countries) { clickedCountry ->
                    selectedCountry = clickedCountry
                }
            } else {
                // DETAIL ANSICHT (Die "Hammer-Card")
                CountryDetailView(selectedCountry!!)
            }
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

        // 1. MAUT INFO (Ganz wichtig!)
        item {
            NeonSection("MAUT & VIGNETTE", Icons.Default.EuroSymbol) {
                val mautText = when(country.tollSystem) {
                    TollSystem.ELECTRONIC -> "Elektronische Mautpflicht!"
                    TollSystem.VIGNETTE -> "Vignettenpflicht!"
                    TollSystem.MIXED -> "Vignette + Go-Box (Mix)"
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

        // 2. TEMPOLIMITS (Fake-Daten, da nicht in deiner Datei, aber wichtig für die Optik)
        item {
            NeonSection("TEMPOLIMITS (LKW > 3.5t)", Icons.Default.Speed) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    SpeedSign("50", "Stadt")
                    SpeedSign("80", "Autobahn") // Standardannahme, später verfeinern
                    SpeedSign("60", "Land")
                }
            }
        }

        // 3. WINTERAUSRÜSTUNG
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

        // 4. FAHRVERBOTE
        if (country.truckDrivingBanInfo != null) {
            item {
                NeonSection("FAHRVERBOTE", Icons.Default.Timer) {
                    Text(country.truckDrivingBanInfo, color = Color.Red)
                }
            }
        }

        // 5. NOTRUF
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

        // 6. TIPPS (Deine Liste)
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