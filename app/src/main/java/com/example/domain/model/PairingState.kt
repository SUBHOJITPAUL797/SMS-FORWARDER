package com.example.domain.model

sealed class PairingState {
    object Idle : PairingState()
    object Loading : PairingState()
    data class CodeGenerated(
        val code: String,
        val expiresAt: Long,
        val remainingSeconds: Long
    ) : PairingState()
    data class Paired(
        val hostUid: String,
        val clientUid: String,
        val deviceName: String
    ) : PairingState()
    data class Error(val message: String) : PairingState()
}
