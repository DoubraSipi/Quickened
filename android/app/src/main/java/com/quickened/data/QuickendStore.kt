package com.quickened.data

import java.util.UUID

// Room-backed store. All calls are suspend; invoke from a coroutine scope.
class QuickendStore(
    private val db: AppDatabase
) {
    suspend fun saveSettings(s: Settings) = db.settingsDao().save(s)

    suspend fun getSettings(): Settings = db.settingsDao().load() ?: Settings()

    suspend fun addSession(
        tone: String,
        activity: String,
        contentPreview: String,
        durationSeconds: Int
    ): Session {
        val now = java.time.Instant.now().toString()
        val session = Session(
            id = UUID.randomUUID().toString(),
            tone = tone,
            activity = activity,
            contentPreview = contentPreview,
            durationSeconds = durationSeconds,
            timestamp = now,
            createdAt = now
        )
        db.sessionDao().insert(session)
        return session
    }

    suspend fun getSessions(): List<Session> = db.sessionDao().getAll()

    suspend fun clearHistory() = db.sessionDao().clearAll()
}
