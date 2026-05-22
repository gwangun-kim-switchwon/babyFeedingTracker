package com.baby.feedingtracker.ui.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * 위젯 전용 독립 데이터 소스.
 *
 * - 앱을 열지 않은 상태에서 Worker가 직접 Firestore를 fetch 한다.
 * - 캐시된 uid를 DataStore에서 읽어 사용한다. (uid는 별도 트랙/CEO 통합 시점에 저장)
 * - 결과(WidgetData)는 JSON으로 직렬화하여 동일 DataStore에 저장한다.
 *
 * 캐시 미스 시 호출자는 graceful fallback 처리해야 한다.
 */
object WidgetDataSource {

    // 별도 인스턴스 (기존 themePreference DataStore와 분리 — 위젯 전용)
    private val Context.widgetPreferencesDataStore by preferencesDataStore(
        name = "widget_preferences"
    )

    val KEY_CACHED_UID: Preferences.Key<String> = stringPreferencesKey("widget_cached_uid")
    val KEY_CACHED_DATA: Preferences.Key<String> = stringPreferencesKey("widget_cached_data")
    val KEY_CACHED_AT: Preferences.Key<Long> = longPreferencesKey("widget_cached_at")

    data class WidgetData(
        val lastTimestamp: Long?,
        val todayCount: Int,
        val recentEntries: List<RecentEntry>,
        val cachedAt: Long = System.currentTimeMillis()
    ) {
        companion object {
            val EMPTY = WidgetData(null, 0, emptyList())
        }
    }

    data class RecentEntry(
        val timestamp: Long,
        val label: String
    )

    /**
     * 위젯 + 버튼 → 즉시 빈 수유 기록 추가.
     * 종류/양/시간은 null로 두고 timestamp만 now로 — 사용자가 앱 열어 record 탭해 보완.
     * cachedUid가 없거나 Firestore 실패시 silent.
     * @return 추가 성공 시 true
     */
    suspend fun addBlankFeedingRecord(context: Context): Boolean {
        val prefs = context.widgetPreferencesDataStore.data.first()
        val uid = prefs[KEY_CACHED_UID] ?: return false
        val recordedBy = try {
            FirebaseAuth.getInstance().currentUser?.uid ?: uid
        } catch (_: Throwable) { uid }
        val now = System.currentTimeMillis()
        return try {
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("feeding_records")
                .add(mapOf(
                    "timestamp" to now,
                    "type" to null,
                    "amountMl" to null,
                    "leftMin" to null,
                    "rightMin" to null,
                    "note" to null,
                    "recordedBy" to recordedBy,
                ))
                .await()
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * 위젯이 독립적으로 Firestore를 조회할 때 사용할 uid를 DataStore에 캐시한다.
     * 로그인 성공 직후 / uid 변경 시점에 호출.
     */
    suspend fun cacheUid(context: Context, uid: String?) {
        context.widgetPreferencesDataStore.edit { mutable ->
            if (uid.isNullOrBlank()) {
                mutable.remove(KEY_CACHED_UID)
            } else {
                mutable[KEY_CACHED_UID] = uid
            }
        }
    }

    /**
     * Firestore에서 최신 데이터를 받아오고 DataStore에 캐시한 뒤 반환.
     * uid 캐시가 없거나 Firestore 호출이 실패하면 마지막 캐시(또는 EMPTY)를 반환한다.
     */
    suspend fun fetchAndCache(context: Context): WidgetData {
        val store = context.widgetPreferencesDataStore
        val prefs = store.data.first()
        val uid = prefs[KEY_CACHED_UID]

        if (uid.isNullOrBlank()) {
            // 로그인 정보 없음 → 기존 캐시 그대로 반환
            return decodeCached(prefs) ?: WidgetData.EMPTY
        }

        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("feeding_records")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()

            val timestamps = snapshot.documents.mapNotNull { doc ->
                val ts = doc.getLong("timestamp") ?: return@mapNotNull null
                val type = doc.getString("type")
                ts to type
            }

            val todayStart = startOfTodayMillis()
            val todayCount = timestamps.count { it.first >= todayStart }
            val lastTs = timestamps.firstOrNull()?.first
            val recent = timestamps.take(3).map { (ts, type) ->
                RecentEntry(timestamp = ts, label = labelFor(type))
            }

            val data = WidgetData(
                lastTimestamp = lastTs,
                todayCount = todayCount,
                recentEntries = recent
            )

            store.edit { mutable ->
                mutable[KEY_CACHED_DATA] = encode(data)
                mutable[KEY_CACHED_AT] = data.cachedAt
            }

            data
        } catch (t: Throwable) {
            decodeCached(prefs) ?: WidgetData.EMPTY
        }
    }

    /**
     * DataStore에서 캐시만 읽어 반환 (네트워크 호출 없음). Glance UI가 호출.
     */
    suspend fun getCached(context: Context): WidgetData? {
        val prefs = context.widgetPreferencesDataStore.data.first()
        return decodeCached(prefs)
    }

    private fun decodeCached(prefs: Preferences): WidgetData? {
        val json = prefs[KEY_CACHED_DATA] ?: return null
        return try {
            val obj = JSONObject(json)
            val lastTs = if (obj.isNull("lastTimestamp")) null else obj.getLong("lastTimestamp")
            val todayCount = obj.optInt("todayCount", 0)
            val cachedAt = obj.optLong("cachedAt", 0L)
            val arr = obj.optJSONArray("recentEntries") ?: JSONArray()
            val recent = buildList {
                for (i in 0 until arr.length()) {
                    val e = arr.getJSONObject(i)
                    add(
                        RecentEntry(
                            timestamp = e.optLong("timestamp", 0L),
                            label = e.optString("label", "")
                        )
                    )
                }
            }
            WidgetData(lastTs, todayCount, recent, cachedAt)
        } catch (t: Throwable) {
            null
        }
    }

    private fun encode(data: WidgetData): String {
        val arr = JSONArray()
        data.recentEntries.forEach { e ->
            arr.put(
                JSONObject()
                    .put("timestamp", e.timestamp)
                    .put("label", e.label)
            )
        }
        val obj = JSONObject()
        if (data.lastTimestamp != null) {
            obj.put("lastTimestamp", data.lastTimestamp)
        } else {
            obj.put("lastTimestamp", JSONObject.NULL)
        }
        obj.put("todayCount", data.todayCount)
        obj.put("recentEntries", arr)
        obj.put("cachedAt", data.cachedAt)
        return obj.toString()
    }

    private fun startOfTodayMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun labelFor(type: String?): String = when (type) {
        "breast" -> "모유"
        "formula" -> "분유"
        "pumped" -> "유축"
        null, "" -> "수유"
        else -> type
    }
}
