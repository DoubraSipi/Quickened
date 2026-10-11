package com.quickened.piper

// Voice catalog: download-once over Wi-Fi, stored app-private, offline after.
// Models: MIT/CC0 (rhasspy/piper-voices). Sizes are approximate.
data class PiperVoice(
    val id: String,
    val label: String,
    val sizeMb: Int,
    val onnxUrl: String,
    val jsonUrl: String
)

object PiperCatalog {
    private const val ROOT = "https://huggingface.co/rhasspy/piper-voices/resolve/main"
    const val EN_US = "en/en_US/lessac/medium/en_US-lessac-medium"

    val voices = listOf(
        PiperVoice(
            id = "lessac",
            label = "Lessac (natural, female)",
            sizeMb = 63,
            onnxUrl = "$ROOT/en/en_US/lessac/medium/en_US-lessac-medium.onnx",
            jsonUrl = "$ROOT/en/en_US/lessac/medium/en_US-lessac-medium.onnx.json"
        ),
        PiperVoice(
            id = "amy",
            label = "Amy (warm, female)",
            sizeMb = 63,
            onnxUrl = "$ROOT/en/en_US/amy/medium/en_US-amy-medium.onnx",
            jsonUrl = "$ROOT/en/en_US/amy/medium/en_US-amy-medium.onnx.json"
        ),
        PiperVoice(
            id = "alan",
            label = "Alan (calm, male)",
            sizeMb = 63,
            onnxUrl = "$ROOT/en/en_GB/alan/medium/en_GB-alan-medium.onnx",
            jsonUrl = "$ROOT/en/en_GB/alan/medium/en_GB-alan-medium.onnx.json"
        )
    )

    fun byId(id: String): PiperVoice? = voices.firstOrNull { it.id == id }
}
