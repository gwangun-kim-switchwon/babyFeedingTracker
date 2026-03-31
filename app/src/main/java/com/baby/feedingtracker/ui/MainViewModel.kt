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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val records: List<FeedingRecord> = emptyList(),
    val elapsedMinutes: Long? = null
)

class MainViewModel(private val repository: FeedingRepository) : ViewModel() {

    // 1분마다 tick을 발행하여 경과 시간 갱신을 트리거 (값 자체는 사용하지 않음)
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

    // 연타 방지: 마지막 기록 시각
    private var lastRecordTime = 0L
    private val debounceInterval = 2_000L

    fun addRecord() {
        val now = System.currentTimeMillis()
        if (now - lastRecordTime < debounceInterval) return
        lastRecordTime = now
        viewModelScope.launch {
            repository.addRecord()
            _refreshTrigger.value = now
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
