package com.baby.feedingtracker.ui.diaper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baby.feedingtracker.data.DiaperRecord
import com.baby.feedingtracker.data.DiaperRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class DiaperUiState(
    val records: List<DiaperRecord> = emptyList(),
    val elapsedMinutes: Long? = null,
    val todayDiaperCount: Int = 0,
    val todayUrineCount: Int = 0,
    val todayStoolCount: Int = 0
)

class DiaperViewModel(private val repository: DiaperRepository) : ViewModel() {

    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(60_000L)
        }
    }

    private val _refreshTrigger = MutableStateFlow(0L)

    val uiState: StateFlow<DiaperUiState> = combine(
        repository.allRecords,
        repository.latestRecord,
        ticker,
        _refreshTrigger
    ) { records, latest, _, _ ->
        val now = System.currentTimeMillis()
        val elapsed = latest?.let { (now - it.timestamp) / 60_000L }

        val todayMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayRecords = records.filter { it.timestamp >= todayMidnight }
        val todayDiaperCount = todayRecords.count { it.type == "diaper" }
        val todayUrineCount = todayRecords.count { it.type == "urine" }
        val todayStoolCount = todayRecords.count { it.type == "stool" }

        DiaperUiState(
            records = records,
            elapsedMinutes = elapsed,
            todayDiaperCount = todayDiaperCount,
            todayUrineCount = todayUrineCount,
            todayStoolCount = todayStoolCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiaperUiState()
    )

    private val _lastAddedRecord = MutableStateFlow<DiaperRecord?>(null)
    val lastAddedRecord: StateFlow<DiaperRecord?> = _lastAddedRecord.asStateFlow()

    fun clearLastAddedRecord() { _lastAddedRecord.value = null }

    private var lastRecordTime = 0L
    private val debounceInterval = 2_000L

    fun addRecord() {
        val now = System.currentTimeMillis()
        if (now - lastRecordTime < debounceInterval) return
        lastRecordTime = now
        viewModelScope.launch {
            val record = repository.addRecord()
            _refreshTrigger.value = now
            _lastAddedRecord.value = record
        }
    }

    fun deleteRecord(record: DiaperRecord) {
        viewModelScope.launch { repository.deleteRecord(record) }
    }

    fun updateType(recordId: String, type: String?) {
        viewModelScope.launch { repository.updateType(recordId, type) }
    }

    fun updateTimestamp(recordId: String, timestamp: Long) {
        viewModelScope.launch { repository.updateTimestamp(recordId, timestamp) }
    }

    companion object {
        fun factory(repository: DiaperRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DiaperViewModel(repository) as T
                }
            }
        }
    }
}
