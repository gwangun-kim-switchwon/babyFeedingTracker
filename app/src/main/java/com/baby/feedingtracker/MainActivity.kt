package com.baby.feedingtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baby.feedingtracker.ui.MainScreen
import com.baby.feedingtracker.ui.MainViewModel
import com.baby.feedingtracker.ui.theme.BabyFeedingTrackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BabyFeedingApp

        setContent {
            BabyFeedingTrackerTheme {
                val repository by app.container.repository.collectAsState()
                val coroutineScope = rememberCoroutineScope()

                val googleSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    coroutineScope.launch {
                        val signInResult = app.container.googleAuthHelper
                            .handleSignInResult(result.data)
                        signInResult.onSuccess { uid ->
                            // Create profile after successful login
                            val email = app.container.googleAuthHelper.currentUserEmail()
                            app.container.userRepository.createProfile(uid, email)
                        }
                    }
                }

                if (repository != null) {
                    val viewModel: MainViewModel = viewModel(
                        factory = MainViewModel.factory(
                            repository!!,
                            app.container.userRepository,
                            app.container.googleAuthHelper,
                            app.container.auth
                        )
                    )

                    // Refresh login state after Google Sign-In callback
                    val googleSignInLauncherWithRefresh = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        coroutineScope.launch {
                            val signInResult = app.container.googleAuthHelper
                                .handleSignInResult(result.data)
                            signInResult.onSuccess { uid ->
                                val email = app.container.googleAuthHelper.currentUserEmail()
                                app.container.userRepository.createProfile(uid, email)
                                viewModel.refreshLoginState()
                            }
                        }
                    }

                    MainScreen(
                        viewModel = viewModel,
                        googleAuthHelper = app.container.googleAuthHelper,
                        googleSignInLauncher = googleSignInLauncherWithRefresh
                    )
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
