package com.example.ui.roleselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.domain.model.UserRole
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoleSelectViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val userEmail: StateFlow<String?> = authRepository.userEmailFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.currentUser?.email)

    fun selectRole(role: UserRole, onComplete: () -> Unit) {
        viewModelScope.launch {
            // Save role locally immediately to prevent any UI freeze or network delay
            authRepository.setUserRoleLocal(role)
            // Trigger instant screen transition
            onComplete()
            // Sync user profile to Firestore in background
            authRepository.syncUserProfile(role)
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
