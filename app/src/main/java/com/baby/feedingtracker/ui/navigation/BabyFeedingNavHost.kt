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
import com.baby.feedingtracker.ui.feeding.FeedingScreen
import com.baby.feedingtracker.ui.feeding.FeedingViewModel
import com.baby.feedingtracker.ui.theme.LocalExtendedColors

@Composable
fun BabyFeedingNavHost(
    feedingViewModel: FeedingViewModel,
    cleaningViewModel: CleaningViewModel,
    googleAuthHelper: GoogleAuthHelper,
    googleSignInLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Feeding.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomNavItem.Feeding.route) {
                FeedingScreen(
                    viewModel = feedingViewModel,
                    googleAuthHelper = googleAuthHelper,
                    googleSignInLauncher = googleSignInLauncher
                )
            }
            composable(BottomNavItem.Cleaning.route) {
                CleaningScreen(viewModel = cleaningViewModel)
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController) {
    val items = listOf(BottomNavItem.Feeding, BottomNavItem.Cleaning)
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
