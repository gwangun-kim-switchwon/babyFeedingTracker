package com.baby.feedingtracker.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.baby.feedingtracker.MainActivity

class FeedingWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent() }
    }

    @Composable
    private fun WidgetContent() {
        val prefs    = currentState<Preferences>()
        val lastTs   = prefs[WidgetPreferenceKeys.LAST_FEEDING_TS]   ?: 0L
        val lastType = prefs[WidgetPreferenceKeys.LAST_FEEDING_TYPE] ?: ""

        val elapsedText = if (lastTs == 0L) {
            "아직 기록 없음"
        } else {
            val elapsed = System.currentTimeMillis() - lastTs
            val hours   = elapsed / 3_600_000L
            val minutes = (elapsed % 3_600_000L) / 60_000L
            if (hours > 0) "${hours}시간 ${minutes}분 전" else "${minutes}분 전"
        }

        val displayType = when (lastType) {
            "breast"  -> "모유"
            "formula" -> "분유"
            "pumped"  -> "유축"
            else      -> lastType.takeIf { it.isNotBlank() }
        }
        val typeText = displayType

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFFFF9D7A)))
                .clickable(actionStartActivity<MainActivity>())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🍼", style = TextStyle(fontSize = 20.sp))
                Spacer(GlanceModifier.width(8.dp))
                Column {
                    Text(
                        text  = "마지막 수유",
                        style = TextStyle(
                            fontSize = 9.sp,
                            color    = ColorProvider(Color.White.copy(alpha = 0.8f)),
                        ),
                    )
                    Text(
                        text  = elapsedText,
                        style = TextStyle(
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color      = ColorProvider(Color.White),
                        ),
                    )
                    if (typeText != null) {
                        Text(
                            text  = typeText,
                            style = TextStyle(
                                fontSize = 9.sp,
                                color    = ColorProvider(Color.White.copy(alpha = 0.75f)),
                            ),
                        )
                    }
                }
            }
        }
    }
}
