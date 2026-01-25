package com.truckershub.features.checklist

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckershub.R
import com.truckershub.core.design.ThubBackground
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.ThubRed
import com.truckershub.core.design.TextWhite

@Composable
fun ChecklistScreen(
    viewModel: ChecklistViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val checklist = viewModel.checklist

    val doneCount = checklist.count { it.isChecked }
    val totalCount = checklist.size
    val progress = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f

    val uiMapping = remember {
        listOf(
            Pair(Icons.Filled.Lightbulb, Color(0xFFFFD700)),
            Pair(Icons.Filled.DonutLarge, Color(0xFFFFA500)),
            Pair(Icons.Filled.Speed, ThubRed),
            Pair(Icons.Filled.Visibility, ThubNeonBlue),
            Pair(Icons.Filled.WaterDrop, Color(0xFF00CED1)),
            Pair(Icons.Filled.LocalShipping, Color(0xFF32CD32)),
            Pair(Icons.Filled.Description, Color.White),
            Pair(Icons.Filled.MedicalServices, ThubRed)
        )
    }

    ThubBackground {
        Box(modifier = Modifier.fillMaxSize()) {

            // Branding-Logo im Hintergrund (dezent)
            Image(
                painter = painterResource(id = R.drawable.thub_logo_bg),
                contentDescription = "Branding Hintergrund",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(350.dp)
                    .offset(x = 40.dp, y = 40.dp)
                    .alpha(0.1f)
            )

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                // --- NEUER HEADER (Pfeil links, Text rechts) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically // Alles schön mittig auf einer Linie
                ) {
                    // 1. Der Zurück-Pfeil (Ganz links)
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = TextWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 2. Der Titel & Status
                    Column {
                        Text(
                            text = "ABFAHRTSKONTROLLE",
                            style = MaterialTheme.typography.headlineSmall, // Etwas kleiner, damit es passt
                            color = TextWhite,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Status: $doneCount von $totalCount geprüft",
                            color = if (doneCount == totalCount) ThubNeonBlue else Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = ThubNeonBlue,
                    trackColor = ThubDarkGray.copy(alpha = 0.5f),
                )

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(checklist) { index, item ->
                        val (icon, color) = uiMapping.getOrElse(index) { Pair(Icons.Filled.CheckCircle, ThubNeonBlue) }

                        ChecklistCard(
                            item = item,
                            icon = icon,
                            accentColor = color,
                            onCheckedChange = { newValue -> viewModel.toggleCheck(index, newValue) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                PowerButton(
                    text = "PROTOKOLLIEREN & SENDEN",
                    enabled = !viewModel.isSaving && doneCount > 0,
                    isLoading = viewModel.isSaving,
                    onClick = {
                        viewModel.saveReport(
                            onSuccess = { Toast.makeText(context, "✅ Gespeichert!", Toast.LENGTH_SHORT).show(); onBack() },
                            onError = { msg -> Toast.makeText(context, "❌ Fehler: $msg", Toast.LENGTH_LONG).show() }
                        )
                    }
                )
            }
        }
    }
}

// ... Rest der Datei (ChecklistCard, PowerButton) bleibt gleich ...
@Composable
fun ChecklistCard(
    item: CheckPoint,
    icon: ImageVector,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ThubDarkGray.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        border = if (item.isChecked) BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.title,
                color = if (item.isChecked) TextWhite else Color.LightGray,
                fontWeight = if (item.isChecked) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = item.isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ThubBlack,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = ThubBlack,
                    uncheckedBorderColor = Color.Gray
                )
            )
        }
    }
}

@Composable
fun PowerButton(text: String, enabled: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    val gradientColors = if (enabled) listOf(Color(0xFF2B2B2B), Color.Black) else listOf(Color.DarkGray, Color.Gray)
    val borderColor = if (enabled) ThubNeonBlue else Color.Gray
    val glowColor = if (enabled) Color.White.copy(alpha = 0.3f) else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(gradientColors))
            .border(BorderStroke(1.dp, Brush.verticalGradient(listOf(glowColor, borderColor))), RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = ThubNeonBlue, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (enabled) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(ThubNeonBlue, CircleShape)
                            .shadow(
                                elevation = 6.dp,
                                shape = CircleShape,
                                spotColor = ThubNeonBlue
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(text = text, color = if (enabled) ThubNeonBlue else ThubBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
            }
        }
    }
}