package com.baby.feedingtracker.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baby.feedingtracker.data.BabyProfile
import com.baby.feedingtracker.ui.theme.LocalExtendedColors

@Composable
fun BabyProfileBanner(
    profile: BabyProfile?,
    daysOld: Int?,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current

    if (profile != null && profile.name.isNotBlank() && daysOld != null) {
        Text(
            text = "${profile.name} · 생후 ${daysOld}일",
            style = MaterialTheme.typography.bodySmall,
            color = extendedColors.subtleText,
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToProfile)
                .padding(top = 12.dp)
        )
    } else {
        Text(
            text = "프로필을 설정하세요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToProfile)
                .padding(top = 12.dp)
        )
    }
}
