package com.baby.feedingtracker.ui.navigation

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.baby.feedingtracker.data.GoogleAuthHelper
import com.baby.feedingtracker.ui.cleaning.CleaningScreen
import com.baby.feedingtracker.ui.cleaning.CleaningViewModel
import com.baby.feedingtracker.ui.diaper.DiaperScreen
import com.baby.feedingtracker.ui.diaper.DiaperViewModel
import com.baby.feedingtracker.ui.feeding.FeedingScreen
import com.baby.feedingtracker.ui.feeding.FeedingViewModel
import com.baby.feedingtracker.ui.profile.BabyProfileScreen
import com.baby.feedingtracker.ui.profile.BabyProfileViewModel
import com.baby.feedingtracker.ui.sleep.SleepScreen
import com.baby.feedingtracker.ui.sleep.SleepViewModel
import com.baby.feedingtracker.ui.statistics.StatisticsScreen
import com.baby.feedingtracker.ui.statistics.StatisticsViewModel
import com.baby.feedingtracker.ui.theme.LocalExtendedColors

@Composable
fun BabyFeedingNavHost(
    feedingViewModel: FeedingViewModel,
    cleaningViewModel: CleaningViewModel,
    diaperViewModel: DiaperViewModel,
    sleepViewModel: SleepViewModel,
    statisticsViewModel: StatisticsViewModel,
    babyProfileViewModel: BabyProfileViewModel,
    googleAuthHelper: GoogleAuthHelper,
    googleSignInLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            // 프로필 화면에서는 BottomNavBar 숨김
            if (currentRoute != "baby_profile") {
                BottomNavBar(navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Feeding.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomNavItem.Feeding.route) {
                FeedingScreen(
                    viewModel = feedingViewModel,
                    babyProfileViewModel = babyProfileViewModel,
                    googleAuthHelper = googleAuthHelper,
                    googleSignInLauncher = googleSignInLauncher,
                    onNavigateToProfile = {
                        navController.navigate("baby_profile")
                    }
                )
            }
            composable(BottomNavItem.Diaper.route) {
                DiaperScreen(
                    viewModel = diaperViewModel,
                    babyProfileViewModel = babyProfileViewModel,
                    onNavigateToProfile = {
                        navController.navigate("baby_profile")
                    }
                )
            }
            composable(BottomNavItem.Cleaning.route) {
                CleaningScreen(
                    viewModel = cleaningViewModel,
                    babyProfileViewModel = babyProfileViewModel,
                    onNavigateToProfile = {
                        navController.navigate("baby_profile")
                    }
                )
            }
            composable(BottomNavItem.Sleep.route) {
                SleepScreen(
                    viewModel = sleepViewModel,
                    babyProfileViewModel = babyProfileViewModel,
                    onNavigateToProfile = {
                        navController.navigate("baby_profile")
                    }
                )
            }
            composable(BottomNavItem.Statistics.route) {
                StatisticsScreen(
                    viewModel = statisticsViewModel,
                    babyProfileViewModel = babyProfileViewModel,
                    onNavigateToProfile = {
                        navController.navigate("baby_profile")
                    }
                )
            }
            composable("baby_profile") {
                BabyProfileScreen(
                    viewModel = babyProfileViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController) {
    val items = listOf(BottomNavItem.Feeding, BottomNavItem.Sleep, BottomNavItem.Diaper, BottomNavItem.Cleaning, BottomNavItem.Statistics)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = LocalExtendedColors.current.subtleText,
                    unselectedTextColor = LocalExtendedColors.current.subtleText,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
