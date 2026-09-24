package com.example.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.local.AppDatabase
import com.example.data.local.QueueStatus
import com.example.data.local.SmsQueueEntity
import com.example.data.remote.AuthSource
import com.example.data.remote.FirestoreSource
import com.example.domain.model.ConnectedDevice
import com.example.domain.model.SmsMessage
import com.example.worker.SmsUploadWorker
import com.example.data.local.HostMessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class InboxSyncScope(val label: String, val maxCount: Int?, val daysLimit: Int?) {
    ALL_TIME("Entire Inbox (All Time - No Limit)", null, null),
    LAST_500("Last 500 Messages", 500, null),
    LAST_100("Last 100 Messages", 100, null),
    LAST_30_DAYS("Last 30 Days", null, 30),
    LAST_7_DAYS("Last 7 Days", null, 7)
}

data class SyncResult(
    val totalFound: Int,
    val newImported: Int,
    val alreadyExisted: Int
)

class SmsRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val firestoreSource: FirestoreSource,
    private val authSource: AuthSource,
    private val preferencesRepository: UserPreferencesRepository
) {
    companion object {
        private const val TAG = "SmsRepository"
    }

    private val recentSmsCache = java.util.Collections.synchronizedMap(
        object : java.util.LinkedHashMap<String, Long>(50, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > 100
            }
        }
    )

    private val smsDao = database.smsQueueDao()
    private val hostMessageDao = database.hostMessageDao()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val hostSmsSyncJobs = mutableMapOf<String, Job>()

    val totalQueueCount: Flow<Int> = smsDao.getTotalCountFlow()
    val uploadedCount: Flow<Int> = smsDao.getUploadedCountFlow()
    val pendingCount: Flow<Int> = smsDao.getPendingCountFlow()
    val localQueueMessages: Flow<List<SmsQueueEntity>> = smsDao.getAllMessages()

    suspend fun handleIncomingSms(
        messageId: String,
        sender: String,
        body: String,
        receivedAt: Long
    ): Result<Unit> {
        val userRole = preferencesRepository.userRoleFlow.firstOrNull() ?: com.example.domain.model.UserRole.CLIENT
        if (userRole == com.example.domain.model.UserRole.HOST) {
            // Check if this incoming SMS is an offline forwarded SMS from a client/sender device
            if (body.startsWith("[FWD:")) {
                Log.i(TAG, "Host device received offline forwarded cellular SMS: $body")
                val fwdRegex = Regex("^\\[FWD:([^\\]]+)\\]\\s*([\\s\\S]*)")
                val match = fwdRegex.find(body)
                val originalSender = match?.groupValues?.getOrNull(1)?.trim() ?: sender
                val originalBody = match?.groupValues?.getOrNull(2)?.trim() ?: body
                val hostCode = preferencesRepository.getOrCreateHostCode()

                // Show rich Host notification with OTP extraction & floating window overlay
                com.example.util.HostNotificationManager.showSmsNotification(
                    context = context,
                    sender = originalSender,
                    body = originalBody,
                    messageId = messageId,
                    hostCode = hostCode
                )

                if (hostCode.isNotBlank()) {
                    val clientDeviceName = "Offline Cellular ($sender)"
                    val fwdMessage = SmsMessage(
                        messageId = messageId,
                        sender = originalSender,
                        body = originalBody,
                        receivedAt = receivedAt,
                        uploadedAt = System.currentTimeMillis(),
                        clientUid = "cellular_fallback",
                        clientDeviceName = clientDeviceName,
                        read = false
                    )
                    firestoreSource.uploadSms(hostCode, fwdMessage)
                }
                return Result.success(Unit)
            }
            // For normal direct SMS on Host device, do not re-forward
            Log.d(TAG, "Host device received direct SMS, skipping forward.")
            return Result.success(Unit)
        }

        val timeBucket = receivedAt / 4_000L
        val dedupKey = "${sender.trim()}|${body.trim()}|$timeBucket"
        val now = System.currentTimeMillis()
        val lastSeen = recentSmsCache[dedupKey]

        if (lastSeen != null && (now - lastSeen) < 4_000L) {
            Log.d(TAG, "Duplicate broadcast SMS ignored by dedup cache: $dedupKey")
            return Result.success(Unit)
        }
        recentSmsCache[dedupKey] = now

        val entity = SmsQueueEntity(
            messageId = messageId,
            sender = sender,
            body = body,
            receivedAt = receivedAt,
            status = QueueStatus.PENDING.name,
            retryCount = 0
        )

        // 1. Insert into local Room DB
        smsDao.insert(entity)

        // 2. Check REAL network connectivity (do not rely on Firestore offline cache when offline)
        val isOnline = com.example.util.NetworkUtils.isOnline(context)
        val hostUid = preferencesRepository.linkedUidFlow.firstOrNull()
        val clientUid = preferencesRepository.getOrCreateDeviceUid()
        val clientDeviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

        if (isOnline && !hostUid.isNullOrEmpty()) {
            val message = SmsMessage(
                messageId = messageId,
                sender = sender,
                body = body,
                receivedAt = receivedAt,
                uploadedAt = System.currentTimeMillis(),
                clientUid = clientUid,
                clientDeviceName = clientDeviceName,
                read = false
            )
            val uploadRes = firestoreSource.uploadSms(hostUid, message)
            if (uploadRes.isSuccess) {
                smsDao.updateStatus(messageId, QueueStatus.UPLOADED.name)
                Log.d(TAG, "SMS $messageId uploaded immediately to Firestore via active internet.")
                return Result.success(Unit)
            } else {
                Log.w(TAG, "Immediate cloud upload failed for $messageId despite network. Falling back to cellular SMS...")
            }
        } else {
            Log.i(TAG, "Device is OFFLINE or unlinked (isOnline=$isOnline, hasHost=${!hostUid.isNullOrEmpty()}). Executing Smart Hybrid Cellular SMS fallback immediately.")
        }

        // 3. Smart Hybrid Fallback: Send via Cellular SMS (using Jio/Carrier 100 free SMS daily quota)
        val sentFallback = trySendOfflineCellularFallback(sender, body)
        Log.i(TAG, "Offline cellular fallback execution complete. Result: $sentFallback")

        // 4. Enqueue expedited WorkManager job to also sync to Cloud DB when internet reconnects
        enqueueUploadWorker(messageId)
        return Result.success(Unit)
    }

    suspend fun trySendOfflineCellularFallback(sender: String, body: String): Boolean {
        try {
            val isFallbackEnabled = preferencesRepository.isOfflineSmsFallbackEnabledFlow.firstOrNull() ?: true
            if (!isFallbackEnabled) {
                Log.d(TAG, "Offline cellular SMS fallback is disabled by user.")
                return false
            }

            val destinationNumber = preferencesRepository.fallbackDestinationNumberFlow.firstOrNull()?.trim() ?: ""
            if (destinationNumber.isBlank()) {
                Log.w(TAG, "No destination mobile number configured for offline cellular SMS fallback.")
                return false
            }

            // Check and reset daily quota for midnight rollovers
            val (currentSim1Count, currentSim2Count) = preferencesRepository.checkAndResetDailyQuota()
            val sim1Limit = preferencesRepository.dailySmsLimitSim1Flow.firstOrNull() ?: 100
            val sim2Limit = preferencesRepository.dailySmsLimitSim2Flow.firstOrNull() ?: 100
            val preferredSlot = preferencesRepository.preferredSimSlotFlow.firstOrNull() ?: 0 // 0 = Auto/SIM1, 1 = SIM1, 2 = SIM2
            val isRolloverEnabled = preferencesRepository.isDualSimRolloverEnabledFlow.firstOrNull() ?: true

            val availableSims = com.example.util.SimUtils.getAvailableSims(context)
            if (availableSims.isEmpty()) {
                Log.w(TAG, "No active SIM card found on device to send offline SMS.")
                return false
            }

            val sim1Info = availableSims.find { it.slotIndex == 0 } ?: availableSims.firstOrNull()
            val sim2Info = availableSims.find { it.slotIndex == 1 }

            var targetSim: com.example.util.SimSlotInfo? = null
            var targetSlotIndex = 0

            if (preferredSlot == 2 && sim2Info != null) {
                // User prefers SIM 2
                if (currentSim2Count < sim2Limit) {
                    targetSim = sim2Info
                    targetSlotIndex = 1
                } else if (isRolloverEnabled && sim1Info != null && currentSim1Count < sim1Limit) {
                    Log.i(TAG, "SIM 2 quota exhausted ($currentSim2Count/$sim2Limit). Rolling over to SIM 1 ($currentSim1Count/$sim1Limit).")
                    targetSim = sim1Info
                    targetSlotIndex = 0
                } else {
                    Log.w(TAG, "Daily SMS quota exhausted on all SIMs ($currentSim1Count/$sim1Limit, $currentSim2Count/$sim2Limit). Skipping cellular SMS.")
                    return false
                }
            } else {
                // Default: prefers SIM 1 (or Auto)
                if (sim1Info != null && currentSim1Count < sim1Limit) {
                    targetSim = sim1Info
                    targetSlotIndex = 0
                } else if (isRolloverEnabled && sim2Info != null && currentSim2Count < sim2Limit) {
                    Log.i(TAG, "SIM 1 quota exhausted ($currentSim1Count/$sim1Limit). Rolling over to SIM 2 ($currentSim2Count/$sim2Limit).")
                    targetSim = sim2Info
                    targetSlotIndex = 1
                } else {
                    Log.w(TAG, "Daily SMS quota exhausted on all SIMs ($currentSim1Count/$sim1Limit, $currentSim2Count/$sim2Limit). Skipping cellular SMS.")
                    return false
                }
            }

            if (targetSim == null) {
                targetSim = availableSims.firstOrNull()
            }

            val formattedText = com.example.util.SimUtils.formatForwardedSms(sender, body)
            val sendResult = com.example.util.SimUtils.sendCellularSms(
                context = context,
                subscriptionId = targetSim?.subscriptionId,
                destinationNumber = destinationNumber,
                messageText = formattedText
            )

            return if (sendResult.isSuccess) {
                val parts = sendResult.getOrNull() ?: 1
                preferencesRepository.incrementSmsSentCount(targetSlotIndex, parts)
                Log.i(TAG, "Successfully dispatched offline fallback SMS to $destinationNumber via SIM ${targetSlotIndex + 1} ($parts parts)")
                true
            } else {
                Log.e(TAG, "Failed to send offline fallback SMS via cellular SIM to $destinationNumber", sendResult.exceptionOrNull())
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in trySendOfflineCellularFallback", e)
            return false
        }
    }

    suspend fun uploadPendingMessage(messageId: String): Result<Unit> {
        val entity = smsDao.getByMessageId(messageId)
            ?: return Result.failure(Exception("Message $messageId not found in local DB"))

        if (entity.status == QueueStatus.UPLOADED.name) {
            return Result.success(Unit)
        }

        val hostUid = preferencesRepository.linkedUidFlow.firstOrNull()
            ?: return Result.failure(Exception("No linked Host UID found"))

        val clientUid = preferencesRepository.getOrCreateDeviceUid()
        val clientDeviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

        val message = SmsMessage(
            messageId = entity.messageId,
            sender = entity.sender,
            body = entity.body,
            receivedAt = entity.receivedAt,
            uploadedAt = System.currentTimeMillis(),
            clientUid = clientUid,
            clientDeviceName = clientDeviceName,
            read = false
        )

        val uploadRes = firestoreSource.uploadSms(hostUid, message)
        return if (uploadRes.isSuccess) {
            smsDao.updateStatus(messageId, QueueStatus.UPLOADED.name)
            Result.success(Unit)
        } else {
            smsDao.incrementRetryCount(messageId)
            smsDao.updateStatus(messageId, QueueStatus.FAILED.name)
            Result.failure(uploadRes.exceptionOrNull() ?: Exception("Upload failed"))
        }
    }

    suspend fun syncAllPendingMessages() {
        // Prune uploaded queue items older than 14 days to keep local DB slim
        val fourteenDaysAgo = System.currentTimeMillis() - (14 * 24 * 3600 * 1000L)
        smsDao.clearOldUploaded(fourteenDaysAgo)

        val pendingList = smsDao.getMessagesByStatus(QueueStatus.PENDING.name) +
                smsDao.getMessagesByStatus(QueueStatus.FAILED.name)
        for (item in pendingList) {
            val result = uploadPendingMessage(item.messageId)
            if (result.isFailure) {
                smsDao.incrementRetryCount(item.messageId)
            }
        }
    }

    /**
     * Reads real SMS messages directly from Android's Telephony ContentProvider (inbox)
     * and forwards them to the paired Host device with support for all-time and custom range sync.
     */
    suspend fun syncRealDeviceInbox(scope: InboxSyncScope = InboxSyncScope.ALL_TIME): Result<SyncResult> {
        return try {
            val contentResolver = context.contentResolver
            val uri = android.provider.Telephony.Sms.Inbox.CONTENT_URI
            val projection = arrayOf(
                android.provider.Telephony.Sms._ID,
                android.provider.Telephony.Sms.ADDRESS,
                android.provider.Telephony.Sms.BODY,
                android.provider.Telephony.Sms.DATE
            )

            var selection: String? = null
            var selectionArgs: Array<String>? = null
            if (scope.daysLimit != null) {
                val cutoff = System.currentTimeMillis() - (scope.daysLimit * 24 * 3600 * 1000L)
                selection = "${android.provider.Telephony.Sms.DATE} >= ?"
                selectionArgs = arrayOf(cutoff.toString())
            }

            val cursor = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${android.provider.Telephony.Sms.DATE} DESC"
            )

            var totalFound = 0
            var newImported = 0
            var alreadyExisted = 0
            val pendingEntities = mutableListOf<SmsQueueEntity>()
            val hostUid = preferencesRepository.linkedUidFlow.firstOrNull()
            val clientUid = preferencesRepository.getOrCreateDeviceUid()
            val clientDeviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

            cursor?.use { c ->
                val addressCol = c.getColumnIndex(android.provider.Telephony.Sms.ADDRESS)
                val bodyCol = c.getColumnIndex(android.provider.Telephony.Sms.BODY)
                val dateCol = c.getColumnIndex(android.provider.Telephony.Sms.DATE)

                while (c.moveToNext()) {
                    if (scope.maxCount != null && totalFound >= scope.maxCount) {
                        break
                    }
                    totalFound++

                    val address = if (addressCol >= 0) c.getString(addressCol) ?: "Unknown" else "Unknown"
                    val body = if (bodyCol >= 0) c.getString(bodyCol) ?: "" else ""
                    val date = if (dateCol >= 0) c.getLong(dateCol) else System.currentTimeMillis()

                    val messageId = com.example.receiver.SmsReceiver.generateMessageId(address, body, date)
                    val existing = smsDao.getByMessageId(messageId)
                    if (existing == null) {
                        val entity = SmsQueueEntity(
                            messageId = messageId,
                            sender = address,
                            body = body,
                            receivedAt = date,
                            status = QueueStatus.PENDING.name,
                            retryCount = 0
                        )
                        pendingEntities.add(entity)
                        newImported++
                    } else {
                        alreadyExisted++
                    }
                }
            }

            // Batch insert all new messages into Room DB
            if (pendingEntities.isNotEmpty()) {
                smsDao.insertAll(pendingEntities)
            }

            // Immediately upload in controlled batches if host is linked
            if (!hostUid.isNullOrEmpty() && pendingEntities.isNotEmpty()) {
                val chunks = pendingEntities.chunked(25)
                for (chunk in chunks) {
                    for (entity in chunk) {
                        val msg = SmsMessage(
                            messageId = entity.messageId,
                            sender = entity.sender,
                            body = entity.body,
                            receivedAt = entity.receivedAt,
                            uploadedAt = System.currentTimeMillis(),
                            clientUid = clientUid,
                            clientDeviceName = clientDeviceName,
                            read = false
                        )
                        val uploadRes = firestoreSource.uploadSms(hostUid, msg)
                        if (uploadRes.isSuccess) {
                            smsDao.updateStatus(entity.messageId, QueueStatus.UPLOADED.name)
                        } else {
                            enqueueUploadWorker(entity.messageId)
                        }
                    }
                    kotlinx.coroutines.delay(20)
                }
            }

            Result.success(SyncResult(totalFound, newImported, alreadyExisted))
        } catch (e: Exception) {
            Log.e(TAG, "Error querying real device SMS inbox", e)
            Result.failure(e)
        }
    }

    fun enqueueUploadWorker(messageId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString(SmsUploadWorker.KEY_MESSAGE_ID, messageId)
            .build()

        val uploadRequest = OneTimeWorkRequestBuilder<SmsUploadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueue(uploadRequest)
    }

    fun startHostSync(hostCode: String) {
        if (hostCode.isBlank()) return
        synchronized(hostSmsSyncJobs) {
            hostSmsSyncJobs.keys.filter { it != hostCode }.forEach { oldCode ->
                hostSmsSyncJobs.remove(oldCode)?.cancel()
            }
            if (hostSmsSyncJobs[hostCode]?.isActive == true) return
            hostSmsSyncJobs[hostCode] = repoScope.launch {
                try {
                    firestoreSource.observeSmsBatch(hostCode, limit = 25).collect { batch ->
                        if (batch.upserted.isNotEmpty()) {
                            val entities = batch.upserted.map { HostMessageEntity.fromSmsMessage(hostCode, it) }
                            hostMessageDao.insertAll(entities)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in host SMS live sync", e)
                }
            }
        }
    }

    fun observeHostSmsList(hostUid: String): Flow<List<SmsMessage>> {
        startHostSync(hostUid)
        return hostMessageDao.observeMessages(hostUid).map { entities ->
            entities.map { it.toSmsMessage() }
        }
    }

    suspend fun loadOlderMessagesFromCloud(hostCode: String, pageSize: Long = 25): Result<Int> {
        return try {
            val oldestTimestamp = hostMessageDao.getOldestTimestamp(hostCode) ?: System.currentTimeMillis()
            val fetchResult = firestoreSource.fetchOlderSms(hostCode, beforeTimestamp = oldestTimestamp, limit = pageSize)
            if (fetchResult.isFailure) {
                return Result.failure(fetchResult.exceptionOrNull() ?: Exception("Failed to fetch older SMS"))
            }
            val messages = fetchResult.getOrNull() ?: emptyList()
            if (messages.isNotEmpty()) {
                val entities = messages.map { HostMessageEntity.fromSmsMessage(hostCode, it) }
                hostMessageDao.insertAll(entities)
            }
            Result.success(messages.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading older SMS from cloud", e)
            Result.failure(e)
        }
    }

    fun observeConnectedClients(hostCode: String): Flow<List<Map<String, Any>>> {
        return firestoreSource.observeConnectedClients(hostCode)
    }

    fun observeConnectedDevices(hostCode: String): Flow<List<ConnectedDevice>> {
        return firestoreSource.observeConnectedDevices(hostCode)
    }

    suspend fun disconnectClient(hostCode: String, clientUid: String): Result<Unit> {
        return firestoreSource.disconnectClient(hostCode, clientUid)
    }

    suspend fun updateClientLastSeen(hostCode: String, clientUid: String): Result<Unit> {
        return firestoreSource.updateClientLastSeen(hostCode, clientUid)
    }

    fun observeClientLink(hostCode: String, clientUid: String): Flow<Map<String, Any>?> {
        return firestoreSource.observeClientLink(hostCode, clientUid)
    }

    suspend fun registerClientLink(hostCode: String): Result<Unit> {
        val clientUid = preferencesRepository.getOrCreateDeviceUid()
        val clientDeviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        return firestoreSource.registerDirectLink(hostCode, clientUid, clientDeviceName)
    }

    suspend fun markAsRead(hostUid: String, messageId: String): Result<Unit> {
        hostMessageDao.markAsRead(messageId)
        return firestoreSource.markSmsAsRead(hostUid, messageId)
    }

    suspend fun markAllAsRead(hostUid: String): Result<Unit> {
        hostMessageDao.markAllAsRead(hostUid)
        return firestoreSource.markAllSmsAsRead(hostUid)
    }

    suspend fun deleteSms(hostUid: String, messageId: String): Result<Unit> {
        // Delete from local Room cache immediately
        hostMessageDao.deleteById(messageId)
        // Delete from local client queue if present
        smsDao.deleteByMessageId(messageId)
        // Delete from Firestore cloud DB
        return firestoreSource.deleteSms(hostUid, messageId)
    }

    suspend fun deleteMultipleSms(hostUid: String, messageIds: List<String>): Result<Unit> {
        // Delete from local Room cache immediately in chunks of 450 to avoid SQLite variable limits
        messageIds.chunked(450).forEach { chunk ->
            hostMessageDao.deleteMultiple(chunk)
            smsDao.deleteMultiple(chunk)
        }
        // Delete from Firestore cloud DB
        return firestoreSource.deleteMultipleSms(hostUid, messageIds)
    }
}
