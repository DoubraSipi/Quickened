package com.quickened.piper

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import java.io.File

// On-device files for Kokoro: one shared model + per-voice packs.
// Downloads via system service (Wi-Fi or data); offline after.
object KokoroStore {

    fun dir(context: Context): File = File(context.getExternalFilesDir(null), "kokoro").apply { mkdirs() }

    fun modelFile(context: Context): File = File(dir(context), "model_uint8.onnx")

    fun voiceFile(context: Context, id: String): File = File(dir(context), "$id.bin")

    fun modelReady(context: Context): Boolean = modelFile(context).exists()

    fun voiceReady(context: Context, id: String): Boolean = voiceFile(context, id).exists()

    fun downloading(context: Context): Boolean {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return false
        return try {
            dm.query(DownloadManager.Query()).use { c ->
                val statusIx = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                val uriIx = c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                while (c.moveToNext()) {
                    val st = c.getInt(statusIx)
                    val uri = c.getString(uriIx) ?: continue
                    if ((st == DownloadManager.STATUS_RUNNING || st == DownloadManager.STATUS_PAUSED) &&
                        (uri.contains("kokoro") || uri.contains("model_uint8"))
                    ) return true
                }
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // Returns false when the system download service refuses.
    fun downloadModel(context: Context): Boolean {
        if (modelReady(context)) return true
        return enqueue(context, KokoroCatalog.MODEL_URL, modelFile(context), "Kokoro voice engine (~177MB, once)")
    }

    fun downloadVoice(context: Context, id: String): Boolean {
        if (voiceReady(context, id)) return true
        return enqueue(context, KokoroCatalog.binUrl(id), voiceFile(context, id), "Kokoro voice")
    }

    fun deleteVoice(context: Context, id: String) {
        voiceFile(context, id).delete()
    }

    private fun enqueue(context: Context, url: String, dest: File, title: String): Boolean {
        return try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: return false
            val req = DownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setDescription("One-time download, then offline")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(dest))
                .setAllowedOverMetered(true)
            dm.enqueue(req)
            true
        } catch (e: Exception) {
            false
        }
    }
}
