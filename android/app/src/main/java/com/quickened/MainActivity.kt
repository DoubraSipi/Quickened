package com.quickened

import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import com.quickened.content.FreshWord
import com.quickened.content.Reflection
import com.quickened.content.StatsCalculator
import com.quickened.data.AppDatabase
import com.quickened.data.QuickendStore
import com.quickened.data.Session
import com.quickened.activity.ActivityDetector
import com.quickened.reminder.ReminderScheduler
import com.quickened.sync.SyncManager
import com.quickened.tts.TtsManager
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import com.quickened.ui.DrawerAction
import com.quickened.ui.ExperienceScreen
import com.quickened.ui.HistoryScreen
import com.quickened.ui.HomeScreen
import com.quickened.ui.QuickenedDrawer
import com.quickened.ui.QuickenedTheme
import com.quickened.ui.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var store: QuickendStore
    private lateinit var tts: TtsManager
    private var themeState = mutableStateOf("system")
    private var activityState = mutableStateOf("walking")

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val activityPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) ActivityDetector.start(this) { detected ->
            runOnUiThread { activityState.value = detected }
        }
    }

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

    private fun ensureActivityDetection() {
        if (Build.VERSION.SDK_INT >= 29 &&
            checkSelfPermission("android.permission.ACTIVITY_RECOGNITION") != PackageManager.PERMISSION_GRANTED
        ) {
            activityPermission.launch("android.permission.ACTIVITY_RECOGNITION")
        } else {
            ActivityDetector.start(this) { detected ->
                runOnUiThread { activityState.value = detected }
            }
        }
    }

    private fun toast(msg: String) {
        runOnUiThread {
            android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
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
        var manualActivity by remember { mutableStateOf("walking") }
        var autoDetect by remember { mutableStateOf(false) }
        val detected by activityState
        val activity = if (autoDetect && detected != "unknown") detected else manualActivity
        var sessions by remember { mutableStateOf<List<Session>>(emptyList()) }
        var filter by remember { mutableStateOf("all") }
        var voice by remember { mutableStateOf("") }
        var voices by remember { mutableStateOf<List<String>>(emptyList()) }
        var statsLine by remember { mutableStateOf("Begin your first moment with God today.") }
        var streakLine by remember { mutableStateOf("Day 1 — a new rhythm begins.") }
        var verseOfDay by remember { mutableStateOf<Reflection?>(null) }
        var reminderOn by remember { mutableStateOf(false) }
        var reminderHour by remember { mutableStateOf(7) }
        var reminderMinute by remember { mutableStateOf(0) }
        val reminderLabel = "%02d:%02d".format(reminderHour, reminderMinute)
        var syncOn by remember { mutableStateOf(false) }
        var syncUrl by remember { mutableStateOf("") }
        var lastSynced by remember { mutableStateOf("") }
        var syncing by remember { mutableStateOf(false) }
        var translation by remember { mutableStateOf("simple") }

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
            reminderOn = st.reminderEnabled
            reminderHour = st.reminderHour
            reminderMinute = st.reminderMinute
            autoDetect = st.autoDetectActivity
            syncOn = st.syncEnabled
            syncUrl = st.syncEndpoint
            lastSynced = st.lastSyncedAt
            translation = st.translation.ifEmpty { "simple" }
            if (reminderOn) ReminderScheduler.schedule(this@MainActivity, reminderHour, reminderMinute)
            if (autoDetect) ensureActivityDetection()
            if (voice.isNotEmpty()) tts.setVoice(voice)
            for (i in 0 until 6) {
                voices = tts.availableVoices()
                if (voices.isNotEmpty()) break
                kotlinx.coroutines.delay(1000)
            }
            val verses = ContentRepository.load(this@MainActivity, "gentle", translation)
            if (verses.isNotEmpty()) {
                verseOfDay = verses[java.time.LocalDate.now().dayOfYear % verses.size]
            }
            refreshAll()
            if (intent?.getBooleanExtra("begin_moment", false) == true) {
                val r = ContentRepository.random(this@MainActivity, tone, translation)
                screen = Screen.Experience(r, tone, activity, System.currentTimeMillis())
            }
        }

        fun finishExperience(exp: Screen.Experience) {
            val secs = ((System.currentTimeMillis() - exp.startMs) / 1000).toInt().coerceAtLeast(1)
            scope.launch {
                store.addSession(exp.tone, exp.activity, exp.reflection.title, secs)
                refreshAll()
                screen = Screen.Home
            }
        }

        val drawer = rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
        fun openMenu() {
            scope.launch { drawer.open() }
        }
        fun closeMenu() {
            scope.launch { drawer.close() }
        }
        fun shareAll() {
            scope.launch {
                ExportHelper.share(this@MainActivity, store.getSessions())
            }
        }
        fun doAction(a: DrawerAction) {
            closeMenu()
            when (a) {
                is DrawerAction.Home -> screen = Screen.Home
                is DrawerAction.Begin -> {
                    val r = ContentRepository.random(this@MainActivity, tone, translation)
                    screen = Screen.Experience(r, tone, activity, System.currentTimeMillis())
                }
                is DrawerAction.Fresh -> {
                    val r = FreshWord.compose(this@MainActivity, tone, activity, translation)
                    screen = Screen.Experience(r, tone, activity, System.currentTimeMillis())
                }
                is DrawerAction.History -> { refreshAll(); screen = Screen.History }
                is DrawerAction.Favorites -> { filter = "favorites"; refreshAll(); screen = Screen.History }
                is DrawerAction.Export -> shareAll()
                is DrawerAction.Reminder -> screen = Screen.Settings
                is DrawerAction.Settings -> screen = Screen.Settings
                is DrawerAction.Voices -> startActivity(android.content.Intent("com.android.settings.TTS_SETTINGS"))
                is DrawerAction.ShareApp -> {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            "Quickened — short spoken Scripture for ordinary moments: https://quickened.netlify.app"
                        )
                    }
                    startActivity(android.content.Intent.createChooser(send, "Share Quickened"))
                }
            }
        }

        ModalNavigationDrawer(
            drawerState = drawer,
            drawerContent = { QuickenedDrawer(headerLine = streakLine, onAction = ::doAction) }
        ) {
        when (val s = screen) {
            is Screen.Home -> HomeScreen(
                tone = tone, onTone = { tone = it; persistSettings() },
                activity = activity, onActivity = { manualActivity = it },
                onStart = {
                    val r = ContentRepository.random(this@MainActivity, tone, translation)
                    screen = Screen.Experience(r, tone, activity, System.currentTimeMillis())
                },
                onFreshWord = {
                    val r = FreshWord.compose(this@MainActivity, tone, activity, translation)
                    screen = Screen.Experience(r, tone, activity, System.currentTimeMillis())
                },
                onHistory = { refreshAll(); screen = Screen.History },
                onSettings = { screen = Screen.Settings },
                onMenu = ::openMenu,
                verseOfDay = verseOfDay,
                streakLine = streakLine,
                statsLine = statsLine
            )
            is Screen.Experience -> ExperienceScreen(
                reflection = s.reflection, tone = s.tone, activity = s.activity, tts = tts,
                translation = translation,
                onTranslation = { tr ->
                    translation = tr
                    scope.launch {
                        store.saveSettings(store.getSettings().copy(translation = tr))
                    }
                },
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
                onBack = { screen = Screen.Home },
                onMenu = ::openMenu
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
                translation = translation,
                onTranslation = { tr ->
                    translation = tr
                    scope.launch {
                        store.saveSettings(store.getSettings().copy(translation = tr))
                    }
                },
                autoDetect = autoDetect,
                onAutoDetect = { on ->
                    autoDetect = on
                    scope.launch {
                        store.saveSettings(store.getSettings().copy(autoDetectActivity = on))
                    }
                    if (on) ensureActivityDetection()
                    else ActivityDetector.stop(this@MainActivity)
                },
                syncEnabled = syncOn,
                onSyncEnabled = { on ->
                    syncOn = on
                    scope.launch {
                        store.saveSettings(store.getSettings().copy(syncEnabled = on))
                    }
                },
                syncEndpoint = syncUrl,
                onSyncEndpoint = { url ->
                    syncUrl = url
                    scope.launch {
                        store.saveSettings(store.getSettings().copy(syncEndpoint = url))
                    }
                },
                lastSynced = lastSynced,
                onSyncNow = {
                    if (syncing) return@SettingsScreen
                    if (syncUrl.isBlank()) {
                        toast("Enter your sync URL first")
                        return@SettingsScreen
                    }
                    syncing = true
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val all = store.getSessions()
                            val deviceId = android.provider.Settings.Secure.getString(
                                contentResolver,
                                android.provider.Settings.Secure.ANDROID_ID
                            ) ?: "unknown"
                            val code = SyncManager.push(syncUrl, SyncManager.payload(all, deviceId))
                            val stamp = java.time.Instant.now().toString()
                            if (code in 200..299) {
                                store.saveSettings(store.getSettings().copy(lastSyncedAt = stamp))
                                lastSynced = stamp
                                toast("Synced ${all.size} moments ✓")
                            } else {
                                toast("Server said $code — kept locally")
                            }
                        } catch (e: Exception) {
                            toast("Offline — kept on device")
                        } finally {
                            syncing = false
                        }
                    }
                },
                reminderEnabled = reminderOn,
                onReminderEnabled = { on ->
                    if (on && Build.VERSION.SDK_INT >= 33 &&
                        checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    reminderOn = on
                    scope.launch {
                        val cur = store.getSettings()
                        store.saveSettings(cur.copy(reminderEnabled = on))
                    }
                    if (on) ReminderScheduler.schedule(this@MainActivity, reminderHour, reminderMinute)
                    else ReminderScheduler.cancel(this@MainActivity)
                },
                reminderTime = reminderLabel,
                onPickTime = {
                    TimePickerDialog(
                        this@MainActivity,
                        { _, h, m ->
                            reminderHour = h
                            reminderMinute = m
                            scope.launch {
                                val cur = store.getSettings()
                                store.saveSettings(cur.copy(reminderHour = h, reminderMinute = m))
                            }
                            if (reminderOn) ReminderScheduler.schedule(this@MainActivity, h, m)
                        },
                        reminderHour, reminderMinute, true
                    ).show()
                },
                onClearHistory = {
                    scope.launch {
                        store.clearHistory()
                        refreshAll()
                        android.widget.Toast.makeText(
                            this@MainActivity, "History cleared", android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onBack = { screen = Screen.Home },
                onMenu = ::openMenu
            )
        }
        }
    }
}
