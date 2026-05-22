package com.baby.feedingtracker.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Mammamia 반응형 위젯의 AppWidget Receiver.
 *
 * - onEnabled: WorkManager 주기 업데이트(20분) 등록 + 즉시 1회 fetch
 * - onUpdate: 즉시 1회 fetch
 * - onDisabled: 모든 작업 cancel
 *
 * 기존 FeedingWidgetReceiver와 별도 entry — 둘은 공존 가능. CEO가 추후 폐기.
 */
class MammamiaWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget = MammamiaWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateWorker.schedulePeriodic(context)
        WidgetUpdateWorker.scheduleOneTime(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdateWorker.scheduleOneTime(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdateWorker.cancelAll(context)
    }
}
