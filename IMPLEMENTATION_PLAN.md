# Quickened — Implementation Plan

Product: Quickened v1.0 — offline-native Android app.
Stack (locked): Kotlin + Jetpack Compose, Room, Auth None, App internal storage (static content only), Android built-in TTS.
Explicit: App and DB run locally for now. No backend, no sync.

## Phase 0 — Project Setup (Day 1-2)

Output: buildable empty app.

- Gradle project, `minSdk 26`, Compose + Material3, no `INTERNET` permission
- Packages:
  - `data/Session.kt`, `data/Settings.kt` (per PRD §1.5)
  - `data/QuickendStore.kt`, `data/AppDatabase.kt`, `data/SessionDao.kt`, `data/SettingsDao.kt`
  - `tts/TtsManager.kt`
  - `content/ContentRepository.kt`
  - `ui/HomeScreen.kt`, `ui/ExperienceScreen.kt`, `ui/HistoryScreen.kt`, `ui/SettingsScreen.kt`
  - `assets/content/gentle.json`, `encouraging.json`, `contemplative.json`, `challenging.json`
- Done: `./gradlew assembleDebug` passes, cold launch <2s, TalkBack navigable.

## Phase 1 — MVP (Weeks 1-3)

Goal: tone → speak → save → history, 100% offline.

1. Room DB: `saveSettings/getSettings/addSession/getSessions/clearHistory`, persists across reboot. Unit tests.
2. Content: 8–20 reflections per tone (80–220 words, Christ-centered), `ContentRepository.getRandom(tone, activity)`.
3. TTS: init/speak/pause/stop, start <500ms, `onDone` saves `Session(id, tone, activity, contentPreview, durationSeconds, timestamp)`.
4. UI: Home (tone + large Start), Experience (preview + Stop/Pause), History (date/tone/preview + replay). Manual toggle Walking/Resting.
5. Done: PRD §2.6 — airplane-mode test, <30s to first experience.

## Phase 2 — Better Content (Weeks 4-5)

- Expand to 40–100 total, ≥10 per tone. Add `type: verse | verse+comment`.
- `StatsCalculator`: sessions this week / total minutes + encouragement line.
- History filter by tone, favorite/replay (`isFavorite`).
- Voice picker (installed offline voices only).
- Optional `ACTIVITY_RECOGNITION`, default manual.
- Done: PRD §3.4, still offline.

## Phase 3 — Polish & Settings (Weeks 6-7)

- Settings: default tone, voice, theme system/light/dark, clear history, About/Privacy.
- Accessibility pass: TalkBack, large targets, focus, high contrast.
- Done: PRD §4.2, sign `v1.0` APK/AAB. Privacy statement visible.

## Phase 4 — Deferred

On-device generation, driving/workout detection, reminders, export, translations, widget, Wear OS. Not in v1.0.

## Build Order

`Setup → Room entities/DAOs → Content JSON → TtsManager → Home → Experience → History → Stats → Settings → Polish`
