package com.quickened.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
val ACTIVITIES = listOf("walking", "resting", "stationary", "driving", "workout")

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
    onFreshWord: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onMenu: () -> Unit,
    userName: String,
    verseOfDay: Reflection?,
    streakLine: String,
    statsLine: String
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DrawerTopBar(title = "Quickened", onMenu = onMenu)
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Grow with God.\nEveryday.",
            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = SerifHeadings, lineHeight = 36.sp)
        )
        Text(
            if (userName.isNotBlank()) "Good day, $userName." else "Good day for faith.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
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
        ACTIVITIES.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { a ->
                    if (a == activity) {
                        BlendedButton(onClick = { onActivity(a) }, modifier = Modifier.weight(1f)) {
                            Text(a.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        }
                    } else {
                        BlendedOutlineButton(onClick = { onActivity(a) }, modifier = Modifier.weight(1f)) {
                            Text(a.replaceFirstChar { it.uppercase() }, fontSize = 17.sp)
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        BlendedButton(onClick = onStart, modifier = Modifier.fillMaxWidth(), height = 64.dp) {
            Text("▶  Begin today's moment", style = MaterialTheme.typography.titleLarge)
        }
        OutlinedButton(
            onClick = onFreshWord,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) { Text("✨ Fresh word") }
        Text(statsLine, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onHistory) { Text("History") }
            TextButton(onClick = onSettings) { Text("Settings") }
        }
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
            ToneMotif(label = label)
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

@Composable
private fun ToneMotif(label: String) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val soft = Color.White.copy(alpha = 0.14f)
        when (label) {
            "gentle" -> {
                // soft waves
                for (i in 0..3) {
                    drawArc(
                        color = soft,
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(-size.width * 0.3f, size.height * (0.15f + i * 0.22f)),
                        size = androidx.compose.ui.geometry.Size(size.width * 1.6f, size.height * 0.5f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                }
            }
            "encouraging" -> {
                // sunburst
                val c = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.2f)
                drawCircle(soft, radius = 22.dp.toPx(), center = c)
                for (a in 0 until 12) {
                    val ang = a * 30f * Math.PI.toFloat() / 180f
                    drawLine(
                        soft,
                        c + androidx.compose.ui.geometry.Offset(kotlin.math.cos(ang) * 30.dp.toPx(), kotlin.math.sin(ang) * 30.dp.toPx()),
                        c + androidx.compose.ui.geometry.Offset(kotlin.math.cos(ang) * 48.dp.toPx(), kotlin.math.sin(ang) * 48.dp.toPx()),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
            "contemplative" -> {
                // moon + stars
                val c = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.25f)
                drawCircle(Color.White.copy(alpha = 0.2f), radius = 20.dp.toPx(), center = c)
                drawCircle(Color.Transparent, radius = 1.dp.toPx(), center = c)
                listOf(
                    androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.15f),
                    androidx.compose.ui.geometry.Offset(size.width * 0.4f, size.height * 0.45f),
                    androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.7f)
                ).forEach { drawCircle(soft, radius = 2.5.dp.toPx(), center = it) }
            }
            else -> {
                // upward path chevrons
                for (i in 0..2) {
                    val y = size.height * (0.75f - i * 0.22f)
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.2f, y)
                        lineTo(size.width * 0.5f, y - 18.dp.toPx())
                        lineTo(size.width * 0.8f, y)
                    }
                    drawPath(path, soft, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
                }
            }
        }
    }
}
