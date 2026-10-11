package com.quickened.piper

import android.content.Context
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import org.json.JSONObject

// Full Piper playback: espeak data + downloaded voice + ONNX + AudioTrack.
// System TTS remains the fallback; callers check ready() first.
class PiperSpeaker(
    private val assets: AssetManager,
    private val filesDir: File,
    voiceDir: File,
    voiceId: String
) {
    private val voice = PiperCatalog.byId(voiceId)
    private val modelFile = voice?.let { File(voiceDir, "${it.id}.onnx") }
    private val mapFile = voice?.let { File(voiceDir, "${it.id}.onnx.json") }
    private var engine: PiperEngine? = null
    private var map: JSONObject? = null
    private var scales: FloatArray = floatArrayOf(0.667f, 1.0f, 0.8f)
    private var track: AudioTrack? = null
    private var stopped = false
    private val renderExec = Executors.newSingleThreadExecutor()

    val ready: Boolean
        get() = voice != null && modelFile?.exists() == true && mapFile?.exists() == true

    fun init(): Boolean {
        if (!ready) return false
        return try {
            copyAssetDir("espeak-ng-data", File(filesDir, "espeak-ng-data"))
            val rate = EspeakBridge.nativeInit(File(filesDir, "espeak-ng-data").absolutePath)
            if (rate <= 0) return false
            val json = JSONObject(mapFile!!.readText())
            map = json.getJSONObject("phoneme_id_map")
            val inf = json.optJSONObject("inference")
            val noiseScale = inf?.optDouble("noise_scale", 0.667)?.toFloat() ?: 0.667f
            val lengthScale = inf?.optDouble("length_scale", 1.0)?.toFloat() ?: 1.0f
            val noiseW = inf?.optDouble("noise_w", 0.8)?.toFloat() ?: 0.8f
            scales = floatArrayOf(noiseScale, lengthScale, noiseW)
            android.util.Log.i("PIPER_APP", "map entries=" + map!!.length() + " scales=" + scales.joinToString())
            engine = PiperEngine(modelFile!!.absolutePath)
            android.util.Log.i("PIPER_APP", "model=" + modelFile!!.name + " bytes=" + modelFile!!.length())
            true
        } catch (e: Exception) {
            android.util.Log.e("PIPER_APP", "init failed", e)
            false
        }
    }

    private fun textToPhonemeIds(phonemes: String, m: JSONObject): LongArray {
        val padId = m.optJSONArray("_")?.optLong(0) ?: 0L
        val bosId = m.optJSONArray("^")?.optLong(0) ?: 1L
        val eosId = m.optJSONArray("$")?.optLong(0) ?: 2L

        val ids = ArrayList<Long>()
        ids.add(bosId)
        ids.add(padId)

        var i = 0
        while (i < phonemes.length) {
            var matched: Long? = null
            var len = 0
            var l = 3.coerceAtMost(phonemes.length - i)
            while (l > 0) {
                val key = phonemes.substring(i, i + l)
                if (m.has(key)) {
                    val arr = m.getJSONArray(key)
                    if (arr.length() > 0) {
                        matched = arr.getLong(0)
                        len = l
                        break
                    }
                }
                l--
            }
            if (matched != null) {
                ids.add(matched)
                ids.add(padId)
                i += len
            } else {
                i += Character.charCount(phonemes.codePointAt(i))
            }
        }
        ids.add(eosId)
        return ids.toLongArray()
    }

    fun speak(text: String, onDone: () -> Unit, onError: () -> Unit = {}) {
        val eng = engine ?: run { onError(); return }
        val m = map ?: run { onError(); return }
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        if (sentences.isEmpty()) {
            onError()
            return
        }
        stopped = false
        Thread {
            try {
                fun renderSentence(sentence: String): FloatArray? {
                    if (stopped) return null
                    val phonemes = EspeakBridge.nativePhonemize("en", sentence) ?: return null
                    val ids = textToPhonemeIds(phonemes, m)
                    if (ids.isEmpty() || stopped) return null
                    return eng.synthesize(ids, scales)
                }

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

                val pauseShorts = ShortArray((sampleRate * 200) / 1000)
                var totalWrittenShorts = 0L

                var pending = renderExec.submit<FloatArray?> { renderSentence(sentences[0]) }

                for (idx in sentences.indices) {
                    if (stopped) break
                    val audio = try {
                        pending.get()
                    } catch (e: Exception) {
                        null
                    }
                    if (idx + 1 < sentences.size && !stopped) {
                        pending = renderExec.submit<FloatArray?> { renderSentence(sentences[idx + 1]) }
                    }

                    if (audio != null && audio.isNotEmpty()) {
                        val shorts = ShortArray(audio.size) { (audio[it].coerceIn(-1f, 1f) * 32767).toInt().toShort() }
                        var written = 0
                        while (written < shorts.size && !stopped) {
                            val w = t.write(shorts, written, shorts.size - written, AudioTrack.WRITE_BLOCKING)
                            if (w > 0) {
                                written += w
                                totalWrittenShorts += w
                            } else break
                        }
                        if (idx < sentences.size - 1 && !stopped && pauseShorts.isNotEmpty()) {
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

                if (!stopped) onDone()
            } catch (e: Exception) {
                android.util.Log.e("PIPER_APP", "speak failed", e)
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
            // already stopped
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

    companion object {
        fun fetchMap(context: Context, voice: PiperVoice): Boolean {
            return try {
                val out = File(PiperVoices.dir(context), "${voice.id}.onnx.json")
                if (out.exists()) return true
                context.assets.open("voices/${voice.id}.onnx.json").use { input ->
                    FileOutputStream(out).use { input.copyTo(it) }
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
