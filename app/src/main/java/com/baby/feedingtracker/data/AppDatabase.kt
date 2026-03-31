package com.baby.feedingtracker.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FeedingRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedingDao(): FeedingDao
}
