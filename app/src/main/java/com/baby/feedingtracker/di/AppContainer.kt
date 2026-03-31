package com.baby.feedingtracker.di

import android.content.Context
import com.baby.feedingtracker.data.FeedingRepository
import com.baby.feedingtracker.data.FirestoreDataSource
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppContainer(context: Context) {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _repository = MutableStateFlow<FeedingRepository?>(null)
    val repository: StateFlow<FeedingRepository?> = _repository.asStateFlow()

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            initRepository(currentUser.uid)
        } else {
            auth.signInAnonymously().addOnSuccessListener { result ->
                initRepository(result.user!!.uid)
            }
        }
    }

    private fun initRepository(uid: String) {
        val dataSource = FirestoreDataSource(firestore, uid)
        _repository.value = FeedingRepository(dataSource)
    }
}
