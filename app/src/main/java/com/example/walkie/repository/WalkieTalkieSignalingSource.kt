package com.example.walkie.repository

import android.util.Log
import com.example.walkie.model.WtIceCandidate
import com.example.walkie.model.WtSession
import com.example.walkie.model.WtSessionStatus
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore signaling layer for the P2P Walkie-Talkie feature.
 * Manages WtSession documents in the "wt_sessions" collection.
 * NOTE: Only SDP and ICE candidate text (a few KB) passes through Firestore.
 * All audio travels P2P over DTLS-SRTP.
 */
class WalkieTalkieSignalingSource(private val firestore: FirebaseFirestore) {

    companion object {
        private const val TAG = "WtSignalingSource"
        const val COLLECTION_WT_SESSIONS = "sms_forwarder_wt_sessions"
    }

    /**
     * Creates a new session document as the initiator.
     * Status starts as "calling".
     */
    suspend fun createSession(session: WtSession): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_WT_SESSIONS)
                .document(session.sessionId)
                .set(session.toMap())
                .await()
            Log.d(TAG, "Session ${session.sessionId} created")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating session", e)
            Result.failure(e)
        }
    }

    /**
     * Writes the SDP offer to an existing session document.
     */
    suspend fun writeOffer(sessionId: String, offerSdp: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_WT_SESSIONS).document(sessionId)
                .set(mapOf("offer" to offerSdp), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing offer", e)
            Result.failure(e)
        }
    }

    /**
     * Writes the SDP answer (called by the receiver/callee).
     */
    suspend fun writeAnswer(sessionId: String, answerSdp: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_WT_SESSIONS).document(sessionId)
                .set(
                    mapOf(
                        "answer" to answerSdp,
                        "status" to WtSessionStatus.ACTIVE.key
                    ),
                    SetOptions.merge()
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing answer", e)
            Result.failure(e)
        }
    }

    /**
     * Appends an ICE candidate from the initiator (side A).
     */
    suspend fun addIceCandidateA(sessionId: String, candidate: WtIceCandidate): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_WT_SESSIONS).document(sessionId)
                .update("iceCandidatesA", FieldValue.arrayUnion(candidate.toMap()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding ICE candidate A", e)
            Result.failure(e)
        }
    }

    /**
     * Appends an ICE candidate from the receiver (side B).
     */
    suspend fun addIceCandidateB(sessionId: String, candidate: WtIceCandidate): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_WT_SESSIONS).document(sessionId)
                .update("iceCandidatesB", FieldValue.arrayUnion(candidate.toMap()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding ICE candidate B", e)
            Result.failure(e)
        }
    }

    /**
     * Updates the session status (e.g., "ended", "declined").
     */
    suspend fun updateStatus(sessionId: String, status: WtSessionStatus): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_WT_SESSIONS).document(sessionId)
                .set(mapOf("status" to status.key), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating session status", e)
            Result.failure(e)
        }
    }

    /**
     * Observes a specific session document for real-time changes.
     * Used by both initiator (to receive answer/ICE) and receiver (to receive offer/ICE).
     */
    fun observeSession(sessionId: String): Flow<WtSession?> = callbackFlow {
        val listener: ListenerRegistration = firestore
            .collection(COLLECTION_WT_SESSIONS)
            .document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing session $sessionId", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                val data = snapshot?.data
                trySend(data?.let { WtSession.fromMap(it) })
            }
        awaitClose { listener.remove() }
    }

    /**
     * Observes all active CALLING sessions targeting a specific receiver UID.
     * Used by the WalkieTalkieService to detect incoming calls.
     */
    fun observeIncomingCalls(receiverUid: String): Flow<List<WtSession>> = callbackFlow {
        val listener: ListenerRegistration = firestore
            .collection(COLLECTION_WT_SESSIONS)
            .whereEqualTo("receiverUid", receiverUid)
            .whereEqualTo("status", WtSessionStatus.CALLING.key)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing incoming calls", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents
                    ?.mapNotNull { doc -> doc.data?.let { WtSession.fromMap(it) } }
                    ?: emptyList()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Deletes a session document after a call ends.
     */
    suspend fun deleteSession(sessionId: String) {
        try {
            firestore.collection(COLLECTION_WT_SESSIONS).document(sessionId).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete session $sessionId", e)
        }
    }
}
