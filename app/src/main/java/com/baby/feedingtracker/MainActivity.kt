package com.baby.feedingtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baby.feedingtracker.di.AppContainer
import com.baby.feedingtracker.ui.cleaning.CleaningViewModel
import com.baby.feedingtracker.ui.diaper.DiaperViewModel
import com.baby.feedingtracker.ui.feeding.FeedingViewModel
import com.baby.feedingtracker.ui.sleep.SleepViewModel
import com.baby.feedingtracker.ui.navigation.BabyFeedingNavHost
import com.baby.feedingtracker.ui.theme.BabyFeedingTrackerTheme
import com.baby.feedingtracker.ui.theme.ThemeMode
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BabyFeedingApp

        setContent {
            val themeMode by app.container.themePreference.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)

            BabyFeedingTrackerTheme(themeMode = themeMode) {
                val repository by app.container.repository.collectAsState()
                val cleaningRepository by app.container.cleaningRepository.collectAsState()
                val diaperRepository by app.container.diaperRepository.collectAsState()
                val sleepRepository by app.container.sleepRepository.collectAsState()
                val initError by app.container.initError.collectAsState()
                val coroutineScope = rememberCoroutineScope()

                if (repository != null && cleaningRepository != null && diaperRepository != null && sleepRepository != null) {
                    // repository 인스턴스가 바뀌면 (Google 로그인 후 uid 변경 등)
                    // ViewModel을 새로 생성하여 새 데이터를 로드
                    val viewModel: FeedingViewModel = viewModel(
                        key = "main_vm_${repository.hashCode()}",
                        factory = FeedingViewModel.factory(
                            repository!!,
                            app.container.userRepository,
                            app.container.googleAuthHelper,
                            app.container.auth,
                            onDataOwnerChanged = { hostUid ->
                                app.container.reinitializeWithDataOwner(hostUid)
                            }
                        )
                    )

                    val cleaningViewModel: CleaningViewModel = viewModel(
                        key = "cleaning_vm_${cleaningRepository.hashCode()}",
                        factory = CleaningViewModel.factory(cleaningRepository!!)
                    )

                    val diaperViewModel: DiaperViewModel = viewModel(
                        key = "diaper_vm_${diaperRepository.hashCode()}",
                        factory = DiaperViewModel.factory(diaperRepository!!)
                    )

                    val sleepViewModel: SleepViewModel = viewModel(
                        key = "sleep_vm_${sleepRepository.hashCode()}",
                        factory = SleepViewModel.factory(sleepRepository!!)
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
                                // uid가 변경되었을 수 있으므로 (앱 재설치 후 Google 로그인)
                                // dataOwnerUid를 다시 확인하고 Repository 재초기화
                                val dataOwnerUid = app.container.userRepository.getDataOwnerUid(uid)
                                app.container.reinitializeWithDataOwner(dataOwnerUid)
                                viewModel.refreshLoginState()
                            }.onFailure { e ->
                                android.util.Log.e("GoogleSignIn", "로그인 실패: ${e.message}", e)
                                android.widget.Toast.makeText(
                                    this@MainActivity,
                                    "Google 로그인 실패: ${e.message}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                    BabyFeedingNavHost(
                        feedingViewModel = viewModel,
                        cleaningViewModel = cleaningViewModel,
                        diaperViewModel = diaperViewModel,
                        sleepViewModel = sleepViewModel,
                        googleAuthHelper = app.container.googleAuthHelper,
                        googleSignInLauncher = googleSignInLauncherWithRefresh
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (initError != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = initError ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = {
                                    // Retry by recreating container
                                    app.container = AppContainer(app)
                                }) {
                                    Text("다시 시도")
                                }
                            }
                        } else {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
