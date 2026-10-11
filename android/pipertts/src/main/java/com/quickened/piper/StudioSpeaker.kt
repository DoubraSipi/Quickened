package com.quickened.piper

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

// Studio voice: server-rendered audio (see /tts-server), cached on device.
// First play of a text needs internet; every replay is offline.
class StudioSpeaker(private val context: Context) {
    private var track: android.media.AudioTrack? = null
    private var stopped = false

    fun speak(
        baseUrl: String,
        apiKey: String,
        text: String,
        onDone: () -> Unit,
        onError: () -> Unit = {},
        onStarted: () -> Unit = {}
    ) {
        stopped = false
        Thread {
            try {
                val file = cachedFile(text)
                val wav: File = if (file.exists()) file
                else render(baseUrl, apiKey, text, file) ?: run {
                    onError()
                    return@Thread
                }
                if (stopped) return@Thread
                playFile(wav, onDone, onStarted)
            } catch (e: Exception) {
                android.util.Log.e("STUDIO_APP", "speak failed", e)
                onError()
            }
        }.start()
    }

    fun stop() {
        stopped = true
        try {
            track?.stop()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun shutdown() {
        stop()
        try {
            track?.release()
        } catch (e: Exception) {
            // ignore
        }
        track = null
    }

    private fun cacheDir(): File = File(context.filesDir, "studio-cache").apply { mkdirs() }

    private fun key(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            .substring(0, 16) + ".wav"
    }

    private fun cachedFile(text: String): File = File(cacheDir(), key(text))

    private fun render(baseUrl: String, apiKey: String, text: String, dest: File): File? {
        return try {
            val url = URL(baseUrl.trimEnd('/') + "/synthesize")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                if (apiKey.isNotEmpty()) setRequestProperty("X-API-Key", apiKey)
                doOutput = true
                connectTimeout = 20000
                readTimeout = 60000
            }
            val body = "{\"text\":" + JSONObjectQuote(text) + "}"
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) {
                android.util.Log.w("STUDIO_APP", "server said " + conn.responseCode)
                return null
            }
            dest.parentFile?.mkdirs()
            val tmp = File(dest.absolutePath + ".part")
            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { input.copyTo(it) }
            }
            tmp.renameTo(dest)
            dest
        } catch (e: Exception) {
            android.util.Log.e("STUDIO_APP", "render failed", e)
            null
        } finally {
        }
    }

    private fun JSONObjectQuote(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        return sb.append("\"").toString()
    }

    private fun playFile(cached: File, onDone: () -> Unit, onStarted: () -> Unit) {
        try {
            val bytes = cached.readBytes()
            if (bytes.size < 44) {
                onDone()
                return
            }
            val pcm = bytes.drop(44).toByteArray()
            // Assume 22050Hz mono 16-bit (server default); header parse kept simple.
            val shorts = ShortArray(pcm.size / 2) { i ->
                ((pcm[i * 2].toInt() and 0xFF) or (pcm[i * 2 + 1].toInt() shl 8)).toShort()
            }
            onStarted()
            val t = android.media.AudioTrack.Builder()
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    android.media.AudioFormat.Builder()
                        .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(22050)
                        .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes((shorts.size * 2).coerceAtLeast(4096))
                .setTransferMode(android.media.AudioTrack.MODE_STATIC)
                .build()
            track = t
            t.write(shorts, 0, shorts.size)
            t.setNotificationMarkerPosition(shorts.size)
            t.setPlaybackPositionUpdateListener(
                object : android.media.AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(track: android.media.AudioTrack) {
                        if (!stopped) onDone()
                    }

                    override fun onPeriodicNotification(track: android.media.AudioTrack) {}
                }
            )
            if (!stopped) t.play() else onDone()
        } catch (e: Exception) {
            onDone()
        }
    }
}
