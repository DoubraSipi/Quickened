package com.quickened.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickened.content.Reflection

val TONES = listOf("gentle", "encouraging", "contemplative", "challenging")
val ACTIVITIES = listOf("walking", "resting")

private val ToneSymbol = mapOf(
    "gentle" to "✦",
    "encouraging" to "☀",
    "contemplative" to "☾",
    "challenging" to "➤"
)

private val ToneGradient = mapOf(
    "gentle" to listOf(Color(0xFF1D4ED8), Color(0xFF0F766E)),
    "encouraging" to listOf(Color(0xFF15803D), Color(0xFF0D9488)),
    "contemplative" to listOf(Color(0xFF6D28D9), Color(0xFF312E81)),
    "challenging" to listOf(Color(0xFFB45309), Color(0xFF78350F))
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
    verseOfDay: Reflection?,
    streakLine: String,
    statsLine: String
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Grow with God.\nEveryday.",
            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = SerifHeadings, lineHeight = 36.sp)
        )
        Text(streakLine, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

        // 1 — Daily verse hero
        if (verseOfDay != null) {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.background(
                        Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF0F766E)))
                    ).padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("TODAY'S VERSE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF99F6E4))
                        Text(
                            verseOfDay.text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = SerifHeadings, fontSize = 19.sp, lineHeight = 28.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 2 — Choose journey
        Text("Choose today's journey", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(TONES) { t ->
                val grad = ToneGradient[t] ?: listOf(Color(0xFF0F766E), Color(0xFF134E4A))
                JourneyCard(
                    label = t,
                    symbol = ToneSymbol[t] ?: "✦",
                    gradient = grad,
                    selected = t == tone,
                    onClick = { onTone(t) }
                )
            }
        }

        // 3 — Activity + begin
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
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(24.dp)
        ) { Text("▶  Begin today's moment", style = MaterialTheme.typography.titleLarge) }
        Text(statsLine, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onHistory) { Text("History") }
            TextButton(onClick = onSettings) { Text("Settings") }
        }
    }
}

@Composable
private fun JourneyCard(label: String, symbol: String, gradient: List<Color>, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(150.dp).height(190.dp),
        shape = RoundedCornerShape(22.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(3.dp, Color.White) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 8.dp else 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(gradient)).padding(14.dp)) {
            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                Text(symbol, fontSize = 28.sp, color = Color.White)
                Column {
                    Text(label.replaceFirstChar { it.uppercase() }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        if (selected) "● Selected" else "Tap to choose",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
