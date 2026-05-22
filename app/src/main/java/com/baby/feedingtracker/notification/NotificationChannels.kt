package com.baby.feedingtracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val FEEDING_REMINDER_CHANNEL_ID = "feeding_reminder"
    private const val FEEDING_REMINDER_CHANNEL_NAME = "수유 리마인더"
    private const val FEEDING_REMINDER_CHANNEL_DESC = "마지막 수유 후 설정 시간이 지나면 알려드려요"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (manager.getNotificationChannel(FEEDING_REMINDER_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    FEEDING_REMINDER_CHANNEL_ID,
                    FEEDING_REMINDER_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = FEEDING_REMINDER_CHANNEL_DESC
                    setShowBadge(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
