package com.baby.feedingtracker.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bathtub
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.baby.feedingtracker.R

/**
 * 맘마미아 아이콘 단일 진입점 (DIP 패턴).
 *
 * ## 사용 규칙
 * - Material Symbols Rounded 기반 아이콘은 [MammamiaIcons.Material] 하위로 접근
 *   - 예: `Icon(imageVector = MammamiaIcons.Material.Sleep, contentDescription = "수면")`
 * - 맘마미아 커스텀 브랜드 아이콘 7종은 [MammamiaIcons.Custom] 하위 @Composable 함수로 접근
 *   - 예: `Icon(painter = MammamiaIcons.Custom.FeedingBottle(), contentDescription = "분유")`
 * - 기저귀 아이콘은 [DiaperType]으로 상태 분기:
 *   - 예: `Icon(painter = MammamiaIcons.Custom.Diaper(DiaperType.Pee), contentDescription = "소변")`
 *
 * ## 디자인 원칙
 * - 모든 커스텀 아이콘은 24dp × 24dp, 2dp stroke, round cap/join
 * - Material Symbols Rounded와 혼재 시 이질감 최소화
 * - tint는 `?attr/colorControlNormal`로 지정되어 테마 컬러 자동 적용
 *
 * ## contentDescription 정책
 * - 모든 호출부에서 **한글** `contentDescription` 필수 (접근성 요구사항)
 *
 * @see com.baby.feedingtracker.ui.theme.MammaMiaTheme
 */
object MammamiaIcons {

    /** Material Symbols Rounded 래퍼 (20종). */
    object Material {
        // 수면
        val Sleep: ImageVector = Icons.Rounded.Bedtime
        val Wake: ImageVector = Icons.Rounded.WbSunny

        // 케어
        val Bath: ImageVector = Icons.Rounded.Bathtub
        val Vaccine: ImageVector = Icons.Rounded.Vaccines
        val Medication: ImageVector = Icons.Rounded.Medication
        val Temperature: ImageVector = Icons.Rounded.Thermostat

        // 성장
        val Weight: ImageVector = Icons.Rounded.MonitorWeight
        val Height: ImageVector = Icons.Rounded.Height

        // 구조
        val Profile: ImageVector = Icons.Rounded.Person
        val Family: ImageVector = Icons.Rounded.FamilyRestroom
        val Notifications: ImageVector = Icons.Rounded.Notifications
        val Calendar: ImageVector = Icons.Rounded.CalendarMonth
        val Chart: ImageVector = Icons.Rounded.BarChart
        val Timeline: ImageVector = Icons.Rounded.Timeline

        // 액션
        val Add: ImageVector = Icons.Rounded.Add
        val Edit: ImageVector = Icons.Rounded.Edit
        val Delete: ImageVector = Icons.Rounded.Delete
        val Close: ImageVector = Icons.Rounded.Close
        val Back: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack
        val Settings: ImageVector = Icons.Rounded.Settings
    }

    /** 맘마미아 커스텀 브랜드 아이콘 (7종). */
    object Custom {
        /** 모유 수유 (왼쪽). */
        @Composable
        fun FeedingBreastLeft(): Painter =
            painterResource(R.drawable.mammamia_ic_feeding_breast_left)

        /** 모유 수유 (오른쪽). */
        @Composable
        fun FeedingBreastRight(): Painter =
            painterResource(R.drawable.mammamia_ic_feeding_breast_right)

        /** 분유 / 유축 젖병. */
        @Composable
        fun FeedingBottle(): Painter =
            painterResource(R.drawable.mammamia_ic_feeding_bottle)

        /**
         * 기저귀 상태별 아이콘.
         * @param type [DiaperType.Pee] (소변) / [DiaperType.Poo] (대변) / [DiaperType.Mixed] (혼합)
         */
        @Composable
        fun Diaper(type: DiaperType = DiaperType.Pee): Painter = painterResource(
            when (type) {
                DiaperType.Pee -> R.drawable.mammamia_ic_diaper_pee
                DiaperType.Poo -> R.drawable.mammamia_ic_diaper_poo
                DiaperType.Mixed -> R.drawable.mammamia_ic_diaper_mixed
            }
        )

        /** 트림 (공기방울 메타포). */
        @Composable
        fun Burp(): Painter = painterResource(R.drawable.mammamia_ic_burp)

        /** 마일스톤 뱃지 (5각 별). */
        @Composable
        fun Milestone(): Painter = painterResource(R.drawable.mammamia_ic_milestone)

        /** 유축 (모터 박스 + 젖병 + 아래 화살표). */
        @Composable
        fun Pump(): Painter = painterResource(R.drawable.mammamia_ic_pump)
    }

    /**
     * 기저귀 타입.
     *
     * 현재 앱 [com.baby.feedingtracker.data.DiaperRecord.type] 문자열과의 매핑:
     * - `"urine"` → [Pee]
     * - `"stool"` → [Poo]
     * - `"mixed"` → [Mixed] (소변 + 대변 동시)
     * - `"diaper"` (전체 교체만 기록) → 호출부에서 기본 [Pee]로 폴백하거나
     *   별도 처리. (v1.3 스펙은 Pee/Poo/Mixed 3종 정의)
     */
    enum class DiaperType { Pee, Poo, Mixed }
}
