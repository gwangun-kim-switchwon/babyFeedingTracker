package com.baby.feedingtracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BabyChangingStation
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Feeding : BottomNavItem("feeding", "수유", Icons.Outlined.LocalDrink)
    object Diaper : BottomNavItem("diaper", "기저귀", Icons.Outlined.BabyChangingStation)
    object Cleaning : BottomNavItem("cleaning", "세척", Icons.Outlined.CleaningServices)
    object Sleep : BottomNavItem("sleep", "수면", Icons.Outlined.Bedtime)
    object Statistics : BottomNavItem("statistics", "통계", Icons.Outlined.BarChart)
}
