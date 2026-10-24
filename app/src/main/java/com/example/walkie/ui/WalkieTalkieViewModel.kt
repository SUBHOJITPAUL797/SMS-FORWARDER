package com.example.walkie.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmsBridgeApp
import com.example.walkie.model.WtSession
import com.example.walkie.model.WtSessionStatus
import com.example.walkie.repository.WalkieTalkieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Walkie-Talkie screen.
 * Phase 1: Holds UI state placeholders and permission state.
 * Phase 2: Will drive PeerConnectionEngine via WalkieTalkieRepository.
 * Phase 3: Will handle full PTT call flow (initiate, answer, end, PTT button).
 */
class WalkieTalkieViewModel(
    private val wtRepository: WalkieTalkieRepository =
        SmsBridgeApp.instance.walkieTalkieRepository
) : ViewModel() {

    // ── Session State ──────────────────────────────────────────
    private val _activeSession = MutableStateFlow<WtSession?>(null)
    val activeSession: StateFlow<WtSession?> = _activeSession.asStateFlow()

    private val _callState = MutableStateFlow(WtCallState.IDLE)
    val callState: StateFlow<WtCallState> = _callState.asStateFlow()

    // ── Permission State ───────────────────────────────────────
    private val _hasMicPermission = MutableStateFlow(false)
    val hasMicPermission: StateFlow<Boolean> = _hasMicPermission.asStateFlow()

    // ── PTT State ──────────────────────────────────────────────
    private val _isTalking = MutableStateFlow(false)
    val isTalking: StateFlow<Boolean> = _isTalking.asStateFlow()

    // ── Peer Info ──────────────────────────────────────────────
    private val _peerName = MutableStateFlow("No device connected")
    val peerName: StateFlow<String> = _peerName.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // ─────────────────────────────────────────────────────────────
    // Public Actions
    // ─────────────────────────────────────────────────────────────

    fun onMicPermissionResult(granted: Boolean) {
        _hasMicPermission.value = granted
        if (!granted) {
            _statusMessage.value = "Microphone permission required for Walkie-Talkie."
        }
    }

    /** PTT button pressed — enable microphone transmission */
    fun onPttDown() {
        if (_callState.value != WtCallState.ACTIVE) return
        _isTalking.value = true
        // Phase 2: peerConnectionEngine.setLocalAudioEnabled(true)
    }

    /** PTT button released — mute microphone */
    fun onPttUp() {
        _isTalking.value = false
        // Phase 2: peerConnectionEngine.setLocalAudioEnabled(false)
    }

    /** Initiates an outgoing call to the paired device */
    fun initiateCall() {
        // Phase 2/3: Full implementation
        _callState.value = WtCallState.CALLING
        _statusMessage.value = "Calling paired device…"
    }

    /** Answers an incoming call */
    fun answerCall(sessionId: String) {
        // Phase 2/3: Full implementation
        _callState.value = WtCallState.ACTIVE
        _statusMessage.value = "Connected"
    }

    /** Ends or declines the current call */
    fun endCall() {
        val session = _activeSession.value ?: return
        viewModelScope.launch {
            wtRepository.endCall(session.sessionId)
            _callState.value = WtCallState.IDLE
            _activeSession.value = null
            _isTalking.value = false
            _statusMessage.value = "Call ended"
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_isTalking.value) onPttUp()
    }
}

/** Represents the UI call lifecycle state */
enum class WtCallState {
    IDLE,
    CALLING,     // Outgoing — offer sent, waiting for answer
    RINGING,     // Incoming — offer received, waiting for user to answer
    ACTIVE,      // Both devices connected (DTLS-SRTP established)
    ENDED
}
