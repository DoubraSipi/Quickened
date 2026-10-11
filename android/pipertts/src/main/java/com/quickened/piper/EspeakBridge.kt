package com.quickened.piper

// JNI into the vendored espeak-ng static lib. init() once with the data dir,
// phonemize() returns the raw IPA string; PiperEngine speaks from IDs mapped
// in Kotlin via the voice's phoneme_id_map.
object EspeakBridge {
    init {
        System.loadLibrary("espeak-ng")
    }

    @JvmStatic
    external fun nativeInit(dataPath: String): Int

    @JvmStatic
    external fun nativePhonemize(voice: String, text: String): String?
}
