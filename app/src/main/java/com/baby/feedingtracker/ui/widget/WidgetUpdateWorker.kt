package com.baby.feedingtracker.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Glance 위젯의 데이터를 주기적/즉시 갱신하는 WorkManager Worker.
 *
 * Firebase는 ContentProvider 자동 초기화로 Worker context에서도 사용 가능.
 * (FirebaseApp.initializeApp 명시 호출 불필요)
 */
class WidgetUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            WidgetDataSource.fetchAndCache(applicationContext)
            MammamiaWidget().updateAll(applicationContext)
            Result.success()
        } catch (t: Throwable) {
            // 네트워크 일시 실패 등은 재시도
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC = "mammamia_widget_update_periodic"
        private const val UNIQUE_ONE_TIME = "mammamia_widget_update_now"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                20, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES // FLEX
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleOneTime(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_TIME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancelAll(context: Context) {
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork(UNIQUE_PERIODIC)
            wm.cancelUniqueWork(UNIQUE_ONE_TIME)
        }
    }
}
