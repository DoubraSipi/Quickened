package com.quickened.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "prayer_watches")
data class PrayerWatch(
    @PrimaryKey val id: String,
    val title: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true
)

@Dao
interface PrayerDao {
    @Insert
    suspend fun insert(item: PrayerWatch)

    @Query("SELECT * FROM prayer_watches ORDER BY hour, minute")
    suspend fun getAll(): List<PrayerWatch>

    @Query("UPDATE prayer_watches SET enabled = :on WHERE id = :id")
    suspend fun setEnabled(id: String, on: Boolean)

    @Query("DELETE FROM prayer_watches WHERE id = :id")
    suspend fun delete(id: String)
}
