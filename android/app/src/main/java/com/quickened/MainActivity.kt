package com.quickened

import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.quickened.content.BibleLibrary
import com.quickened.content.ContentRepository
import com.quickened.content.ExportHelper
import com.quickened.content.FreshWord
import com.quickened.content.Reflection
import com.quickened.content.StatsCalculator
import com.quickened.content.spokenRef
import com.quickened.data.AppDatabase
import com.quickened.data.BookProgress
import com.quickened.data.FastRecord
import com.quickened.data.PrayerWatch
import com.quickened.data.QuickendStore
import com.quickened.data.Session
import com.quickened.reminder.PrayerScheduler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.quickened.activity.ActivityDetector
import com.quickened.reminder.ReminderScheduler
import com.quickened.piper.KokoroCatalog
import com.quickened.piper.KokoroSpeaker
import com.quickened.piper.KokoroStore
import com.quickened.piper.PiperCatalog
import com.quickened.piper.PiperSpeaker
import com.quickened.piper.PiperVoices
import com.quickened.piper.StudioSpeaker
import com.quickened.sync.SyncManager
import com.quickened.tts.TtsManager
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import com.quickened.reader.BookReader
import com.quickened.ui.DrawerAction
import com.quickened.ui.BrandSplash
import com.quickened.ui.ExperienceScreen
import com.quickened.ui.FastingScreen
import com.quickened.ui.HistoryScreen
import com.quickened.ui.HomeScreen
import com.quickened.ui.OnboardingScreen
import com.quickened.ui.PrayerScreen
import com.quickened.ui.QuickenedDrawer
import com.quickened.ui.ReaderScreen
import com.quickened.ui.QuickenedTheme
import com.quickened.ui.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var store: QuickendStore
    private lateinit var tts: TtsManager
    private var piperSpeaker: PiperSpeaker? = null
    private var currentPiperVoiceId: String? = null
    private var kokoroSpeaker: KokoroSpeaker? = null
    private var currentKokoroVoiceId: String? = null
    private var studioSpeaker: StudioSpeaker? = null
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
        try {
            piperSpeaker?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
        try {
            kokoroSpeaker?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
        try {
            studioSpeaker?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
        tts.shutdown()
        super.onDestroy()
    }

    private suspend fun ensureKokoro(id: String, attempts: Int = 2): KokoroSpeaker? {
        if (kokoroSpeaker != null && currentKokoroVoiceId == id) return kokoroSpeaker
        try {
            kokoroSpeaker?.shutdown()
            kokoroSpeaker = null
            currentKokoroVoiceId = null
            val speaker = KokoroSpeaker(assets, filesDir, this, id)
            repeat(attempts) { tryN ->
                try {
                    if (speaker.init()) {
                        kokoroSpeaker = speaker
                        currentKokoroVoiceId = id
                        android.util.Log.i("KOKORO_APP", "speaker ready: $id (try ${tryN + 1})")
                        return speaker
                    }
                    android.util.Log.w("KOKORO_APP", "init not ready: $id (try ${tryN + 1})")
                } catch (t: Throwable) {
                    android.util.Log.e("KOKORO_APP", "speaker failed", t)
                }
                kotlinx.coroutines.delay(1500)
            }
            return null
        } catch (t: Throwable) {
            android.util.Log.e("KOKORO_APP", "speaker failed", t)
            return null
        }
    }

    private suspend fun ensurePiper(id: String, attempts: Int = 2): PiperSpeaker? {
        if (id.isEmpty()) return null
        if (piperSpeaker != null && currentPiperVoiceId == id) return piperSpeaker
        try {
            piperSpeaker?.shutdown()
            piperSpeaker = null
            currentPiperVoiceId = null
            repeat(attempts) { tryN ->
                try {
                    val voice = PiperCatalog.byId(id) ?: return null
                    if (!PiperSpeaker.fetchMap(this, voice)) {
                        android.util.Log.w("PIPER_APP", "map missing for $id")
                        return null
                    }
                    val speaker = PiperSpeaker(assets, filesDir, PiperVoices.dir(this), id)
                    if (speaker.init()) {
                        piperSpeaker = speaker
                        currentPiperVoiceId = id
                        android.util.Log.i("PIPER_APP", "speaker ready: $id (try ${tryN + 1})")
                        return speaker
                    }
                    android.util.Log.w("PIPER_APP", "speaker init failed for $id (try ${tryN + 1})")
                } catch (t: Throwable) {
                    android.util.Log.e("PIPER_APP", "speaker failed", t)
                }
                kotlinx.coroutines.delay(1500)
            }
            return null
        } catch (t: Throwable) {
            android.util.Log.e("PIPER_APP", "speaker failed", t)
            return null
        }
    }

    private sealed interface Screen {
        data object Home : Screen
        data class Experience(
            val reflection: Reflection,
            val tone: String,
            val activity: String,
            val startMs: Long,
            val piper: PiperSpeaker? = null,
            val kokoro: KokoroSpeaker? = null,
            val studio: StudioSpeaker? = null,
            val studioUrl: String = "",
            val studioKey: String = ""
        ) : Screen
        data object History : Screen
        data object Settings : Screen
        data object Prayer : Screen
        data object Fasting : Screen
        data object Reader : Screen
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
        var piperSelected by remember { mutableStateOf("") }
        var piperStates by remember { mutableStateOf<Map<String, PiperVoices.Status>>(emptyMap()) }
        var kokoroModelReady by remember { mutableStateOf(false) }
        var kokoroWorking by remember { mutableStateOf(false) }
        var kokoroReady by remember { mutableStateOf(setOf<String>()) }
        var studioUrl by remember { mutableStateOf("") }
        var studioKey by remember { mutableStateOf("") }

        fun refreshPiper() {
            piperStates = PiperCatalog.voices.associate { it.id to PiperVoices.status(this@MainActivity, it) }
            kokoroModelReady = KokoroStore.modelReady(this@MainActivity)
            kokoroReady = KokoroCatalog.voices.filter { KokoroStore.voiceReady(this@MainActivity, it.id) }
                .map { it.id }.toSet()
        }
        var recentTitles by remember { mutableStateOf(listOf<String>()) }
        var bibleReady by remember { mutableStateOf(false) }
        var watches by remember { mutableStateOf<List<PrayerWatch>>(emptyList()) }
        var appReady by remember { mutableStateOf(false) }
        var activeFast by remember { mutableStateOf<FastRecord?>(null) }
        var fastHistory by remember { mutableStateOf<List<FastRecord>>(emptyList()) }
        var fastCount by remember { mutableStateOf(0) }
        var customHours by remember { mutableStateOf("") }
        var fastIntention by remember { mutableStateOf("") }
        var readerBook by remember { mutableStateOf<BookReader.Book?>(null) }
        var readerUri by remember { mutableStateOf("") }
        var readerChapter by remember { mutableStateOf(0) }
        var readerPageIx by remember { mutableStateOf(0) }
        var readerPlaying by remember { mutableStateOf(false) }
        var readerStatus by remember { mutableStateOf("") }

        fun refreshFasts() {
            scope.launch {
                activeFast = store.db().fastDao().active()
                fastHistory = store.db().fastDao().history()
                fastCount = store.db().fastDao().completedCount()
            }
        }
        val drawer = rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
        var userName by remember { mutableStateOf<String?>(null) }

        suspend fun fullBibleReflection(): Reflection? {
            val cap = ActivityDetector.verseMaxLen(activity)
            val first = BibleLibrary.randomVerse(store.db(), cap) ?: return null
            val text = BibleLibrary.passage(store.db(), first)
            if (text.length > cap + 80) return null
            return Reflection(
                title = "${first.book} ${first.chapter}:${first.verse}",
                text = text,
                type = "verse"
            )
        }

        suspend fun drawNext(): Reflection {
            val cap = ActivityDetector.verseMaxLen(activity)
            val pool = ContentRepository.load(
                this@MainActivity, tone, translation
            ).filter { it.title !in recentTitles.toSet() && it.text.length <= cap + 120 }
            val candidates = if (pool.isNotEmpty()) pool else ContentRepository.load(
                this@MainActivity, tone, translation
            )
            val pick = candidates.random()
            val r = if (pick.type == "verse" && bibleReady && translation == "kjv") {
                fullBibleReflection() ?: pick
            } else pick
            android.util.Log.i("PICK", "tone=$tone title=${r.title}")
            recentTitles = (recentTitles + r.title).takeLast(5)
            return r
        }

        fun beginExperience(r: Reflection) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val sel = piperSelected
                val p = if (sel.startsWith("kokoro:") || sel == "studio") null else ensurePiper(sel)
                val k = if (sel.startsWith("kokoro:")) ensureKokoro(sel.removePrefix("kokoro:")) else null
                val st = if (sel == "studio") {
                    if (studioSpeaker == null) studioSpeaker = StudioSpeaker(this@MainActivity)
                    studioSpeaker
                } else null
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    com.quickened.service.QuickenedAudioService.start(
                        this@MainActivity, r.title, "Playing in background…"
                    )
                    screen = Screen.Experience(r, tone, activity, System.currentTimeMillis(), p, k, st, studioUrl, studioKey)
                }
            }
        }

        fun pickFresh() {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val r = drawNext()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    beginExperience(r)
                }
            }
        }
        var confirmExit by remember { mutableStateOf(false) }

        BackHandler {
            if (drawer.isOpen) {
                scope.launch { drawer.close() }
            } else if (screen == Screen.Home) {
                confirmExit = true
            } else {
                tts.stop()
                com.quickened.service.QuickenedAudioService.stop(this@MainActivity)
                screen = Screen.Home
            }
        }
        if (confirmExit) {
            AlertDialog(
                onDismissRequest = { confirmExit = false },
                title = { Text("Leave Quickened?") },
                text = { Text("Your moments stay saved on this device.") },
                confirmButton = {
                    TextButton(onClick = { finish() }) { Text("Yes, leave") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmExit = false }) { Text("Stay") }
                }
            )
        }

        fun persistSettings() {
            scope.launch {
                store.saveSettings(store.getSettings().copy(preferredTone = tone, voice = voice, theme = theme))
            }
        }

        fun refreshWatches() {
            scope.launch {
                watches = store.db().prayerDao().getAll()
            }
        }

        fun speakBookText(text: String, onDone: () -> Unit) {
            val spoken = spokenRef(text)
            val sel = piperSelected
            when {
                sel == "studio" && studioUrl.isNotBlank() -> {
                    if (studioSpeaker == null) studioSpeaker = StudioSpeaker(this@MainActivity)
                    studioSpeaker!!.speak(studioUrl, studioKey, spoken, onDone, {
                        tts.onDone = onDone
                        tts.speak(spoken, 1.0f)
                    }) {}
                }
                sel.startsWith("kokoro:") -> {
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val k = ensureKokoro(sel.removePrefix("kokoro:"))
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (k != null) k.speak(spoken, onDone, {
                                tts.onDone = onDone
                                tts.speak(spoken, 1.0f)
                            }) {}
                            else {
                                tts.onDone = onDone
                                tts.speak(spoken, 1.0f)
                            }
                        }
                    }
                }
                sel.isNotEmpty() -> {
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val p = ensurePiper(sel)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (p != null) p.speak(spoken, onDone) {
                                tts.onDone = onDone
                                tts.speak(spoken, 1.0f)
                            }
                            else {
                                tts.onDone = onDone
                                tts.speak(spoken, 1.0f)
                            }
                        }
                    }
                }
                else -> {
                    tts.onDone = onDone
                    tts.speak(spoken, 1.0f)
                }
            }
        }

        fun haltBookAudio() {
            com.quickened.service.QuickenedAudioService.stop(this@MainActivity)
            try {
                tts.stop()
            } catch (e: Exception) {
                // ignore
            }
            try {
                piperSpeaker?.stop()
            } catch (e: Exception) {
                // ignore
            }
            try {
                kokoroSpeaker?.stop()
            } catch (e: Exception) {
                // ignore
            }
            try {
                studioSpeaker?.stop()
            } catch (e: Exception) {
                // ignore
            }
        }

        fun playReaderChapter(ix: Int) {
            val book = readerBook ?: return
            if (ix !in book.chapters.indices) return
            readerChapter = ix
            readerPlaying = true
            readerStatus = "Reading part ${ix + 1} of ${book.chapters.size}"
            com.quickened.service.QuickenedAudioService.start(
                this@MainActivity,
                book.title,
                "Part ${ix + 1} of ${book.chapters.size}"
            )
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                store.db().bookDao().save(BookProgress(readerUri, book.title, ix))
            }
            speakBookText(book.chapters[ix]) {
                if (readerPlaying && ix + 1 in book.chapters.indices) {
                    playReaderChapter(ix + 1)
                } else {
                    readerPlaying = false
                    readerStatus = "Finished “${book.title}”"
                    com.quickened.service.QuickenedAudioService.stop(this@MainActivity)
                }
            }
        }

        fun startFast(kind: String, title: String, hours: Int) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val pool = ContentRepository.load(this@MainActivity, "gentle", translation)
                val verse = if (pool.isNotEmpty()) pool.random() else null
                val fast = FastRecord(
                    id = java.util.UUID.randomUUID().toString(),
                    kind = kind,
                    title = title,
                    targetHours = hours,
                    intention = fastIntention,
                    verseTitle = verse?.title ?: "",
                    verseText = verse?.text ?: "Be still, and know that I am God.",
                    startedAt = java.time.Instant.now().toString()
                )
                store.db().fastDao().insert(fast)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    refreshFasts()
                }
            }
        }

        fun endFast(completed: Boolean) {
            val f = activeFast ?: return
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                store.db().fastDao().finish(
                    f.id,
                    if (completed) "completed" else "broken",
                    java.time.Instant.now().toString()
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    refreshFasts()
                }
            }
        }

        fun refreshHero() {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val hero = if (bibleReady && translation == "kjv") {
                    val first = BibleLibrary.randomVerse(store.db())
                    if (first != null) Reflection(
                        title = "Today's Word — ${first.book} ${first.chapter}:${first.verse}",
                        text = BibleLibrary.passage(store.db(), first),
                        type = "verse"
                    ) else null
                } else null
                val pool = ContentRepository.load(this@MainActivity, "gentle", translation)
                val fallback = if (pool.isNotEmpty()) pool.random() else null
                verseOfDay = hero ?: fallback
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
                recentTitles = (all.take(5).map { it.contentPreview } + recentTitles).distinct().takeLast(5)
            }
        }

        fun silentSync() {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    if (!syncOn || syncUrl.isBlank()) return@launch
                    val all = store.getSessions()
                    val deviceId = android.provider.Settings.Secure.getString(
                        contentResolver,
                        android.provider.Settings.Secure.ANDROID_ID
                    ) ?: "unknown"
                    val code = SyncManager.push(syncUrl, SyncManager.payload(all, deviceId))
                    if (code in 200..299) {
                        val stamp = java.time.Instant.now().toString()
                        store.saveSettings(store.getSettings().copy(lastSyncedAt = stamp))
                        lastSynced = stamp
                    }
                } catch (e: Exception) {
                    // offline — stays local, tries next time
                }
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
            studioUrl = st.studioUrl
            studioKey = st.studioKey
            translation = st.translation.ifEmpty { "simple" }
            piperSelected = st.piperVoice
            refreshPiper()
            userName = st.userName.ifEmpty { null }
            if (reminderOn) ReminderScheduler.schedule(this@MainActivity, reminderHour, reminderMinute)
            if (autoDetect) ensureActivityDetection()
            if (voice.isNotEmpty()) tts.setVoice(voice)
            for (i in 0 until 6) {
                voices = tts.availableVoices()
                if (voices.isNotEmpty()) break
                kotlinx.coroutines.delay(1000)
            }
            bibleReady = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                BibleLibrary.ensureImported(this@MainActivity, store.db())
            }
            refreshHero()
            refreshAll()
            refreshFasts()
            silentSync()
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                if (piperSelected.startsWith("kokoro:")) {
                    ensureKokoro(piperSelected.removePrefix("kokoro:"))
                } else if (piperSelected.isNotEmpty()) {
                    ensurePiper(piperSelected)
                }
            }
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val all = store.db().prayerDao().getAll()
                watches = all
                all.filter { it.enabled }.forEach {
                    PrayerScheduler.schedule(this@MainActivity, it.id, it.title, it.hour, it.minute)
                }
            }
            appReady = true
            if (intent?.getBooleanExtra("begin_moment", false) == true) {
                pickFresh()
            }
        }

        fun finishExperience(firstTitle: String, count: Int, startMs: Long) {
            val secs = ((System.currentTimeMillis() - startMs) / 1000).toInt().coerceAtLeast(1)
            com.quickened.service.QuickenedAudioService.stop(this@MainActivity)
            scope.launch {
                val preview = if (count > 1) "$firstTitle (+${count - 1} more)" else firstTitle
                store.addSession(tone, activity, preview, secs)
                refreshAll()
                screen = Screen.Home
                silentSync()
            }
        }

        DisposableEffect(Unit) {
            val obs = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    refreshHero()
                    refreshAll()
                }
            }
            lifecycle.addObserver(obs)
            onDispose { lifecycle.removeObserver(obs) }
        }

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
                is DrawerAction.Fast -> { refreshFasts(); screen = Screen.Fasting }
                is DrawerAction.Begin -> pickFresh()
                is DrawerAction.Fresh -> {
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val r = FreshWord.compose(this@MainActivity, tone, activity, translation)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            beginExperience(r)
                        }
                    }
                }
                is DrawerAction.History -> { refreshAll(); screen = Screen.History }
                is DrawerAction.Favorites -> { filter = "favorites"; refreshAll(); screen = Screen.History }
                is DrawerAction.Reader -> screen = Screen.Reader
                is DrawerAction.Export -> shareAll()
                is DrawerAction.Reminder -> screen = Screen.Settings
                is DrawerAction.Prayer -> { refreshWatches(); screen = Screen.Prayer }
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
        if (!appReady) {
            BrandSplash("Preparing your Bible…")
        } else if (userName == null) {
            OnboardingScreen(onDone = { name ->
                userName = name
                scope.launch {
                    store.saveSettings(store.getSettings().copy(userName = name))
                }
            })
        } else when (val s = screen) {
            is Screen.Home -> HomeScreen(
                tone = tone, onTone = { tone = it; persistSettings() },
                activity = activity, onActivity = { manualActivity = it },
                onStart = { pickFresh() },
                onFreshWord = {
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val base = if (bibleReady && translation == "kjv") {
                            val first = BibleLibrary.randomVerse(store.db())
                            if (first != null) Reflection(
                                title = "${first.book} ${first.chapter}:${first.verse}",
                                text = BibleLibrary.passage(store.db(), first),
                                type = "verse"
                            ) else null
                        } else null
                        val r = if (base != null) FreshWord.composeFrom(base, tone, activity)
                        else FreshWord.compose(this@MainActivity, tone, activity, translation)
                        val p = ensurePiper(piperSelected)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            screen = Screen.Experience(r, tone, activity, System.currentTimeMillis(), p)
                        }
                    }
                },
                onHistory = { refreshAll(); screen = Screen.History },
                onSettings = { screen = Screen.Settings },
                onMenu = ::openMenu,
                userName = userName ?: "",
                verseOfDay = verseOfDay,
                streakLine = streakLine,
                statsLine = statsLine,
                readerBookTitle = readerBook?.title,
                readerChapter = readerChapter,
                onOpenReader = { screen = Screen.Reader }
            )
            is Screen.Experience -> ExperienceScreen(
                reflection = s.reflection, tone = s.tone, activity = s.activity, tts = tts,
                piper = s.piper, kokoro = s.kokoro, studio = s.studio,
                studioUrl = s.studioUrl, studioKey = s.studioKey,
                translation = translation,
                onTranslation = { tr ->
                    translation = tr
                    scope.launch {
                        store.saveSettings(store.getSettings().copy(translation = tr))
                    }
                },
                onNext = { _, done ->
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val r = drawNext()
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            done(r)
                        }
                    }
                },
                onStop = { firstTitle, count -> finishExperience(firstTitle, count, s.startMs) }
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
                    if (it == "studio") {
                        piperSelected = "studio"
                        scope.launch {
                            store.saveSettings(store.getSettings().copy(piperVoice = "studio"))
                        }
                        toast("Saved")
                    } else if (it.startsWith("kokoro:")) {
                        piperSelected = it
                        scope.launch {
                            store.saveSettings(store.getSettings().copy(piperVoice = it))
                        }
                        toast("Saved")
                    } else if (it.startsWith("piper:")) {
                        val id = it.removePrefix("piper:")
                        piperSelected = id
                        scope.launch {
                            store.saveSettings(store.getSettings().copy(piperVoice = id))
                        }
                        toast("Saved")
                    } else {
                        voice = it
                        if (it.isNotEmpty()) tts.setVoice(it)
                        persistSettings()
                    }
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
                userName = userName ?: "",
                onUserName = { name ->
                    userName = name
                    scope.launch {
                        store.saveSettings(store.getSettings().copy(userName = name))
                    }
                },
                piperStates = piperStates,
                piperSelected = piperSelected,
                onPiperDownload = { id ->
                    val ok = PiperCatalog.byId(id)?.let { PiperVoices.download(this@MainActivity, it) } == true
                    toast(if (ok) "Downloading voice (Wi-Fi)…" else "Download unavailable on this device")
                    scope.launch {
                        kotlinx.coroutines.delay(5000)
                        refreshPiper()
                    }
                },
                onPiperDelete = { id ->
                    PiperCatalog.byId(id)?.let { PiperVoices.delete(this@MainActivity, it) }
                    if (piperSelected == id) {
                        piperSelected = ""
                        scope.launch {
                            store.saveSettings(store.getSettings().copy(piperVoice = ""))
                        }
                    }
                    refreshPiper()
                },
                onPiperSelect = { id ->
                    piperSelected = id
                    scope.launch {
                        store.saveSettings(store.getSettings().copy(piperVoice = id))
                    }
                    toast("Saved — in-app playback arrives with the voice engine")
                },
                kokoroModelReady = kokoroModelReady,
                kokoroWorking = kokoroWorking,
                kokoroVoicesReady = kokoroReady,
                onKokoroModelDownload = {
                    kokoroWorking = true
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val ok = KokoroStore.downloadModel(this@MainActivity)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            kokoroWorking = false
                            toast(if (ok) "Engine downloading…" else "Download unavailable on this device")
                        }
                        kotlinx.coroutines.delay(15000)
                        refreshPiper()
                    }
                },
                onKokoroDownload = { id ->
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val ok = KokoroStore.downloadVoice(this@MainActivity, id)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            toast(if (ok) "Downloading voice…" else "Download unavailable on this device")
                        }
                        kotlinx.coroutines.delay(8000)
                        refreshPiper()
                    }
                },
                onKokoroDelete = { id ->
                    KokoroStore.deleteVoice(this@MainActivity, id)
                    if (piperSelected == "kokoro:$id") {
                        piperSelected = ""
                        scope.launch {
                            store.saveSettings(store.getSettings().copy(piperVoice = ""))
                        }
                    }
                    refreshPiper()
                },
                onKokoroSelect = { id ->
                    piperSelected = "kokoro:$id"
                    scope.launch {
                        store.saveSettings(store.getSettings().copy(piperVoice = "kokoro:$id"))
                    }
                },
                studioUrl = studioUrl,
                studioKey = studioKey,
                onStudioSave = { url, key ->
                    studioUrl = url
                    studioKey = key
                    scope.launch {
                        val cur = store.getSettings()
                        store.saveSettings(cur.copy(studioUrl = url, studioKey = key))
                    }
                    toast("Studio server saved")
                },
                onStudioSelect = {
                    if (studioUrl.isBlank()) {
                        toast("Enter the server URL first")
                    } else {
                        piperSelected = "studio"
                        scope.launch {
                            store.saveSettings(store.getSettings().copy(piperVoice = "studio"))
                        }
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
            is Screen.Prayer -> PrayerScreen(
                watches = watches,
                onAdd = { title ->
                    TimePickerDialog(
                        this@MainActivity,
                        { _, h, m ->
                            val item = PrayerWatch(
                                id = java.util.UUID.randomUUID().toString(),
                                title = title, hour = h, minute = m, enabled = true
                            )
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                store.db().prayerDao().insert(item)
                                PrayerScheduler.schedule(this@MainActivity, item.id, item.title, h, m)
                                refreshWatches()
                            }
                        },
                        6, 0, true
                    ).show()
                },
                onToggle = { w ->
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        store.db().prayerDao().setEnabled(w.id, !w.enabled)
                        if (w.enabled) PrayerScheduler.cancel(this@MainActivity, w.id, w.title)
                        else PrayerScheduler.schedule(this@MainActivity, w.id, w.title, w.hour, w.minute)
                        refreshWatches()
                    }
                },
                onDelete = { w ->
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        PrayerScheduler.cancel(this@MainActivity, w.id, w.title)
                        store.db().prayerDao().delete(w.id)
                        refreshWatches()
                    }
                },
                onBack = { screen = Screen.Home },
                onMenu = ::openMenu
            )
            is Screen.Fasting -> FastingScreen(
                active = activeFast,
                history = fastHistory,
                completedCount = fastCount,
                customHours = customHours,
                onCustomHours = { customHours = it },
                intention = fastIntention,
                onIntention = { fastIntention = it },
                onStart = { kind, title, hours -> startFast(kind, title, hours) },
                onEnd = { completed -> endFast(completed) },
                onBack = { screen = Screen.Home },
                onMenu = ::openMenu
            )
            is Screen.Reader -> ReaderScreen(
                book = readerBook,
                bookUri = readerUri,
                chapter = readerChapter,
                playing = readerPlaying,
                statusLine = readerStatus,
                onPick = { uri, name ->
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (e: Exception) {
                            // already held or one-shot
                        }
                        try {
                            val book = BookReader.open(this@MainActivity, uri, name)
                            val saved = store.db().bookDao().load(uri.toString())
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                readerBook = book
                                readerUri = uri.toString()
                                readerChapter = saved?.chapter ?: 0
                                readerStatus = "Opened “${book.title}” — ${book.chapters.size} parts"
                            }
                        } catch (e: Exception) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                readerStatus = "Could not read that file (${e.message ?: "unknown error"})"
                            }
                        }
                    }
                },
                onChapter = { ix -> playReaderChapter(ix) },
                onPlayPage = { ix ->
                    fun playAt(at: Int) {
                        val book = readerBook
                        if (book == null || at !in book.pages.indices) return
                        readerPageIx = at
                        readerPlaying = true
                        readerStatus = "Reading page ${at + 1} of ${book.pages.size}"
                        speakBookText(book.pages[at]) {
                            if (readerPlaying) {
                                if (at + 1 in book.pages.indices) playAt(at + 1)
                                else {
                                    readerPlaying = false
                                    readerStatus = "Finished “${book.title}”"
                                }
                            }
                        }
                    }
                    playAt(ix)
                },
                pageIx = readerPageIx,
                onPageIx = { readerPageIx = it },
                onPauseResume = {
                    if (readerPlaying) {
                        haltBookAudio()
                        readerPlaying = false
                        readerStatus = "Paused"
                    } else {
                        playReaderChapter(readerChapter)
                    }
                },
                onStop = {
                    haltBookAudio()
                    readerPlaying = false
                    readerStatus = "Stopped"
                },
                onBack = {
                    haltBookAudio()
                    readerPlaying = false
                    screen = Screen.Home
                },
                onMenu = ::openMenu
            )
        }
        }
    }
}
