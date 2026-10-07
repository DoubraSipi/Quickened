package com.quickened.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val THEMES = listOf("system", "light", "dark")
val TRANSLATIONS = listOf("simple", "kjv", "web", "asv")

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
    userName: String,
    onUserName: (String) -> Unit,
    translation: String,
    onTranslation: (String) -> Unit,
    reminderEnabled: Boolean,
    onReminderEnabled: (Boolean) -> Unit,
    reminderTime: String,
    onPickTime: () -> Unit,
    autoDetect: Boolean,
    onAutoDetect: (Boolean) -> Unit,
    syncEnabled: Boolean,
    onSyncEnabled: (Boolean) -> Unit,
    syncEndpoint: String,
    onSyncEndpoint: (String) -> Unit,
    lastSynced: String,
    onSyncNow: () -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    onMenu: () -> Unit
) {
    var voiceExpanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        DrawerTopBar(title = "Settings", onMenu = onMenu)
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = SerifHeadings))
        Text(
            "✓ Changes save automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Usual feeling", style = MaterialTheme.typography.titleMedium)
        TONES.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { t ->
                    if (t == preferredTone) {
                        BlendedButton(onClick = { onTone(t) }, modifier = Modifier.weight(1f)) {
                            Text(t.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        }
                    } else {
                        BlendedOutlineButton(onClick = { onTone(t) }, modifier = Modifier.weight(1f)) {
                            Text(t.replaceFirstChar { it.uppercase() }, fontSize = 17.sp)
                        }
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
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(16.dp)
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
        Text("Your name", style = MaterialTheme.typography.titleMedium)
        var nameDraft by remember { mutableStateOf(userName) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = nameDraft,
                onValueChange = { nameDraft = it },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )
            BlendedButton(onClick = {
                if (nameDraft.isNotBlank()) onUserName(nameDraft.trim())
            }) { Text("Save", fontSize = 15.sp) }
        }
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            THEMES.forEach { th ->
                if (th == theme) {
                    BlendedButton(onClick = { onTheme(th) }, modifier = Modifier.weight(1f)) {
                        Text(th.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                } else {
                    BlendedOutlineButton(onClick = { onTheme(th) }, modifier = Modifier.weight(1f)) {
                        Text(th.replaceFirstChar { it.uppercase() }, fontSize = 15.sp)
                    }
                }
            }
        }
        Text("Bible", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            TRANSLATIONS.forEach { tr ->
                if (tr == translation) {
                    BlendedButton(onClick = { onTranslation(tr) }, modifier = Modifier.weight(1f)) {
                        Text(tr.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                } else {
                    BlendedOutlineButton(onClick = { onTranslation(tr) }, modifier = Modifier.weight(1f)) {
                        Text(tr.replaceFirstChar { it.uppercase() }, fontSize = 15.sp)
                    }
                }
            }
        }
        Text("Daily reminder", style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (reminderEnabled) "On • $reminderTime" else "Off", fontSize = 17.sp)
            BlendedOutlineButton(onClick = { onReminderEnabled(!reminderEnabled) }) {
                Text(if (reminderEnabled) "Turn off" else "Turn on", fontSize = 17.sp)
            }
        }
        if (reminderEnabled) {
            BlendedOutlineButton(onClick = onPickTime, modifier = Modifier.fillMaxWidth()) {
                Text("Change time ($reminderTime)", fontSize = 17.sp)
            }
        }
        Text("Activity", style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (autoDetect) "Auto-detect on" else "Manual toggle", fontSize = 17.sp)
            BlendedOutlineButton(onClick = { onAutoDetect(!autoDetect) }) {
                Text(if (autoDetect) "Use manual" else "Auto-detect", fontSize = 17.sp)
            }
        }
        Text("Cloud backup (optional)", style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (syncEnabled) "On" else "Off", fontSize = 17.sp)
            BlendedOutlineButton(onClick = { onSyncEnabled(!syncEnabled) }) {
                Text(if (syncEnabled) "Turn off" else "Turn on", fontSize = 17.sp)
            }
        }
        if (syncEnabled) {
            var draft by remember { mutableStateOf(syncEndpoint) }
            TextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("Sync URL (your Netlify function)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            BlendedOutlineButton(onClick = { onSyncEndpoint(draft) }, modifier = Modifier.fillMaxWidth()) {
                Text("Save URL", fontSize = 17.sp)
            }
            if (lastSynced.isNotEmpty()) Text("Last synced: $lastSynced")
            BlendedOutlineButton(onClick = onSyncNow, modifier = Modifier.fillMaxWidth()) {
                Text("⤴ Sync now", fontSize = 17.sp)
            }
        }
        Text("Privacy", style = MaterialTheme.typography.titleMedium)
        Card(shape = RoundedCornerShape(20.dp)) {
            Text(
                "Quickened works fully offline. No accounts, no analytics, no network requests. " +
                    "All preferences and moments stay on this device. You can delete them at any time.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        BlendedOutlineButton(onClick = onClearHistory, modifier = Modifier.fillMaxWidth()) {
            Text("Clear all moments", fontSize = 17.sp)
        }
        BlendedOutlineButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("← Home", fontSize = 17.sp) }
    }
    }
}
