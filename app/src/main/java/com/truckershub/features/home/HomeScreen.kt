package com.truckershub.features.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckershub.R
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.TextWhite
import com.truckershub.features.checklist.ChecklistScreen
import com.truckershub.features.profile.ProfileScreen
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogoutClick: () -> Unit
) {
    // Hier merken wir uns, welcher Tab gerade aktiv ist (0=Map, 1=Check, 2=Profil)
    var selectedTab by remember { mutableIntStateOf(0) }

    // Status für das seitliche Menü (Drawer)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 1. DER DRAWER WRAPPER (Das Menü-Gerüst)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // --- INHALT DES SEITENMENÜS ---
            ModalDrawerSheet(
                drawerContainerColor = ThubBlack, // Satt Schwarz
                drawerContentColor = ThubNeonBlue
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header (Logo oben im Menü)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(id = R.drawable.thub_logo_bg),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "TRUCKERS HUB",
                                color = ThubNeonBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Menü-Punkt: Freunde
                    NavigationDrawerItem(
                        label = { Text("Freunde & Kollegen", color = TextWhite) },
                        icon = { Icon(Icons.Default.Group, null, tint = ThubNeonBlue) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )

                    // Menü-Punkt: Nachrichten
                    NavigationDrawerItem(
                        label = { Text("Nachrichten", color = TextWhite) },
                        icon = { Icon(Icons.Default.Chat, null, tint = ThubNeonBlue) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )

                    // Menü-Punkt: Einstellungen
                    NavigationDrawerItem(
                        label = { Text("Einstellungen", color = TextWhite) },
                        icon = { Icon(Icons.Default.Settings, null, tint = ThubNeonBlue) },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() } },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )

                    Spacer(modifier = Modifier.weight(1f)) // Schiebt Logout ganz nach unten

                    HorizontalDivider(color = Color.Gray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    // Menü-Punkt: Logout
                    NavigationDrawerItem(
                        label = { Text("Abmelden", color = Color.Red) },
                        icon = { Icon(Icons.Default.Logout, null, tint = Color.Red) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onLogoutClick()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    ) {
        // 2. DEIN ORIGINAL SCAFFOLD (Der Hauptinhalt)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("TRUCKERS HUB", color = ThubNeonBlue) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ThubBlack),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menü", tint = ThubNeonBlue)
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
                    // HIER WAR DER FEHLER: Jetzt mit .fillMaxSize()
                    0 -> OsmMap(modifier = Modifier.fillMaxSize())
                    1 -> ChecklistScreen()
                    2 -> ProfileScreen(onBackClick = { selectedTab = 0 })
                }
            }
        }
    }
}