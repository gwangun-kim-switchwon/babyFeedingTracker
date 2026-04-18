package com.baby.feedingtracker.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baby.feedingtracker.data.BabyProfile
import com.baby.feedingtracker.data.BabyProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BabyProfileViewModel(
    private val repository: BabyProfileRepository
) : ViewModel() {

    val profile: StateFlow<BabyProfile?> = repository.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val daysOld: StateFlow<Int?> = repository.getDaysOld()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveProfile(name: String, birthDate: Long, gender: String?) {
        viewModelScope.launch {
            repository.saveProfile(
                BabyProfile(
                    name = name.trim(),
                    birthDate = birthDate,
                    gender = gender
                )
            )
        }
    }

    companion object {
        fun factory(repository: BabyProfileRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BabyProfileViewModel(repository) as T
                }
            }
        }
    }
}
