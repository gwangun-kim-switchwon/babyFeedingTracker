package com.baby.feedingtracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baby.feedingtracker.data.ReminderPreference
import com.baby.feedingtracker.data.ReminderSettings
import com.baby.feedingtracker.notification.FeedingReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReminderSettingsUiState(
    val reminderEnabled: Boolean = false,
    val intervalHours: Int = 3,
    val dndEnabled: Boolean = true,
    val dndStartHour: Int = 23,
    val dndEndHour: Int = 6,
)

private fun ReminderSettings.toUiState() = ReminderSettingsUiState(
    reminderEnabled = reminderEnabled,
    intervalHours = intervalHours,
    dndEnabled = dndEnabled,
    dndStartHour = dndStartHour,
    dndEndHour = dndEndHour,
)

class ReminderSettingsViewModel(
    private val reminderPreference: ReminderPreference,
    private val scheduler: FeedingReminderScheduler,
) : ViewModel() {

    val uiState: StateFlow<ReminderSettingsUiState> =
        reminderPreference.settings
            .map { it.toUiState() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ReminderSettingsUiState(),
            )

    fun toggleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            reminderPreference.setEnabled(enabled)
            if (enabled) scheduler.schedule() else scheduler.cancel()
        }
    }

    fun setInterval(hours: Int) {
        viewModelScope.launch {
            reminderPreference.setInterval(hours)
            // 활성 상태면 변경된 간격으로 재예약
            if (uiState.value.reminderEnabled) {
                scheduler.reschedule()
            }
        }
    }

    fun toggleDnd(enabled: Boolean) {
        viewModelScope.launch {
            reminderPreference.setDndEnabled(enabled)
        }
    }

    fun setDndRange(startHour: Int, endHour: Int) {
        viewModelScope.launch {
            reminderPreference.setDndRange(startHour, endHour)
        }
    }

    companion object {
        fun factory(
            reminderPreference: ReminderPreference,
            scheduler: FeedingReminderScheduler,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReminderSettingsViewModel(reminderPreference, scheduler) as T
            }
        }
    }
}
