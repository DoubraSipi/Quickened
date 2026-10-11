package com.quickened.piper

import android.content.Context
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

// App-facing Kokoro speaker: same speak/stop shape as PiperSpeaker so
// ExperienceScreen can route to either engine.
class KokoroSpeaker(
    private val assets: AssetManager,
    private val filesDir: File,
    private val context: Context,
    private val voiceId: String
) {
    private var engine: KokoroEngine? = null
    private var vocab: Map<String, Long>? = null
    private var stopped = false
    private var track: AudioTrack? = null
    private val renderExec = Executors.newSingleThreadExecutor()

    val ready: Boolean
        get() = KokoroStore.modelReady(context) && KokoroStore.voiceReady(context, voiceId)

    fun init(): Boolean {
        if (!ready) return false
        return try {
            copyAssetDir("espeak-ng-data", File(filesDir, "espeak-ng-data"))
            val rate = EspeakBridge.nativeInit(File(filesDir, "espeak-ng-data").absolutePath)
            if (rate <= 0) return false
            vocab = KokoroEngine.loadVocab(
                assets.open("kokoro/tokenizer.json").bufferedReader().use { it.readText() }
            )
            val model = KokoroStore.modelFile(context).absolutePath
            val voice = KokoroStore.voiceFile(context, voiceId).absolutePath
            engine = KokoroEngine(model, voice, vocab!!)
            android.util.Log.i("KOKORO_APP", "speaker ready: $voiceId inputs=" + engine!!.inputNames)
            true
        } catch (e: Exception) {
            android.util.Log.e("KOKORO_APP", "init failed", e)
            false
        } catch (t: Throwable) {
            android.util.Log.e("KOKORO_APP", "init failed", t)
            false
        }
    }

    private fun splitSentences(text: String): List<String> {
        val raw = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val out = ArrayList<String>()
        for (s in raw) {
            if (s.length > 150) {
                out.addAll(s.split(Regex("(?<=[,;:!?.])\\s+")).map { it.trim() }.filter { it.isNotEmpty() })
            } else {
                out.add(s)
            }
        }
        return if (out.isNotEmpty()) out else listOf(text)
    }

    fun speak(text: String, onDone: () -> Unit, onError: () -> Unit = {}, onStarted: () -> Unit = {}) {
        val eng = engine ?: run { onError(); return }
        val v = vocab ?: run { onError(); return }

        val key = cacheKey(text)
        val hit = cacheFile(key)
        if (hit.exists()) {
            playFile(hit, onDone, onStarted)
            return
        }

        val units = splitSentences(text)
        if (units.isEmpty()) {
            onError()
            return
        }

        stopped = false
        Thread {
            try {
                val sampleRate = eng.sampleRate
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(16384)

                val t = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track = t
                t.play()

                val pauseShorts = ShortArray((sampleRate * 220) / 1000)
                val rendered = ArrayList<Short>()
                var totalWrittenShorts = 0L
                var firstAudio = true

                fun renderUnit(u: String): FloatArray? {
                    if (stopped) return null
                    return try {
                        val phonemes = EspeakBridge.nativePhonemize("en", u) ?: return null
                        val ids = eng.tokenize(phonemes)
                        if (ids.isEmpty() || stopped) return null
                        eng.synthesize(ids)
                    } catch (e: Exception) {
                        android.util.Log.e("KOKORO_APP", "unit render failed", e)
                        null
                    }
                }

                var pending = renderExec.submit<FloatArray?> { renderUnit(units[0]) }

                for (idx in units.indices) {
                    if (stopped) break
                    val audio = try {
                        pending.get()
                    } catch (e: Exception) {
                        null
                    }

                    if (idx + 1 < units.size && !stopped) {
                        pending = renderExec.submit<FloatArray?> { renderUnit(units[idx + 1]) }
                    }

                    if (audio != null && audio.isNotEmpty()) {
                        if (firstAudio) {
                            firstAudio = false
                            onStarted()
                        }
                        val shorts = ShortArray(audio.size) { (audio[it].coerceIn(-1f, 1f) * 32767).toInt().toShort() }
                        for (s in shorts) rendered.add(s)

                        var written = 0
                        while (written < shorts.size && !stopped) {
                            val w = t.write(shorts, written, shorts.size - written, AudioTrack.WRITE_BLOCKING)
                            if (w > 0) {
                                written += w
                                totalWrittenShorts += w
                            } else break
                        }

                        if (idx < units.size - 1 && !stopped && pauseShorts.isNotEmpty()) {
                            for (s in pauseShorts) rendered.add(s)
                            val w = t.write(pauseShorts, 0, pauseShorts.size, AudioTrack.WRITE_BLOCKING)
                            if (w > 0) totalWrittenShorts += w
                        }
                    }
                }

                while (!stopped && t.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    val head = t.playbackHeadPosition.toLong() and 0xFFFFFFFFL
                    if (head >= totalWrittenShorts) break
                    try { Thread.sleep(30) } catch (e: InterruptedException) { break }
                }

                try {
                    t.stop()
                    t.flush()
                    t.release()
                } catch (e: Exception) {
                    // ignore
                }
                if (track === t) track = null

                if (!stopped) {
                    if (rendered.isNotEmpty()) {
                        saveCache(key, rendered.toShortArray(), sampleRate)
                    }
                    onDone()
                }
            } catch (e: Exception) {
                android.util.Log.e("KOKORO_APP", "speak failed", e)
                onError()
            }
        }.start()
    }

    fun stop() {
        stopped = true
        try {
            track?.stop()
            track?.flush()
            track?.release()
        } catch (e: Exception) {
            // ignore
        }
        track = null
    }

    fun shutdown() {
        stop()
        try {
            renderExec.shutdownNow()
        } catch (e: Exception) {
            // ignore
        }
        try {
            engine?.close()
        } catch (e: Exception) {
            // ignore
        }
        engine = null
    }

    private fun cacheKey(text: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            .substring(0, 16) + ".wav"
    }

    private fun cacheFile(key: String): File =
        File(File(filesDir, "audio-cache").apply { mkdirs() }, key)

    private fun saveCache(key: String, shorts: ShortArray, sampleRate: Int) {
        try {
            val out = cacheFile(key)
            val data = java.nio.ByteBuffer.allocate(44 + shorts.size * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            data.put("RIFF".toByteArray()); data.putInt(36 + shorts.size * 2); data.put("WAVE".toByteArray())
            data.put("fmt ".toByteArray()); data.putInt(16); data.putShort(1); data.putShort(1)
            data.putInt(sampleRate); data.putInt(sampleRate * 2); data.putShort(2); data.putShort(16)
            data.put("data".toByteArray()); data.putInt(shorts.size * 2)
            shorts.forEach { data.putShort(it) }
            FileOutputStream(out).use { it.write(data.array()) }
        } catch (e: Exception) {
            android.util.Log.w("KOKORO_APP", "cache save failed", e)
        }
    }

    private fun playFile(cached: File, onDone: () -> Unit, onStarted: () -> Unit) {
        stopped = false
        Thread {
            try {
                val bytes = cached.readBytes()
                if (bytes.size < 44) {
                    onDone()
                    return@Thread
                }
                val pcm = bytes.drop(44).toByteArray()
                val shorts = ShortArray(pcm.size / 2) { i ->
                    ((pcm[i * 2].toInt() and 0xFF) or (pcm[i * 2 + 1].toInt() shl 8)).toShort()
                }

                val sampleRate = 24000
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(16384)

                val t = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track = t
                onStarted()
                t.play()

                var written = 0
                while (written < shorts.size && !stopped) {
                    val w = t.write(shorts, written, shorts.size - written, AudioTrack.WRITE_BLOCKING)
                    if (w > 0) written += w
                    else break
                }

                while (!stopped && t.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    val head = t.playbackHeadPosition.toLong() and 0xFFFFFFFFL
                    if (head >= shorts.size) break
                    try { Thread.sleep(30) } catch (e: InterruptedException) { break }
                }

                try {
                    t.stop()
                    t.flush()
                    t.release()
                } catch (e: Exception) {
                    // ignore
                }
                if (track === t) track = null

                if (!stopped) onDone()
            } catch (e: Exception) {
                onDone()
            }
        }.start()
    }

    private fun copyAssetDir(assetPath: String, dest: File) {
        val list = assets.list(assetPath) ?: return
        if (list.isEmpty()) {
            if (!dest.exists()) {
                dest.parentFile?.mkdirs()
                assets.open(assetPath).use { input ->
                    FileOutputStream(dest).use { input.copyTo(it) }
                }
            }
            return
        }
        dest.mkdirs()
        list.forEach { copyAssetDir("$assetPath/$it", File(dest, it)) }
    }
}
