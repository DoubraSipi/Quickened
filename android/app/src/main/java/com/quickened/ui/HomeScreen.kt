package com.quickened.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val TONES = listOf("gentle", "encouraging", "contemplative", "challenging")
val ACTIVITIES = listOf("walking", "resting")

private val ToneSymbol = mapOf(
    "gentle" to "✦",
    "encouraging" to "☀",
    "contemplative" to "☾",
    "challenging" to "➤"
)

@Composable
fun HomeScreen(
    tone: String,
    onTone: (String) -> Unit,
    activity: String,
    onActivity: (String) -> Unit,
    onStart: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    lastTone: String?,
    statsLine: String
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Good day for faith.",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = SerifHeadings)
        )
        Text("Quickened", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Text(
                statsLine,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SerifHeadings, fontSize = 18.sp)
            )
        }
        Text("How should this moment feel?", style = MaterialTheme.typography.titleMedium)
        TONES.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { t ->
                    ToneCard(
                        label = t,
                        symbol = ToneSymbol[t] ?: "✦",
                        selected = t == tone,
                        onClick = { onTone(t) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Text("While you are…", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ACTIVITIES.forEach { a ->
                if (a == activity) {
                    Button(
                        onClick = { onActivity(a) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text(a.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold) }
                } else {
                    OutlinedButton(
                        onClick = { onActivity(a) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text(a.replaceFirstChar { it.uppercase() }) }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(68.dp),
            shape = RoundedCornerShape(24.dp)
        ) { Text("▶  Begin", style = MaterialTheme.typography.titleLarge) }
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onHistory) { Text("History") }
            TextButton(onClick = onSettings) { Text("Settings") }
        }
        if (lastTone != null) Text("Last moment: $lastTone", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ToneCard(label: String, symbol: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) ToneTint[label] ?: MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(
            2.dp, MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(symbol, fontSize = 20.sp)
            Text(label.replaceFirstChar { it.uppercase() }, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
