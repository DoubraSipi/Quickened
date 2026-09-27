# PRD: Quickened

## 0. Confirmed Technical Decisions (Locked)

- **Framework:** Kotlin + Jetpack Compose (Material3)
- **Database:** Room — settings and session history
- **Authentication:** None — no accounts, no login
- **File Storage:** App internal storage, static content only (`assets/content/*.json`)
- **Text-to-Speech:** Android built-in TTS (offline voices)
- **Min SDK:** Android 8.0 (API 26)

**Explicit:** App and DB run locally for now. No backend, no network requests for core features, no tracking, no analytics, no cloud sync in Phase 1–3.

---

## 1. Overview

**Product Name:** Quickened
**Version:** 1.0
**Platform:** Android (Offline-Native App)
**Platform Description:** Fully offline-capable Android application. All core functionality works without network. Uses on-device storage and Android Text-to-Speech.
**Core Requirement:** Fully offline-capable; behaves like a native app with no backend dependency.

### 1.1 Purpose

A lightweight, privacy-first Christian app that turns everyday activities (walking, commuting, working out, resting) into short spoken Scripture reflections, encouragements, or prayers. Users receive adaptive faith moments that fit their current activity and chosen tone so faith becomes part of daily life instead of something limited to set prayer or church times.

### 1.2 Target Audience

- Christians who want their faith present in ordinary moments
- Busy believers who struggle to keep consistent spiritual habits outside morning quiet time, church, or bedtime
- People who already walk, commute, or exercise and want those moments to draw them closer to Christ
- Privacy-conscious users who prefer local-only data

### 1.3 Design Principles

- Offline-First — Every core feature works without network connectivity
- Privacy by Default — No network requests, no tracking, no analytics, no accounts
- Simplicity — Time to first experience < 30 seconds
- Local Storage — All preferences and history stay on device
- Faithful Content — All spoken content is Christ-centered and Scripture-rooted
- Accessibility — Clear audio, large touch targets, screen-reader friendly

### 1.4 Architecture

- Native Android app (Kotlin + Jetpack Compose)
- No backend or user accounts
- On-device Text-to-Speech (Android TTS)
- Local storage via Room for preferences and session history
- Optional future on-device content generation (kept simple for MVP)

### 1.5 Technical Requirements

Data Model:

```kotlin
// Session Entry
data class Session(
    val id: String,              // UUID
    val tone: String,            // "gentle" | "encouraging" | "contemplative" | "challenging"
    val activity: String,        // "walking" | "stationary" | "driving" | "unknown"
    val contentPreview: String,  // Short title or first line of reflection
    val durationSeconds: Int,
    val timestamp: String,       // ISO 8601
    val createdAt: String
)

// Settings
data class Settings(
    val preferredTone: String,   // default tone
    val voice: String,           // TTS voice preference if available
    val theme: String,           // "system" | "light" | "dark"
    val autoDetectActivity: Boolean
)
```

Storage:

- Room for settings and history
- No cloud sync in Phase 1–3
- Content library stored as local text assets in app internal storage (`assets/content/*.json`)

Device Support:

- Android 8.0 (API 26) and above
- Works on phones and tablets
- Offline TTS required

### 1.6 Non-Functional Requirements

- Performance: App launch < 2s; experience start < 1s; TTS begins within 500ms
- Reliability: Data persists across app restarts and device reboots
- Privacy: Zero network requests for core features; data never leaves the device
- Accessibility: TalkBack compatible; large touch targets; clear focus states
- Battery: Minimal background usage; no continuous sensor drain

## 2. Phase 1 — MVP (Weeks 1-3)

Goal: Ship a fully functional offline Christian experience app with tone selection, short spoken reflections, activity detection (or simple toggle), and session history.

### 2.1 Features

Core Experience:
- Tone Selection (Gentle, Encouraging, Contemplative, Challenging) — P0
- Start Experience (one-tap short spoken reflection) — P0
- Text-to-Speech (Android built-in) — P0
- Stop / Pause — P0
- Session History (tone + date) — P0
- Offline Operation — P0

Content:
- Pre-written Reflections (8–20 per tone, Christ-centered) — P0
- Scripture Moments (verse or verse + brief reflection) — P0
- Length Control (1–3 min) — P0

Activity Awareness:
- Manual Activity Toggle (Walking / Resting) — P0
- Basic Detection (Activity Recognition API, optional) — P1

### 2.2 User Experience

Flow 1 — First Experience: open → select tone → Start Experience → listen → end → home confirmation → saved to history.
Flow 2 — Repeat Use: home shows last tone + Start → optionally change tone → play → history.
Flow 3 — History: list date/tone/preview → replay/review.

Responsive: mobile-first, large touch targets, calm readable UI for use while moving.

### 2.3 Technical Details

- Kotlin + Jetpack Compose, Home → Experience → History, Material3 calm theme
- Local JSON assets fed to Android TextToSpeech, no external AI API
- `QuickendStore`: `saveSettings/getSettings/addSession/getSessions/clearHistory`
- Permissions: none for MVP; optional `ACTIVITY_RECOGNITION`

### 2.4 Design System (Locked Sep 28, 2026)

- Font: Inter (system fallback) — readable while walking, calm, free, works for headings + body.
- Palette: Primary #0D9488 (peace/trust), Primary Dark #0F766E (pressed), BG Light #F8FAFC, BG Dark #0F172A, Text #0F172A / #64748B, Accent #10B981 (growth).
- Tones: Gentle #3B82F6, Encouraging #22C55E, Contemplative #8B5CF6, Challenging #D97706 — quick mood recognition without visual noise.
- Components: large Start button (min 56px), card history list, minimal chrome.
- Why changed: replaced vague "soft blue / deep teal" with exact hex for consistent Compose theming; chose high-contrast text for use while moving; restrained palette keeps spoken content the focus. Preview: `design.html`.

### 2.5 Success Metrics

- Time to first experience < 30s, 100% offline core flows, TTS < 500ms, 100% session save, cold launch < 2s

### 2.6 Acceptance Criteria

- [ ] Select tone + start offline, TTS speaks clear content, stop works, history viewable, fully offline, persists after restart, usable while walking, large accessible targets

## 3. Phase 2 — Better Content (Weeks 4-5)

- Expanded library 40–100, Scripture + Reflection mix, activity-aware pacing — P0/P1
- Simple stats (sessions/week, minutes), Favorite/Replay, Voice preference — P1
- Home encouragement line, filter history by tone
- Acceptance: ≥10 per tone, stats visible, replay works, still offline

## 4. Phase 3 — Polish & Settings (Weeks 6-7)

- Settings (tone default, voice, theme, history), Light/Dark/System, Clear History, Accessibility pass, About/Privacy — P0
- Acceptance: settings persist, theme correct, TalkBack announces, clear works, privacy visible

## 5. Phase 4 — Future

On-device generation, driving/workout detection, reminders, export, translations, widget, Wear OS.

## 6. Risks & Mitigations

- TTS quality varies (High/Med) → voice selection, test common devices
- Repetitive content (Med/High) → solid library + expansion
- Expect online AI (Med/Med) → clear offline messaging
- Battery (Med/Med) → optional detection, manual default
- Scope creep (High/High) → strict MVP focus

## 7. Appendix

### 7.1 Tones

- `gentle` — Soft, comforting, peaceful
- `encouraging` — Uplifting, strengthening
- `contemplative` — Reflective, quiet, deeper
- `challenging` — Direct, calls to obedience and growth

### 7.2 Content Structure

80–220 words, Christ-centered, speakable, tone-matched, non-divisive.

### 7.3 Privacy Commitment

No accounts, no analytics, no network for core features, all data on-device, deletable anytime.
