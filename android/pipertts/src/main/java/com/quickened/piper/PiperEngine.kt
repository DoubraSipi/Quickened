package com.quickened.piper

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlin.math.min

/**
 * Spike step 1: loads a Piper voice model and runs inference from phoneme IDs.
 * Phonemes still come from espeak-ng (JNI bridge is the next build step); this
 * proves ONNX Runtime executes the voice on-device within realtime budget.
 */
class PiperEngine(modelPath: String) : AutoCloseable {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(
        modelPath,
        OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(min(4, Runtime.getRuntime().availableProcessors()))
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }
    )

    val inputNames: Set<String> = session.inputNames
    val sampleRate: Int = 22050

    fun synthesize(phonemeIds: LongArray, scales: FloatArray = floatArrayOf(0.667f, 1.0f, 0.8f)): FloatArray {
        val tInput = OnnxTensor.createTensor(env, arrayOf(phonemeIds))
        val tLengths = OnnxTensor.createTensor(env, longArrayOf(phonemeIds.size.toLong()))
        val tScales = OnnxTensor.createTensor(env, scales)
        val inputs = mapOf(
            "input" to tInput,
            "input_lengths" to tLengths,
            "scales" to tScales
        )
        try {
            session.run(inputs).use { out ->
                var node: Any? = out[0].value
                while (node is Array<*>) {
                    node = node[0]
                }
                val audio = node as? FloatArray
                    ?: throw IllegalStateException("unexpected audio shape")
                return audio
            }
        } finally {
            try { tInput.close() } catch (e: Exception) {}
            try { tLengths.close() } catch (e: Exception) {}
            try { tScales.close() } catch (e: Exception) {}
        }
    }

    override fun close() {
        try {
            session.close()
        } catch (e: Exception) {
            // ignore
        }
    }
}
