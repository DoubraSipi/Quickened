package com.quickened.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickened.data.FAST_PLANS
import com.quickened.data.FastRecord
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@Composable
fun FastingScreen(
    active: FastRecord?,
    history: List<FastRecord>,
    completedCount: Int,
    customHours: String,
    onCustomHours: (String) -> Unit,
    intention: String,
    onIntention: (String) -> Unit,
    onStart: (kind: String, title: String, hours: Int) -> Unit,
    onEnd: (completed: Boolean) -> Unit,
    onBack: () -> Unit,
    onMenu: () -> Unit
) {
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    if (active != null) {
        LaunchedEffect(active.id) {
            while (true) {
                delay(1000)
                nowMs = System.currentTimeMillis()
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        DrawerTopBar(title = "Fasting", onMenu = onMenu)
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Fast and pray", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = SerifHeadings))
            if (active != null) {
                val start = Instant.parse(active.startedAt).toEpochMilli()
                val elapsedH = (nowMs - start) / 3_600_000.0
                val progress = (elapsedH / active.targetHours).toFloat().coerceIn(0f, 1f)
                val remain = Duration.ofMillis((active.targetHours * 3_600_000L - (nowMs - start)).coerceAtLeast(0))
                Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(active.title, style = MaterialTheme.typography.titleLarge.copy(fontFamily = SerifHeadings))
                        Text(
                            "%dh %02dm elapsed • %dh %02dm left".format(
                                elapsedH.toInt(), ((elapsedH % 1) * 60).toInt(),
                                remain.toHours(), (remain.toMinutes() % 60)
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        if (active.intention.isNotBlank()) {
                            Text("Praying for: ${active.intention}", style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            "“${active.verseText}”",
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SerifHeadings, fontSize = 18.sp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            BlendedButton(onClick = { onEnd(true) }, modifier = Modifier.weight(1f)) {
                                Text("Complete", fontSize = 17.sp)
                            }
                            BlendedOutlineButton(onClick = { onEnd(false) }, modifier = Modifier.weight(1f)) {
                                Text("Break fast", fontSize = 17.sp)
                            }
                        }
                    }
                }
            } else {
                Text("Choose your fast", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = intention,
                    onValueChange = onIntention,
                    label = { Text("Prayer focus (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                FAST_PLANS.forEach { plan ->
                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plan.title, style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifHeadings))
                                Text(plan.note, style = MaterialTheme.typography.bodySmall)
                            }
                            BlendedButton(onClick = { onStart(plan.kind, plan.title, plan.hours) }) {
                                Text("Start", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customHours,
                        onValueChange = onCustomHours,
                        label = { Text("Custom hours") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                    BlendedOutlineButton(onClick = {
                        customHours.toIntOrNull()?.takeIf { it in 1..720 }?.let {
                            onStart("custom", "Custom fast — ${it}h", it)
                        }
                    }) { Text("Start", fontSize = 16.sp) }
                }
                Text(
                    "Completed fasts: $completedCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (history.isNotEmpty()) {
                    Text("Recent fasts", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(220.dp)) {
                        items(history) { f ->
                            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(f.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${f.status} • ${f.targetHours}h" + if (f.intention.isNotBlank()) " • ${f.intention}" else "",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
            BlendedOutlineButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("← Home", fontSize = 17.sp) }
        }
    }
}
