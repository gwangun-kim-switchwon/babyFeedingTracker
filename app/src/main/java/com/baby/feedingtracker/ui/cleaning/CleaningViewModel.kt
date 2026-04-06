package com.baby.feedingtracker.ui.cleaning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baby.feedingtracker.data.CleaningRecord
import com.baby.feedingtracker.data.CleaningRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CleaningUiState(
    val records: List<CleaningRecord> = emptyList(),
    val elapsedMinutes: Long? = null,
    val perTypeElapsed: Map<String, Long> = emptyMap()  // itemType → 경과 분
)

class CleaningViewModel(private val repository: CleaningRepository) : ViewModel() {

    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(60_000L)
        }
    }

    private val _refreshTrigger = MutableStateFlow(0L)

    val uiState: StateFlow<CleaningUiState> = combine(
        repository.allRecords,
        repository.latestRecord,
        ticker,
        _refreshTrigger
    ) { records, latest, _, _ ->
        val now = System.currentTimeMillis()
        val elapsed = latest?.let { (now - it.timestamp) / 60_000L }
        // 종류별 마지막 세척 경과 시간 (itemType이 있는 기록만)
        val perTypeElapsed = records
            .filter { it.itemType != null }
            .groupBy { it.itemType!! }
            .mapValues { (_, typeRecords) ->
                val latestOfType = typeRecords.maxByOrNull { it.timestamp }
                latestOfType?.let { (now - it.timestamp) / 60_000L } ?: 0L
            }
        CleaningUiState(records = records, elapsedMinutes = elapsed, perTypeElapsed = perTypeElapsed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CleaningUiState()
    )

    private val _lastAddedRecord = MutableStateFlow<CleaningRecord?>(null)
    val lastAddedRecord: StateFlow<CleaningRecord?> = _lastAddedRecord.asStateFlow()

    fun clearLastAddedRecord() { _lastAddedRecord.value = null }

    private var lastRecordTime = 0L
    private val debounceInterval = 2_000L

    fun addRecord() {
        val now = System.currentTimeMillis()
        if (now - lastRecordTime < debounceInterval) return
        lastRecordTime = now
        viewModelScope.launch {
            try {
                val record = repository.addRecord()
                _refreshTrigger.value = now
                _lastAddedRecord.value = record
            } catch (e: Exception) {
                // Firestore 오류 시 무시 (오프라인 캐시가 처리)
            }
        }
    }

    fun deleteRecord(record: CleaningRecord) {
        viewModelScope.launch {
            try {
                repository.deleteRecord(record)
            } catch (e: Exception) {
                // Firestore 오류 시 무시 (오프라인 캐시가 처리)
            }
        }
    }

    fun updateItemType(recordId: String, itemType: String?) {
        viewModelScope.launch {
            try {
                repository.updateItemType(recordId, itemType)
            } catch (e: Exception) {
                // Firestore 오류 시 무시 (오프라인 캐시가 처리)
            }
        }
    }

    fun updateTimestamp(recordId: String, timestamp: Long) {
        viewModelScope.launch {
            try {
                repository.updateTimestamp(recordId, timestamp)
            } catch (e: Exception) {
                // Firestore 오류 시 무시 (오프라인 캐시가 처리)
            }
        }
    }

    companion object {
        fun factory(repository: CleaningRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CleaningViewModel(repository) as T
                }
            }
        }
    }
}
