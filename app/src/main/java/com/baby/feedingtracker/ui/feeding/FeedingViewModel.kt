package com.baby.feedingtracker.ui.feeding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baby.feedingtracker.data.DataResult
import com.baby.feedingtracker.data.FeedingRecord
import com.baby.feedingtracker.data.FeedingRepository
import com.baby.feedingtracker.data.GoogleAuthHelper
import com.baby.feedingtracker.data.SharingState
import com.baby.feedingtracker.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class FeedingUiState(
    val records: List<FeedingRecord> = emptyList(),
    val elapsedMinutes: Long? = null,
    val isLoadingMore: Boolean = false,
    val hasMoreData: Boolean = true
)

class FeedingViewModel(
    private val repository: FeedingRepository,
    private val userRepository: UserRepository,
    private val googleAuthHelper: GoogleAuthHelper,
    private val auth: FirebaseAuth,
    private val onDataOwnerChanged: (String) -> Unit = {}
) : ViewModel() {

    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(60_000L)
        }
    }

    private val _refreshTrigger = MutableStateFlow(0L)
    private val _olderRecords = MutableStateFlow<List<FeedingRecord>>(emptyList())
    private val _isLoadingMore = MutableStateFlow(false)
    private val _hasMoreData = MutableStateFlow(true)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    val uiState: StateFlow<FeedingUiState> = combine(
        repository.recentRecords,
        _olderRecords,
        ticker,
        _refreshTrigger,
        _isLoadingMore,
        _hasMoreData
    ) { values ->
        val recentRecords = values[0] as List<FeedingRecord>
        val olderRecords = values[1] as List<FeedingRecord>
        @Suppress("UNUSED_VARIABLE") val tick = values[2]
        @Suppress("UNUSED_VARIABLE") val refresh = values[3]
        val isLoadingMore = values[4] as Boolean
        val hasMoreData = values[5] as Boolean

        val allRecords = recentRecords + olderRecords
        val latest = allRecords.firstOrNull()
        val now = System.currentTimeMillis()
        val elapsed = latest?.let {
            (now - it.timestamp) / 60_000L
        }
        FeedingUiState(
            records = allRecords,
            elapsedMinutes = elapsed,
            isLoadingMore = isLoadingMore,
            hasMoreData = hasMoreData
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedingUiState()
    )

    // -- Last added record (for auto-opening bottom sheet) --

    private val _lastAddedRecord = MutableStateFlow<FeedingRecord?>(null)
    val lastAddedRecord: StateFlow<FeedingRecord?> = _lastAddedRecord.asStateFlow()

    fun clearLastAddedRecord() {
        _lastAddedRecord.value = null
    }

    // -- Debounce for add --

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

    fun deleteRecord(record: FeedingRecord) {
        viewModelScope.launch {
            val result = repository.deleteRecord(record)
            if (result is DataResult.Error) {
                _errorMessage.value = result.message
            }
        }
    }

    fun updateRecordType(recordId: String, type: String?, amountMl: Int?, leftMin: Int? = null, rightMin: Int? = null) {
        viewModelScope.launch {
            val result = repository.updateRecord(recordId, type, amountMl, leftMin, rightMin)
            if (result is DataResult.Error) {
                _errorMessage.value = result.message
            }
        }
    }

    fun updateRecordTimestamp(recordId: String, timestamp: Long) {
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

    // ══════════════════════════════════════════════
    // Sharing Feature
    // ══════════════════════════════════════════════

    private val _isGoogleLoggedIn = MutableStateFlow(googleAuthHelper.isLoggedIn())
    val isGoogleLoggedIn: StateFlow<Boolean> = _isGoogleLoggedIn.asStateFlow()

    private val _sharingState = MutableStateFlow<SharingState>(SharingState.NotConnected)
    val sharingState: StateFlow<SharingState> = _sharingState.asStateFlow()

    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()

    private val _sharingError = MutableStateFlow<String?>(null)
    val sharingError: StateFlow<String?> = _sharingError.asStateFlow()

    private var sharingStateJob: Job? = null

    init {
        observeSharingState()
    }

    private fun observeSharingState() {
        sharingStateJob?.cancel()
        val uid = auth.currentUser?.uid ?: return
        sharingStateJob = viewModelScope.launch {
            userRepository.sharingState(uid).collect { state ->
                _sharingState.value = state
            }
        }
    }

    fun refreshLoginState() {
        _isGoogleLoggedIn.value = googleAuthHelper.isLoggedIn()
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val email = googleAuthHelper.currentUserEmail()
            userRepository.createProfile(uid, email)
            observeSharingState()
        }
    }

    fun generateInviteCode() {
        viewModelScope.launch {
            try {
                val code = userRepository.generateInviteCode()
                _inviteCode.value = code
                _sharingError.value = null
            } catch (e: Exception) {
                _sharingError.value = e.message
            }
        }
    }

    fun redeemInviteCode(code: String) {
        viewModelScope.launch {
            val result = userRepository.redeemInviteCode(code.uppercase())
            result.onSuccess { hostUid ->
                _sharingError.value = null
                onDataOwnerChanged(hostUid)
            }
            result.onFailure { e ->
                _sharingError.value = e.message
            }
        }
    }

    fun clearInviteCode() {
        _inviteCode.value = null
    }

    fun clearSharingError() {
        _sharingError.value = null
    }

    companion object {
        fun factory(
            repository: FeedingRepository,
            userRepository: UserRepository,
            googleAuthHelper: GoogleAuthHelper,
            auth: FirebaseAuth,
            onDataOwnerChanged: (String) -> Unit
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FeedingViewModel(repository, userRepository, googleAuthHelper, auth, onDataOwnerChanged) as T
                }
            }
        }
    }
}
