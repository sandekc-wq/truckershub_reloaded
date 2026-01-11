package com.truckershub.features.checklist

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.TextWhite

data class CheckPoint(val id: Int, val title: String, var isChecked: Boolean)

@Composable
fun ChecklistScreen(
    // Wir holen uns das ViewModel automatisch
    viewModel: ChecklistViewModel = viewModel()
) {
    val context = LocalContext.current

    // Daten aus dem ViewModel lesen
    val checklist = viewModel.checklist
    val doneCount = checklist.count { it.isChecked }
    val totalCount = checklist.size
    val progress = doneCount.toFloat() / totalCount.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThubBlack)
            .padding(16.dp)
    ) {
        // --- KOPFBEREICH ---
        Text(
            text = "ABFAHRTSKONTROLLE",
            style = MaterialTheme.typography.headlineMedium,
            color = TextWhite,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Status: $doneCount von $totalCount geprüft", color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = ThubNeonBlue,
            trackColor = ThubDarkGray,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- DIE LISTE ---
        // 'weight(1f)' sorgt dafür, dass die Liste den Platz in der Mitte nimmt,
        // aber dem Button unten Platz lässt.
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(checklist) { index, item ->
                CheckItemRow(
                    item = item,
                    onCheckedChange = { newValue ->
                        viewModel.toggleCheck(index, newValue)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- DER SENDEN BUTTON ---
        Button(
            onClick = {
                viewModel.saveReport(
                    onSuccess = {
                        Toast.makeText(context, "✅ Protokoll gespeichert!", Toast.LENGTH_LONG).show()
                    },
                    onError = { errorMsg ->
                        Toast.makeText(context, "❌ Fehler: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                )
            },
            // Button ist nur aktiv, wenn nicht gerade gespeichert wird
            enabled = !viewModel.isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = ThubNeonBlue,
                disabledContainerColor = ThubDarkGray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (viewModel.isSaving) {
                CircularProgressIndicator(color = ThubBlack, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "PROTOKOLLIEREN & SENDEN",
                    color = ThubBlack,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun CheckItemRow(item: CheckPoint, onCheckedChange: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.title,
                color = if (item.isChecked) ThubNeonBlue else TextWhite,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.isChecked) FontWeight.Bold else FontWeight.Normal
            )

            Switch(
                checked = item.isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ThubBlack,
                    checkedTrackColor = ThubNeonBlue,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = ThubBlack,
                    uncheckedBorderColor = Color.Gray
                )
            )
        }
    }
}