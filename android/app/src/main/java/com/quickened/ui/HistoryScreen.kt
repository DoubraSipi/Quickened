package com.quickened.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quickened.data.Session

@Composable
fun HistoryScreen(
    sessions: List<Session>,
    onReplay: (Session) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("History", style = MaterialTheme.typography.headlineMedium)
        if (sessions.isEmpty()) {
            Text("No sessions yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(sessions) { s ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(s.contentPreview, style = MaterialTheme.typography.titleSmall)
                            Text("${s.tone} • ${s.activity} • ${s.durationSeconds}s",
                                style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { onReplay(s) }) { Text("Replay preview") }
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Clear history") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Back") }
    }
}
