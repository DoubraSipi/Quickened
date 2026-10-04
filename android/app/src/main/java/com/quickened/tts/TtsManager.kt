package com.quickened.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

// Offline Android TTS wrapper. MVP: stop() doubles as pause (resume restarts).
class TtsManager(context: Context) {
    var onDone: (() -> Unit)? = null
        set(value) {
            field = value
            tts?.setOnUtteranceProgressListener(listener)
        }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onError(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) {
            onDone?.invoke()
        }
    }

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(listener)
            }
        }
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "quickened-1")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
