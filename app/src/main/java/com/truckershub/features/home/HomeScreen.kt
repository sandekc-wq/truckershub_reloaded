package com.truckershub.features.home

import androidx.activity.compose.BackHandler // <--- WICHTIG: Neu importiert
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
import com.truckershub.features.community.BuddyScreen
import com.truckershub.features.guide.EUGuideScreen // <--- WICHTIG: Neu importiert

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current

    // Tabs: 0=Map, 1=Check (Hidden), 2=Profil, 3=Community
    var selectedTab by remember { mutableIntStateOf(0) }

    // Welches Profil schauen wir an? (null = meins, String = Lisa etc.)
    var viewingForeignUserId by remember { mutableStateOf<String?>(null) }

    var isMenuOpen by remember { mutableStateOf(false) }

    // NEU: Steuerung für den EU Guide
    var showEUGuide by remember { mutableStateOf(false) }

    val isOnline by rememberOnlineStatus(context)
    val lastDataUpdate = remember { System.currentTimeMillis() }

    // NEU: BackHandler fängt die "Zurück"-Taste ab
    // Priorität: 1. Guide schließen, 2. Profil schließen (zurück zur Map), 3. Menü schließen
    BackHandler(enabled = isMenuOpen || showEUGuide || viewingForeignUserId != null) {
        when {
            isMenuOpen -> isMenuOpen = false
            showEUGuide -> showEUGuide = false
            viewingForeignUserId != null -> {
                viewingForeignUserId = null
                selectedTab = 0
            }
        }
    }

    Scaffold(
        topBar = {
            // TopBar nur anzeigen, wenn EU Guide NICHT offen ist (der hat seine eigene)
            if (!showEUGuide) {
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
            }
        },
        bottomBar = {
            // BottomBar ausblenden, wenn Checklist oder EU Guide offen sind
            if (selectedTab != 1 && !showEUGuide) {
                NavigationBar(containerColor = ThubBlack) {
                    // Home
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            viewingForeignUserId = null
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Karte") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = ThubBlack, selectedTextColor = ThubNeonBlue, indicatorColor = ThubNeonBlue, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                    )

                    // Buddies
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = {
                            selectedTab = 3
                            viewingForeignUserId = null
                        },
                        icon = { Icon(Icons.Default.Group, contentDescription = "Community") },
                        label = { Text("Buddies") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = ThubBlack, selectedTextColor = ThubNeonBlue, indicatorColor = ThubNeonBlue, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                    )

                    // Profil
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            viewingForeignUserId = null
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                        label = { Text("Profil") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = ThubBlack, selectedTextColor = ThubNeonBlue, indicatorColor = ThubNeonBlue, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                    )
                }
            }
        },
        containerColor = ThubBlack
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // HAUPT-INHALT (Nur wenn Guide nicht aktiv ist, um Ressourcen zu sparen)
            if (!showEUGuide) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OfflineWarningBanner(isVisible = !isOnline, lastUpdated = lastDataUpdate)

                    Surface(modifier = Modifier.fillMaxSize().weight(1f), color = ThubBlack) {
                        when (selectedTab) {
                            0 -> OsmMap(
                                modifier = Modifier.fillMaxSize(),
                                onOpenProfile = { targetId ->
                                    viewingForeignUserId = targetId
                                    selectedTab = 2
                                },
                                // HIER IST DAS FEHLENDE KABEL: 👇
                                onOpenEUGuide = { showEUGuide = true }
                            )
                            1 -> ChecklistScreen(
                                // Optional: Callback hinzufügen, um zurück zur Map zu kommen
                                // onBack = { selectedTab = 0 }
                            )
                            2 -> ProfileScreen(
                                targetUserId = viewingForeignUserId,
                                onBackClick = {
                                    if (viewingForeignUserId != null) {
                                        viewingForeignUserId = null
                                        selectedTab = 0
                                    } else {
                                        selectedTab = 0
                                    }
                                }
                            )
                            3 -> BuddyScreen()
                        }
                    }
                }
            }

            // NEU: DER EU GUIDE OVERLAY 🇪🇺
            // Er legt sich über alles andere, wenn showEUGuide == true
            if (showEUGuide) {
                Surface(modifier = Modifier.fillMaxSize(), color = ThubBlack) {
                    EUGuideScreen(onBack = { showEUGuide = false })
                }
            }

            // SIDE MENU (Liegt immer ganz oben)
            SideMenu(
                isOpen = isMenuOpen,
                onToggle = { isMenuOpen = !isMenuOpen },
                onLogoutClick = onLogoutClick,
                onChecklistClick = {
                    isMenuOpen = false // Menü zu
                    selectedTab = 1    // Zur Checkliste wechseln
                    showEUGuide = false // Sicherheitshalber Guide zu
                },
                onEUGuideClick = {     // <--- DAS NEUE KABEL!
                    isMenuOpen = false // Menü zu
                    showEUGuide = true // Guide auf
                }
            )
        }
    }
}