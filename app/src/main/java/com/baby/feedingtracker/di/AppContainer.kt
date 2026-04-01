package com.baby.feedingtracker.di

import android.content.Context
import com.baby.feedingtracker.data.CleaningDataSource
import com.baby.feedingtracker.data.CleaningRepository
import com.baby.feedingtracker.data.FeedingRepository
import com.baby.feedingtracker.data.FirestoreDataSource
import com.baby.feedingtracker.data.GoogleAuthHelper
import com.baby.feedingtracker.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
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

    val userRepository = UserRepository(firestore, auth)
    val googleAuthHelper = GoogleAuthHelper(auth, context)

    private val _repository = MutableStateFlow<FeedingRepository?>(null)
    val repository: StateFlow<FeedingRepository?> = _repository.asStateFlow()

    private val _cleaningRepository = MutableStateFlow<CleaningRepository?>(null)
    val cleaningRepository: StateFlow<CleaningRepository?> = _cleaningRepository.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            resolveAndInitRepository(currentUser.uid)
        } else {
            auth.signInAnonymously().addOnSuccessListener { result ->
                resolveAndInitRepository(result.user!!.uid)
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
        val dataSource = FirestoreDataSource(firestore, dataOwnerUid)
        _repository.value = FeedingRepository(dataSource)
        val cleaningDataSource = CleaningDataSource(firestore, dataOwnerUid)
        _cleaningRepository.value = CleaningRepository(cleaningDataSource)
    }

    /**
     * Called after a guest redeems an invite code. Re-initializes the repository
     * to point at the host's data collection.
     */
    fun reinitializeWithDataOwner(dataOwnerUid: String) {
        initRepository(dataOwnerUid)
    }
}
