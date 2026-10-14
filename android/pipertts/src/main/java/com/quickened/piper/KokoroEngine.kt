package com.quickened.piper

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.json.JSONObject

// Kokoro-82M (uint8) spike driver. Reuses the espeak bridge for phonemes;
// tokenizes via the bundled transformers.js vocab, styles from the voice pack.
class KokoroEngine(modelPath: String, voicePath: String, vocab: Map<String, Long>) : AutoCloseable {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(
        modelPath,
        OrtSession.SessionOptions().apply {
            // Thread sweep on itel P70 (Unisoc T606), int8 82M model:
            // 2 threads ~7x realtime, 4 and 8 threads ~4x realtime.
            // Chip-bound, not thread-bound: keep 4 (less battery, same speed).
            setIntraOpNumThreads(4)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }
    )
    private val style: FloatArray = loadStyle(voicePath)
    private val vocab: Map<String, Long> = vocab

    val inputNames: Set<String> = session.inputNames
    val sampleRate: Int = 24000

    fun tokenize(phonemes: String): LongArray {
        val ids = ArrayList<Long>()
        ids.add(0L)
        var i = 0
        while (i < phonemes.length) {
            var matched: Long? = null
            var len = 0
            var l = 4.coerceAtMost(phonemes.length - i)
            while (l > 0) {
                val key = phonemes.substring(i, i + l)
                val id = vocab[key]
                if (id != null) {
                    matched = id
                    len = l
                    break
                }
                l--
            }
            if (matched != null) {
                ids.add(matched)
                i += len
            } else {
                i += Character.charCount(phonemes.codePointAt(i))
            }
        }
        ids.add(0L)
        return ids.toLongArray()
    }

    fun synthesize(tokenIds: LongArray, speed: Float = 1.0f): FloatArray {
        val styleInput = Array(1) { style }
        val tIds = OnnxTensor.createTensor(env, arrayOf(tokenIds))
        val tStyle = OnnxTensor.createTensor(env, styleInput)
        val tSpeed = OnnxTensor.createTensor(env, floatArrayOf(speed))
        val inputs = mapOf(
            "input_ids" to tIds,
            "style" to tStyle,
            "speed" to tSpeed
        )
        try {
            session.run(inputs).use { out ->
                val first = out[0]
                val root = first.value
                val audio: FloatArray = when (root) {
                    is FloatArray -> root
                    is Array<*> -> when (val inner = root[0]) {
                        is FloatArray -> inner
                        is Array<*> -> (inner[0] as FloatArray)
                        else -> throw IllegalStateException("unexpected audio shape")
                    }
                    else -> throw IllegalStateException("unexpected audio root")
                }
                return audio
            }
        } finally {
            try { tIds.close() } catch (e: Exception) {}
            try { tStyle.close() } catch (e: Exception) {}
            try { tSpeed.close() } catch (e: Exception) {}
        }
    }

    override fun close() {
        try {
            session.close()
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        fun loadStyle(voicePath: String): FloatArray {
            val bytes = File(voicePath).readBytes()
            val floats = FloatArray(bytes.size / 4)
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(floats)
            // Style dim is 256; packs carry repetitions — average them.
            val dim = 256
            val reps = floats.size / dim
            val avg = FloatArray(dim)
            for (r in 0 until reps) {
                for (d in 0 until dim) avg[d] += floats[r * dim + d] / reps
            }
            return avg
        }

        fun loadVocab(tokenizerJson: String): Map<String, Long> {
            val vocab = JSONObject(tokenizerJson).getJSONObject("model").getJSONObject("vocab")
            val map = HashMap<String, Long>()
            val keys = vocab.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = vocab.getLong(k)
            }
            return map
        }
    }
}
