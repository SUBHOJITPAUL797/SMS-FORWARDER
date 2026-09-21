package com.example.data.remote

import android.os.Build
import android.util.Log
import com.example.domain.model.CallRecord
import com.example.domain.model.SmsMessage
import com.example.domain.model.UserRole
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreSource(private val firestore: FirebaseFirestore) {

    companion object {
        private const val TAG = "FirestoreSource"

        // Dedicated, isolated collections for SMS Forwarder to ensure clean database organization
        const val COLLECTION_USERS = "sms_forwarder_users"
        const val COLLECTION_PAIRINGS = "sms_forwarder_pairings"
        const val COLLECTION_LINKS = "sms_forwarder_links"
        const val COLLECTION_SMS = "sms_forwarder_messages"
        const val COLLECTION_CALLS = "sms_forwarder_calls"
    }

    suspend fun saveUserProfile(
        uid: String,
        email: String,
        role: UserRole,
        fcmToken: String = "",
        linkedUid: String? = null
    ): Result<Unit> {
        return try {
            val userMap = mutableMapOf<String, Any>(
                "uid" to uid,
                "email" to email,
                "role" to role.key,
                "deviceName" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (fcmToken.isNotEmpty()) userMap["fcmToken"] = fcmToken
            if (linkedUid != null) userMap["linkedUid"] = linkedUid

            firestore.collection(COLLECTION_USERS).document(uid)
                .set(userMap, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile", e)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<Map<String, Any>?> {
        return try {
            val doc = firestore.collection(COLLECTION_USERS).document(uid).get().await()
            Result.success(doc.data)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile", e)
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(uid: String, token: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_USERS).document(uid)
                .update("fcmToken", token)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------- Pairing APIs -----------------

    suspend fun createPairingCode(
        code: String,
        clientUid: String,
        fcmToken: String,
        deviceName: String
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val expiresAt = now + (10 * 60 * 1000) // 10 minutes

            val pairingData = mapOf(
                "code" to code,
                "clientUid" to clientUid,
                "clientFcmToken" to fcmToken,
                "deviceName" to deviceName,
                "createdAt" to now,
                "expiresAt" to expiresAt,
                "status" to "pending",
                "hostUid" to null
            )

            firestore.collection(COLLECTION_PAIRINGS).document(code)
                .set(pairingData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating pairing code", e)
            Result.failure(e)
        }
    }

    fun observePairingDoc(code: String): Flow<Map<String, Any>?> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection(COLLECTION_PAIRINGS).document(code)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing pairing doc", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.data)
            }
        awaitClose { listener.remove() }
    }

    suspend fun pairWithCode(
        code: String,
        hostUid: String
    ): Result<Pair<String, String>> {
        // Returns Pair(clientUid, clientDeviceName)
        return try {
            val pairingRef = firestore.collection(COLLECTION_PAIRINGS).document(code)
            val doc = pairingRef.get().await()

            if (!doc.exists()) {
                return Result.failure(Exception("Invalid pairing code. Please check and try again."))
            }

            val expiresAt = doc.getLong("expiresAt") ?: 0L
            if (System.currentTimeMillis() > expiresAt) {
                return Result.failure(Exception("Pairing code has expired. Please generate a new code on the client device."))
            }

            val clientUid = doc.getString("clientUid")
                ?: return Result.failure(Exception("Corrupt pairing payload."))
            val clientDeviceName = doc.getString("deviceName") ?: "Client Device"

            // 1. Update pairing doc to paired
            pairingRef.update(
                mapOf(
                    "hostUid" to hostUid,
                    "status" to "paired"
                )
            ).await()

            // 2. Create link doc
            val linkId = "${hostUid}__${clientUid}"
            val linkData = mapOf(
                "hostUid" to hostUid,
                "clientUid" to clientUid,
                "clientDeviceName" to clientDeviceName,
                "pairedAt" to FieldValue.serverTimestamp(),
                "active" to true
            )
            firestore.collection(COLLECTION_LINKS).document(linkId)
                .set(linkData, SetOptions.merge())
                .await()

            // 3. Update host user doc with linked clientUid
            firestore.collection(COLLECTION_USERS).document(hostUid)
                .update(
                    mapOf(
                        "role" to UserRole.HOST.key,
                        "linkedUid" to clientUid,
                        "linkedDeviceName" to clientDeviceName
                    )
                ).await()

            // 4. Update client user doc with linked hostUid
            firestore.collection(COLLECTION_USERS).document(clientUid)
                .update(
                    mapOf(
                        "role" to UserRole.CLIENT.key,
                        "linkedUid" to hostUid
                    )
                ).await()

            Result.success(Pair(clientUid, clientDeviceName))
        } catch (e: Exception) {
            Log.e(TAG, "Error completing pairing", e)
            Result.failure(e)
        }
    }

    suspend fun registerDirectLink(
        hostCode: String,
        clientUid: String,
        clientDeviceName: String
    ): Result<Unit> {
        return try {
            val linkId = "${hostCode}__${clientUid}"
            val linkData = mapOf(
                "hostUid" to hostCode,
                "clientUid" to clientUid,
                "clientDeviceName" to clientDeviceName,
                "pairedAt" to FieldValue.serverTimestamp(),
                "active" to true
            )
            firestore.collection(COLLECTION_LINKS).document(linkId)
                .set(linkData, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering direct link", e)
            Result.failure(e)
        }
    }

    fun observeConnectedClients(hostCode: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_LINKS)
            .whereEqualTo("hostUid", hostCode)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing connected clients", error)
                    return@addSnapshotListener
                }
                val clients = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
                trySend(clients)
            }
        awaitClose { listener.remove() }
    }

    suspend fun removePairingDoc(code: String) {
        try {
            firestore.collection(COLLECTION_PAIRINGS).document(code).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clean up pairing code $code", e)
        }
    }

    // ----------------- SMS Forwarding APIs -----------------

    suspend fun uploadSms(hostUid: String, message: SmsMessage): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_SMS)
                .document(hostUid)
                .collection("messages")
                .document(message.messageId)
                .set(message.toMap(), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload SMS to Firestore", e)
            Result.failure(e)
        }
    }

    data class SmsSyncBatch(
        val upserted: List<SmsMessage>,
        val removedIds: List<String>
    )

    fun observeSmsMessages(hostUid: String, limit: Long = 25): Flow<List<SmsMessage>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_SMS)
            .document(hostUid)
            .collection("messages")
            .orderBy("receivedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to SMS collection", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { SmsMessage.fromMap(it) }
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    fun observeSmsBatch(hostUid: String, limit: Long = 25): Flow<SmsSyncBatch> = callbackFlow {
        if (hostUid.isBlank()) {
            trySend(SmsSyncBatch(emptyList(), emptyList()))
            close()
            return@callbackFlow
        }
        val listener = firestore.collection(COLLECTION_SMS)
            .document(hostUid)
            .collection("messages")
            .orderBy("receivedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to SMS batch", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val upserted = mutableListOf<SmsMessage>()
                val removedIds = mutableListOf<String>()

                for (dc in snapshot.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                            val msg = SmsMessage.fromMap(dc.document.data)
                            upserted.add(msg)
                        }
                        DocumentChange.Type.REMOVED -> {
                            removedIds.add(dc.document.id)
                        }
                    }
                }
                trySend(SmsSyncBatch(upserted, removedIds))
            }
        awaitClose { listener.remove() }
    }

    suspend fun fetchOlderSms(hostUid: String, beforeTimestamp: Long, limit: Long = 25): Result<List<SmsMessage>> {
        if (hostUid.isBlank()) return Result.success(emptyList())
        return try {
            val snapshot = firestore.collection(COLLECTION_SMS)
                .document(hostUid)
                .collection("messages")
                .whereLessThan("receivedAt", beforeTimestamp)
                .orderBy("receivedAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val messages = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { SmsMessage.fromMap(it) }
            }
            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching older SMS messages", e)
            Result.failure(e)
        }
    }

    suspend fun markSmsAsRead(hostUid: String, messageId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_SMS)
                .document(hostUid)
                .collection("messages")
                .document(messageId)
                .update("read", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllSmsAsRead(hostUid: String): Result<Unit> {
        return try {
            val unreadDocs = firestore.collection(COLLECTION_SMS)
                .document(hostUid)
                .collection("messages")
                .whereEqualTo("read", false)
                .get()
                .await()

            val batch = firestore.batch()
            for (doc in unreadDocs.documents) {
                batch.update(doc.reference, "read", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSms(hostUid: String, messageId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_SMS)
                .document(hostUid)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting SMS $messageId from Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMultipleSms(hostUid: String, messageIds: List<String>): Result<Unit> {
        return try {
            // Firestore batches support up to 500 operations per batch
            val chunks = messageIds.chunked(450)
            for (chunk in chunks) {
                val batch = firestore.batch()
                for (id in chunk) {
                    val docRef = firestore.collection(COLLECTION_SMS)
                        .document(hostUid)
                        .collection("messages")
                        .document(id)
                    batch.delete(docRef)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting multiple SMS from Firestore", e)
            Result.failure(e)
        }
    }

    // ----------------- Call Forwarding APIs -----------------

    suspend fun uploadCall(hostUid: String, call: CallRecord): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_CALLS)
                .document(hostUid)
                .collection("calls")
                .document(call.callId)
                .set(call.toMap(), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload Call to Firestore", e)
            Result.failure(e)
        }
    }

    data class CallSyncBatch(
        val upserted: List<CallRecord>,
        val removedIds: List<String>
    )

    fun observeCallRecords(hostUid: String, limit: Long = 25): Flow<List<CallRecord>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_CALLS)
            .document(hostUid)
            .collection("calls")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to Calls collection", error)
                    return@addSnapshotListener
                }
                val calls = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { CallRecord.fromMap(it) }
                } ?: emptyList()
                trySend(calls)
            }
        awaitClose { listener.remove() }
    }

    fun observeCallBatch(hostUid: String, limit: Long = 25): Flow<CallSyncBatch> = callbackFlow {
        if (hostUid.isBlank()) {
            trySend(CallSyncBatch(emptyList(), emptyList()))
            close()
            return@callbackFlow
        }
        val listener = firestore.collection(COLLECTION_CALLS)
            .document(hostUid)
            .collection("calls")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to Calls batch", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val upserted = mutableListOf<CallRecord>()
                val removedIds = mutableListOf<String>()

                for (dc in snapshot.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                            val record = CallRecord.fromMap(dc.document.data)
                            upserted.add(record)
                        }
                        DocumentChange.Type.REMOVED -> {
                            removedIds.add(dc.document.id)
                        }
                    }
                }
                trySend(CallSyncBatch(upserted, removedIds))
            }
        awaitClose { listener.remove() }
    }

    suspend fun fetchOlderCalls(hostUid: String, beforeTimestamp: Long, limit: Long = 25): Result<List<CallRecord>> {
        if (hostUid.isBlank()) return Result.success(emptyList())
        return try {
            val snapshot = firestore.collection(COLLECTION_CALLS)
                .document(hostUid)
                .collection("calls")
                .whereLessThan("timestamp", beforeTimestamp)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val calls = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { CallRecord.fromMap(it) }
            }
            Result.success(calls)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching older call records", e)
            Result.failure(e)
        }
    }

    suspend fun markCallAsRead(hostUid: String, callId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_CALLS)
                .document(hostUid)
                .collection("calls")
                .document(callId)
                .update("read", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllCallsAsRead(hostUid: String): Result<Unit> {
        return try {
            val unreadDocs = firestore.collection(COLLECTION_CALLS)
                .document(hostUid)
                .collection("calls")
                .whereEqualTo("read", false)
                .get()
                .await()

            val batch = firestore.batch()
            for (doc in unreadDocs.documents) {
                batch.update(doc.reference, "read", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCall(hostUid: String, callId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_CALLS)
                .document(hostUid)
                .collection("calls")
                .document(callId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting call $callId from Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMultipleCalls(hostUid: String, callIds: List<String>): Result<Unit> {
        return try {
            val chunks = callIds.chunked(450)
            for (chunk in chunks) {
                val batch = firestore.batch()
                for (id in chunk) {
                    val docRef = firestore.collection(COLLECTION_CALLS)
                        .document(hostUid)
                        .collection("calls")
                        .document(id)
                    batch.delete(docRef)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting multiple calls from Firestore", e)
            Result.failure(e)
        }
    }
}
