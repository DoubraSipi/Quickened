package com.quickened.piper

// Kokoro-82M voices (Apache-2.0). Model is one 177MB download shared by all
// voices; each voice pack is ~512KB. Offline after download.
data class KokoroVoice(val id: String, val label: String)

object KokoroCatalog {
    private const val ROOT = "https://huggingface.co/onnx-community/Kokoro-82M-ONNX/resolve/main"
    const val MODEL_URL = "$ROOT/onnx/model_uint8.onnx"
    const val MODEL_MB = 177

    val voices = listOf(
        KokoroVoice("sarah", "Sarah (warm, female)"),
        KokoroVoice("bella", "Bella (bright, female)"),
        KokoroVoice("michael", "Michael (steady, male)"),
        KokoroVoice("george", "George (deep, male)")
    )

    fun binUrl(id: String): String = when (id) {
        "sarah" -> "$ROOT/voices/af_sarah.bin"
        "bella" -> "$ROOT/voices/af_bella.bin"
        "michael" -> "$ROOT/voices/am_michael.bin"
        "george" -> "$ROOT/voices/bm_george.bin"
        else -> "$ROOT/voices/af_sarah.bin"
    }

    fun byId(id: String): KokoroVoice? = voices.firstOrNull { it.id == id }
}
