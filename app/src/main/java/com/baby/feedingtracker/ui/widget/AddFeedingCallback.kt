package com.baby.feedingtracker.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/**
 * 위젯 + 버튼 액션: 즉시 빈 수유 기록을 Firestore에 추가.
 *
 * - 종류·양·시간 선택 단계 없이 timestamp만 now로 기록 (메모리 "원탭 즉시 기록" 원칙).
 * - 사용자가 종류 등을 수정하고 싶다면 앱을 열어 record 탭하여 BottomSheet에서 수정.
 * - 추가 직후 위젯 캐시 갱신 + 위젯 UI 재렌더.
 */
class AddFeedingCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val added = WidgetDataSource.addBlankFeedingRecord(context)
        if (added) {
            // 캐시 즉시 갱신 → 위젯 UI 업데이트
            WidgetDataSource.fetchAndCache(context)
            MammamiaWidget().update(context, glanceId)
        }
    }
}
