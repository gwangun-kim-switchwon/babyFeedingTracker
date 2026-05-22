package com.baby.feedingtracker.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.baby.feedingtracker.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 맘마미아 반응형 Glance 위젯.
 *
 * 사이즈 모드:
 *  - SMALL  (≈ 2x1)
 *  - MEDIUM (≈ 3x1)
 *  - LARGE  (≈ 4x2)
 *
 * 데이터는 WidgetDataSource.getCached() 에서 읽음 (즉시 표시).
 * 캐시 갱신은 WidgetUpdateWorker 가 담당.
 */
class MammamiaWidget : GlanceAppWidget() {

    companion object {
        val SMALL_SIZE = DpSize(110.dp, 40.dp)
        val MEDIUM_SIZE = DpSize(180.dp, 40.dp)
        val LARGE_SIZE = DpSize(250.dp, 100.dp)

        // 코럴 그라데이션 (Brush 미지원 환경 대비 — Glance background는 단색)
        private val BG_COLOR = Color(0xFFFF9D7A)
        private val BG_COLOR_DARK = Color(0xFFFF8A7A)
        private val WHITE = Color(0xFFFFFFFF)
        private val WHITE_70 = Color(0xB3FFFFFF)
        private val WHITE_55 = Color(0x8CFFFFFF)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL_SIZE, MEDIUM_SIZE, LARGE_SIZE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = WidgetDataSource.getCached(context)
        provideContent {
            Content(initial)
        }
    }

    @Composable
    private fun Content(initial: WidgetDataSource.WidgetData?) {
        val context = LocalContext.current
        var data by remember { mutableStateOf(initial ?: WidgetDataSource.WidgetData.EMPTY) }

        // 캐시가 비었거나 갱신이 필요할 수 있는 경우 한 번 더 시도
        LaunchedEffect(Unit) {
            val fresh = WidgetDataSource.getCached(context)
            if (fresh != null) data = fresh
        }

        val size = LocalSize.current
        val openAction = actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        )
        // 위젯 + 버튼 → 즉시 빈 수유 기록 추가 (앱 열지 않음)
        // 사용자가 종류/양 등은 나중에 앱에서 record 탭해 수정
        val addAction = actionRunCallback<AddFeedingCallback>()

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(ColorProvider(BG_COLOR))
                .clickable(openAction)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            when {
                size.width >= LARGE_SIZE.width && size.height >= LARGE_SIZE.height ->
                    LargeContent(data, addAction)
                size.width >= MEDIUM_SIZE.width ->
                    MediumContent(data, addAction)
                else ->
                    SmallContent(data, addAction)
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // SMALL: 경과 시간 + 작은 + 버튼
    // ────────────────────────────────────────────────────────────────────
    @Composable
    private fun SmallContent(
        data: WidgetDataSource.WidgetData,
        addAction: androidx.glance.action.Action
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "마지막 수유",
                    style = labelStyle()
                )
                Text(
                    text = elapsedText(data.lastTimestamp),
                    style = titleStyle(14.sp),
                    maxLines = 1
                )
            }
            Spacer(GlanceModifier.width(6.dp))
            PlusButton(addAction, sizeDp = 28)
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // MEDIUM: 경과 + "오늘 N회" + + 버튼
    // ────────────────────────────────────────────────────────────────────
    @Composable
    private fun MediumContent(
        data: WidgetDataSource.WidgetData,
        addAction: androidx.glance.action.Action
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(text = "마지막 수유", style = labelStyle())
                Text(
                    text = elapsedText(data.lastTimestamp),
                    style = titleStyle(15.sp),
                    maxLines = 1
                )
                Text(
                    text = "오늘 ${data.todayCount}회",
                    style = subStyle(),
                    maxLines = 1
                )
            }
            Spacer(GlanceModifier.width(6.dp))
            PlusButton(addAction, sizeDp = 32)
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // LARGE: 경과 + 오늘 카운트 + 최근 기록 2~3줄
    // ────────────────────────────────────────────────────────────────────
    @Composable
    private fun LargeContent(
        data: WidgetDataSource.WidgetData,
        addAction: androidx.glance.action.Action
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Top row: 요약 + 버튼
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(text = "마지막 수유", style = labelStyle())
                    Text(
                        text = elapsedText(data.lastTimestamp),
                        style = titleStyle(17.sp),
                        maxLines = 1
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "오늘",
                        style = labelStyle()
                    )
                    Text(
                        text = "${data.todayCount}회",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(WHITE)
                        ),
                        maxLines = 1
                    )
                }
                Spacer(GlanceModifier.width(8.dp))
                PlusButton(addAction, sizeDp = 36)
            }

            Spacer(GlanceModifier.height(6.dp))

            // 가장 최근 기록 1줄만 (오너 피드백 — 여러 줄은 산만)
            val latest = data.recentEntries.firstOrNull()
            if (latest == null) {
                Text(
                    text = "최근 기록이 없어요",
                    style = subStyle(),
                    maxLines = 1
                )
            } else {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "·", style = subStyle())
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = formatTime(latest.timestamp),
                        style = subStyle(),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        text = latest.label,
                        style = subStyle(),
                        maxLines = 1
                    )
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // 공통 컴포넌트 / 스타일
    // ────────────────────────────────────────────────────────────────────
    @Composable
    private fun PlusButton(
        action: androidx.glance.action.Action,
        sizeDp: Int
    ) {
        Box(
            modifier = GlanceModifier
                .size(sizeDp.dp)
                .cornerRadius((sizeDp / 2).dp)
                .background(ColorProvider(BG_COLOR_DARK))
                .clickable(action),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                style = TextStyle(
                    fontSize = (sizeDp - 12).sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(WHITE)
                )
            )
        }
    }

    private fun labelStyle() = TextStyle(
        fontSize = 9.sp,
        color = ColorProvider(WHITE_70)
    )

    private fun titleStyle(size: androidx.compose.ui.unit.TextUnit) = TextStyle(
        fontSize = size,
        fontWeight = FontWeight.Bold,
        color = ColorProvider(WHITE)
    )

    private fun subStyle() = TextStyle(
        fontSize = 10.sp,
        color = ColorProvider(WHITE_55)
    )

    private fun elapsedText(lastTs: Long?): String {
        if (lastTs == null || lastTs == 0L) return "기록 없음"
        val elapsed = System.currentTimeMillis() - lastTs
        if (elapsed < 0L) return "방금"
        val hours = elapsed / 3_600_000L
        val minutes = (elapsed % 3_600_000L) / 60_000L
        return if (hours > 0) "${hours}시간 ${minutes}분 전" else "${minutes}분 전"
    }

    private fun formatTime(ts: Long): String {
        if (ts <= 0L) return "--:--"
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        return fmt.format(Date(ts))
    }
}
