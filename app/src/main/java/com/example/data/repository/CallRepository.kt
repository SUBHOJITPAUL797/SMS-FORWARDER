package com.example.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.local.AppDatabase
import com.example.data.local.CallQueueEntity
import com.example.data.local.QueueStatus
import com.example.data.remote.AuthSource
import com.example.data.remote.FirestoreSource
import com.example.data.local.HostCallEntity
import com.example.domain.model.CallRecord
import com.example.domain.model.CallType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.LinkedHashMap

class CallRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val firestoreSource: FirestoreSource,
    private val authSource: AuthSource,
    private val preferencesRepository: UserPreferencesRepository
) {
    companion object {
        private const val TAG = "CallRepository"
    }

    private val recentCallCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(50, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > 100
            }
        }
    )

    private val callDao = database.callQueueDao()
    private val hostCallDao = database.hostCallDao()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val hostCallSyncJobs = mutableMapOf<String, Job>()

    val totalQueueCount: Flow<Int> = callDao.getTotalCountFlow()
    val pendingCount: Flow<Int> = callDao.getPendingCountFlow()
    val localQueueCalls: Flow<List<CallQueueEntity>> = callDao.getAllCalls()

    /**
     * Called by PhoneCallReceiver when an incoming/missed/outgoing call event occurs.
     */
    suspend fun handleIncomingCall(
        callId: String,
        phoneNumber: String,
        contactName: String,
        callType: CallType,
        durationSeconds: Int,
        timestamp: Long,
        simSlot: Int
    ): Result<Unit> {
        val dedupKey = "${phoneNumber.trim()}|${callType.name}|${timestamp / 4_000L}"
        val now = System.currentTimeMillis()
        val lastSeen = recentCallCache[dedupKey]

        if (lastSeen != null && (now - lastSeen) < 4_000L) {
            Log.d(TAG, "Duplicate call event ignored by dedup cache: $dedupKey")
            return Result.success(Unit)
        }
        recentCallCache[dedupKey] = now

        // Check if Call Forwarding is enabled by user
        val isEnabled = preferencesRepository.isCallForwardingEnabledFlow.firstOrNull() ?: true
        if (!isEnabled) {
            Log.d(TAG, "Call forwarding is toggled OFF by user. Skipping.")
            return Result.success(Unit)
        }

        val entity = CallQueueEntity(
            callId = callId,
            phoneNumber = phoneNumber,
            contactName = contactName,
            callType = callType.name,
            durationSeconds = durationSeconds,
            timestamp = timestamp,
            simSlot = simSlot,
            status = QueueStatus.PENDING.name,
            retryCount = 0
        )

        // 1. Insert into local Room DB
        callDao.insert(entity)

        // 2. Attempt immediate upload if hostUid is known
        val hostUid = preferencesRepository.linkedUidFlow.firstOrNull()
        val clientUid = preferencesRepository.getOrCreateDeviceUid()
        val clientDeviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

        if (!hostUid.isNullOrEmpty()) {
            val record = CallRecord(
                callId = callId,
                phoneNumber = phoneNumber,
                contactName = contactName,
                callType = callType,
                durationSeconds = durationSeconds,
                timestamp = timestamp,
                uploadedAt = now,
                clientUid = clientUid,
                clientDeviceName = clientDeviceName,
                simSlot = simSlot,
                read = false
            )

            val uploadResult = firestoreSource.uploadCall(hostUid, record)
            if (uploadResult.isSuccess) {
                callDao.updateStatus(callId, QueueStatus.UPLOADED.name)
                Log.i(TAG, "Call $callId successfully uploaded to Host $hostUid")
            } else {
                Log.w(TAG, "Failed to upload call immediately, remains PENDING in queue", uploadResult.exceptionOrNull())
            }
        } else {
            Log.w(TAG, "No linked Host UID found. Call stored in queue.")
        }

        return Result.success(Unit)
    }

    suspend fun syncAllPendingCalls(): Int {
        val hostUid = preferencesRepository.linkedUidFlow.firstOrNull()
        if (hostUid.isNullOrEmpty()) return 0

        val pending = callDao.getCallsByStatus(QueueStatus.PENDING.name)
        if (pending.isEmpty()) return 0

        val clientUid = preferencesRepository.getOrCreateDeviceUid()
        val clientDeviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        var successCount = 0

        for (item in pending) {
            val record = CallRecord(
                callId = item.callId,
                phoneNumber = item.phoneNumber,
                contactName = item.contactName,
                callType = CallType.fromString(item.callType),
                durationSeconds = item.durationSeconds,
                timestamp = item.timestamp,
                uploadedAt = System.currentTimeMillis(),
                clientUid = clientUid,
                clientDeviceName = clientDeviceName,
                simSlot = item.simSlot,
                read = false
            )

            val res = firestoreSource.uploadCall(hostUid, record)
            if (res.isSuccess) {
                callDao.updateStatus(item.callId, QueueStatus.UPLOADED.name)
                successCount++
            } else {
                callDao.incrementRetryCount(item.callId)
            }
        }

        Log.i(TAG, "Sync complete: $successCount / ${pending.size} calls uploaded")
        return successCount
    }

    // ----------------- Host Observation APIs -----------------

    fun startHostSync(hostCode: String) {
        if (hostCode.isBlank()) return
        synchronized(hostCallSyncJobs) {
            hostCallSyncJobs.keys.filter { it != hostCode }.forEach { oldCode ->
                hostCallSyncJobs.remove(oldCode)?.cancel()
            }
            if (hostCallSyncJobs[hostCode]?.isActive == true) return
            hostCallSyncJobs[hostCode] = repoScope.launch {
                try {
                    firestoreSource.observeCallBatch(hostCode, limit = 25).collect { batch ->
                        if (batch.upserted.isNotEmpty()) {
                            val entities = batch.upserted.map { HostCallEntity.fromCallRecord(hostCode, it) }
                            hostCallDao.insertAll(entities)
                        }
                        if (batch.removedIds.isNotEmpty()) {
                            hostCallDao.deleteMultiple(batch.removedIds)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in host call live sync", e)
                }
            }
        }
    }

    fun observeHostCallList(hostUid: String): Flow<List<CallRecord>> {
        startHostSync(hostUid)
        return hostCallDao.observeCalls(hostUid).map { entities ->
            entities.map { it.toCallRecord() }
        }
    }

    suspend fun loadOlderCallsFromCloud(hostCode: String, pageSize: Long = 25): Result<Int> {
        return try {
            val oldestTimestamp = hostCallDao.getOldestTimestamp(hostCode) ?: System.currentTimeMillis()
            val fetchResult = firestoreSource.fetchOlderCalls(hostCode, beforeTimestamp = oldestTimestamp, limit = pageSize)
            if (fetchResult.isFailure) {
                return Result.failure(fetchResult.exceptionOrNull() ?: Exception("Failed to fetch older calls"))
            }
            val calls = fetchResult.getOrNull() ?: emptyList()
            if (calls.isNotEmpty()) {
                val entities = calls.map { HostCallEntity.fromCallRecord(hostCode, it) }
                hostCallDao.insertAll(entities)
            }
            Result.success(calls.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading older calls from cloud", e)
            Result.failure(e)
        }
    }

    suspend fun markCallAsRead(hostUid: String, callId: String): Result<Unit> {
        hostCallDao.markAsRead(callId)
        return firestoreSource.markCallAsRead(hostUid, callId)
    }

    suspend fun markAllCallsAsRead(hostUid: String): Result<Unit> {
        hostCallDao.markAllAsRead(hostUid)
        return firestoreSource.markAllCallsAsRead(hostUid)
    }

    suspend fun deleteCall(hostUid: String, callId: String): Result<Unit> {
        // Delete from local Room cache immediately
        hostCallDao.deleteById(callId)
        // Delete from local client queue if present
        callDao.deleteByCallId(callId)
        // Delete from Firestore cloud DB
        return firestoreSource.deleteCall(hostUid, callId)
    }

    suspend fun deleteMultipleCalls(hostUid: String, callIds: List<String>): Result<Unit> {
        // Delete from local Room cache immediately
        hostCallDao.deleteMultiple(callIds)
        // Delete from local client queue if present
        callDao.deleteMultiple(callIds)
        // Delete from Firestore cloud DB
        return firestoreSource.deleteMultipleCalls(hostUid, callIds)
    }
}
