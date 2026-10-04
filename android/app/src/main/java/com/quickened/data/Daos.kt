package com.quickened.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: Session)

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    suspend fun getAll(): List<Session>

    @Query("DELETE FROM sessions")
    suspend fun clearAll()
}

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: Settings)

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun load(): Settings?
}
