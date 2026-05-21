package com.baby.feedingtracker.ui.growth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baby.feedingtracker.data.DataResult
import com.baby.feedingtracker.data.GrowthRecord
import com.baby.feedingtracker.data.GrowthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GrowthUiState(
    val records: List<GrowthRecord> = emptyList(),
    val latestHeight: Float? = null,
    val latestWeight: Float? = null,
    val latestHead: Float? = null,
    val isLoadingMore: Boolean = false,
    val hasMoreData: Boolean = true,
)

class GrowthViewModel(
    private val repository: GrowthRepository
) : ViewModel() {

    private val _olderRecords   = MutableStateFlow<List<GrowthRecord>>(emptyList())
    private val _isLoadingMore  = MutableStateFlow(false)
    private val _hasMoreData    = MutableStateFlow(true)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() { _errorMessage.value = null }

    val uiState: StateFlow<GrowthUiState> = combine(
        repository.recentRecords,
        _olderRecords,
        _isLoadingMore,
        _hasMoreData,
    ) { recent, older, loading, hasMore ->
        val all = recent + older
        GrowthUiState(
            records      = all,
            latestHeight = all.firstOrNull { it.heightCm != null }?.heightCm,
            latestWeight = all.firstOrNull { it.weightKg != null }?.weightKg,
            latestHead   = all.firstOrNull { it.headCm   != null }?.headCm,
            isLoadingMore = loading,
            hasMoreData   = hasMore,
        )
    }.stateIn(
        scope   = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = GrowthUiState(),
    )

    fun addRecord(record: GrowthRecord) {
        if (!record.hasAnyMeasurement) {
            _errorMessage.value = "최소 하나의 측정값을 입력해주세요"
            return
        }
        viewModelScope.launch {
            val result = repository.addGrowthRecord(record)
            if (result is DataResult.Error) _errorMessage.value = result.message
        }
    }

    fun deleteRecord(record: GrowthRecord) {
        viewModelScope.launch {
            val result = repository.deleteRecord(record)
            if (result is DataResult.Error) _errorMessage.value = result.message
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value || !_hasMoreData.value) return
        val oldest = uiState.value.records.lastOrNull()?.timestamp ?: return
        _isLoadingMore.value = true
        viewModelScope.launch {
            try {
                val older = repository.loadMore(oldest)
                if (older.isEmpty()) {
                    _hasMoreData.value = false
                } else {
                    _olderRecords.value = _olderRecords.value + older
                    if (older.size < 20) _hasMoreData.value = false
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    companion object {
        fun factory(repository: GrowthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GrowthViewModel(repository) as T
            }
    }
}
