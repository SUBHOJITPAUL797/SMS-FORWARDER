package com.example.data.remote

import android.os.Build
import android.util.Log
import com.example.domain.model.SmsMessage
import com.example.domain.model.UserRole
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

            firestore.collection("users").document(uid)
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
            val doc = firestore.collection("users").document(uid).get().await()
            Result.success(doc.data)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile", e)
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(uid: String, token: String): Result<Unit> {
        return try {
            firestore.collection("users").document(uid)
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

            firestore.collection("pairings").document(code)
                .set(pairingData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating pairing code", e)
            Result.failure(e)
        }
    }

    fun observePairingDoc(code: String): Flow<Map<String, Any>?> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection("pairings").document(code)
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
            val pairingRef = firestore.collection("pairings").document(code)
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
            firestore.collection("links").document(linkId)
                .set(linkData, SetOptions.merge())
                .await()

            // 3. Update host user doc with linked clientUid
            firestore.collection("users").document(hostUid)
                .update(
                    mapOf(
                        "role" to UserRole.HOST.key,
                        "linkedUid" to clientUid,
                        "linkedDeviceName" to clientDeviceName
                    )
                ).await()

            // 4. Update client user doc with linked hostUid
            firestore.collection("users").document(clientUid)
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
            firestore.collection("links").document(linkId)
                .set(linkData, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering direct link", e)
            Result.failure(e)
        }
    }

    fun observeConnectedClients(hostCode: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = firestore.collection("links")
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
            firestore.collection("pairings").document(code).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clean up pairing code $code", e)
        }
    }

    // ----------------- SMS Forwarding APIs -----------------

    suspend fun uploadSms(hostUid: String, message: SmsMessage): Result<Unit> {
        return try {
            firestore.collection("sms")
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

    fun observeSmsMessages(hostUid: String): Flow<List<SmsMessage>> = callbackFlow {
        val listener = firestore.collection("sms")
            .document(hostUid)
            .collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to SMS collection", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { SmsMessage.fromMap(it) }
                }?.sortedByDescending { it.receivedAt } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markSmsAsRead(hostUid: String, messageId: String): Result<Unit> {
        return try {
            firestore.collection("sms")
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
            val unreadDocs = firestore.collection("sms")
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
}
