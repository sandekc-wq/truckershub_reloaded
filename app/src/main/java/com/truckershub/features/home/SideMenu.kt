package com.truckershub.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckershub.R
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubNeonBlue

/**
 * Kompaktes Side-Menü für Truckers Hub
 * Jetzt inkl. FEATURES Sektion (Abfahrtskontrolle etc.)
 */
@Composable
fun SideMenu(
    isOpen: Boolean = false,
    onToggle: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onChecklistClick: () -> Unit = {} // NEU: Callback für Checkliste
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Semi-transparenter Overlay
        if (isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(enabled = true) { onToggle() }
            )
        }

        // Das Menü selbst
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = ThubBlack),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header mit Logo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(id = R.drawable.thub_logo_bg),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "TRUCKERS HUB",
                                color = ThubNeonBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = ThubNeonBlue.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // === SEKTION: FEATURES ===
                    Text(
                        text = "FEATURES",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
                    )

                    // Menü-Punkt: Abfahrtskontrolle
                    NavigationDrawerItem(
                        label = { Text("Abfahrtskontrolle", color = TextWhite) },
                        icon = { Icon(Icons.AutoMirrored.Filled.FactCheck, null, tint = ThubNeonBlue) },
                        selected = false,
                        onClick = {
                            onToggle()
                            onChecklistClick() // Öffnet den Check-Screen
                        },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )

                    // Platzhalter für später (EU Richtlinien etc.)
                    /*
                    NavigationDrawerItem(
                        label = { Text("EU Richtlinien", color = TextWhite) },
                        icon = { Icon(Icons.Default.MenuBook, null, tint = ThubNeonBlue) },
                        selected = false,
                        onClick = { onToggle() },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )
                    */

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = Color.Gray.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // === SEKTION: ALLGEMEIN ===

                    // Menü-Punkt: Freunde
                    NavigationDrawerItem(
                        label = { Text("Freunde & Kollegen", color = TextWhite) },
                        icon = { Icon(Icons.Default.Group, null, tint = ThubNeonBlue) },
                        selected = false,
                        onClick = { onToggle() },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )

                    // Menü-Punkt: Nachrichten
                    NavigationDrawerItem(
                        label = { Text("Nachrichten", color = TextWhite) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, null, tint = ThubNeonBlue) },
                        selected = false,
                        onClick = { onToggle() },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )

                    // Menü-Punkt: Einstellungen
                    NavigationDrawerItem(
                        label = { Text("Einstellungen", color = TextWhite) },
                        icon = { Icon(Icons.Default.Settings, null, tint = ThubNeonBlue) },
                        selected = false,
                        onClick = { onToggle() },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = ThubNeonBlue.copy(alpha = 0.3f)
                    )

                    // Menü-Punkt: Logout
                    NavigationDrawerItem(
                        label = { Text("Abmelden", color = Color.Red) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red) },
                        selected = false,
                        onClick = {
                            onToggle()
                            onLogoutClick()
                        },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}