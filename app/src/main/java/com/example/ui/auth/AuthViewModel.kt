package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.domain.model.UserRole
import com.example.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val destination: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val role = authRepository.userRoleFlow.firstOrNull() ?: UserRole.UNSET
            val target = when (role) {
                UserRole.HOST -> Screen.HostHome.route
                UserRole.CLIENT -> Screen.ClientHome.route
                UserRole.UNSET -> Screen.RoleSelect.route
            }
            _uiState.value = AuthUiState.Success(target)
        }
    }

    fun register(email: String, pass: String, confirmPass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = AuthUiState.Success(Screen.RoleSelect.route)
        }
    }
}
