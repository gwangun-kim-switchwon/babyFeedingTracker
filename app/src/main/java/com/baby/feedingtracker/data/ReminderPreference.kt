package com.baby.feedingtracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 수유 리마인더 설정 저장소 (DataStore Preferences).
 * - reminderEnabled: 알림 ON/OFF (기본 false)
 * - intervalHours: 알림 간격 시간 단위 (1~6, 기본 3)
 * - dndEnabled: 야간 방해금지 ON/OFF (기본 true)
 * - dndStartHour: 방해금지 시작 시각 (0~23, 기본 23)
 * - dndEndHour: 방해금지 종료 시각 (0~23, 기본 6)
 */
private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "reminder_settings")

data class ReminderSettings(
    val reminderEnabled: Boolean = false,
    val intervalHours: Int = 3,
    val dndEnabled: Boolean = true,
    val dndStartHour: Int = 23,
    val dndEndHour: Int = 6,
)

class ReminderPreference(private val context: Context) {

    private val KEY_ENABLED = booleanPreferencesKey("reminder_enabled")
    private val KEY_INTERVAL = intPreferencesKey("reminder_interval_hours")
    private val KEY_DND_ENABLED = booleanPreferencesKey("reminder_dnd_enabled")
    private val KEY_DND_START = intPreferencesKey("reminder_dnd_start_hour")
    private val KEY_DND_END = intPreferencesKey("reminder_dnd_end_hour")

    val settings: Flow<ReminderSettings> = context.reminderDataStore.data.map { prefs ->
        ReminderSettings(
            reminderEnabled = prefs[KEY_ENABLED] ?: false,
            intervalHours = (prefs[KEY_INTERVAL] ?: 3).coerceIn(1, 6),
            dndEnabled = prefs[KEY_DND_ENABLED] ?: true,
            dndStartHour = (prefs[KEY_DND_START] ?: 23).coerceIn(0, 23),
            dndEndHour = (prefs[KEY_DND_END] ?: 6).coerceIn(0, 23),
        )
    }

    suspend fun current(): ReminderSettings = settings.first()

    suspend fun setEnabled(enabled: Boolean) {
        context.reminderDataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setInterval(hours: Int) {
        context.reminderDataStore.edit { it[KEY_INTERVAL] = hours.coerceIn(1, 6) }
    }

    suspend fun setDndEnabled(enabled: Boolean) {
        context.reminderDataStore.edit { it[KEY_DND_ENABLED] = enabled }
    }

    suspend fun setDndRange(startHour: Int, endHour: Int) {
        context.reminderDataStore.edit {
            it[KEY_DND_START] = startHour.coerceIn(0, 23)
            it[KEY_DND_END] = endHour.coerceIn(0, 23)
        }
    }
}
