package com.baby.feedingtracker.ui.diaper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baby.feedingtracker.data.DataResult
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
    val todayStoolCount: Int = 0,
    val isLoadingMore: Boolean = false,
    val hasMoreData: Boolean = true
)

class DiaperViewModel(private val repository: DiaperRepository) : ViewModel() {

    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(60_000L)
        }
    }

    private val _refreshTrigger = MutableStateFlow(0L)
    private val _olderRecords = MutableStateFlow<List<DiaperRecord>>(emptyList())
    private val _isLoadingMore = MutableStateFlow(false)
    private val _hasMoreData = MutableStateFlow(true)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    val uiState: StateFlow<DiaperUiState> = combine(
        repository.recentRecords,
        _olderRecords,
        ticker,
        _refreshTrigger,
        _isLoadingMore,
        _hasMoreData
    ) { values ->
        val recentRecords = values[0] as List<DiaperRecord>
        val olderRecords = values[1] as List<DiaperRecord>
        @Suppress("UNUSED_VARIABLE") val tick = values[2]
        @Suppress("UNUSED_VARIABLE") val refresh = values[3]
        val isLoadingMore = values[4] as Boolean
        val hasMoreData = values[5] as Boolean

        val allRecords = recentRecords + olderRecords
        val latest = allRecords.firstOrNull()
        val now = System.currentTimeMillis()
        val elapsed = latest?.let { (now - it.timestamp) / 60_000L }

        val todayMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayRecords = allRecords.filter { it.timestamp >= todayMidnight }
        val todayDiaperCount = todayRecords.count { it.type == "diaper" }
        val todayUrineCount = todayRecords.count { it.type == "urine" }
        val todayStoolCount = todayRecords.count { it.type == "stool" }

        DiaperUiState(
            records = allRecords,
            elapsedMinutes = elapsed,
            todayDiaperCount = todayDiaperCount,
            todayUrineCount = todayUrineCount,
            todayStoolCount = todayStoolCount,
            isLoadingMore = isLoadingMore,
            hasMoreData = hasMoreData
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
            val result = repository.addRecord()
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

    fun deleteRecord(record: DiaperRecord) {
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

    fun updateNote(recordId: String, note: String?) {
        viewModelScope.launch {
            val result = repository.updateNote(recordId, note)
            if (result is DataResult.Error) {
                _errorMessage.value = result.message
            }
        }
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
