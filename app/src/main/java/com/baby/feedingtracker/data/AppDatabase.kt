package com.baby.feedingtracker.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FeedingRecord::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedingDao(): FeedingDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE feeding_records ADD COLUMN type TEXT")
                db.execSQL("ALTER TABLE feeding_records ADD COLUMN amountMl INTEGER")
            }
        }
    }
}
