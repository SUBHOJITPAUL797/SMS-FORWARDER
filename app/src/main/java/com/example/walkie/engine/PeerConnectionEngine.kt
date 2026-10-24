package com.example.walkie.engine

import android.content.Context
import android.util.Log
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/**
 * WebRTC PeerConnection engine for P2P Walkie-Talkie audio communication.
 * Handles:
 * - PeerConnectionFactory initialization
 * - AudioSource and AudioTrack with hardware AEC, NS, and AGC
 * - ICE candidate gathering and STUN server configuration
 * - SDP Offer/Answer negotiation
 * - Half-Duplex Push-to-Talk (PTT) microphone muting/unmuting
 */
class PeerConnectionEngine(
    private val context: Context,
    private val listener: Listener
) {

    interface Listener {
        fun onLocalIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String)
        fun onConnectionStateChange(state: PeerConnection.IceConnectionState)
    }

    companion object {
        private const val TAG = "PeerConnectionEngine"
        private const val AUDIO_TRACK_ID = "wt_audio_track"
        private const val STREAM_ID = "wt_audio_stream"

        @Volatile
        private var isFactoryInitialized = false

        fun initFactoryIfNeeded(context: Context) {
            if (!isFactoryInitialized) {
                synchronized(PeerConnectionEngine::class.java) {
                    if (!isFactoryInitialized) {
                        val initOptions = PeerConnectionFactory.InitializationOptions
                            .builder(context.applicationContext)
                            .setEnableInternalTracer(false)
                            .createInitializationOptions()
                        PeerConnectionFactory.initialize(initOptions)
                        isFactoryInitialized = true
                        Log.d(TAG, "WebRTC PeerConnectionFactory initialized globally")
                    }
                }
            }
        }
    }

    private var factory: PeerConnectionFactory? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var peerConnection: PeerConnection? = null

    init {
        initFactoryIfNeeded(context)
        setupPeerConnection()
    }

    private fun setupPeerConnection() {
        val options = PeerConnectionFactory.Options()
        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()

        // 1. Audio Constraints: Echo cancellation, noise suppression, AGC
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }

        audioSource = factory?.createAudioSource(audioConstraints)
        localAudioTrack = factory?.createAudioTrack(AUDIO_TRACK_ID, audioSource)?.apply {
            // Half-duplex: Default to disabled (mic muted) until PTT is held
            setEnabled(false)
        }

        // 2. Free Google STUN Servers
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = factory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    Log.d(TAG, "Local ICE candidate discovered: ${candidate.sdpMid}")
                    listener.onLocalIceCandidate(
                        candidate.sdpMid ?: "",
                        candidate.sdpMLineIndex,
                        candidate.sdp
                    )
                }
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE Connection State: $newState")
                newState?.let { listener.onConnectionStateChange(it) }
            }

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "ICE Gathering State: $state")
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "Signaling State: $state")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dataChannel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
            override fun onTrack(transceiver: RtpTransceiver?) {
                Log.d(TAG, "Remote audio track attached automatically via WebRTC")
            }
        })

        // 3. Attach local audio track
        localAudioTrack?.let { track ->
            peerConnection?.addTrack(track, listOf(STREAM_ID))
        }
    }

    /**
     * Creates an SDP Offer (Caller/Initiator).
     */
    fun createOffer(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val sdpConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null) {
                    onFailure("SDP offer description was null")
                    return
                }
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        Log.d(TAG, "Local SDP offer set successfully")
                        onSuccess(desc.description)
                    }

                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "Failed to set local SDP offer: $error")
                        onFailure(error ?: "Failed to set local description")
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, desc)
            }

            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Failed to create SDP offer: $error")
                onFailure(error ?: "Failed to create SDP offer")
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, sdpConstraints)
    }

    /**
     * Sets the remote SDP Offer and creates an SDP Answer (Callee/Receiver).
     */
    fun setRemoteOfferAndCreateAnswer(
        offerSdp: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val remoteDesc = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(TAG, "Remote SDP offer set successfully, generating answer...")
                val sdpConstraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
                }

                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answerDesc: SessionDescription?) {
                        if (answerDesc == null) {
                            onFailure("SDP answer description was null")
                            return
                        }
                        peerConnection?.setLocalDescription(object : SdpObserver {
                            override fun onSetSuccess() {
                                Log.d(TAG, "Local SDP answer set successfully")
                                onSuccess(answerDesc.description)
                            }

                            override fun onSetFailure(error: String?) {
                                Log.e(TAG, "Failed to set local SDP answer: $error")
                                onFailure(error ?: "Failed to set local description")
                            }

                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(p0: String?) {}
                        }, answerDesc)
                    }

                    override fun onCreateFailure(error: String?) {
                        Log.e(TAG, "Failed to create SDP answer: $error")
                        onFailure(error ?: "Failed to create answer")
                    }

                    override fun onSetSuccess() {}
                    override fun onSetFailure(p0: String?) {}
                }, sdpConstraints)
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Failed to set remote SDP offer: $error")
                onFailure(error ?: "Failed to set remote offer")
            }

            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, remoteDesc)
    }

    /**
     * Sets the remote SDP Answer on the Initiator side once receiver accepts.
     */
    fun setRemoteAnswer(answerSdp: String, onDone: () -> Unit = {}) {
        val remoteDesc = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(TAG, "Remote SDP answer applied successfully")
                onDone()
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Failed to set remote SDP answer: $error")
            }

            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, remoteDesc)
    }

    /**
     * Adds an ICE candidate received from the remote peer.
     */
    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        peerConnection?.addIceCandidate(candidate)
    }

    /**
     * Enables or disables local microphone transmission.
     * In Half-Duplex Push-to-Talk (PTT), enabled = true while holding button.
     */
    fun setMicEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
        Log.d(TAG, "Local microphone enabled: $enabled")
    }

    /**
     * Disposes all native WebRTC resources cleanly.
     */
    fun dispose() {
        try {
            localAudioTrack?.setEnabled(false)
            localAudioTrack?.dispose()
            audioSource?.dispose()
            peerConnection?.close()
            peerConnection?.dispose()
            factory?.dispose()
            Log.d(TAG, "PeerConnectionEngine disposed cleanly")
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing PeerConnectionEngine", e)
        } finally {
            localAudioTrack = null
            audioSource = null
            peerConnection = null
            factory = null
        }
    }
}
