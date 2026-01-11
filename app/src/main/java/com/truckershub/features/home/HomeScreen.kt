package com.truckershub.features.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.TextWhite
import com.truckershub.features.checklist.ChecklistScreen
import com.truckershub.features.profile.ProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogoutClick: () -> Unit
) {
    // Hier merken wir uns, welcher Tab gerade aktiv ist (0=Map, 1=Check, 2=Profil)
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TRUCKERS HUB", color = ThubNeonBlue) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThubBlack),
                actions = {
                    TextButton(onClick = onLogoutClick) {
                        Text("Logout", color = Color.Gray)
                    }
                }
            )
        },
        // HIER IST DAS NEUE LENKRAD (Unten)
        bottomBar = {
            NavigationBar(containerColor = ThubBlack) {
                // Button 1: Karte
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Karte") },
                    label = { Text("Karte") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThubBlack,
                        selectedTextColor = ThubNeonBlue,
                        indicatorColor = ThubNeonBlue, // Der Leucht-Kreis
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                // Button 2: Checkliste
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Check") },
                    label = { Text("Check") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThubBlack,
                        selectedTextColor = ThubNeonBlue,
                        indicatorColor = ThubNeonBlue,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                // Button 3: Profil
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                    label = { Text("Profil") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThubBlack,
                        selectedTextColor = ThubNeonBlue,
                        indicatorColor = ThubNeonBlue,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        },
        containerColor = ThubBlack
    ) { paddingValues ->
        // Der Inhalt wechselt je nach Knopfdruck
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = ThubBlack
        ) {
            when (selectedTab) {
                0 -> OsmMap(modifier = Modifier.fillMaxSize()) // Deine Karte
                1 -> ChecklistScreen() // Die neue leere Seite
                2 -> ProfileScreen()   // Die neue leere Seite
            }
        }
    }
}