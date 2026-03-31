package com.baby.feedingtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baby.feedingtracker.data.FeedingRecord
import com.baby.feedingtracker.data.FeedingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val records: List<FeedingRecord> = emptyList(),
    val elapsedMinutes: Long? = null
)

class MainViewModel(private val repository: FeedingRepository) : ViewModel() {

    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(60_000L)
        }
    }

    private val _refreshTrigger = MutableStateFlow(0L)

    val uiState: StateFlow<MainUiState> = combine(
        repository.allRecords,
        repository.latestRecord,
        ticker,
        _refreshTrigger
    ) { records, latest, _, _ ->
        val now = System.currentTimeMillis()
        val elapsed = latest?.let {
            (now - it.timestamp) / 60_000L
        }
        MainUiState(
            records = records,
            elapsedMinutes = elapsed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    // 새로 추가된 레코드 (바텀시트 자동 오픈용)
    private val _lastAddedRecord = MutableStateFlow<FeedingRecord?>(null)
    val lastAddedRecord: StateFlow<FeedingRecord?> = _lastAddedRecord.asStateFlow()

    fun clearLastAddedRecord() {
        _lastAddedRecord.value = null
    }

    // 연타 방지
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

    fun deleteRecord(record: FeedingRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    fun updateRecordType(recordId: Long, type: String?, amountMl: Int?) {
        viewModelScope.launch {
            repository.updateRecord(recordId, type, amountMl)
        }
    }

    companion object {
        fun factory(repository: FeedingRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository) as T
                }
            }
        }
    }
}
