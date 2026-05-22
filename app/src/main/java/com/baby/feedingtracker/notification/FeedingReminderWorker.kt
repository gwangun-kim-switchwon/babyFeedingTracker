package com.baby.feedingtracker.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.baby.feedingtracker.BabyFeedingApp
import com.baby.feedingtracker.MainActivity
import com.baby.feedingtracker.R
import com.baby.feedingtracker.data.FeedingRecord
import com.baby.feedingtracker.data.FeedingRepository
import com.baby.feedingtracker.data.ReminderPreference
import com.baby.feedingtracker.data.ReminderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

/**
 * 마지막 수유 후 설정된 간격이 경과했는지 확인하여 알림을 표시하는 Worker.
 * - DND(야간 방해금지) 시간대면 알림을 표시하지 않고 성공 종료.
 * - 경과 시간이 부족하면 알림 없이 종료(스케줄러가 다시 잡아준 시점에 재평가).
 */
class FeedingReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val app = context.applicationContext as? BabyFeedingApp ?: return Result.success()

        // ReminderPreference는 Context 기반 DataStore라 직접 생성 가능 (AppContainer 의존 없음)
        val reminderPreference = ReminderPreference(context)
        val settings: ReminderSettings = try {
            reminderPreference.current()
        } catch (_: Exception) {
            return Result.success()
        }

        if (!settings.reminderEnabled) {
            return Result.success()
        }

        if (isInDndWindow(settings)) {
            return Result.success()
        }

        // FeedingRepository는 AppContainer가 인증 완료 후 노출 (StateFlow).
        // CEO 통합 단계에서 AppContainer에 `repository: StateFlow<FeedingRepository?>`가 이미 존재함을 사용.
        val repositoryFlow: Flow<FeedingRepository?> = app.container.repository
        val repository: FeedingRepository = withTimeoutOrNull(5_000L) {
            repositoryFlow.filterNotNull().first()
        } ?: return Result.retry()

        val latest: FeedingRecord? = try {
            withTimeoutOrNull(5_000L) {
                repository.recentRecords.mapNotNull { it.firstOrNull() }.first()
            }
        } catch (_: Exception) {
            null
        }

        val now = System.currentTimeMillis()
        val intervalMs = settings.intervalHours * 60L * 60L * 1000L
        val elapsed = if (latest != null) now - latest.timestamp else Long.MAX_VALUE
        if (elapsed < intervalMs) {
            return Result.success()
        }

        showNotification(context, hours = settings.intervalHours, elapsedMs = elapsed)
        return Result.success()
    }

    private fun isInDndWindow(settings: ReminderSettings): Boolean {
        if (!settings.dndEnabled) return false
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val start = settings.dndStartHour
        val end = settings.dndEndHour
        return if (start == end) {
            false
        } else if (start < end) {
            // 같은 날 범위 (e.g. 13~17)
            hour in start until end
        } else {
            // 자정 가로지름 (e.g. 23~6)
            hour >= start || hour < end
        }
    }

    private fun showNotification(context: Context, hours: Int, elapsedMs: Long) {
        NotificationChannels.ensureCreated(context)

        val elapsedHours = (elapsedMs / (60L * 60L * 1000L)).coerceAtLeast(hours.toLong())

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val logIntent = Intent(context, FeedingReminderActionReceiver::class.java).apply {
            action = FeedingReminderActionReceiver.ACTION_LOG_FEEDING
        }
        val logPending = PendingIntent.getBroadcast(
            context,
            1,
            logIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, FeedingReminderActionReceiver::class.java).apply {
            action = FeedingReminderActionReceiver.ACTION_SNOOZE_30
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NotificationChannels.FEEDING_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("수유 시간이에요")
            .setContentText("마지막 수유 후 ${elapsedHours}시간 지났어요. 아기 기록 어떠세요?")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("마지막 수유 후 ${elapsedHours}시간 지났어요. 아기 기록 어떠세요?")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, "방금 수유", logPending)
            .addAction(0, "30분 후 다시", snoozePending)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS 권한 거부 시 조용히 무시
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
