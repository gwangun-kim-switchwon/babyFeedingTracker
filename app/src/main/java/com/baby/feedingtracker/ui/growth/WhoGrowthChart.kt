package com.baby.feedingtracker.ui.growth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.baby.feedingtracker.data.GrowthRecord
import com.baby.feedingtracker.data.who.WhoGrowthStandards
import java.util.concurrent.TimeUnit

// ── 차트 타입 ────────────────────────────────────────────────
enum class GrowthChartType(val label: String) {
    WEIGHT("몸무게"),
    HEIGHT("키"),
    HEAD("머리둘레"),
}

// ── WHO 성장 곡선 차트 ────────────────────────────────────────
/**
 * WHO P3/P50/P97 기준선과 실측 데이터를 Compose Canvas로 그리는 차트.
 *
 * @param records       성장 기록 목록 (최신순 정렬 무관, 내부에서 정렬)
 * @param birthDateMs   아기 생일 epoch millis
 * @param gender        "male" / "female" / null (null이면 남아 기준)
 * @param chartType     [GrowthChartType] WEIGHT / HEIGHT / HEAD
 */
@Composable
fun WhoGrowthChart(
    records: List<GrowthRecord>,
    birthDateMs: Long,
    gender: String?,
    chartType: GrowthChartType,
    modifier: Modifier = Modifier,
) {
    val isMale = gender != "female"

    // 차트 타입별 WHO 데이터 선택
    val whoData: Map<Int, WhoGrowthStandards.Percentiles> = remember(chartType, isMale) {
        when (chartType) {
            GrowthChartType.WEIGHT -> if (isMale) WhoGrowthStandards.weightBoyKg else WhoGrowthStandards.weightGirlKg
            GrowthChartType.HEIGHT -> if (isMale) WhoGrowthStandards.heightBoyCm else WhoGrowthStandards.heightGirlCm
            GrowthChartType.HEAD   -> if (isMale) WhoGrowthStandards.headBoyCm   else WhoGrowthStandards.headGirlCm
        }
    }

    // 실측 데이터 포인트 추출 (오래된 순)
    val dataPoints: List<Pair<Int, Float>> = remember(records, birthDateMs, chartType) {
        records
            .mapNotNull { r ->
                val value = when (chartType) {
                    GrowthChartType.WEIGHT -> r.weightKg
                    GrowthChartType.HEIGHT -> r.heightCm
                    GrowthChartType.HEAD   -> r.headCm
                }
                if (value == null) null
                else {
                    val ageMonths = TimeUnit.MILLISECONDS.toDays(r.timestamp - birthDateMs)
                        .coerceAtLeast(0L)
                        .toInt() / 30
                    Pair(ageMonths, value)
                }
            }
            .sortedBy { it.first }
            .filter { it.first in 0..24 }
    }

    // X축 범위: 0~24개월, Y축 범위: WHO 데이터에서 동적 결정
    val xMin = 0
    val xMax = 24

    val whoValues = (0..24).mapNotNull { WhoGrowthStandards.interpolate(whoData, it) }
    val dataValues = dataPoints.map { it.second }
    val allValues = whoValues.flatMap { listOf(it.p3, it.p97) } + dataValues

    val yMin = (allValues.minOrNull() ?: 0f) * 0.97f
    val yMax = (allValues.maxOrNull() ?: 1f) * 1.03f

    // 색상
    val growthGreen = Color(0xFFA8D4A0)
    val growthGreenDark = Color(0xFF5E9E56)
    val p50Color = Color(0xFF4A8F44)
    val bandFill = growthGreen.copy(alpha = 0.18f)
    val axisColor = Color(0xFFCCCCCC)
    val gridColor = Color(0xFFF0F0F0)
    val textColor = Color(0xFF888888)

    val density = LocalDensity.current
    val textSizePx = with(density) { 9.dp.toPx() }
    val labelSizePx = with(density) { 8.dp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.Transparent)
    ) {
        val paddingLeft   = 46f
        val paddingRight  = 16f
        val paddingTop    = 12f
        val paddingBottom = 28f

        val chartW = size.width  - paddingLeft - paddingRight
        val chartH = size.height - paddingTop  - paddingBottom

        fun xToCanvas(month: Float): Float =
            paddingLeft + (month - xMin) / (xMax - xMin).toFloat() * chartW

        fun yToCanvas(value: Float): Float =
            paddingTop + chartH - (value - yMin) / (yMax - yMin) * chartH

        // ── 그리드 라인 ──────────────────────────────
        val yGridCount = 4
        for (i in 0..yGridCount) {
            val yVal = yMin + (yMax - yMin) * i / yGridCount
            val cy = yToCanvas(yVal)
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, cy),
                end   = Offset(paddingLeft + chartW, cy),
                strokeWidth = 1f,
            )
        }

        // X축 그리드 (월령 6개월 단위)
        listOf(0, 6, 12, 18, 24).forEach { month ->
            val cx = xToCanvas(month.toFloat())
            drawLine(
                color = gridColor,
                start = Offset(cx, paddingTop),
                end   = Offset(cx, paddingTop + chartH),
                strokeWidth = 1f,
            )
        }

        // ── 축 라인 ──────────────────────────────────
        // Y축
        drawLine(
            color       = axisColor,
            start       = Offset(paddingLeft, paddingTop),
            end         = Offset(paddingLeft, paddingTop + chartH),
            strokeWidth = 1.5f,
        )
        // X축
        drawLine(
            color       = axisColor,
            start       = Offset(paddingLeft, paddingTop + chartH),
            end         = Offset(paddingLeft + chartW, paddingTop + chartH),
            strokeWidth = 1.5f,
        )

        // ── WHO P3~P97 밴드 (음영) ────────────────────
        val bandPath = Path()
        var firstP3 = true
        for (month in xMin..xMax) {
            val pct = WhoGrowthStandards.interpolate(whoData, month) ?: continue
            val cx = xToCanvas(month.toFloat())
            val cy3 = yToCanvas(pct.p3)
            if (firstP3) { bandPath.moveTo(cx, cy3); firstP3 = false }
            else bandPath.lineTo(cx, cy3)
        }
        // P97 경계를 역방향으로 연결
        for (month in xMax downTo xMin) {
            val pct = WhoGrowthStandards.interpolate(whoData, month) ?: continue
            val cx = xToCanvas(month.toFloat())
            val cy97 = yToCanvas(pct.p97)
            bandPath.lineTo(cx, cy97)
        }
        bandPath.close()
        drawPath(path = bandPath, color = bandFill)

        // ── WHO P3 라인 (점선) ────────────────────────
        drawWhoLine(
            scope      = this,
            whoData    = whoData,
            xMin       = xMin,
            xMax       = xMax,
            selector   = { it.p3 },
            color      = growthGreen,
            strokeWidth = 1.5f,
            dashed     = true,
            xToCanvas  = ::xToCanvas,
            yToCanvas  = ::yToCanvas,
        )

        // ── WHO P97 라인 (점선) ───────────────────────
        drawWhoLine(
            scope      = this,
            whoData    = whoData,
            xMin       = xMin,
            xMax       = xMax,
            selector   = { it.p97 },
            color      = growthGreen,
            strokeWidth = 1.5f,
            dashed     = true,
            xToCanvas  = ::xToCanvas,
            yToCanvas  = ::yToCanvas,
        )

        // ── WHO P50 라인 (실선) ───────────────────────
        drawWhoLine(
            scope      = this,
            whoData    = whoData,
            xMin       = xMin,
            xMax       = xMax,
            selector   = { it.p50 },
            color      = p50Color,
            strokeWidth = 2f,
            dashed     = false,
            xToCanvas  = ::xToCanvas,
            yToCanvas  = ::yToCanvas,
        )

        // ── 실측 데이터 연결선 ────────────────────────
        if (dataPoints.size >= 2) {
            val linePath = Path()
            dataPoints.forEachIndexed { i, (month, value) ->
                val cx = xToCanvas(month.toFloat())
                val cy = yToCanvas(value)
                if (i == 0) linePath.moveTo(cx, cy) else linePath.lineTo(cx, cy)
            }
            drawPath(
                path   = linePath,
                color  = growthGreenDark,
                style  = Stroke(width = 2.5f, cap = StrokeCap.Round),
            )
        }

        // ── 실측 데이터 포인트 (원) ───────────────────
        dataPoints.forEach { (month, value) ->
            val cx = xToCanvas(month.toFloat())
            val cy = yToCanvas(value)
            // 외곽 흰 링
            drawCircle(color = Color.White, radius = 5.5f, center = Offset(cx, cy))
            // 내부 색 원
            drawCircle(color = growthGreenDark, radius = 4f, center = Offset(cx, cy))
        }

        // ── Y축 레이블 ────────────────────────────────
        val yGridCountLabel = 4
        for (i in 0..yGridCountLabel) {
            val yVal = yMin + (yMax - yMin) * i / yGridCountLabel
            val cy = yToCanvas(yVal)
            val labelText = when (chartType) {
                GrowthChartType.WEIGHT -> "%.1f".format(yVal)
                GrowthChartType.HEIGHT -> "%.0f".format(yVal)
                GrowthChartType.HEAD   -> "%.0f".format(yVal)
            }
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                paddingLeft - 6f,
                cy + textSizePx / 2.5f,
                android.graphics.Paint().apply {
                    color = textColor.toArgb()
                    textSize = textSizePx
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
            )
        }

        // ── X축 레이블 (월령) ─────────────────────────
        listOf(0, 6, 12, 18, 24).forEach { month ->
            val cx = xToCanvas(month.toFloat())
            val labelText = if (month == 0) "0" else "${month}개월"
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                cx,
                paddingTop + chartH + labelSizePx + 12f,
                android.graphics.Paint().apply {
                    color = textColor.toArgb()
                    textSize = labelSizePx
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }

        // ── P3/P50/P97 레이블 (우측 끝) ──────────────
        listOf(
            Triple({ p: WhoGrowthStandards.Percentiles -> p.p3 },  "P3",  growthGreen),
            Triple({ p: WhoGrowthStandards.Percentiles -> p.p50 }, "P50", p50Color),
            Triple({ p: WhoGrowthStandards.Percentiles -> p.p97 }, "P97", growthGreen),
        ).forEach { (selector, label, color) ->
            val lastPct = WhoGrowthStandards.interpolate(whoData, xMax) ?: return@forEach
            val cy = yToCanvas(selector(lastPct))
            drawContext.canvas.nativeCanvas.drawText(
                label,
                paddingLeft + chartW + paddingRight - 2f,
                cy + labelSizePx / 2.5f,
                android.graphics.Paint().apply {
                    this.color = color.toArgb()
                    textSize = labelSizePx
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
            )
        }
    }
}

// ── WHO 라인 그리기 헬퍼 ─────────────────────────────────────
private fun drawWhoLine(
    scope: DrawScope,
    whoData: Map<Int, WhoGrowthStandards.Percentiles>,
    xMin: Int,
    xMax: Int,
    selector: (WhoGrowthStandards.Percentiles) -> Float,
    color: Color,
    strokeWidth: Float,
    dashed: Boolean,
    xToCanvas: (Float) -> Float,
    yToCanvas: (Float) -> Float,
) {
    val path = Path()
    var isFirst = true
    for (month in xMin..xMax) {
        val pct = WhoGrowthStandards.interpolate(whoData, month) ?: continue
        val cx = xToCanvas(month.toFloat())
        val cy = yToCanvas(selector(pct))
        if (isFirst) { path.moveTo(cx, cy); isFirst = false }
        else path.lineTo(cx, cy)
    }
    val pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f) else null
    scope.drawPath(
        path   = path,
        color  = color,
        style  = Stroke(
            width      = strokeWidth,
            cap        = StrokeCap.Round,
            pathEffect = pathEffect,
        ),
    )
}
