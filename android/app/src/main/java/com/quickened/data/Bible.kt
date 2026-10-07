package com.quickened.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "bible_verses")
data class BibleVerse(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

@Dao
interface BibleDao {
    @Query("SELECT COUNT(*) FROM bible_verses")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<BibleVerse>)

    @Query("SELECT * FROM bible_verses WHERE length(text) BETWEEN :minLen AND :maxLen ORDER BY RANDOM() LIMIT 1")
    suspend fun randomVerse(minLen: Int = 40, maxLen: Int = 400): BibleVerse?

    @Query("SELECT * FROM bible_verses WHERE book = :book AND chapter = :chapter AND verse >= :fromVerse ORDER BY verse LIMIT :take")
    suspend fun passage(book: String, chapter: Int, fromVerse: Int, take: Int = 2): List<BibleVerse>
}
