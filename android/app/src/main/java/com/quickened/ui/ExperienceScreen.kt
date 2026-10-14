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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickened.activity.ActivityDetector
import com.quickened.content.Reflection
import com.quickened.content.spokenRef
import com.quickened.piper.KokoroSpeaker
import com.quickened.piper.PiperSpeaker
import com.quickened.piper.StudioSpeaker
import com.quickened.tts.TtsManager

@Composable
fun ExperienceScreen(
    reflection: Reflection,
    tone: String,
    activity: String,
    tts: TtsManager,
    piper: PiperSpeaker?,
    kokoro: KokoroSpeaker?,
    studio: StudioSpeaker?,
    studioUrl: String,
    studioKey: String,
    translation: String,
    onTranslation: (String) -> Unit,
    onNext: (Reflection, (Reflection) -> Unit) -> Unit,
    onStop: (firstTitle: String, count: Int) -> Unit
) {
    var current by remember { mutableStateOf(reflection) }
    var paused by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(1) }
    var preparing by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val rate = ActivityDetector.speechRate(activity)
    val shownText = if (translation != "simple" && current.alt.containsKey(translation)) {
        current.alt.getValue(translation)
    } else {
        current.text
    }
    // Render-ahead: cache this verse's audio while the user reads, so
    // Listen starts instantly. Best-effort; speak() works without it.
    LaunchedEffect(current, translation, kokoro) {
        try {
            val k = kokoro
            if (k != null && k.ready) k.prefetch(spokenRef(shownText))
        } catch (e: Exception) {
            // ignore
        }
    }

    fun speak(text: String, onDone: () -> Unit) {
        val spoken = spokenRef(text)
        preparing = kokoro != null || piper != null || studio != null
        com.quickened.service.QuickenedAudioService.start(context, current.title, spoken)
        if (studio != null && studioUrl.isNotBlank()) {
            studio.speak(studioUrl, studioKey, spoken, onDone, {
                tts.onDone = onDone
                tts.speak(spoken, rate)
            }) { preparing = false }
        } else if (kokoro != null) {
            kokoro.speak(spoken, onDone, {
                tts.onDone = onDone
                tts.speak(spoken, rate)
            }) { preparing = false }
        } else if (piper != null) {
            piper.speak(spoken, onDone, {
                tts.onDone = onDone
                tts.speak(spoken, rate)
            })
            preparing = false
        } else {
            preparing = false
            tts.onDone = onDone
            tts.speak(spoken, rate)
        }
    }

    fun halt() {
        com.quickened.service.QuickenedAudioService.stop(context)
        try {
            tts.stop()
        } catch (e: Exception) {
            // ignore
        }
        try {
            piper?.stop()
        } catch (e: Exception) {
            // ignore
        }
        try {
            kokoro?.stop()
        } catch (e: Exception) {
            // ignore
        }
        try {
            studio?.stop()
        } catch (e: Exception) {
            // ignore
        }
    }

    LaunchedEffect(current, paused, translation) {
        if (!paused) speak(shownText) {
            if (!paused) onNext(current) { next ->
                current = next
                count++
            }
        } else halt()
    }
    DisposableEffect(Unit) {
        onDispose {
            halt()
            tts.onDone = null
        }
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
            when {
                paused -> "❚❚ Paused"
                preparing -> "◌ Preparing voice…"
                else -> "♪ Listening… (#$count)"
            },
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
                    current.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = SerifHeadings),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (current.type == "verse") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("simple", "kjv", "web", "asv").chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                pair.forEach { tr ->
                                    val label = if (tr == "simple") "Simple" else tr.uppercase()
                                    TextButton(onClick = { onTranslation(tr) }) {
                                        Text(
                                            label,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            fontWeight = if (tr == translation) FontWeight.Bold else FontWeight.Normal,
                                            color = if (tr == translation) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
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
                onClick = { paused = !paused },
                modifier = Modifier.weight(1f)
            ) { Text(if (paused) "▶ Resume" else "❚❚ Pause") }
            BlendedOutlineButton(
                onClick = { halt(); onStop(reflection.title, count) },
                modifier = Modifier.weight(1f)
            ) { Text("■ Stop") }
        }
    }
}
