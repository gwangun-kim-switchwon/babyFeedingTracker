package com.baby.feedingtracker.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.baby.feedingtracker.data.ReminderPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 수유 리마인더 OneTimeWorkRequest 스케줄러.
 * - schedule(): 현재 설정된 intervalHours만큼 지연 후 Worker 실행.
 * - cancel(): UniqueWork 취소.
 * - reschedule(): 새 수유 기록 추가 시 호출 — cancel 후 다시 schedule.
 * - scheduleOneTime(delayMinutes): "30분 후 다시" 등 명시적 delay 예약.
 *
 * Worker 자체가 다시 실행을 예약하지는 않는다(트랙 외 정책 결정 회피).
 * 대신 새 수유 기록 추가 시 ViewModel/Repository에서 reschedule()을 호출하면 충분.
 */
class FeedingReminderScheduler(
    private val context: Context,
    private val reminderPreference: ReminderPreference,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun schedule() {
        scope.launch {
            val settings = reminderPreference.current()
            if (!settings.reminderEnabled) {
                cancel()
                return@launch
            }
            scheduleOneTimeMinutes(settings.intervalHours * 60L)
        }
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    fun reschedule() {
        scope.launch {
            val settings = reminderPreference.current()
            if (!settings.reminderEnabled) {
                cancel()
                return@launch
            }
            scheduleOneTimeMinutes(settings.intervalHours * 60L)
        }
    }

    /** 명시적 분 단위 delay로 예약 (e.g. snooze 30분). */
    fun scheduleOneTime(delayMinutes: Long) {
        scheduleOneTimeMinutes(delayMinutes)
    }

    private fun scheduleOneTimeMinutes(delayMinutes: Long) {
        val request = OneTimeWorkRequestBuilder<FeedingReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "feeding_reminder"
    }
}
