package com.baby.feedingtracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.baby.feedingtracker.BabyFeedingApp
import com.baby.feedingtracker.data.FeedingRepository
import com.baby.feedingtracker.data.ReminderPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 알림 액션 수신 BroadcastReceiver.
 * - "LOG_FEEDING": FeedingRepository.addRecord() 호출 후 알림 dismiss + reschedule.
 * - "SNOOZE_30": 30분 후 다시 알림 예약 + 현재 알림 dismiss.
 */
class FeedingReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? BabyFeedingApp ?: return

        // ReminderPreference / Scheduler는 Context 기반 — AppContainer 의존 없이 생성
        val reminderPreference = ReminderPreference(context)
        val scheduler = FeedingReminderScheduler(context, reminderPreference)

        when (intent.action) {
            ACTION_LOG_FEEDING -> {
                val pending = goAsync()
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                scope.launch {
                    try {
                        val repo: FeedingRepository? = withTimeoutOrNull(5_000L) {
                            app.container.repository.filterNotNull().first()
                        }
                        repo?.addRecord()
                        // 추가 후 다음 알림 재예약
                        scheduler.reschedule()
                    } catch (_: Exception) {
                        // 조용히 무시 — UX 우선
                    } finally {
                        dismissNotification(context)
                        pending.finish()
                    }
                }
            }
            ACTION_SNOOZE_30 -> {
                scheduler.scheduleOneTime(30L)
                dismissNotification(context)
            }
        }
    }

    private fun dismissNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(FeedingReminderWorker.NOTIFICATION_ID)
        } catch (_: Exception) {}
    }

    companion object {
        const val ACTION_LOG_FEEDING = "com.baby.feedingtracker.action.LOG_FEEDING"
        const val ACTION_SNOOZE_30 = "com.baby.feedingtracker.action.SNOOZE_30"
    }
}
