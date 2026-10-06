package com.quickened.watch

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import org.json.JSONArray
import java.util.Locale

data class Verse(val title: String, val text: String)

fun loadVerses(context: Context): List<Verse> {
    return try {
        // NOTE: duplicate of phone assets/content/gentle.json until a shared module exists.
        val json = context.assets.open("gentle.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Verse(o.getString("title"), o.getString("text"))
        }
    } catch (e: Exception) {
        listOf(Verse("Quiet moment", "Be still, and know that I am God."))
    }
}

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
        }
        setContent { WatchApp() }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }

    @Composable
    private fun WatchApp() {
        var verses by remember { mutableStateOf(loadVerses(this)) }
        var index by remember { mutableStateOf(java.time.LocalDate.now().dayOfYear % verses.size) }
        var speaking by remember { mutableStateOf(false) }

        DisposableEffect(Unit) { onDispose { tts?.stop() } }

        MaterialTheme {
            ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    TimeText()
                }
                item {
                    Text(
                        verses[index].title,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text(
                        verses[index].text,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Button(
                        onClick = {
                            if (speaking) {
                                tts?.stop()
                                speaking = false
                            } else {
                                tts?.speak(verses[index].text, TextToSpeech.QUEUE_FLUSH, null, "q-1")
                                speaking = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (speaking) "Stop" else "Speak")
                    }
                }
                item {
                    Chip(
                        label = { Text("Next verse") },
                        onClick = {
                            tts?.stop()
                            speaking = false
                            index = (index + 1) % verses.size
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
