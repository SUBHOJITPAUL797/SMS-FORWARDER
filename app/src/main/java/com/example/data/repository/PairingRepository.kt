package com.example.data.repository

import android.os.Build
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.remote.AuthSource
import com.example.data.remote.FirestoreSource
import com.example.domain.model.PairingState
import com.example.domain.model.UserRole
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class PairingRepository(
    private val authSource: AuthSource,
    private val firestoreSource: FirestoreSource,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Ambiguity-free alphanumeric
    private val random = SecureRandom()

    fun generateRandomCode(length: Int = 6): String {
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }

    suspend fun startClientPairing(code: String): Result<Unit> {
        val clientUid = preferencesRepository.getOrCreateDeviceUid()
        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            ""
        }
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        return firestoreSource.createPairingCode(
            code = code,
            clientUid = clientUid,
            fcmToken = token,
            deviceName = deviceName
        )
    }

    suspend fun directPairClientToHost(hostCode: String): Result<String> {
        val cleanHostCode = hostCode.trim().uppercase()
        val clientUid = preferencesRepository.getOrCreateDeviceUid()
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        preferencesRepository.setUserRole(UserRole.CLIENT)
        preferencesRepository.setLinkedDevice(cleanHostCode, "Host ($cleanHostCode)")

        // Register link in firestore
        firestoreSource.registerDirectLink(cleanHostCode, clientUid, deviceName)
        return Result.success("Host ($cleanHostCode)")
    }

    fun observeClientPairingState(code: String): Flow<PairingState> = flow {
        emit(PairingState.Loading)
        firestoreSource.observePairingDoc(code).collect { data ->
            if (data == null) {
                // If doc deleted or not found
                return@collect
            }
            val status = data["status"] as? String ?: "pending"
            val expiresAt = (data["expiresAt"] as? Number)?.toLong() ?: 0L
            val hostUid = data["hostUid"] as? String

            if (status == "paired" && !hostUid.isNullOrEmpty()) {
                val clientUid = data["clientUid"] as? String ?: ""
                val deviceName = data["deviceName"] as? String ?: "Client Device"
                // Persist pairing in DataStore
                preferencesRepository.setUserRole(UserRole.CLIENT)
                preferencesRepository.setLinkedDevice(hostUid, "Host Device")
                // Cleanup pairing doc
                firestoreSource.removePairingDoc(code)
                emit(PairingState.Paired(hostUid = hostUid, clientUid = clientUid, deviceName = deviceName))
            } else {
                val remaining = ((expiresAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                emit(PairingState.CodeGenerated(code = code, expiresAt = expiresAt, remainingSeconds = remaining))
            }
        }
    }

    suspend fun pairAsHost(code: String): Result<String> {
        val hostUid = preferencesRepository.getOrCreateDeviceUid()
        val cleanCode = code.trim().uppercase()
        val result = firestoreSource.pairWithCode(cleanCode, hostUid)
        return if (result.isSuccess) {
            val (clientUid, clientDeviceName) = result.getOrThrow()
            preferencesRepository.setUserRole(UserRole.HOST)
            preferencesRepository.setLinkedDevice(clientUid, clientDeviceName)
            Result.success(clientDeviceName)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Pairing failed"))
        }
    }
}
