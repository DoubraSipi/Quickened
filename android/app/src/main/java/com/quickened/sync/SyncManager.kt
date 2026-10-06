package com.quickened.sync

import com.quickened.data.Session
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Opt-in mirror to a Postgres-backed endpoint (e.g. Netlify Function).
// Local Room stays source of truth. Never blocks offline use: throws on
// no-network and callers must catch and report gently.
object SyncManager {
    fun payload(sessions: List<Session>, deviceId: String): String {
        val arr = JSONArray()
        sessions.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("tone", s.tone)
                    .put("activity", s.activity)
                    .put("preview", s.contentPreview)
                    .put("duration_seconds", s.durationSeconds)
                    .put("timestamp", s.timestamp)
                    .put("favorite", s.isFavorite)
            )
        }
        return JSONObject().put("device_id", deviceId).put("sessions", arr).toString()
    }

    fun push(endpoint: String, body: String): Int {
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        return try {
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }
}
