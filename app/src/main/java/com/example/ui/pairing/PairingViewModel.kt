package com.example.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.PairingRepository
import com.example.domain.model.PairingState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HostPairingUiState {
    object Idle : HostPairingUiState()
    object Loading : HostPairingUiState()
    data class Success(val clientDeviceName: String) : HostPairingUiState()
    data class Error(val message: String) : HostPairingUiState()
}

class PairingViewModel(
    private val pairingRepository: PairingRepository
) : ViewModel() {

    private val _clientState = MutableStateFlow<PairingState>(PairingState.Idle)
    val clientState: StateFlow<PairingState> = _clientState.asStateFlow()

    private val _hostState = MutableStateFlow<HostPairingUiState>(HostPairingUiState.Idle)
    val hostState: StateFlow<HostPairingUiState> = _hostState.asStateFlow()

    private var clientObserveJob: Job? = null
    private var countdownJob: Job? = null
    private var currentCode: String = ""

    fun generateClientCode() {
        clientObserveJob?.cancel()
        countdownJob?.cancel()

        viewModelScope.launch {
            _clientState.value = PairingState.Loading
            val code = pairingRepository.generateRandomCode()
            currentCode = code
            val result = pairingRepository.startClientPairing(code)
            if (result.isSuccess) {
                // Observe pairing document
                clientObserveJob = launch {
                    pairingRepository.observeClientPairingState(code).collect { state ->
                        _clientState.value = state
                        when (state) {
                            is PairingState.CodeGenerated -> {
                                startCountdown(state.expiresAt)
                            }
                            is PairingState.Paired,
                            is PairingState.Error -> {
                                countdownJob?.cancel()
                                clientObserveJob?.cancel()
                            }
                            else -> {}
                        }
                    }
                }
            } else {
                _clientState.value = PairingState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to generate pairing code."
                )
            }
        }
    }

    private fun startCountdown(expiresAt: Long) {
        if (expiresAt <= 0L) return
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val remaining = ((expiresAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
                val current = _clientState.value
                if (current !is PairingState.CodeGenerated) {
                    return@launch
                }
                _clientState.value = current.copy(remainingSeconds = remaining)
                if (remaining <= 0L) {
                    _clientState.value = PairingState.Error("Pairing code has expired. Please tap 'Regenerate Code'.")
                    clientObserveJob?.cancel()
                    return@launch
                }
                delay(1000)
            }
        }
    }

    fun pairAsHost(inputCode: String) {
        if (_hostState.value is HostPairingUiState.Loading) return
        val clean = inputCode.trim().uppercase()
        if (clean.length < 6) {
            _hostState.value = HostPairingUiState.Error("Please enter the complete 6-character code.")
            return
        }

        viewModelScope.launch {
            _hostState.value = HostPairingUiState.Loading
            val result = pairingRepository.pairAsHost(clean)
            if (result.isSuccess) {
                val deviceName = result.getOrThrow()
                _hostState.value = HostPairingUiState.Success(deviceName)
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Pairing failed."
                _hostState.value = HostPairingUiState.Error(errorMsg)
            }
        }
    }

    fun resetHostState() {
        _hostState.value = HostPairingUiState.Idle
    }

    fun directPairClientToHost(hostCode: String) {
        val clean = hostCode.trim().uppercase()
        if (clean.length < 4) {
            _clientState.value = PairingState.Error("Please enter a valid Host Channel Code.")
            return
        }
        clientObserveJob?.cancel()
        countdownJob?.cancel()
        viewModelScope.launch {
            _clientState.value = PairingState.Loading
            val result = pairingRepository.directPairClientToHost(clean)
            if (result.isSuccess) {
                _clientState.value = PairingState.Paired(
                    hostUid = clean,
                    clientUid = "",
                    deviceName = "Host ($clean)"
                )
            } else {
                _clientState.value = PairingState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to connect to Host."
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        clientObserveJob?.cancel()
        countdownJob?.cancel()
    }
}
