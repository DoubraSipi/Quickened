package com.quickened.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "fasts")
data class FastRecord(
    @PrimaryKey val id: String,
    val kind: String, // "water24" | "partial3" | "daniel21" | "custom"
    val title: String,
    val targetHours: Int,
    val intention: String,
    val verseTitle: String,
    val verseText: String,
    val startedAt: String, // ISO 8601
    val endedAt: String? = null,
    val status: String = "active" // "active" | "completed" | "broken"
)

data class FastPlan(val kind: String, val title: String, val hours: Int, val note: String)

val FAST_PLANS = listOf(
    FastPlan("water24", "Water fast — 24h", 24, "Water only. Pray when hunger speaks."),
    FastPlan("partial3", "Partial fast — 3 days", 72, "Simple meals, no sweets or feasting."),
    FastPlan("daniel21", "Daniel fast — 21 days", 504, "No meat, sweets, or bread. Prayer and Scripture daily."),
    FastPlan("sunrise", "Sunrise to sunset", 12, "From waking light to evening. Break with thanksgiving.")
)

@Dao
interface FastDao {
    @Insert
    suspend fun insert(fast: FastRecord)

    @Query("SELECT * FROM fasts WHERE status = 'active' LIMIT 1")
    suspend fun active(): FastRecord?

    @Query("SELECT * FROM fasts WHERE status != 'active' ORDER BY startedAt DESC LIMIT 50")
    suspend fun history(): List<FastRecord>

    @Query("SELECT COUNT(*) FROM fasts WHERE status = 'completed'")
    suspend fun completedCount(): Int

    @Query("UPDATE fasts SET status = :status, endedAt = :endedAt WHERE id = :id")
    suspend fun finish(id: String, status: String, endedAt: String)
}
