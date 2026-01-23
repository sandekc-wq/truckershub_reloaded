package com.truckershub.features.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import com.truckershub.features.guide.EUGuideScreen
import com.truckershub.features.feed.FeedScreen // <--- WICHTIG: Importieren!

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current

    // TABS:
    // 0 = Map (Home)
    // 1 = Feed (Neu)
    // 2 = Buddies
    // 3 = Profil
    // 4 = Checkliste (Bar ausgeblendet)
    var selectedTab by remember { mutableIntStateOf(0) }

    // Welches Profil schauen wir an?
    var viewingForeignUserId by remember { mutableStateOf<String?>(null) }

    var isMenuOpen by remember { mutableStateOf(false) }
    var showEUGuide by remember { mutableStateOf(false) }

    val isOnline by rememberOnlineStatus(context)
    val lastDataUpdate = remember { System.currentTimeMillis() }

    // BackHandler: Zurück-Taste Logik
    BackHandler(enabled = isMenuOpen || showEUGuide || viewingForeignUserId != null || selectedTab != 0) {
        when {
            isMenuOpen -> isMenuOpen = false
            showEUGuide -> showEUGuide = false
            viewingForeignUserId != null -> {
                viewingForeignUserId = null
                selectedTab = 2 // Zurück zur Buddy Liste
            }
            selectedTab != 0 -> selectedTab = 0 // Sonst zurück zur Map
        }
    }

    Scaffold(
        topBar = {
            if (!showEUGuide) {
                TopAppBar(
                    title = { Text("TRUCKERS HUB", color = ThubNeonBlue) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ThubBlack),
                    actions = {
                        IconButton(onClick = { isMenuOpen = !isMenuOpen }) {
                            Icon(
                                if (isMenuOpen) Icons.Default.Close else Icons.Default.Menu,
                                contentDescription = "Menü",
                                tint = ThubNeonBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            // Bar ausblenden bei Checkliste (4) oder Guide
            if (selectedTab != 4 && !showEUGuide) {
                NavigationBar(containerColor = ThubBlack) {

                    // 1. HOME (MAP)
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; viewingForeignUserId = null },
                        icon = { Icon(Icons.Default.Home, "Karte") },
                        label = { Text("Map") },
                        colors = navColors()
                    )

                    // 2. FEED (NEU!) ⛽
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; viewingForeignUserId = null },
                        // DynamicFeed Icon ist manchmal zickig, wenn nicht vorhanden nimm List
                        icon = { Icon(Icons.Default.DynamicFeed, "Feed") },
                        label = { Text("Feed") },
                        colors = navColors()
                    )

                    // 3. BUDDIES
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2; viewingForeignUserId = null },
                        icon = { Icon(Icons.Default.Group, "Community") },
                        label = { Text("Buddies") },
                        colors = navColors()
                    )

                    // 4. PROFIL
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3; viewingForeignUserId = null },
                        icon = { Icon(Icons.Default.Person, "Profil") },
                        label = { Text("Profil") },
                        colors = navColors()
                    )
                }
            }
        },
        containerColor = ThubBlack
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            if (!showEUGuide) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OfflineWarningBanner(isVisible = !isOnline, lastUpdated = lastDataUpdate)

                    Surface(modifier = Modifier.fillMaxSize().weight(1f), color = ThubBlack) {
                        // HIER IST DIE LOGIK (Nicht in der BottomBar!)
                        when (selectedTab) {
                            0 -> OsmMap(
                                modifier = Modifier.fillMaxSize(),
                                onOpenProfile = { targetId ->
                                    viewingForeignUserId = targetId
                                    selectedTab = 3 // Wechsel zu Profil Tab
                                },
                                onOpenEUGuide = { showEUGuide = true }
                            )

                            // HIER IST DER NEUE FEED SCREEN! 👇
                            1 -> FeedScreen(
                                onPostClick = { /* Später: Detailansicht */ },
                                onFabClick = {
                                    // Später: Upload Dialog öffnen
                                    Toast.makeText(context, "Upload kommt gleich!", Toast.LENGTH_SHORT).show()
                                }
                            )

                            2 -> BuddyScreen()

                            3 -> ProfileScreen(
                                targetUserId = viewingForeignUserId,
                                onBackClick = {
                                    if (viewingForeignUserId != null) {
                                        viewingForeignUserId = null
                                        selectedTab = 2 // Zurück zu Buddies
                                    } else {
                                        selectedTab = 0 // Zurück zur Map
                                    }
                                }
                            )

                            4 -> ChecklistScreen() // Versteckter Tab
                        }
                    }
                }
            }

            // EU GUIDE OVERLAY
            if (showEUGuide) {
                Surface(modifier = Modifier.fillMaxSize(), color = ThubBlack) {
                    EUGuideScreen(onBack = { showEUGuide = false })
                }
            }

            // SIDE MENU
            SideMenu(
                isOpen = isMenuOpen,
                onToggle = { isMenuOpen = !isMenuOpen },
                onLogoutClick = onLogoutClick,
                onChecklistClick = {
                    isMenuOpen = false
                    selectedTab = 4 // Setzt den versteckten Tab für Checkliste
                    showEUGuide = false
                },
                onEUGuideClick = {
                    isMenuOpen = false
                    showEUGuide = true
                }
            )
        }
    }
}

// Kleine Hilfsfunktion für die Farben (spart Platz)
@Composable
fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = ThubBlack,
    selectedTextColor = ThubNeonBlue,
    indicatorColor = ThubNeonBlue,
    unselectedIconColor = Color.Gray,
    unselectedTextColor = Color.Gray
)