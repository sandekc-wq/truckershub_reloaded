package com.truckershub.features.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.network.rememberOnlineStatus
import com.truckershub.core.ui.components.OfflineWarningBanner
import com.truckershub.features.checklist.ChecklistScreen
import com.truckershub.features.profile.ProfileScreen
import com.truckershub.features.community.BuddyScreen
import com.truckershub.features.community.ChatScreen
import com.truckershub.features.community.CommentScreen
import com.truckershub.features.guide.EUGuideScreen
import com.truckershub.features.feed.FeedScreen
import com.truckershub.features.map.SavedLocationsScreen
import com.truckershub.features.translator.TranslatorScreen
import com.truckershub.core.data.model.Location
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // TABS: 0=Map, 1=Feed, 2=Buddies, 3=Profil, 4=Checkliste
    var selectedTab by remember { mutableIntStateOf(0) }

    // Navigation Status Variablen
    var viewingForeignUserId by remember { mutableStateOf<String?>(null) }
    var activeChatBuddy by remember { mutableStateOf<String?>(null) }
    var viewingCommentsForPostId by remember { mutableStateOf<String?>(null) }

    // Overlay States
    var isMenuOpen by remember { mutableStateOf(false) }
    var showEUGuide by remember { mutableStateOf(false) }
    var showTranslator by remember { mutableStateOf(false) }
    var showWiki by remember { mutableStateOf(false) } // <--- NEU: Wiki State

    // State für den Ziel-Sprung (Autopilot für die Karte)
    var jumpToLocation by remember { mutableStateOf<Location?>(null) }

    val isOnline by rememberOnlineStatus(context)
    val lastDataUpdate = remember { System.currentTimeMillis() }

    // --- ZENTRALER BACK-HANDLER ---
    // Hier regeln wir, was passiert, wenn man die "Zurück"-Taste am Handy drückt
    BackHandler(
        enabled = isMenuOpen || showEUGuide || showTranslator || showWiki ||
                viewingCommentsForPostId != null || viewingForeignUserId != null ||
                activeChatBuddy != null || selectedTab != 0
    ) {
        when {
            isMenuOpen -> isMenuOpen = false
            showTranslator -> showTranslator = false
            showEUGuide -> showEUGuide = false
            showWiki -> showWiki = false
            viewingCommentsForPostId != null -> viewingCommentsForPostId = null
            activeChatBuddy != null -> activeChatBuddy = null
            viewingForeignUserId != null -> {
                viewingForeignUserId = null
                selectedTab = 2
            }
            selectedTab != 0 -> selectedTab = 0
        }
    }

    Scaffold(
        topBar = {
            // TopBar ausblenden, wenn Overlays offen sind
            if (!showEUGuide && !showTranslator && !showWiki &&
                viewingCommentsForPostId == null && activeChatBuddy == null
            ) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "THUB",
                                color = ThubNeonBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "The Truckers Knife",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    },
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
            // BottomBar ausblenden bei Overlays oder Checkliste
            if (selectedTab != 4 && !showEUGuide && !showTranslator && !showWiki &&
                viewingCommentsForPostId == null && activeChatBuddy == null
            ) {
                NavigationBar(containerColor = ThubBlack) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; viewingForeignUserId = null },
                        icon = { Icon(Icons.Default.Home, "Karte") },
                        label = { Text("Map") },
                        colors = navColors()
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; viewingForeignUserId = null },
                        icon = { Icon(Icons.Default.DynamicFeed, "Feed") },
                        label = { Text("Feed") },
                        colors = navColors()
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2; viewingForeignUserId = null },
                        icon = { Icon(Icons.Default.Group, "Community") },
                        label = { Text("Buddies") },
                        colors = navColors()
                    )
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // HAUPT-INHALT (Wird nur angezeigt, wenn kein Vollbild-Overlay aktiv ist)
            if (!showEUGuide && !showTranslator && !showWiki && viewingCommentsForPostId == null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (activeChatBuddy == null) {
                        OfflineWarningBanner(isVisible = !isOnline, lastUpdated = lastDataUpdate)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        color = ThubBlack
                    ) {
                        when (selectedTab) {
                            0 -> OsmMap(
                                modifier = Modifier.fillMaxSize(),
                                onOpenProfile = { targetId ->
                                    viewingForeignUserId = targetId
                                    selectedTab = 3
                                },
                                onOpenEUGuide = { showEUGuide = true },
                                // HIER ÜBERGEBEN WIR DAS ZIEL AN DIE KARTE 👇
                                jumpToLocation = jumpToLocation,
                                onLocationJumped = { jumpToLocation = null } // Reset nach Ankunft
                            )

                            1 -> FeedScreen(
                                onPostClick = { },
                                onFabClick = {
                                    Toast.makeText(context, "Upload...", Toast.LENGTH_SHORT).show()
                                },
                                onCommentClick = { postId -> viewingCommentsForPostId = postId }
                            )

                            2 -> {
                                if (activeChatBuddy != null) {
                                    ChatScreen(
                                        buddyName = activeChatBuddy!!,
                                        onBack = { activeChatBuddy = null }
                                    )
                                } else {
                                    BuddyScreen(
                                        onChatClick = { buddyName ->
                                            activeChatBuddy = buddyName
                                        }
                                    )
                                }
                            }

                            3 -> ProfileScreen(
                                targetUserId = viewingForeignUserId,
                                onBackClick = {
                                    if (viewingForeignUserId != null) {
                                        viewingForeignUserId = null
                                        selectedTab = 2
                                    } else {
                                        selectedTab = 0
                                    }
                                }
                            )

                            4 -> ChecklistScreen(
                                onBack = { selectedTab = 0 }
                            )
                        }
                    }
                }
            }

            // --- OVERLAYS (Legen sich über alles drüber) ---

            if (viewingCommentsForPostId != null) {
                Surface(modifier = Modifier.fillMaxSize(), color = ThubBlack) {
                    CommentScreen(
                        postId = viewingCommentsForPostId!!,
                        onBack = { viewingCommentsForPostId = null }
                    )
                }
            }

            if (showEUGuide) {
                Surface(modifier = Modifier.fillMaxSize(), color = ThubBlack) {
                    EUGuideScreen(onBack = { showEUGuide = false })
                }
            }

            if (showTranslator) {
                Surface(modifier = Modifier.fillMaxSize(), color = ThubBlack) {
                    TranslatorScreen(onBack = { showTranslator = false })
                }
            }

            // WIKI OVERLAY (Unser neues Feature)
            if (showWiki) {
                Surface(modifier = Modifier.fillMaxSize(), color = ThubBlack) {
                    SavedLocationsScreen(
                        onClose = { showWiki = false },
                        onJumpToLocation = { location ->
                            // HIER SETZEN WIR DAS ZIEL 👇
                            showWiki = false
                            selectedTab = 0 // Sicherstellen, dass wir auf der Karte sind
                            jumpToLocation = location // Abflug! ✈️
                            Toast.makeText(
                                context,
                                "Fliege zu: ${location.name} 🛫",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            // SIDE MENU
            SideMenu(
                isOpen = isMenuOpen,
                onToggle = { isMenuOpen = !isMenuOpen },
                onLogoutClick = onLogoutClick,
                onChecklistClick = {
                    isMenuOpen = false
                    selectedTab = 4
                    showEUGuide = false
                    showTranslator = false
                    showWiki = false
                },
                onEUGuideClick = {
                    isMenuOpen = false
                    showEUGuide = true
                    showTranslator = false
                    showWiki = false
                },
                onBuddiesClick = {
                    isMenuOpen = false
                    selectedTab = 2
                    showEUGuide = false
                    showTranslator = false
                    showWiki = false
                    viewingForeignUserId = null
                },
                onTranslatorClick = {
                    isMenuOpen = false
                    showTranslator = true
                    showEUGuide = false
                    showWiki = false
                },
                onWikiClick = {
                    isMenuOpen = false
                    showWiki = true
                    showEUGuide = false
                    showTranslator = false
                }
            )
        }
    }
}

@Composable
fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = ThubBlack,
    selectedTextColor = ThubNeonBlue,
    indicatorColor = ThubNeonBlue,
    unselectedIconColor = Color.Gray,
    unselectedTextColor = Color.Gray
)