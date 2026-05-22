package com.baby.feedingtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baby.feedingtracker.di.AppContainer
import com.baby.feedingtracker.notification.NotificationChannels
import com.baby.feedingtracker.ui.diaper.DiaperViewModel
import com.baby.feedingtracker.ui.feeding.FeedingViewModel
import com.baby.feedingtracker.ui.profile.BabyProfileViewModel
import com.baby.feedingtracker.ui.settings.ReminderSettingsViewModel
import com.baby.feedingtracker.ui.sleep.SleepViewModel
import com.baby.feedingtracker.ui.growth.GrowthViewModel
import com.baby.feedingtracker.ui.statistics.StatisticsViewModel
import com.baby.feedingtracker.ui.navigation.BabyFeedingNavHost
import com.baby.feedingtracker.ui.theme.BabyFeedingTrackerTheme
import com.baby.feedingtracker.ui.theme.ThemeMode
import com.baby.feedingtracker.ui.widget.WidgetDataSource
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BabyFeedingApp

        // 알림 채널 보장 (Android 8+)
        NotificationChannels.ensureCreated(this)

        setContent {
            val themeMode by app.container.themePreference.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)

            BabyFeedingTrackerTheme(themeMode = themeMode) {
                val repository by app.container.repository.collectAsState()
                val diaperRepository by app.container.diaperRepository.collectAsState()
                val sleepRepository by app.container.sleepRepository.collectAsState()
                val babyProfileRepository by app.container.babyProfileRepository.collectAsState()
                val statisticsRepository by app.container.statisticsRepository.collectAsState()
                val growthRepository by app.container.growthRepository.collectAsState()
                val milestoneManager by app.container.milestoneManager.collectAsState()
                val initError by app.container.initError.collectAsState()
                val coroutineScope = rememberCoroutineScope()

                if (repository != null && diaperRepository != null && sleepRepository != null && babyProfileRepository != null && statisticsRepository != null && growthRepository != null && milestoneManager != null) {
                    // repository 인스턴스가 바뀌면 (Google 로그인 후 uid 변경 등)
                    // ViewModel을 새로 생성하여 새 데이터를 로드
                    val viewModel: FeedingViewModel = viewModel(
                        key = "main_vm_${repository.hashCode()}",
                        factory = FeedingViewModel.factory(
                            repository!!,
                            app.container.userRepository,
                            app.container.googleAuthHelper,
                            app.container.auth,
                            appContext = applicationContext,
                            onDataOwnerChanged = { hostUid ->
                                app.container.reinitializeWithDataOwner(hostUid)
                            }
                        )
                    )

                    val diaperViewModel: DiaperViewModel = viewModel(
                        key = "diaper_vm_${diaperRepository.hashCode()}",
                        factory = DiaperViewModel.factory(diaperRepository!!)
                    )

                    val sleepViewModel: SleepViewModel = viewModel(
                        key = "sleep_vm_${sleepRepository.hashCode()}",
                        factory = SleepViewModel.factory(sleepRepository!!)
                    )

                    val babyProfileViewModel: BabyProfileViewModel = viewModel(
                        key = "baby_profile_vm_${babyProfileRepository.hashCode()}",
                        factory = BabyProfileViewModel.factory(babyProfileRepository!!)
                    )

                    val statisticsViewModel: StatisticsViewModel = viewModel(
                        key = "statistics_vm_${statisticsRepository.hashCode()}",
                        factory = StatisticsViewModel.factory(
                            statisticsRepository!!,
                            milestoneManager!!,
                            babyProfileRepository!!
                        )
                    )

                    val growthViewModel: GrowthViewModel = viewModel(
                        key = "growth_vm_${growthRepository.hashCode()}",
                        factory = GrowthViewModel.factory(growthRepository!!)
                    )

                    val reminderSettingsViewModel: ReminderSettingsViewModel = viewModel(
                        key = "reminder_settings_vm",
                        factory = ReminderSettingsViewModel.factory(
                            reminderPreference = app.container.reminderPreference,
                            scheduler = app.container.reminderScheduler,
                        )
                    )

                    // 로그인 uid를 위젯용 DataStore에 캐시 (위젯이 독립 fetch 가능하도록)
                    LaunchedEffect(repository) {
                        app.container.auth.currentUser?.uid?.let { uid ->
                            WidgetDataSource.cacheUid(applicationContext, uid)
                        }
                    }

                    // Android 13+ POST_NOTIFICATIONS 권한 (앱 첫 진입 시 한 번 요청, 거부해도 앱 동작)
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { /* 결과 무시 — 사용자가 거부해도 앱은 정상 동작, 알림만 안 옴 */ }
                    LaunchedEffect(Unit) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

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
                        diaperViewModel = diaperViewModel,
                        sleepViewModel = sleepViewModel,
                        statisticsViewModel = statisticsViewModel,
                        growthViewModel = growthViewModel,
                        babyProfileViewModel = babyProfileViewModel,
                        reminderSettingsViewModel = reminderSettingsViewModel,
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
