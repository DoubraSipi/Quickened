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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickened.activity.ActivityDetector
import com.quickened.content.Reflection
import com.quickened.tts.TtsManager

@Composable
fun ExperienceScreen(
    reflection: Reflection,
    tone: String,
    activity: String,
    tts: TtsManager,
    translation: String,
    onTranslation: (String) -> Unit,
    onDone: () -> Unit,
    onStop: () -> Unit
) {
    val rate = ActivityDetector.speechRate(activity)
    val shownText = if (translation != "simple" && reflection.alt.containsKey(translation)) {
        reflection.alt.getValue(translation)
    } else {
        reflection.text
    }
    LaunchedEffect(reflection, translation) {
        tts.onDone = onDone
        tts.speak(shownText, rate)
    }
    DisposableEffect(Unit) {
        onDispose { tts.onDone = null }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "$tone • $activity",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "♪ Listening…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    reflection.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = SerifHeadings),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (reflection.type == "verse") {
                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        listOf("simple", "kjv", "web", "asv").forEach { tr ->
                            TextButton(onClick = { onTranslation(tr) }) {
                                Text(
                                    if (tr == "simple") "Simple" else "KJV",
                                    fontWeight = if (tr == translation) FontWeight.Bold else FontWeight.Normal,
                                    color = if (tr == translation) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
                Text(
                    shownText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = SerifHeadings, fontSize = 21.sp, lineHeight = 33.sp
                    )
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            BlendedButton(
                onClick = { tts.speak(shownText, rate) },
                modifier = Modifier.weight(1f)
            ) { Text("↻ Replay") }
            BlendedOutlineButton(
                onClick = { tts.stop(); onStop() },
                modifier = Modifier.weight(1f)
            ) { Text("■ Stop") }
        }
    }
}
