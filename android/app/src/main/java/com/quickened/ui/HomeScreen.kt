package com.quickened.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Teal = Color(0xFF0D9488)

val TONES = listOf("gentle", "encouraging", "contemplative", "challenging")
val ACTIVITIES = listOf("walking", "resting")

@Composable
fun HomeScreen(
    tone: String,
    onTone: (String) -> Unit,
    activity: String,
    onActivity: (String) -> Unit,
    onStart: () -> Unit,
    onHistory: () -> Unit,
    lastTone: String?
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Quickened", style = MaterialTheme.typography.headlineLarge)
        Text("Short spoken Scripture for ordinary moments.", style = MaterialTheme.typography.bodyLarge)
        if (lastTone != null) Text("Last tone: $lastTone", style = MaterialTheme.typography.bodyMedium)
        Text("Tone", style = MaterialTheme.typography.titleMedium)
        TONES.chunked(2).forEach { row ->
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { t ->
                    val selected = t == tone
                    if (selected) {
                        Button(
                            onClick = { onTone(t) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal)
                        ) { Text(t) }
                    } else {
                        OutlinedButton(
                            onClick = { onTone(t) },
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) { Text(t) }
                    }
                }
            }
        }
        Text("Activity", style = MaterialTheme.typography.titleMedium)
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ACTIVITIES.forEach { a ->
                if (a == activity) {
                    Button(
                        onClick = { onActivity(a) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal)
                    ) { Text(a) }
                } else {
                    OutlinedButton(
                        onClick = { onActivity(a) },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) { Text(a) }
                }
            }
        }
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal)
        ) { Text("▶ Start Experience", style = MaterialTheme.typography.titleLarge) }
        OutlinedButton(
            onClick = onHistory,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("History") }
    }
}
