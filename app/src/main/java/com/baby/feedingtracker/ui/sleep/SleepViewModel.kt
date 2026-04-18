package com.baby.feedingtracker.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baby.feedingtracker.data.DataResult
import com.baby.feedingtracker.data.SleepRecord
import com.baby.feedingtracker.data.SleepRepository
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

data class SleepUiState(
    val records: List<SleepRecord> = emptyList(),
    val elapsedMinutes: Long? = null,
    val todayTotalSleepMinutes: Long = 0,
    val todayNapCount: Int = 0,
    val todayNightCount: Int = 0,
    val isCurrentlySleeping: Boolean = false,
    val currentSleepRecord: SleepRecord? = null,
    val isLoadingMore: Boolean = false,
    val hasMoreData: Boolean = true
)

class SleepViewModel(private val repository: SleepRepository) : ViewModel() {

    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(60_000L)
        }
    }

    private val _refreshTrigger = MutableStateFlow(0L)
    private val _olderRecords = MutableStateFlow<List<SleepRecord>>(emptyList())
    private val _isLoadingMore = MutableStateFlow(false)
    private val _hasMoreData = MutableStateFlow(true)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    val uiState: StateFlow<SleepUiState> = combine(
        repository.recentRecords,
        _olderRecords,
        ticker,
        _refreshTrigger,
        _isLoadingMore,
        _hasMoreData
    ) { values ->
        val recentRecords = values[0] as List<SleepRecord>
        val olderRecords = values[1] as List<SleepRecord>
        @Suppress("UNUSED_VARIABLE") val tick = values[2]
        @Suppress("UNUSED_VARIABLE") val refresh = values[3]
        val isLoadingMore = values[4] as Boolean
        val hasMoreData = values[5] as Boolean

        val allRecords = recentRecords + olderRecords
        val now = System.currentTimeMillis()

        // 현재 수면 중인 기록 (endTimestamp == null)
        val currentSleepRecord = allRecords.firstOrNull { it.endTimestamp == null }
        val isCurrentlySleeping = currentSleepRecord != null

        // 경과 시간: 수면 중이면 시작 시각 기준, 아니면 마지막 수면 종료 기준
        val elapsed = if (isCurrentlySleeping) {
            (now - currentSleepRecord!!.timestamp) / 60_000L
        } else {
            val latestFinished = allRecords.firstOrNull { it.endTimestamp != null }
            latestFinished?.endTimestamp?.let { (now - it) / 60_000L }
        }

        // 오늘 통계
        val todayMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayRecords = allRecords.filter { it.timestamp >= todayMidnight }

        // 오늘 총 수면 시간 (완료된 수면만 + 진행 중인 수면의 현재까지 시간)
        val todayTotalSleepMinutes = todayRecords.sumOf { record ->
            val end = record.endTimestamp ?: now
            ((end - record.timestamp) / 60_000L).coerceAtLeast(0)
        }

        val todayNapCount = todayRecords.count { it.type == "nap" }
        val todayNightCount = todayRecords.count { it.type == "night" }

        SleepUiState(
            records = allRecords,
            elapsedMinutes = elapsed,
            todayTotalSleepMinutes = todayTotalSleepMinutes,
            todayNapCount = todayNapCount,
            todayNightCount = todayNightCount,
            isCurrentlySleeping = isCurrentlySleeping,
            currentSleepRecord = currentSleepRecord,
            isLoadingMore = isLoadingMore,
            hasMoreData = hasMoreData
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SleepUiState()
    )

    private val _lastAddedRecord = MutableStateFlow<SleepRecord?>(null)
    val lastAddedRecord: StateFlow<SleepRecord?> = _lastAddedRecord.asStateFlow()

    fun clearLastAddedRecord() { _lastAddedRecord.value = null }

    private var lastRecordTime = 0L
    private val debounceInterval = 2_000L

    fun addRecord() {
        if (uiState.value.isCurrentlySleeping) return
        val now = System.currentTimeMillis()
        if (now - lastRecordTime < debounceInterval) return
        lastRecordTime = now
        viewModelScope.launch {
            val result = repository.addSleepRecord()
            when (result) {
                is DataResult.Success -> {
                    _refreshTrigger.value = now
                    _lastAddedRecord.value = result.data
                }
                is DataResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun endSleep() {
        val currentRecord = uiState.value.currentSleepRecord ?: return
        viewModelScope.launch {
            val result = repository.endSleep(currentRecord.id)
            when (result) {
                is DataResult.Success -> {
                    _refreshTrigger.value = System.currentTimeMillis()
                }
                is DataResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun deleteRecord(record: SleepRecord) {
        viewModelScope.launch {
            val result = repository.deleteRecord(record)
            if (result is DataResult.Error) {
                _errorMessage.value = result.message
            }
        }
    }

    fun updateType(recordId: String, type: String?) {
        viewModelScope.launch {
            val result = repository.updateType(recordId, type)
            if (result is DataResult.Error) {
                _errorMessage.value = result.message
            }
        }
    }

    fun updateTimestamp(recordId: String, timestamp: Long) {
        viewModelScope.launch {
            val result = repository.updateTimestamp(recordId, timestamp)
            if (result is DataResult.Error) {
                _errorMessage.value = result.message
            }
        }
    }

    fun updateNote(recordId: String, note: String?) {
        viewModelScope.launch {
            val result = repository.updateNote(recordId, note)
            if (result is DataResult.Error) {
                _errorMessage.value = result.message
            }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value || !_hasMoreData.value) return
        val currentRecords = uiState.value.records
        val oldestTimestamp = currentRecords.lastOrNull()?.timestamp ?: return

        _isLoadingMore.value = true
        viewModelScope.launch {
            try {
                val older = repository.loadMore(oldestTimestamp)
                if (older.isEmpty()) {
                    _hasMoreData.value = false
                } else {
                    _olderRecords.value = _olderRecords.value + older
                    if (older.size < 20) _hasMoreData.value = false
                }
            } catch (e: Exception) {
                // Firestore 오류 시 무시
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    companion object {
        fun factory(repository: SleepRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SleepViewModel(repository) as T
                }
            }
        }
    }
}
