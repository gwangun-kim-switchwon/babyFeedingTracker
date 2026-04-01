package com.baby.feedingtracker.ui.cleaning

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.baby.feedingtracker.ui.theme.LocalExtendedColors

@Composable
fun CleaningScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "준비 중",
            style = MaterialTheme.typography.headlineSmall,
            color = LocalExtendedColors.current.subtleText
        )
    }
}
