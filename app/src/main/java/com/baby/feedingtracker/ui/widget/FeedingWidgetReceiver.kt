package com.baby.feedingtracker.ui.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class FeedingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = FeedingWidget()
}
