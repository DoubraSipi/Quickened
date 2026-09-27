# Quickened

## Overview

Quickened is a lightweight, privacy-first Android app that turns everyday moments (walking, commuting, working out, resting) into short spoken Scripture reflections, encouragements, or prayers.

## Problem

Faith often stays limited to morning quiet time, church, or bedtime. Busy believers struggle to keep consistent spiritual habits in ordinary moments.

## Target Users

- Christians who want faith present in ordinary moments
- Busy believers outside set prayer/church times
- People who walk, commute, or exercise and want those moments Christ-centered
- Privacy-conscious users who prefer local-only data

## Planned Features (Phase 1 MVP)

- Tone selection: Gentle, Encouraging, Contemplative, Challenging
- One-tap Start Experience (1–3 min spoken reflection)
- Android built-in TTS, Stop / Pause
- Session history with tone, date, preview + replay
- Manual activity toggle: Walking / Resting
- Full offline operation

## Technology / Framework

- **Framework:** Kotlin + Jetpack Compose (Material3)
- **Database:** Room — settings and session history
- **Authentication:** None — no accounts, no login
- **File Storage:** App internal storage, static content only (`assets/content/*.json`)
- **Text-to-Speech:** Android built-in TTS (offline voices)
- **Min SDK:** Android 8.0 (API 26), phones + tablets

**Explicit:** App and DB run locally for now. No backend, no network requests for core features, no tracking, no analytics, no cloud sync in Phase 1–3.

## Installation and Running

Requirements: Android Studio Ladybug+, JDK 17, Android SDK 26+.

```sh
git clone https://github.com/DoubraSipi/Quickened.git
# Open folder in Android Studio
# Let Gradle sync, ensure offline TTS voice installed on device/emulator
./gradlew assembleDebug
# Run: Shift+F10 in Android Studio, or:
./gradlew installDebug
```

Use airplane mode to verify offline: tone → Start → Stop → History.

## Status

Planning + Phase 0 setup. See `IMPLEMENTATION_PLAN.md` for ordered phases.
