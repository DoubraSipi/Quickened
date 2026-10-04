package com.quickened.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Session::class, Settings::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun settingsDao(): SettingsDao
}
