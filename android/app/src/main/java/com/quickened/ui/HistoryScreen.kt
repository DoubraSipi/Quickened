package com.quickened.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quickened.data.Session

val HISTORY_FILTERS = listOf("all", "gentle", "encouraging", "contemplative", "challenging", "favorites")

@Composable
fun HistoryScreen(
    sessions: List<Session>,
    filter: String,
    onFilter: (String) -> Unit,
    onReplay: (Session) -> Unit,
    onToggleFavorite: (Session) -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your moments", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = SerifHeadings))
        Text("${sessions.size} kept close", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(HISTORY_FILTERS) { f ->
                FilterChip(
                    selected = f == filter,
                    onClick = { onFilter(f) },
                    label = { Text(f.replaceFirstChar { it.uppercase() }) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
        if (sessions.isEmpty()) {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "No moments yet. Begin your first one from home.",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SerifHeadings)
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(sessions) { s ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                s.contentPreview,
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifHeadings)
                            )
                            Text(
                                "${s.tone.replaceFirstChar { it.uppercase() }} • ${s.activity} • ${s.durationSeconds}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row {
                                TextButton(onClick = { onReplay(s) }) { Text("↻ Replay") }
                                TextButton(onClick = { onToggleFavorite(s) }) {
                                    Text(if (s.isFavorite) "★ Kept" else "☆ Keep")
                                }
                            }
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("⤴ Export as text") }
        OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("Clear all") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("← Home") }
    }
}
