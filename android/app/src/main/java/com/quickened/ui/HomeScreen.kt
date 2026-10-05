package com.quickened.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Teal = Color(0xFF0D9488)

val TONES = listOf("gentle", "encouraging", "contemplative", "challenging")
val ACTIVITIES = listOf("walking", "resting")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    tone: String,
    onTone: (String) -> Unit,
    activity: String,
    onActivity: (String) -> Unit,
    onStart: () -> Unit,
    onHistory: () -> Unit,
    lastTone: String?,
    statsLine: String,
    voices: List<String>,
    voice: String,
    onVoice: (String) -> Unit,
    onDownloadVoices: () -> Unit
) {
    var voiceExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Quickened", style = MaterialTheme.typography.headlineLarge)
        Text(statsLine, style = MaterialTheme.typography.bodyMedium)
        if (lastTone != null) Text("Last tone: $lastTone", style = MaterialTheme.typography.bodyMedium)
        Text("Tone", style = MaterialTheme.typography.titleMedium)
        TONES.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { t ->
                    if (t == tone) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
        Text("Voice", style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(expanded = voiceExpanded, onExpandedChange = { voiceExpanded = !voiceExpanded }) {
            TextField(
                value = voice.ifEmpty { "System default" },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = voiceExpanded, onDismissRequest = { voiceExpanded = false }) {
                DropdownMenuItem(text = { Text("System default") }, onClick = { onVoice(""); voiceExpanded = false })
                voices.take(30).forEach { v ->
                    DropdownMenuItem(text = { Text(v) }, onClick = { onVoice(v); voiceExpanded = false })
                }
                DropdownMenuItem(
                    text = { Text("⬇ Download more voices (online, once)") },
                    onClick = { onDownloadVoices(); voiceExpanded = false }
                )
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
