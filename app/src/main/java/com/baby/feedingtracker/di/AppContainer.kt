package com.baby.feedingtracker.di

import android.content.Context
import com.baby.feedingtracker.data.ThemePreference
import com.baby.feedingtracker.data.BabyProfileDataSource
import com.baby.feedingtracker.data.BabyProfileRepository
import com.baby.feedingtracker.data.CleaningDataSource
import com.baby.feedingtracker.data.CleaningRepository
import com.baby.feedingtracker.data.DiaperDataSource
import com.baby.feedingtracker.data.DiaperRepository
import com.baby.feedingtracker.data.FeedingRepository
import com.baby.feedingtracker.data.SleepDataSource
import com.baby.feedingtracker.data.SleepRepository
import com.baby.feedingtracker.data.FeedingDataSource
import com.baby.feedingtracker.data.GoogleAuthHelper
import com.baby.feedingtracker.data.MilestoneManager
import com.baby.feedingtracker.data.StatisticsRepository
import com.baby.feedingtracker.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AppContainer(context: Context) {
    val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    val themePreference: ThemePreference by lazy { ThemePreference(context) }
    val userRepository = UserRepository(firestore, auth)
    val googleAuthHelper = GoogleAuthHelper(auth, context)

    private val _initError = MutableStateFlow<String?>(null)
    val initError: StateFlow<String?> = _initError.asStateFlow()

    private val _repository = MutableStateFlow<FeedingRepository?>(null)
    val repository: StateFlow<FeedingRepository?> = _repository.asStateFlow()

    private val _cleaningRepository = MutableStateFlow<CleaningRepository?>(null)
    val cleaningRepository: StateFlow<CleaningRepository?> = _cleaningRepository.asStateFlow()

    private val _diaperRepository = MutableStateFlow<DiaperRepository?>(null)
    val diaperRepository: StateFlow<DiaperRepository?> = _diaperRepository.asStateFlow()

    private val _sleepRepository = MutableStateFlow<SleepRepository?>(null)
    val sleepRepository: StateFlow<SleepRepository?> = _sleepRepository.asStateFlow()

    private val _babyProfileRepository = MutableStateFlow<BabyProfileRepository?>(null)
    val babyProfileRepository: StateFlow<BabyProfileRepository?> = _babyProfileRepository.asStateFlow()

    private val _statisticsRepository = MutableStateFlow<StatisticsRepository?>(null)
    val statisticsRepository: StateFlow<StatisticsRepository?> = _statisticsRepository.asStateFlow()

    private val _milestoneManager = MutableStateFlow<MilestoneManager?>(null)
    val milestoneManager: StateFlow<MilestoneManager?> = _milestoneManager.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        firestore.firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
        }
        val currentUser = auth.currentUser
        if (currentUser != null) {
            resolveAndInitRepository(currentUser.uid)
        } else {
            auth.signInAnonymously()
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener
                    resolveAndInitRepository(uid)
                }
                .addOnFailureListener { e ->
                    _initError.value = e.message ?: "인증에 실패했습니다"
                }
        }
    }

    private fun resolveAndInitRepository(uid: String) {
        scope.launch {
            val dataOwnerUid = userRepository.getDataOwnerUid(uid)
            initRepository(dataOwnerUid)
        }
    }

    private fun initRepository(dataOwnerUid: String) {
        val currentUserUid = auth.currentUser?.uid ?: dataOwnerUid

        val dataSource = FeedingDataSource(firestore, dataOwnerUid, currentUserUid)
        _repository.value = FeedingRepository(dataSource)
        val cleaningDataSource = CleaningDataSource(firestore, dataOwnerUid, currentUserUid)
        _cleaningRepository.value = CleaningRepository(cleaningDataSource)
        val diaperDataSource = DiaperDataSource(firestore, dataOwnerUid, currentUserUid)
        _diaperRepository.value = DiaperRepository(diaperDataSource)
        val sleepDataSource = SleepDataSource(firestore, dataOwnerUid, currentUserUid)
        _sleepRepository.value = SleepRepository(sleepDataSource)
        val babyProfileDataSource = BabyProfileDataSource(firestore, dataOwnerUid)
        _babyProfileRepository.value = BabyProfileRepository(babyProfileDataSource)

        _statisticsRepository.value = StatisticsRepository(
            feedingRepository = _repository.value!!,
            diaperRepository = _diaperRepository.value!!,
            cleaningRepository = _cleaningRepository.value!!,
            sleepRepository = _sleepRepository.value!!,
            firestore = firestore,
            dataOwnerUid = dataOwnerUid
        )
        _milestoneManager.value = MilestoneManager(
            statisticsRepository = _statisticsRepository.value!!,
            babyProfileRepository = _babyProfileRepository.value!!
        )
    }

    /**
     * Called after a guest redeems an invite code. Re-initializes the repository
     * to point at the host's data collection.
     */
    fun reinitializeWithDataOwner(dataOwnerUid: String) {
        initRepository(dataOwnerUid)
    }
}
