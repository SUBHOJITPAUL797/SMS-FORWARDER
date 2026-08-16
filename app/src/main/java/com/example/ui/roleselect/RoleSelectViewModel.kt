package com.example.ui.roleselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
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
        // Immediate, 0ms synchronous UI navigation
        onComplete()

        // Background persistence so no blocking or lagging occurs
        viewModelScope.launch(Dispatchers.IO) {
            try {
                authRepository.setUserRoleLocal(role)
                authRepository.syncUserProfile(role)
            } catch (e: Exception) {
                // Ignore background sync errors
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        onLoggedOut()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                authRepository.logout()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
