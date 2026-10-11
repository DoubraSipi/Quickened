package com.quickened.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "book_progress")
data class BookProgress(
    @PrimaryKey val uri: String,
    val title: String,
    val chapter: Int = 0
)

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(progress: BookProgress)

    @Query("SELECT * FROM book_progress WHERE uri = :uri")
    suspend fun load(uri: String): BookProgress?
}
