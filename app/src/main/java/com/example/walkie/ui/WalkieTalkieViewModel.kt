package com.example.walkie.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmsBridgeApp
import com.example.data.datastore.UserPreferencesRepository
import com.example.domain.model.UserRole
import com.example.walkie.audio.AudioFocusManager
import com.example.walkie.audio.ConsentRecorder
import com.example.walkie.engine.PeerConnectionEngine
import com.example.walkie.model.WtIceCandidate
import com.example.walkie.model.WtSession
import com.example.walkie.model.WtSessionStatus
import com.example.walkie.repository.WalkieTalkieRepository
import com.example.walkie.service.WalkieTalkieService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection

/**
 * Production-ready ViewModel driving the P2P Walkie-Talkie feature.
 * Connects WebRTC PeerConnectionEngine, AudioFocusManager, ConsentRecorder,
 * and Firebase Firestore real-time signaling.
 */
class WalkieTalkieViewModel(
    private val wtRepository: WalkieTalkieRepository = SmsBridgeApp.instance.walkieTalkieRepository,
    private val preferencesRepository: UserPreferencesRepository = SmsBridgeApp.instance.preferencesRepository,
    private val context: Context = SmsBridgeApp.instance
) : ViewModel() {

    companion object {
        private const val TAG = "WalkieTalkieVM"
    }

    // ── Audio & WebRTC Helpers ─────────────────────────────────
    private val audioFocusManager = AudioFocusManager(context)
    private val consentRecorder = ConsentRecorder(context)
    private var engine: PeerConnectionEngine? = null

    // ── Session State ──────────────────────────────────────────
    private val _activeSession = MutableStateFlow<WtSession?>(null)
    val activeSession: StateFlow<WtSession?> = _activeSession.asStateFlow()

    private val _callState = MutableStateFlow(WtCallState.IDLE)
    val callState: StateFlow<WtCallState> = _callState.asStateFlow()

    // ── Permissions & PTT State ────────────────────────────────
    private val _hasMicPermission = MutableStateFlow(false)
    val hasMicPermission: StateFlow<Boolean> = _hasMicPermission.asStateFlow()

    private val _isTalking = MutableStateFlow(false)
    val isTalking: StateFlow<Boolean> = _isTalking.asStateFlow()

    private val _isFullDuplex = MutableStateFlow(false)
    val isFullDuplex: StateFlow<Boolean> = _isFullDuplex.asStateFlow()

    val isRecording: StateFlow<Boolean> = consentRecorder.isRecordingFlow

    // ── Pairing & Peer Info ────────────────────────────────────
    private val smsRepository: com.example.data.repository.SmsRepository = SmsBridgeApp.instance.smsRepository
    private val _connectedDevices = MutableStateFlow<List<com.example.domain.model.ConnectedDevice>>(emptyList())
    val connectedDevices: StateFlow<List<com.example.domain.model.ConnectedDevice>> = _connectedDevices.asStateFlow()

    private val _selectedTargetDevice = MutableStateFlow<com.example.domain.model.ConnectedDevice?>(null)
    val selectedTargetDevice: StateFlow<com.example.domain.model.ConnectedDevice?> = _selectedTargetDevice.asStateFlow()

    private val _peerName = MutableStateFlow("Paired Device")
    val peerName: StateFlow<String> = _peerName.asStateFlow()

    private val _hostCode = MutableStateFlow("")
    val hostCode: StateFlow<String> = _hostCode.asStateFlow()

    private val _myUid = MutableStateFlow("")
    val myUid: StateFlow<String> = _myUid.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready for walkie-talkie communication")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun selectTargetDevice(device: com.example.domain.model.ConnectedDevice?) {
        _selectedTargetDevice.value = device
        _peerName.value = device?.clientDeviceName ?: "All Connected Devices"
        _statusMessage.value = if (device != null) "Targeting ${device.clientDeviceName}" else "Broadcasting to all paired devices"
    }

    // Active session observation jobs
    private var sessionObserverJob: Job? = null
    private var incomingCallObserverJob: Job? = null
    private var reconnectTimeoutJob: Job? = null
    private var endedResetJob: Job? = null
    private val processedCandidates = mutableSetOf<String>()

    init {
        loadDeviceIdentityAndListen()
    }

    private fun loadDeviceIdentityAndListen() {
        viewModelScope.launch {
            val uid = preferencesRepository.getOrCreateDeviceUid()
            _myUid.value = uid

            // Determine shared pairing code
            val role = preferencesRepository.userRoleFlow.firstOrNull() ?: UserRole.HOST
            val code = if (role == UserRole.HOST) {
                preferencesRepository.getOrCreateHostCode()
            } else {
                preferencesRepository.linkedUidFlow.firstOrNull() ?: preferencesRepository.getOrCreateHostCode()
            }
            _hostCode.value = code

            val linkedName = preferencesRepository.linkedDeviceNameFlow.firstOrNull() ?: "Paired Device"
            _peerName.value = linkedName

            if (role == UserRole.HOST && code.isNotBlank()) {
                launch {
                    smsRepository.observeConnectedDevices(code).collect { devices ->
                        val active = devices.filter { it.active }
                        _connectedDevices.value = active
                        if (_selectedTargetDevice.value == null && active.isNotEmpty()) {
                            if (active.size == 1) {
                                _selectedTargetDevice.value = active.first()
                                _peerName.value = active.first().clientDeviceName
                            } else {
                                _peerName.value = "${active.size} Devices (All Broadcast)"
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "Initialized: UID=$uid, HostCode=$code, Peer=$linkedName")
            startListeningForIncomingCalls(code, uid)
        }
    }

    private fun startListeningForIncomingCalls(code: String, uid: String) {
        incomingCallObserverJob?.cancel()
        incomingCallObserverJob = viewModelScope.launch {
            wtRepository.observeIncomingCallsForHostCode(code, uid).collect { calls ->
                // If we are IDLE and an incoming call is present
                if (_callState.value == WtCallState.IDLE && calls.isNotEmpty()) {
                    endedResetJob?.cancel()
                    val incoming = calls.first()
                    _activeSession.value = incoming
                    _callState.value = WtCallState.RINGING
                    _statusMessage.value = "Incoming Call from ${_peerName.value}…"
                    WalkieTalkieService.showIncomingCallNotification(context, incoming.sessionId, _peerName.value)
                    Log.d(TAG, "Incoming call received: ${incoming.sessionId}")
                } else if (_callState.value == WtCallState.RINGING) {
                    val activeId = _activeSession.value?.sessionId
                    if (activeId == null || calls.none { it.sessionId == activeId }) {
                        // Caller cancelled before callee answered
                        WalkieTalkieService.cancelIncomingCallNotification(context)
                        _activeSession.value = null
                        _callState.value = WtCallState.IDLE
                        _statusMessage.value = "Call cancelled"
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Call Lifecycle Actions
    // ─────────────────────────────────────────────────────────────

    fun onMicPermissionResult(granted: Boolean) {
        _hasMicPermission.value = granted
        if (!granted) {
            _statusMessage.value = "Microphone permission required for Walkie-Talkie."
        }
    }

    private fun handleIceConnectionState(state: PeerConnection.IceConnectionState) {
        when (state) {
            PeerConnection.IceConnectionState.CONNECTED,
            PeerConnection.IceConnectionState.COMPLETED -> {
                reconnectTimeoutJob?.cancel()
                reconnectTimeoutJob = null
                _callState.value = WtCallState.ACTIVE
                _statusMessage.value = "Connected • P2P Audio Active"
            }
            PeerConnection.IceConnectionState.DISCONNECTED -> {
                _statusMessage.value = "Connection interrupted • Reconnecting…"
                if (reconnectTimeoutJob?.isActive != true) {
                    reconnectTimeoutJob = viewModelScope.launch {
                        delay(5000L)
                        if (_callState.value == WtCallState.ACTIVE || _callState.value == WtCallState.CALLING) {
                            _statusMessage.value = "Connection lost (timed out)"
                            endCall()
                        }
                    }
                }
            }
            PeerConnection.IceConnectionState.FAILED,
            PeerConnection.IceConnectionState.CLOSED -> {
                reconnectTimeoutJob?.cancel()
                reconnectTimeoutJob = null
                _statusMessage.value = "Connection lost: $state"
                if (_callState.value == WtCallState.ACTIVE || _callState.value == WtCallState.CALLING) {
                    endCall()
                }
            }
            else -> {}
        }
    }

    /**
     * Initiates an outgoing call to the paired device via WebRTC.
     */
    fun initiateCall() {
        if (_callState.value != WtCallState.IDLE && _callState.value != WtCallState.ENDED) {
            return
        }
        if (!_hasMicPermission.value) {
            _statusMessage.value = "Cannot call: Microphone permission not granted."
            return
        }
        val code = _hostCode.value
        if (code.isBlank()) {
            _statusMessage.value = "Cannot call: Device is not paired yet."
            return
        }

        endedResetJob?.cancel()
        reconnectTimeoutJob?.cancel()
        val sessionId = wtRepository.generateSessionId()
        val uid = _myUid.value
        _callState.value = WtCallState.CALLING
        _statusMessage.value = "Connecting to ${_peerName.value}…"
        processedCandidates.clear()

        // Acquire audio focus and route to speaker
        audioFocusManager.acquireFocusAndRouteToSpeaker {
            Log.w(TAG, "Audio focus interrupted during call")
        }

        // Start foreground service
        try {
            context.startService(WalkieTalkieService.startIntent(context, sessionId, _peerName.value))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WalkieTalkieService", e)
        }

        // Initialize PeerConnectionEngine
        engine = PeerConnectionEngine(context, object : PeerConnectionEngine.Listener {
            override fun onLocalIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
                viewModelScope.launch {
                    val cand = WtIceCandidate(sdpMid = sdpMid, sdpMLineIndex = sdpMLineIndex, sdp = sdp)
                    wtRepository.addIceCandidateA(sessionId, cand)
                }
            }

            override fun onConnectionStateChange(state: PeerConnection.IceConnectionState) {
                viewModelScope.launch {
                    handleIceConnectionState(state)
                }
            }
        })

        // Create SDP Offer
        engine?.createOffer(
            onSuccess = { offerSdp ->
                viewModelScope.launch {
                    val result = wtRepository.initiateCall(
                        sessionId = sessionId,
                        initiatorUid = uid,
                        receiverUid = "paired_device",
                        hostCode = code,
                        offerSdp = offerSdp
                    )
                    if (result.isSuccess) {
                        _activeSession.value = WtSession(
                            sessionId = sessionId,
                            initiatorUid = uid,
                            receiverUid = "paired_device",
                            hostCode = code,
                            status = WtSessionStatus.CALLING,
                            offer = offerSdp
                        )
                        observeActiveSession(sessionId, isInitiator = true)
                    } else {
                        _statusMessage.value = "Call initiation failed: ${result.exceptionOrNull()?.message}"
                        endCall()
                    }
                }
            },
            onFailure = { error ->
                _statusMessage.value = "WebRTC offer creation failed: $error"
                endCall()
            }
        )
    }

    /**
     * Answers an incoming call.
     */
    fun answerCall(sessionId: String) {
        if (_callState.value != WtCallState.RINGING && _callState.value != WtCallState.IDLE) {
            return
        }
        val session = _activeSession.value ?: return
        if (session.offer.isBlank()) {
            _statusMessage.value = "Cannot answer: Missing SDP offer."
            return
        }

        endedResetJob?.cancel()
        reconnectTimeoutJob?.cancel()
        WalkieTalkieService.cancelIncomingCallNotification(context)
        _callState.value = WtCallState.CALLING
        _statusMessage.value = "Connecting P2P audio…"
        processedCandidates.clear()

        // Acquire audio focus and route to speaker
        audioFocusManager.acquireFocusAndRouteToSpeaker()

        // Start foreground service
        try {
            context.startService(WalkieTalkieService.startIntent(context, sessionId, _peerName.value))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WalkieTalkieService", e)
        }

        // Initialize PeerConnectionEngine as receiver
        engine = PeerConnectionEngine(context, object : PeerConnectionEngine.Listener {
            override fun onLocalIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
                viewModelScope.launch {
                    val cand = WtIceCandidate(sdpMid = sdpMid, sdpMLineIndex = sdpMLineIndex, sdp = sdp)
                    wtRepository.addIceCandidateB(sessionId, cand)
                }
            }

            override fun onConnectionStateChange(state: PeerConnection.IceConnectionState) {
                viewModelScope.launch {
                    handleIceConnectionState(state)
                }
            }
        })

        // Apply remote offer and generate answer
        engine?.setRemoteOfferAndCreateAnswer(
            offerSdp = session.offer,
            onSuccess = { answerSdp ->
                viewModelScope.launch {
                    wtRepository.acceptCall(sessionId, answerSdp)
                    observeActiveSession(sessionId, isInitiator = false)
                }
            },
            onFailure = { error ->
                _statusMessage.value = "Answer generation failed: $error"
                endCall()
            }
        )
    }

    private fun observeActiveSession(sessionId: String, isInitiator: Boolean) {
        sessionObserverJob?.cancel()
        sessionObserverJob = viewModelScope.launch {
            wtRepository.observeSession(sessionId).collect { session ->
                if (session == null || session.status == WtSessionStatus.ENDED || session.status == WtSessionStatus.DECLINED) {
                    val endReason = if (session?.status == WtSessionStatus.DECLINED) "Call declined" else "Call ended"
                    cleanUpEngineAndAudio()
                    transitionToEndedThenIdle(endReason)
                    return@collect
                }

                _activeSession.value = session

                if (isInitiator) {
                    // Initiator reads answer from receiver
                    if (session.status == WtSessionStatus.ACTIVE && session.answer.isNotBlank() && _callState.value == WtCallState.CALLING) {
                        engine?.setRemoteAnswer(session.answer) {
                            _statusMessage.value = "SDP connected • Negotiating audio…"
                        }
                    }

                    // Initiator processes receiver's ICE candidates (iceCandidatesB)
                    for (cand in session.iceCandidatesB) {
                        val key = "${cand.sdpMid}_${cand.sdpMLineIndex}_${cand.sdp}"
                        if (processedCandidates.add(key)) {
                            engine?.addRemoteIceCandidate(cand.sdpMid, cand.sdpMLineIndex, cand.sdp)
                        }
                    }
                } else {
                    // Receiver processes initiator's ICE candidates (iceCandidatesA)
                    for (cand in session.iceCandidatesA) {
                        val key = "${cand.sdpMid}_${cand.sdpMLineIndex}_${cand.sdp}"
                        if (processedCandidates.add(key)) {
                            engine?.addRemoteIceCandidate(cand.sdpMid, cand.sdpMLineIndex, cand.sdp)
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Push-to-Talk (PTT) Audio Controls
    // ─────────────────────────────────────────────────────────────

    /**
     * PTT button held down — enable microphone transmission.
     */
    fun onPttDown() {
        if (_callState.value != WtCallState.ACTIVE) return
        if (!_isFullDuplex.value) {
            engine?.setMicEnabled(true)
            _isTalking.value = true
        }
    }

    /**
     * PTT button released — mute microphone.
     */
    fun onPttUp() {
        if (!_isFullDuplex.value) {
            engine?.setMicEnabled(false)
            _isTalking.value = false
        }
    }

    /**
     * Toggle Full-Duplex (two-way handsfree) vs Half-Duplex (Push-To-Talk).
     */
    fun toggleFullDuplex() {
        if (_callState.value != WtCallState.ACTIVE) return
        val next = !_isFullDuplex.value
        _isFullDuplex.value = next
        if (next) {
            engine?.setMicEnabled(true)
            _isTalking.value = true
        } else {
            engine?.setMicEnabled(false)
            _isTalking.value = false
        }
    }

    /**
     * Consent-based local audio recording toggle.
     */
    fun toggleRecording() {
        val session = _activeSession.value
        if (consentRecorder.isRecording) {
            consentRecorder.stopRecording()
            _statusMessage.value = "Recording saved locally"
        } else {
            val id = session?.sessionId ?: "local"
            val file = consentRecorder.startRecording(id)
            if (file != null) {
                _statusMessage.value = "Recording active (${file.name})"
            }
        }
    }

    /**
     * Ends or declines the current call.
     */
    fun endCall() {
        if (_callState.value == WtCallState.IDLE || _callState.value == WtCallState.ENDED) return
        val session = _activeSession.value
        cleanUpEngineAndAudio()
        transitionToEndedThenIdle("Call ended")
        if (session != null) {
            viewModelScope.launch {
                wtRepository.endCall(session.sessionId)
                wtRepository.cleanupSession(session.sessionId)
            }
        }
    }

    private fun transitionToEndedThenIdle(reason: String) {
        _activeSession.value = null
        _isTalking.value = false
        _isFullDuplex.value = false
        _callState.value = WtCallState.ENDED
        _statusMessage.value = reason
        endedResetJob?.cancel()
        endedResetJob = viewModelScope.launch {
            delay(1000L)
            if (_callState.value == WtCallState.ENDED) {
                _callState.value = WtCallState.IDLE
                _statusMessage.value = "Ready for walkie-talkie communication"
            }
        }
    }

    private fun cleanUpEngineAndAudio() {
        reconnectTimeoutJob?.cancel()
        reconnectTimeoutJob = null
        sessionObserverJob?.cancel()
        sessionObserverJob = null
        engine?.dispose()
        engine = null
        audioFocusManager.releaseFocus()
        if (consentRecorder.isRecording) {
            consentRecorder.stopRecording()
        }
        WalkieTalkieService.cancelIncomingCallNotification(context)
        try {
            context.startService(WalkieTalkieService.stopIntent(context))
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping WalkieTalkieService", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        endedResetJob?.cancel()
        cleanUpEngineAndAudio()
        incomingCallObserverJob?.cancel()
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
