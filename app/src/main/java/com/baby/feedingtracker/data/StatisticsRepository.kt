package com.baby.feedingtracker.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

data class DailyStats(
    val date: Long,
    val feedingCount: Int,
    val breastCount: Int,
    val formulaCount: Int,
    val diaperCount: Int,
    val sleepTotalMinutes: Long,
    val cleaningCount: Int
)

data class WeeklyStats(
    val startDate: Long,
    val endDate: Long,
    val dailyStats: List<DailyStats>,
    val totalFeedings: Int,
    val totalDiapers: Int,
    val totalSleepMinutes: Long,
    val averageFeedingInterval: Long?,
    val feedingByHour: Map<Int, Int>
)

data class PartnerContribution(
    val user1Uid: String,
    val user2Uid: String?,
    val user1Count: Int,
    val user2Count: Int,
    val user1Percentage: Float,
    val user2Percentage: Float,
    val byCategory: Map<String, Pair<Int, Int>>
)

class StatisticsRepository(
    private val feedingRepository: FeedingRepository,
    private val diaperRepository: DiaperRepository,
    private val cleaningRepository: CleaningRepository,
    private val sleepRepository: SleepRepository,
    private val firestore: FirebaseFirestore,
    private val dataOwnerUid: String
) {
    private val userDoc get() = firestore.collection("users").document(dataOwnerUid)

    suspend fun getFeedingRecords(startTime: Long, endTime: Long): List<FeedingRecord> {
        val snapshot = userDoc.collection("feeding_records")
            .whereGreaterThanOrEqualTo("timestamp", startTime)
            .whereLessThan("timestamp", endTime)
            .get().await()
        return snapshot.documents.map { doc ->
            FeedingRecord(
                id = doc.id,
                timestamp = doc.getLong("timestamp") ?: 0L,
                type = doc.getString("type"),
                amountMl = doc.getLong("amountMl")?.toInt(),
                leftMin = doc.getLong("leftMin")?.toInt(),
                rightMin = doc.getLong("rightMin")?.toInt(),
                note = doc.getString("note"),
                recordedBy = doc.getString("recordedBy")
            )
        }
    }

    suspend fun getDiaperRecords(startTime: Long, endTime: Long): List<DiaperRecord> {
        val snapshot = userDoc.collection("diaper_records")
            .whereGreaterThanOrEqualTo("timestamp", startTime)
            .whereLessThan("timestamp", endTime)
            .get().await()
        return snapshot.documents.map { doc ->
            DiaperRecord(
                id = doc.id,
                timestamp = doc.getLong("timestamp") ?: 0L,
                type = doc.getString("type"),
                note = doc.getString("note"),
                recordedBy = doc.getString("recordedBy")
            )
        }
    }

    suspend fun getCleaningRecords(startTime: Long, endTime: Long): List<CleaningRecord> {
        val snapshot = userDoc.collection("cleaning_records")
            .whereGreaterThanOrEqualTo("timestamp", startTime)
            .whereLessThan("timestamp", endTime)
            .get().await()
        return snapshot.documents.map { doc ->
            CleaningRecord(
                id = doc.id,
                timestamp = doc.getLong("timestamp") ?: 0L,
                itemType = doc.getString("itemType"),
                note = doc.getString("note"),
                recordedBy = doc.getString("recordedBy")
            )
        }
    }

    suspend fun getSleepRecords(startTime: Long, endTime: Long): List<SleepRecord> {
        val snapshot = userDoc.collection("sleep_records")
            .whereGreaterThanOrEqualTo("timestamp", startTime)
            .whereLessThan("timestamp", endTime)
            .get().await()
        return snapshot.documents.map { doc ->
            SleepRecord(
                id = doc.id,
                timestamp = doc.getLong("timestamp") ?: 0L,
                endTimestamp = doc.getLong("endTimestamp"),
                type = doc.getString("type"),
                note = doc.getString("note"),
                recordedBy = doc.getString("recordedBy")
            )
        }
    }

    suspend fun getTodayStats(): DailyStats {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000L

        return buildDailyStats(startOfDay, endOfDay)
    }

    suspend fun getWeeklyStats(): WeeklyStats {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endDate = calendar.timeInMillis + 24 * 60 * 60 * 1000L

        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = calendar.timeInMillis

        val dailyStatsList = mutableListOf<DailyStats>()
        val tempCal = Calendar.getInstance().apply { timeInMillis = startDate }
        for (i in 0 until 7) {
            val dayStart = tempCal.timeInMillis
            val dayEnd = dayStart + 24 * 60 * 60 * 1000L
            dailyStatsList.add(buildDailyStats(dayStart, dayEnd))
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val feedings = getFeedingRecords(startDate, endDate)
        val diapers = getDiaperRecords(startDate, endDate)
        val sleepRecords = getSleepRecords(startDate, endDate)

        val totalSleepMinutes = sleepRecords.sumOf { record ->
            val end = record.endTimestamp ?: return@sumOf 0L
            (end - record.timestamp) / 60_000L
        }

        val sortedFeedings = feedings.sortedBy { it.timestamp }
        val averageFeedingInterval = if (sortedFeedings.size >= 2) {
            val intervals = sortedFeedings.zipWithNext { a, b -> (b.timestamp - a.timestamp) / 60_000L }
            intervals.average().toLong()
        } else {
            null
        }

        val feedingByHour = feedings.groupBy { record ->
            Calendar.getInstance().apply { timeInMillis = record.timestamp }.get(Calendar.HOUR_OF_DAY)
        }.mapValues { it.value.size }

        return WeeklyStats(
            startDate = startDate,
            endDate = endDate,
            dailyStats = dailyStatsList,
            totalFeedings = feedings.size,
            totalDiapers = diapers.size,
            totalSleepMinutes = totalSleepMinutes,
            averageFeedingInterval = averageFeedingInterval,
            feedingByHour = feedingByHour
        )
    }

    suspend fun getPartnerContribution(startTime: Long, endTime: Long): PartnerContribution {
        val allRecords = mutableListOf<BaseRecord>()
        allRecords.addAll(getFeedingRecords(startTime, endTime))
        allRecords.addAll(getDiaperRecords(startTime, endTime))
        allRecords.addAll(getCleaningRecords(startTime, endTime))
        allRecords.addAll(getSleepRecords(startTime, endTime))

        val byUser = allRecords.groupBy { it.recordedBy ?: "unknown" }
        val uids = byUser.keys.filterNot { it == "unknown" }.sorted()

        val user1Uid = uids.getOrElse(0) { dataOwnerUid }
        val user2Uid = uids.getOrNull(1)

        val user1Count = (byUser[user1Uid]?.size ?: 0) + (if (uids.isEmpty()) byUser["unknown"]?.size ?: 0 else 0)
        val user2Count = if (user2Uid != null) byUser[user2Uid]?.size ?: 0 else 0
        val total = (user1Count + user2Count).coerceAtLeast(1)

        val categories = mapOf(
            "feeding" to getFeedingRecords(startTime, endTime),
            "diaper" to getDiaperRecords(startTime, endTime),
            "cleaning" to getCleaningRecords(startTime, endTime),
            "sleep" to getSleepRecords(startTime, endTime)
        )

        val byCategory = categories.mapValues { (_, records) ->
            val u1 = records.count { it.recordedBy == user1Uid || (uids.isEmpty() && (it.recordedBy == null || it.recordedBy == "unknown")) }
            val u2 = if (user2Uid != null) records.count { it.recordedBy == user2Uid } else 0
            Pair(u1, u2)
        }

        return PartnerContribution(
            user1Uid = user1Uid,
            user2Uid = user2Uid,
            user1Count = user1Count,
            user2Count = user2Count,
            user1Percentage = user1Count.toFloat() / total * 100f,
            user2Percentage = user2Count.toFloat() / total * 100f,
            byCategory = byCategory
        )
    }

    private suspend fun buildDailyStats(startTime: Long, endTime: Long): DailyStats {
        val feedings = getFeedingRecords(startTime, endTime)
        val diapers = getDiaperRecords(startTime, endTime)
        val sleepRecords = getSleepRecords(startTime, endTime)
        val cleanings = getCleaningRecords(startTime, endTime)

        val sleepMinutes = sleepRecords.sumOf { record ->
            val end = record.endTimestamp ?: return@sumOf 0L
            val effectiveStart = record.timestamp.coerceAtLeast(startTime)
            val effectiveEnd = end.coerceAtMost(endTime)
            if (effectiveEnd > effectiveStart) (effectiveEnd - effectiveStart) / 60_000L else 0L
        }

        return DailyStats(
            date = startTime,
            feedingCount = feedings.size,
            breastCount = feedings.count { it.type == "breast" },
            formulaCount = feedings.count { it.type == "formula" },
            diaperCount = diapers.size,
            sleepTotalMinutes = sleepMinutes,
            cleaningCount = cleanings.size
        )
    }

    /** 전체 레코드 수를 가져옴 (마일스톤 계산용) */
    suspend fun getTotalFeedingCount(): Int {
        val snapshot = userDoc.collection("feeding_records").get().await()
        return snapshot.size()
    }

    suspend fun getTotalDiaperCount(): Int {
        val snapshot = userDoc.collection("diaper_records").get().await()
        return snapshot.size()
    }
}
