package com.quickened.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickened.data.PrayerWatch

@Composable
fun PrayerScreen(
    watches: List<PrayerWatch>,
    onAdd: (String) -> Unit,
    onToggle: (PrayerWatch) -> Unit,
    onDelete: (PrayerWatch) -> Unit,
    onBack: () -> Unit,
    onMenu: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize()) {
        DrawerTopBar(title = "Prayer watch", onMenu = onMenu)
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Set watches; an alarm calls each one daily.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("e.g. Morning mercy") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
                BlendedButton(onClick = {
                    if (draft.isNotBlank()) {
                        onAdd(draft.trim())
                        draft = ""
                    }
                }) { Text("+ Add", fontSize = 17.sp) }
            }
            if (watches.isEmpty()) {
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No watches yet. Add one above — dawn, noon, night.",
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SerifHeadings)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    items(watches, key = { it.id }) { w ->
                        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        w.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifHeadings)
                                    )
                                    Text(
                                        "%02d:%02d daily • %s".format(w.hour, w.minute, if (w.enabled) "on" else "off"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                BlendedOutlineButton(onClick = { onToggle(w) }) {
                                    Text(if (w.enabled) "On" else "Off", fontSize = 15.sp)
                                }
                                BlendedOutlineButton(onClick = { onDelete(w) }) {
                                    Text("✕", fontSize = 15.sp)
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
