package com.baby.feedingtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baby.feedingtracker.ui.MainScreen
import com.baby.feedingtracker.ui.MainViewModel
import com.baby.feedingtracker.ui.theme.BabyFeedingTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BabyFeedingApp
        val repository = app.container.repository

        setContent {
            BabyFeedingTrackerTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModel.factory(repository)
                )
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
