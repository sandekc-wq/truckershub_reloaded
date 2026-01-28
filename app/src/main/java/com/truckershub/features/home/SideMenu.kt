package com.truckershub.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.TextWhite

@Composable
fun SideMenu(
    isOpen: Boolean,
    onToggle: () -> Unit,
    onLogoutClick: () -> Unit,
    onChecklistClick: () -> Unit,
    onEUGuideClick: () -> Unit,
    onBuddiesClick: () -> Unit,
    onTranslatorClick: () -> Unit,
    onWikiClick: () -> Unit // <--- NEU: Der Stecker für das Wiki
) {
    if (isOpen) {
        // Dunkler Schleier im Hintergrund
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onToggle() }
        )

        // Das eigentliche Menü
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .background(ThubBlack)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    "MENÜ",
                    color = ThubNeonBlue,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Menüpunkte
                DrawerItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Abfahrtskontrolle",
                    onClick = onChecklistClick
                )

                DrawerItem(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = "EU Guide (Lenkzeiten)",
                    onClick = onEUGuideClick
                )

                DrawerItem(
                    icon = Icons.Default.Group,
                    label = "Freundesliste",
                    onClick = onBuddiesClick
                )

                // --- NEU: FIRMEN WIKI ---
                DrawerItem(
                    icon = Icons.Default.Business, // Aktenkoffer Icon passt gut
                    label = "Firmen-Wiki",
                    onClick = onWikiClick
                )

                DrawerItem(
                    icon = Icons.Default.Translate,
                    label = "Dolmetscher",
                    onClick = onTranslatorClick
                )

                Spacer(modifier = Modifier.weight(1f))

                // Logout unten
                DrawerItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    label = "Abmelden",
                    onClick = onLogoutClick,
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color = TextWhite
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = color,
            fontSize = 18.sp
        )
    }
}