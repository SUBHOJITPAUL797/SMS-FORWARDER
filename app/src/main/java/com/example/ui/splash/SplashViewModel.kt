package com.example.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.domain.model.UserRole
import com.example.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SplashViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<String?>(null)
    val destination: StateFlow<String?> = _destination.asStateFlow()

    init {
        checkInitialDestination()
    }

    private fun checkInitialDestination() {
        viewModelScope.launch {
            // Splash screen delay of 1.2 seconds for visual branding
            delay(1200)
            val role = authRepository.userRoleFlow.firstOrNull() ?: UserRole.UNSET
            when (role) {
                UserRole.HOST -> _destination.value = Screen.HostHome.route
                UserRole.CLIENT -> _destination.value = Screen.ClientHome.route
                UserRole.UNSET -> _destination.value = Screen.RoleSelect.route
            }
        }
    }
}
