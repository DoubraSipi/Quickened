package com.quickened

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.quickened.content.ContentRepository
import com.quickened.content.ExportHelper
import com.quickened.content.Reflection
import com.quickened.content.StatsCalculator
import com.quickened.data.AppDatabase
import com.quickened.data.QuickendStore
import com.quickened.data.Session
import com.quickened.tts.TtsManager
import com.quickened.ui.ExperienceScreen
import com.quickened.ui.HistoryScreen
import com.quickened.ui.HomeScreen
import com.quickened.ui.QuickenedTheme
import com.quickened.ui.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var store: QuickendStore
    private lateinit var tts: TtsManager
    private var themeState = mutableStateOf("system")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(
            applicationContext, AppDatabase::class.java, "quickened.db"
        ).fallbackToDestructiveMigration().build()
        store = QuickendStore(db)
        tts = TtsManager(this)
        setContent {
            val theme by themeState
            val sysDark = androidx.compose.foundation.isSystemInDarkTheme()
            val dark = theme == "dark" || (theme == "system" && sysDark)
            QuickenedTheme(dark = dark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QuickenedApp(theme = theme, onTheme = { themeState.value = it })
                }
            }
        }
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }

    private sealed interface Screen {
        data object Home : Screen
        data class Experience(val reflection: Reflection, val tone: String, val activity: String, val startMs: Long) : Screen
        data object History : Screen
        data object Settings : Screen
    }

    @Composable
    private fun QuickenedApp(theme: String, onTheme: (String) -> Unit) {
        val scope = rememberCoroutineScope()
        var screen by remember { mutableStateOf<Screen>(Screen.Home) }
        var tone by remember { mutableStateOf("gentle") }
        var activity by remember { mutableStateOf("walking") }
        var sessions by remember { mutableStateOf<List<Session>>(emptyList()) }
        var filter by remember { mutableStateOf("all") }
        var voice by remember { mutableStateOf("") }
        var voices by remember { mutableStateOf<List<String>>(emptyList()) }
        var statsLine by remember { mutableStateOf("Begin your first moment with God today.") }
        var streakLine by remember { mutableStateOf("Day 1 — a new rhythm begins.") }
        var verseOfDay by remember { mutableStateOf<Reflection?>(null) }

        fun persistSettings() {
            scope.launch {
                store.saveSettings(store.getSettings().copy(preferredTone = tone, voice = voice, theme = theme))
            }
        }

        fun refreshAll() {
            scope.launch {
                val all = store.getSessions()
                val stats = StatsCalculator.compute(all)
                statsLine = StatsCalculator.encouragement(stats)
                streakLine = if (stats.streakDays >= 2) "🔥 ${stats.streakDays}-day rhythm"
                else if (stats.totalSessions > 0) "🌱 Rhythm started — day 1"
                else "🌱 Day 1 — a new rhythm begins."
                sessions = if (filter == "favorites") all.filter { it.isFavorite }
                else store.getSessionsByTone(filter)
            }
        }

        LaunchedEffect(Unit) {
            tts.onReady = { scope.launch { voices = tts.availableVoices() } }
            val st = store.getSettings()
            tone = st.preferredTone
            voice = st.voice
            onTheme(st.theme)
            if (voice.isNotEmpty()) tts.setVoice(voice)
            for (i in 0 until 6) {
                voices = tts.availableVoices()
                if (voices.isNotEmpty()) break
                kotlinx.coroutines.delay(1000)
            }
            val verses = ContentRepository.load(this@MainActivity, "gentle")
            if (verses.isNotEmpty()) {
                verseOfDay = verses[java.time.LocalDate.now().dayOfYear % verses.size]
            }
            refreshAll()
        }

        fun finishExperience(exp: Screen.Experience) {
            val secs = ((System.currentTimeMillis() - exp.startMs) / 1000).toInt().coerceAtLeast(1)
            scope.launch {
                store.addSession(exp.tone, exp.activity, exp.reflection.title, secs)
                refreshAll()
                screen = Screen.Home
            }
        }

        when (val s = screen) {
            is Screen.Home -> HomeScreen(
                tone = tone, onTone = { tone = it; persistSettings() },
                activity = activity, onActivity = { activity = it },
                onStart = {
                    val r = ContentRepository.random(this@MainActivity, tone)
                    screen = Screen.Experience(r, tone, activity, System.currentTimeMillis())
                },
                onHistory = { refreshAll(); screen = Screen.History },
                onSettings = { screen = Screen.Settings },
                verseOfDay = verseOfDay,
                streakLine = streakLine,
                statsLine = statsLine
            )
            is Screen.Experience -> ExperienceScreen(
                reflection = s.reflection, tone = s.tone, activity = s.activity, tts = tts,
                onDone = { finishExperience(s) },
                onStop = { finishExperience(s) }
            )
            is Screen.History -> HistoryScreen(
                sessions = sessions,
                filter = filter,
                onFilter = {
                    filter = it
                    refreshAll()
                },
                onReplay = { sess -> tts.speak(sess.contentPreview) },
                onToggleFavorite = { sess ->
                    scope.launch {
                        store.setFavorite(sess.id, !sess.isFavorite)
                        refreshAll()
                    }
                },
                onExport = {
                    scope.launch {
                        val all = store.getSessions()
                        ExportHelper.share(this@MainActivity, all)
                    }
                },
                onClear = { scope.launch { store.clearHistory(); refreshAll() } },
                onBack = { screen = Screen.Home }
            )
            is Screen.Settings -> SettingsScreen(
                preferredTone = tone,
                onTone = { tone = it; persistSettings() },
                voices = voices,
                voice = voice,
                onVoice = {
                    voice = it
                    if (it.isNotEmpty()) tts.setVoice(it)
                    persistSettings()
                },
                onDownloadVoices = {
                    startActivity(android.content.Intent("com.android.settings.TTS_SETTINGS"))
                },
                theme = theme,
                onTheme = {
                    onTheme(it)
                    scope.launch {
                        store.saveSettings(store.getSettings().copy(theme = it))
                    }
                },
                onClearHistory = { scope.launch { store.clearHistory(); refreshAll() } },
                onBack = { screen = Screen.Home }
            )
        }
    }
}
