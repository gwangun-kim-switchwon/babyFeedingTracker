package com.baby.feedingtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baby.feedingtracker.ui.MainScreen
import com.baby.feedingtracker.ui.MainViewModel
import com.baby.feedingtracker.ui.theme.BabyFeedingTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BabyFeedingApp

        setContent {
            BabyFeedingTrackerTheme {
                val repository by app.container.repository.collectAsState()

                if (repository != null) {
                    val viewModel: MainViewModel = viewModel(
                        factory = MainViewModel.factory(repository!!)
                    )
                    MainScreen(viewModel = viewModel)
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
