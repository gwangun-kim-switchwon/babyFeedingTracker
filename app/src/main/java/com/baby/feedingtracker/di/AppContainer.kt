package com.baby.feedingtracker.di

import android.content.Context
import androidx.room.Room
import com.baby.feedingtracker.data.AppDatabase
import com.baby.feedingtracker.data.FeedingRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "feeding-db"
    ).addMigrations(AppDatabase.MIGRATION_1_2)
     .build()

    val repository = FeedingRepository(database.feedingDao())
}
