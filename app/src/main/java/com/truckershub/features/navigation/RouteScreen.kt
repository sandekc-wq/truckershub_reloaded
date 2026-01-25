package com.truckershub.features.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubNeonBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteScreen(
    onBack: () -> Unit,
    viewModel: RouteViewModel = viewModel()
) {
    // KORREKTER ZUGRIFF: route -> routeDetails -> instructions
    val route = viewModel.currentRoute
    val instructions = route?.routeDetails?.instructions ?: emptyList()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wegbeschreibung", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = ThubNeonBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThubBlack)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThubBlack)
                .padding(padding)
        ) {
            if (instructions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Keine Route aktiv.", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(instructions) { instr ->
                        ListItem(
                            headlineContent = { Text(instr.text ?: "Weg folgen", color = TextWhite) },
                            supportingContent = {
                                Text("${instr.distance.toInt()}m", color = ThubNeonBlue)
                            },
                            colors = ListItemDefaults.colors(containerColor = ThubBlack)
                        )
                        HorizontalDivider(color = Color.DarkGray)
                    }
                }
            }
        }
    }
}