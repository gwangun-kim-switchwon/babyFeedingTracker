package com.baby.feedingtracker.ui.feeding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    val elapsedMinutes: Long? = null
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

    val uiState: StateFlow<FeedingUiState> = combine(
        repository.allRecords,
        repository.latestRecord,
        ticker,
        _refreshTrigger
    ) { records, latest, _, _ ->
        val now = System.currentTimeMillis()
        val elapsed = latest?.let {
            (now - it.timestamp) / 60_000L
        }
        FeedingUiState(
            records = records,
            elapsedMinutes = elapsed
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
            try {
                val record = repository.addRecord()
                _refreshTrigger.value = now
                _lastAddedRecord.value = record
            } catch (e: Exception) {
                // Firestore 오류 시 무시 (오프라인 캐시가 처리)
            }
        }
    }

    fun deleteRecord(record: FeedingRecord) {
        viewModelScope.launch {
            try {
                repository.deleteRecord(record)
            } catch (e: Exception) {
                // Firestore 오류 시 무시 (오프라인 캐시가 처리)
            }
        }
    }

    fun updateRecordType(recordId: String, type: String?, amountMl: Int?, leftMin: Int? = null, rightMin: Int? = null) {
        viewModelScope.launch {
            try {
                repository.updateRecord(recordId, type, amountMl, leftMin, rightMin)
            } catch (e: Exception) {
                // Firestore 오류 시 무시 (오프라인 캐시가 처리)
            }
        }
    }

    fun updateRecordTimestamp(recordId: String, timestamp: Long) {
        viewModelScope.launch {
            try {
                repository.updateTimestamp(recordId, timestamp)
            } catch (e: Exception) {
                // Firestore 오류 시 무시 (오프라인 캐시가 처리)
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
        // After login, ensure profile exists and start observing sharing state
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
                // 게스트의 데이터 경로를 호스트로 전환
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
