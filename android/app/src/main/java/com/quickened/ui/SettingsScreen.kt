package com.quickened.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
val THEMES = listOf("system", "light", "dark")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferredTone: String,
    onTone: (String) -> Unit,
    voices: List<String>,
    voice: String,
    onVoice: (String) -> Unit,
    onDownloadVoices: () -> Unit,
    theme: String,
    onTheme: (String) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit
) {
    var voiceExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Default tone", style = MaterialTheme.typography.titleMedium)
        TONES.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { t ->
                    if (t == preferredTone) {
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
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            THEMES.forEach { th ->
                if (th == theme) {
                    Button(
                        onClick = { onTheme(th) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal)
                    ) { Text(th) }
                } else {
                    OutlinedButton(
                        onClick = { onTheme(th) },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) { Text(th) }
                }
            }
        }
        Text("Privacy", style = MaterialTheme.typography.titleMedium)
        Text(
            "Quickened works fully offline. No accounts, no analytics, no network requests. " +
                "All preferences and history stay on this device. You can delete history at any time.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedButton(onClick = onClearHistory, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Clear history")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Back") }
    }
}
