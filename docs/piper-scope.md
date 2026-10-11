# Piper TTS Integration — Scope (Phase 5, post-release)

## Goal
Offline neural voices inside Quickened, no system-TTS dependency, no API keys.

## Approach
- `:pipertts` module: ONNX Runtime Android AAR (`com.microsoft.onnxruntime:onnxruntime-android`)
  + espeak-ng phonemizer via JNI (CMake NDK build from rhasspy/espeak-ng sources).
- Voice catalog (download-once over Wi-Fi, stored app-private, offline after):
  - `en_US-lessac-medium` (~60MB, natural female) — default recommendation
  - `en_US-amy-medium`, `en_GB-alan-medium` as alternates
- `PiperEngine`: load model → synthesize PCM → AudioTrack playback. Same interface
  shape as `TtsManager` (speak/stop/onDone) so `ExperienceScreen` swaps cleanly.
- Settings → Voice gains a second section: **System voices** (today) vs
  **Quickened voices** (Piper downloads with progress bar + delete).

## Phasing
1. Spike: ONNX Runtime inference of one voice on itel P70, measure RTF + RAM.
2. espeak-ng JNI bridge (the hard part): phonemize → feed Piper.
3. Download manager + storage + picker UI.
4. Swap-in behind a Settings toggle ("Use Quickened voices"), system TTS stays fallback.
5. QA: airplane mode, calls/interruptions, battery, 5 voices.

## Effort & risks
- Estimate: 2–4 weeks solo-dev equivalent; espeak-ng native build is the long pole.
- itel P70: 16kHz medium voices expected real-time; monitor thermal throttling.
- APK stays lean (models are downloads, not bundled).
- License: MIT (engine) + voice models CC0/public — store-compliant.
- Offline-first and privacy posture unchanged.

## Acceptance
- Airplane-mode speak in Piper voice, pause/stop parity, no regression when
  Piper voice absent (falls back to system TTS with a notice).
