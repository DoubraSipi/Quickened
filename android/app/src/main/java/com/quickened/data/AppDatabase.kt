package com.quickened.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Session::class, Settings::class, BibleVerse::class, PrayerWatch::class, FastRecord::class, BookProgress::class],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun settingsDao(): SettingsDao
    abstract fun bibleDao(): BibleDao
    abstract fun prayerDao(): PrayerDao
    abstract fun fastDao(): FastDao
    abstract fun bookDao(): BookDao
}
