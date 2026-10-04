package com.quickened.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quickened.content.Reflection
import com.quickened.tts.TtsManager

private val Teal = Color(0xFF0D9488)

@Composable
fun ExperienceScreen(
    reflection: Reflection,
    tone: String,
    activity: String,
    tts: TtsManager,
    onDone: () -> Unit,
    onStop: () -> Unit
) {
    LaunchedEffect(reflection) {
        tts.onDone = onDone
        tts.speak(reflection.text)
    }
    DisposableEffect(Unit) {
        onDispose { tts.onDone = null }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("$tone • $activity", style = MaterialTheme.typography.labelLarge)
        Text(reflection.title, style = MaterialTheme.typography.headlineSmall)
        Text(reflection.text, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { tts.speak(reflection.text) },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) { Text("Replay") }
            OutlinedButton(
                onClick = { tts.stop(); onStop() },
                modifier = Modifier.weight(1f).height(56.dp)
            ) { Text("Stop") }
        }
    }
}
