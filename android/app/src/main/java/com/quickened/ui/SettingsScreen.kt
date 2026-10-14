package com.quickened.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.quickened.piper.KokoroCatalog
import com.quickened.piper.PiperCatalog
import com.quickened.piper.PiperVoices

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
    piperStates: Map<String, PiperVoices.Status>,
    piperSelected: String,
    onPiperDownload: (String) -> Unit,
    onPiperDelete: (String) -> Unit,
    onPiperSelect: (String) -> Unit,
    kokoroModelReady: Boolean,
    kokoroWorking: Boolean,
    kokoroVoicesReady: Set<String>,
    onKokoroModelDownload: () -> Unit,
    onKokoroDownload: (String) -> Unit,
    onKokoroDelete: (String) -> Unit,
    onKokoroSelect: (String) -> Unit,
    studioUrl: String,
    studioKey: String,
    onStudioSave: (String, String) -> Unit,
    onStudioSelect: () -> Unit,
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
                            Text(t.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                        }
                    } else {
                        BlendedOutlineButton(onClick = { onTone(t) }, modifier = Modifier.weight(1f)) {
                            Text(t.replaceFirstChar { it.uppercase() }, fontSize = 15.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
        Text("Voice", style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(expanded = voiceExpanded, onExpandedChange = { voiceExpanded = !voiceExpanded }) {
            TextField(
                value = if (piperSelected == "studio") {
                    "✦ Studio voice"
                } else if (piperSelected.startsWith("kokoro:")) {
                    "✦ " + (KokoroCatalog.byId(piperSelected.removePrefix("kokoro:"))?.label ?: piperSelected)
                } else if (piperSelected.isNotEmpty()) {
                    "✦ " + (PiperCatalog.byId(piperSelected)?.label ?: piperSelected)
                } else voice.ifEmpty { "System default" },
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
                PiperCatalog.voices.filter { piperStates[it.id] is PiperVoices.Status.Ready }.forEach { v ->
                    DropdownMenuItem(
                        text = { Text("✦ ${v.label} (Quickened)" + if (v.id == PiperCatalog.RECOMMENDED) " ★" else "") },
                        onClick = { onVoice("piper:${v.id}"); voiceExpanded = false }
                    )
                }
                KokoroCatalog.voices.filter { kokoroVoicesReady.contains(it.id) }.forEach { v ->
                    DropdownMenuItem(
                        text = { Text("✦ ${v.label} (Kokoro)") },
                        onClick = { onVoice("kokoro:${v.id}"); voiceExpanded = false }
                    )
                }
                if (studioUrl.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("✦ Studio voice (online)") },
                        onClick = { onVoice("studio"); voiceExpanded = false }
                    )
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
        Text("Quickened voices (Piper)", style = MaterialTheme.typography.titleMedium)
        Text(
            "Download once over Wi-Fi, then offline. In-app playback arrives with the native voice engine.",
            style = MaterialTheme.typography.bodySmall
        )
        PiperCatalog.voices.forEach { v ->
            val st = piperStates[v.id] ?: PiperVoices.Status.Missing
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            v.label + if (v.id == PiperCatalog.RECOMMENDED) " ★ Recommended" else "",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "~${v.sizeMb}MB • " + when (st) {
                                is PiperVoices.Status.Ready -> if (v.id == piperSelected) "ready • selected" else "ready"
                                is PiperVoices.Status.Downloading -> "downloading…"
                                else -> "not downloaded"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    when (st) {
                        is PiperVoices.Status.Missing -> BlendedOutlineButton(onClick = { onPiperDownload(v.id) }) {
                            Text("Get", fontSize = 15.sp)
                        }
                        is PiperVoices.Status.Downloading -> Text("…", fontSize = 17.sp)
                        is PiperVoices.Status.Ready -> {
                            if (v.id != piperSelected) {
                                BlendedOutlineButton(onClick = { onPiperSelect(v.id) }) {
                                    Text("Use", fontSize = 15.sp)
                                }
                            }
                            androidx.compose.material3.TextButton(onClick = { onPiperDelete(v.id) }) {
                                Text("remove", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
        Text("Quickened voices (Kokoro)", style = MaterialTheme.typography.titleMedium)
        Text(
            "Natural voices. One 177MB engine download shared by all, then voice packs. Offline after.",
            style = MaterialTheme.typography.bodySmall
        )
        if (!kokoroModelReady) {
            BlendedOutlineButton(onClick = onKokoroModelDownload, modifier = Modifier.fillMaxWidth()) {
                Text(if (kokoroWorking) "Downloading engine…" else "Get voice engine (~177MB)", fontSize = 15.sp)
            }
        }
        KokoroCatalog.voices.forEach { v ->
            val ready = kokoroVoicesReady.contains(v.id)
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(v.label, style = MaterialTheme.typography.titleSmall)
                        Text(
                            when {
                                !kokoroModelReady -> "needs engine"
                                ready && "kokoro:${v.id}" == piperSelected -> "ready • selected"
                                ready -> "ready"
                                else -> "not downloaded"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (kokoroModelReady && !ready) {
                        BlendedOutlineButton(onClick = { onKokoroDownload(v.id) }) {
                            Text("Get", fontSize = 15.sp)
                        }
                    } else if (ready) {
                        if ("kokoro:${v.id}" != piperSelected) {
                            BlendedOutlineButton(onClick = { onKokoroSelect(v.id) }) {
                                Text("Use", fontSize = 15.sp)
                            }
                        }
                        androidx.compose.material3.TextButton(onClick = { onKokoroDelete(v.id) }) {
                            Text("remove", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        var studioOpen by remember { mutableStateOf(studioUrl.isNotBlank()) }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Studio voice (needs server)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            BlendedOutlineButton(onClick = { studioOpen = !studioOpen }) {
                Text(if (studioOpen) "Hide" else "Set up", fontSize = 14.sp)
            }
        }
        if (studioOpen) {
        Text(
            "Server-rendered top voice. Needs internet once per moment, then cached offline.",
            style = MaterialTheme.typography.bodySmall
        )
        var urlDraft by remember { mutableStateOf(studioUrl) }
        var keyDraft by remember { mutableStateOf(studioKey) }
        TextField(
            value = urlDraft,
            onValueChange = { urlDraft = it },
            label = { Text("Server URL (https://…)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        TextField(
            value = keyDraft,
            onValueChange = { keyDraft = it },
            label = { Text("Server key") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            BlendedOutlineButton(onClick = { onStudioSave(urlDraft.trim(), keyDraft.trim()) }, modifier = Modifier.weight(1f)) {
                Text("Save", fontSize = 15.sp)
            }
            BlendedButton(onClick = { onStudioSelect() }, modifier = Modifier.weight(1f)) {
                Text(if (piperSelected == "studio") "Selected ✓" else "Use Studio", fontSize = 15.sp)
            }
        }
        }
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            THEMES.forEach { th ->
                if (th == theme) {
                    BlendedButton(onClick = { onTheme(th) }, modifier = Modifier.weight(1f)) {
                        Text(th.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                    }
                } else {
                    BlendedOutlineButton(onClick = { onTheme(th) }, modifier = Modifier.weight(1f)) {
                        Text(th.replaceFirstChar { it.uppercase() }, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
        Text("Bible", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            TRANSLATIONS.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { tr ->
                    val label = if (tr == "simple") "Simple" else tr.uppercase()
                    if (tr == translation) {
                        BlendedButton(onClick = { onTranslation(tr) }, modifier = Modifier.weight(1f)) {
                            Text(
                                label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    } else {
                        BlendedOutlineButton(onClick = { onTranslation(tr) }, modifier = Modifier.weight(1f)) {
                            Text(label, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
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
