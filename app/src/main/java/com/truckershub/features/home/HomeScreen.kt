package com.truckershub.features.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.network.rememberOnlineStatus
import com.truckershub.core.ui.components.OfflineWarningBanner
import com.truckershub.features.checklist.ChecklistScreen
import com.truckershub.features.profile.ProfileScreen

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current

    // Tabs: 0=Map, 1=Check (Hidden), 2=Profil
    var selectedTab by remember { mutableIntStateOf(0) }

    // Status für das Seitenmenü
    var isMenuOpen by remember { mutableStateOf(false) }

    // Online-Status Prüfung
    val isOnline by rememberOnlineStatus(context)
    val lastDataUpdate = remember { System.currentTimeMillis() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TRUCKERS HUB", color = ThubNeonBlue) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThubBlack),
                actions = {
                    IconButton(onClick = { isMenuOpen = !isMenuOpen }) {
                        Icon(
                            if (isMenuOpen) Icons.Default.Close else Icons.Default.Menu,
                            contentDescription = if (isMenuOpen) "Menü schließen" else "Menü öffnen",
                            tint = ThubNeonBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = ThubBlack) {
                // Button 1: Karte
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Karte") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThubBlack,
                        selectedTextColor = ThubNeonBlue,
                        indicatorColor = ThubNeonBlue,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                // HIER WAR VORHER DER CHECK-BUTTON - JETZT GELÖSCHT!
                // Platz ist jetzt frei für spätere Features (Chat etc.)

                // Button 2: Profil (war vorher 3)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                OfflineWarningBanner(
                    isVisible = !isOnline,
                    lastUpdated = lastDataUpdate
                )

                // Der Inhalt
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    color = ThubBlack
                ) {
                    when (selectedTab) {
                        0 -> OsmMap(modifier = Modifier.fillMaxSize())
                        1 -> ChecklistScreen() // Wird jetzt über das Menü aufgerufen!
                        2 -> ProfileScreen(onBackClick = { selectedTab = 0 })
                    }
                }
            }

            // SideMenu überlagert den Screen
            SideMenu(
                isOpen = isMenuOpen,
                onToggle = { isMenuOpen = !isMenuOpen },
                onLogoutClick = onLogoutClick,
                // WICHTIG: Hier verbinden wir das Menü mit dem Screen-Wechsel
                onChecklistClick = {
                    selectedTab = 1
                }
            )
        }
    }
}