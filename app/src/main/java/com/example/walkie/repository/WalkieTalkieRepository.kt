package com.example.walkie.repository

import android.util.Log
import com.example.data.remote.FirestoreSource
import com.example.walkie.model.WtIceCandidate
import com.example.walkie.model.WtSession
import com.example.walkie.model.WtSessionStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository layer for Walkie-Talkie P2P sessions.
 * Bridges the ViewModel/Engine with the Firestore signaling source.
 * Reuses the same FirebaseFirestore instance already used by the rest of the app.
 */
class WalkieTalkieRepository(firestore: FirebaseFirestore) {

    companion object {
        private const val TAG = "WalkieTalkieRepo"
    }

    private val signalingSource = WalkieTalkieSignalingSource(firestore)

    /** Generate a fresh session ID for a new call */
    fun generateSessionId(): String = UUID.randomUUID().toString()

    /**
     * Initiator creates the session doc and writes the SDP offer.
     */
    suspend fun initiateCall(
        sessionId: String,
        initiatorUid: String,
        receiverUid: String,
        hostCode: String,
        offerSdp: String
    ): Result<Unit> {
        val session = WtSession(
            sessionId = sessionId,
            initiatorUid = initiatorUid,
            receiverUid = receiverUid,
            hostCode = hostCode,
            status = WtSessionStatus.CALLING,
            offer = offerSdp
        )
        return signalingSource.createSession(session).also {
            if (it.isSuccess) Log.d(TAG, "Call initiated: $sessionId")
        }
    }

    /**
     * Receiver accepts the call and writes the SDP answer.
     */
    suspend fun acceptCall(sessionId: String, answerSdp: String): Result<Unit> {
        return signalingSource.writeAnswer(sessionId, answerSdp).also {
            if (it.isSuccess) Log.d(TAG, "Call accepted: $sessionId")
        }
    }

    /**
     * Add ICE candidate from the initiator side.
     */
    suspend fun addIceCandidateA(sessionId: String, candidate: WtIceCandidate): Result<Unit> =
        signalingSource.addIceCandidateA(sessionId, candidate)

    /**
     * Add ICE candidate from the receiver side.
     */
    suspend fun addIceCandidateB(sessionId: String, candidate: WtIceCandidate): Result<Unit> =
        signalingSource.addIceCandidateB(sessionId, candidate)

    /**
     * End the active call (both sides can call this).
     */
    suspend fun endCall(sessionId: String): Result<Unit> {
        return signalingSource.updateStatus(sessionId, WtSessionStatus.ENDED).also {
            // Schedule document deletion after a short delay in the service
            Log.d(TAG, "Call ended: $sessionId")
        }
    }

    /**
     * Decline an incoming call.
     */
    suspend fun declineCall(sessionId: String): Result<Unit> {
        return signalingSource.updateStatus(sessionId, WtSessionStatus.DECLINED)
    }

    /**
     * Observe real-time updates to a specific session.
     * Used by PeerConnectionEngine to drive the WebRTC state machine.
     */
    fun observeSession(sessionId: String): Flow<WtSession?> =
        signalingSource.observeSession(sessionId)

    /**
     * Observe incoming CALLING sessions for a given receiver UID.
     * Used by WalkieTalkieService background listener.
     */
    fun observeIncomingCalls(receiverUid: String): Flow<List<WtSession>> =
        signalingSource.observeIncomingCalls(receiverUid)

    /**
     * Clean up the Firestore session document after the call ends.
     */
    suspend fun cleanupSession(sessionId: String) {
        signalingSource.deleteSession(sessionId)
    }
}
