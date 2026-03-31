package com.baby.feedingtracker

import android.app.Application
import com.baby.feedingtracker.di.AppContainer

class BabyFeedingApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
