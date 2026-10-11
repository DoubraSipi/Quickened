package com.quickened.piper

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import java.io.File

// Manages on-device Piper voice files (app-private, offline after download).
// Download progress is coarse (system DownloadManager); playback arrives
// with the native phoneme bridge.
object PiperVoices {

    sealed interface Status {
        data object Missing : Status
        data object Downloading : Status
        data object Ready : Status
    }

    // App-specific external storage: DownloadManager cannot write to internal
    // filesDir on modern Android; this needs no storage permission.
    fun dir(context: Context): File =
        File(context.getExternalFilesDir(null), "piper").apply { mkdirs() }

    fun modelFile(context: Context, voice: PiperVoice): File =
        File(dir(context), "${voice.id}.onnx")

    fun status(context: Context, voice: PiperVoice): Status {
        if (modelFile(context, voice).exists()) return Status.Ready
        val id = downloadId(context, voice) ?: return Status.Missing
        val dm = context.getSystemService(DownloadManager::class.java)
        dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
            if (!c.moveToFirst()) {
                forget(context, voice)
                return Status.Missing
            }
            return when (c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    forget(context, voice)
                    Status.Ready
                }
                DownloadManager.STATUS_FAILED -> {
                    forget(context, voice)
                    Status.Missing
                }
                else -> Status.Downloading
            }
        }
    }

    // Returns false when the system download service refuses (missing/blocked).
    fun download(context: Context, voice: PiperVoice): Boolean {
        if (status(context, voice) != Status.Missing) return true
        return try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: return false
            val req = DownloadManager.Request(Uri.parse(voice.onnxUrl))
                .setTitle("Quickened voice: ${voice.label}")
                .setDescription("One-time download (~${voice.sizeMb}MB), then offline")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(modelFile(context, voice)))
                .setAllowedOverMetered(true)
            val id = dm.enqueue(req)
            context.getSharedPreferences("piper", Context.MODE_PRIVATE).edit()
                .putLong("dl_${voice.id}", id).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun delete(context: Context, voice: PiperVoice) {
        downloadId(context, voice)?.let { id ->
            try {
                context.getSystemService(DownloadManager::class.java).remove(id)
            } catch (e: Exception) {
                // already gone
            }
        }
        forget(context, voice)
        modelFile(context, voice).delete()
        File(dir(context), "${voice.id}.onnx.json").delete()
    }

    private fun downloadId(context: Context, voice: PiperVoice): Long? {
        val id = context.getSharedPreferences("piper", Context.MODE_PRIVATE)
            .getLong("dl_${voice.id}", -1)
        return if (id < 0) null else id
    }

    private fun forget(context: Context, voice: PiperVoice) {
        context.getSharedPreferences("piper", Context.MODE_PRIVATE).edit()
            .remove("dl_${voice.id}").apply()
    }
}
