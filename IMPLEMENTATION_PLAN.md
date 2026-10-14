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

## Agent Lanes (Oct 2026)

- `master` always builds. No agent commits to it directly.
- opencode works on `opencode/<task>` (owns: voice engine, reader, build/phone testing).
- cursor works on `cursor/<task>` (owns: voice quality + download integrity — see below).
- Merge to `master` only after `:app:assembleDebug --offline` passes.
- Locked toolchain: AGP 8.5.2, Gradle 9.6.0, Kotlin 2.0.20. minSdk 26. Offline-first, Room source of truth.

## Voice Task — owner: cursor (status: open)

Problem: realistic voice unusable on itel P70 (Unisoc T606) — slow start + stutter gaps (user). Findings so far (measured on-device, itel P70, `adb logcat -s KOKORO_APP`):

1. Kokoro-82M int8 (`model_uint8.onnx`, 177,465,212 bytes, sha256 `9748ecc9…38cea`) would not load on onnxruntime-android 1.22 (`ORT_INVALID_PROTOBUF`). Fixed by bump to **1.31.0** (`android/pipertts/build.gradle.kts`). No re-download needed.
2. Render speed wall (per-unit `renderMs` vs `audioMs` in logs): 2 threads ~7x realtime, 4 threads ~4x, 8 threads ~4x. Chip-bound (fixed ~10s/run + ~0.15s/char). Verdict: Kokoro-82M cannot play live on this chip. Kept at 4 threads (`KokoroEngine.kt`). Do not re-sweep threads without new data.
3. Mitigations already in `KokoroSpeaker.kt`: engine warm-up at init, render-ahead `prefetch()` with per-unit cache (`audio-cache/units/`), generation-guarded cancel, per-unit timing logs. `ExperienceScreen.kt` prefetches the shown verse.
4. Piper is the daily-use path. `PiperCatalog.RECOMMENDED = "ryan"` (en_US-ryan-high, 120,786,792 bytes, sha256 `b3990d76…154eca`). Voice map `ryan.onnx.json` bundled in `pipertts/src/main/assets/voices/`. Settings shows ★ badge; fresh installs auto-select once downloaded (`MainActivity.kt`).
5. **Open bug (ryan falls back to system TTS):** `ryan.onnx` downloaded with exact byte size but wrong sha256 (corrupt). Deleted from test phone; needs clean re-download + verify. Root cause unknown — prime suspect: DownloadManager + HuggingFace XET-redirect range reassembly.
6. **Your task:** (a) verify ryan end-to-end after clean download; (b) add download-integrity checks (expected size + sha256 in catalog, verify before marking Ready, auto-delete + re-queue on mismatch — do it for Kokoro model too); (c) if ryan is still too slow/weak on the T606, evaluate next option (server Studio voice is already client-ready; needs Render/Fly deploy per `tts-server/DEPLOY.md`).
7. Constraints: keep offline-first fallbacks (system TTS on any failure), no auto-downloads >10MB without a tap, all ONNX runs serialized on the single-thread executors (OrtSession is not thread-safe).
