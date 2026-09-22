package com.example.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CallLog
import android.util.Log
import androidx.core.content.ContextCompat
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
    val uploadedCount: Flow<Int> = callDao.getUploadedCountFlow()
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

    private fun mapCallType(typeInt: Int): CallType {
        return when (typeInt) {
            CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
            CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
            CallLog.Calls.MISSED_TYPE -> CallType.MISSED
            CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
            else -> CallType.MISSED
        }
    }

    fun generateCallId(phoneNumber: String, timestamp: Long, type: CallType): String {
        val cleanNum = phoneNumber.filter { it.isDigit() }.takeLast(8)
        val timeBucket = timestamp / 5_000L
        val hash = Math.abs("${cleanNum}_${type.name}_$timeBucket".hashCode())
        return "call_${timeBucket}_$hash"
    }

    /**
     * Scans real call log history from Android's CallLog ContentProvider and forwards to the paired Host.
     */
    suspend fun syncRealDeviceCallLog(scope: InboxSyncScope = InboxSyncScope.ALL_TIME): Result<SyncResult> {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                return Result.failure(SecurityException("READ_CALL_LOG permission not granted"))
            }

            val contentResolver = context.contentResolver
            val uri = CallLog.Calls.CONTENT_URI
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION,
                CallLog.Calls.DATE
            )

            var selection: String? = null
            var selectionArgs: Array<String>? = null
            if (scope.daysLimit != null) {
                val cutoff = System.currentTimeMillis() - (scope.daysLimit * 24 * 3600 * 1000L)
                selection = "${CallLog.Calls.DATE} >= ?"
                selectionArgs = arrayOf(cutoff.toString())
            }

            val cursor = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC"
            )

            var totalFound = 0
            var newImported = 0
            var alreadyExisted = 0
            val pendingEntities = mutableListOf<CallQueueEntity>()
            val hostUid = preferencesRepository.linkedUidFlow.firstOrNull()
            val clientUid = preferencesRepository.getOrCreateDeviceUid()
            val clientDeviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

            cursor?.use { c ->
                val numberCol = c.getColumnIndex(CallLog.Calls.NUMBER)
                val nameCol = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeCol = c.getColumnIndex(CallLog.Calls.TYPE)
                val durationCol = c.getColumnIndex(CallLog.Calls.DURATION)
                val dateCol = c.getColumnIndex(CallLog.Calls.DATE)

                while (c.moveToNext()) {
                    if (scope.maxCount != null && totalFound >= scope.maxCount) {
                        break
                    }
                    totalFound++

                    val number = if (numberCol >= 0) c.getString(numberCol) ?: "Unknown" else "Unknown"
                    var name = if (nameCol >= 0) c.getString(nameCol) ?: "" else ""
                    if (name.isBlank() && number != "Unknown") {
                        name = com.example.util.ContactUtils.resolveContactName(context, number)
                    }
                    val typeInt = if (typeCol >= 0) c.getInt(typeCol) else CallLog.Calls.MISSED_TYPE
                    val duration = if (durationCol >= 0) c.getInt(durationCol) else 0
                    val date = if (dateCol >= 0) c.getLong(dateCol) else System.currentTimeMillis()

                    val callType = mapCallType(typeInt)
                    val callId = generateCallId(number, date, callType)

                    val existing = callDao.getByCallId(callId)
                    if (existing == null) {
                        val entity = CallQueueEntity(
                            callId = callId,
                            phoneNumber = number,
                            contactName = name,
                            callType = callType.name,
                            durationSeconds = duration,
                            timestamp = date,
                            simSlot = 1,
                            status = QueueStatus.PENDING.name,
                            retryCount = 0
                        )
                        pendingEntities.add(entity)
                        newImported++
                    } else if (existing.status == QueueStatus.PENDING.name) {
                        pendingEntities.add(existing)
                    } else {
                        alreadyExisted++
                    }
                }
            }

            // Also pick up any previous PENDING calls that may have failed before
            val priorPending = callDao.getCallsByStatus(QueueStatus.PENDING.name)
            for (p in priorPending) {
                if (pendingEntities.none { it.callId == p.callId }) {
                    pendingEntities.add(p)
                }
            }

            if (pendingEntities.isNotEmpty()) {
                callDao.insertAll(pendingEntities)
            }

            // Immediately batch-upload to Firestore if host is linked
            if (!hostUid.isNullOrEmpty() && pendingEntities.isNotEmpty()) {
                val chunks = pendingEntities.chunked(25)
                for (chunk in chunks) {
                    for (entity in chunk) {
                        val record = CallRecord(
                            callId = entity.callId,
                            phoneNumber = entity.phoneNumber,
                            contactName = entity.contactName,
                            callType = CallType.fromString(entity.callType),
                            durationSeconds = entity.durationSeconds,
                            timestamp = entity.timestamp,
                            uploadedAt = System.currentTimeMillis(),
                            clientUid = clientUid,
                            clientDeviceName = clientDeviceName,
                            simSlot = entity.simSlot,
                            read = false
                        )
                        val uploadRes = firestoreSource.uploadCall(hostUid, record)
                        if (uploadRes.isSuccess) {
                            callDao.updateStatus(entity.callId, QueueStatus.UPLOADED.name)
                        }
                    }
                    kotlinx.coroutines.delay(20)
                }
            }

            Result.success(SyncResult(totalFound, newImported, alreadyExisted))
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing real device call log", e)
            Result.failure(e)
        }
    }

    /**
     * Syncs latest N calls from CallLog (used by CallLogObserver and PhoneCallReceiver).
     */
    suspend fun syncLatestRecentCalls(limit: Int = 5): Int {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return 0
        }
        val hostUid = preferencesRepository.linkedUidFlow.firstOrNull() ?: return 0
        val clientUid = preferencesRepository.getOrCreateDeviceUid()
        val clientDeviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

        return try {
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION,
                CallLog.Calls.DATE
            )
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )
            var count = 0
            cursor?.use { c ->
                val numberCol = c.getColumnIndex(CallLog.Calls.NUMBER)
                val nameCol = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeCol = c.getColumnIndex(CallLog.Calls.TYPE)
                val durationCol = c.getColumnIndex(CallLog.Calls.DURATION)
                val dateCol = c.getColumnIndex(CallLog.Calls.DATE)

                while (c.moveToNext() && count < limit) {
                    val number = if (numberCol >= 0) c.getString(numberCol) ?: "Unknown" else "Unknown"
                    var name = if (nameCol >= 0) c.getString(nameCol) ?: "" else ""
                    if (name.isBlank() && number != "Unknown") {
                        name = com.example.util.ContactUtils.resolveContactName(context, number)
                    }
                    val typeInt = if (typeCol >= 0) c.getInt(typeCol) else CallLog.Calls.MISSED_TYPE
                    val duration = if (durationCol >= 0) c.getInt(durationCol) else 0
                    val date = if (dateCol >= 0) c.getLong(dateCol) else System.currentTimeMillis()

                    val callType = mapCallType(typeInt)
                    val callId = generateCallId(number, date, callType)

                    val existing = callDao.getByCallId(callId)
                    if (existing == null) {
                        val entity = CallQueueEntity(
                            callId = callId,
                            phoneNumber = number,
                            contactName = name,
                            callType = callType.name,
                            durationSeconds = duration,
                            timestamp = date,
                            simSlot = 1,
                            status = QueueStatus.PENDING.name,
                            retryCount = 0
                        )
                        callDao.insert(entity)

                        val record = CallRecord(
                            callId = callId,
                            phoneNumber = number,
                            contactName = name,
                            callType = callType,
                            durationSeconds = duration,
                            timestamp = date,
                            uploadedAt = System.currentTimeMillis(),
                            clientUid = clientUid,
                            clientDeviceName = clientDeviceName,
                            simSlot = 1,
                            read = false
                        )
                        val res = firestoreSource.uploadCall(hostUid, record)
                        if (res.isSuccess) {
                            callDao.updateStatus(callId, QueueStatus.UPLOADED.name)
                        }
                        count++
                    }
                }
            }
            count
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncLatestRecentCalls", e)
            0
        }
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
        val call = hostCallDao.getById(callId)
        val idsToDelete = mutableSetOf(callId)
        if (call != null) {
            val minTs = call.timestamp - 6_000L
            val maxTs = call.timestamp + 6_000L
            val duplicates = hostCallDao.findMatchingCallIds(call.hostCode, call.phoneNumber, minTs, maxTs)
            idsToDelete.addAll(duplicates)
        }
        val finalIds = idsToDelete.toList()
        // Delete from local Room cache immediately
        hostCallDao.deleteMultiple(finalIds)
        // Delete from local client queue if present
        callDao.deleteMultiple(finalIds)
        // Delete from Firestore cloud DB
        return firestoreSource.deleteMultipleCalls(hostUid, finalIds)
    }

    suspend fun deleteMultipleCalls(hostUid: String, callIds: List<String>): Result<Unit> {
        val allIdsToDelete = callIds.toMutableSet()
        for (id in callIds) {
            val call = hostCallDao.getById(id)
            if (call != null) {
                val minTs = call.timestamp - 6_000L
                val maxTs = call.timestamp + 6_000L
                val duplicates = hostCallDao.findMatchingCallIds(call.hostCode, call.phoneNumber, minTs, maxTs)
                allIdsToDelete.addAll(duplicates)
            }
        }
        val finalIds = allIdsToDelete.toList()
        // Delete from local Room cache immediately
        hostCallDao.deleteMultiple(finalIds)
        // Delete from local client queue if present
        callDao.deleteMultiple(finalIds)
        // Delete from Firestore cloud DB
        return firestoreSource.deleteMultipleCalls(hostUid, finalIds)
    }
}
