package com.quickened.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey val id: String,
    val tone: String, // "gentle" | "encouraging" | "contemplative" | "challenging"
    val activity: String, // "walking" | "stationary" | "driving" | "unknown"
    val contentPreview: String,
    val durationSeconds: Int,
    val timestamp: String, // ISO 8601
    val createdAt: String
)

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val preferredTone: String = "gentle",
    val voice: String = "",
    val theme: String = "system", // "system" | "light" | "dark"
    val autoDetectActivity: Boolean = false
)
