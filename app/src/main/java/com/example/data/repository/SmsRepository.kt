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
import com.example.domain.model.SmsMessage
import com.example.worker.SmsUploadWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

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
        val timeBucket = receivedAt / 15_000L
        val dedupKey = "${sender.trim()}|${body.trim()}|$timeBucket"
        val now = System.currentTimeMillis()
        val lastSeen = recentSmsCache[dedupKey]

        if (lastSeen != null && (now - lastSeen) < 15_000L) {
            Log.d(TAG, "Duplicate SMS ignored by dedup cache: $dedupKey")
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

        // 2. Attempt immediate upload if hostUid is known
        val hostUid = preferencesRepository.linkedUidFlow.firstOrNull()
        val clientUid = preferencesRepository.getOrCreateDeviceUid()
        val clientDeviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

        if (!hostUid.isNullOrEmpty()) {
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
                Log.d(TAG, "SMS $messageId uploaded immediately to Firestore.")
                return Result.success(Unit)
            } else {
                Log.w(TAG, "Immediate upload failed for $messageId. Enqueuing WorkManager retry.")
            }
        }

        // 3. If immediate upload failed or not connected, enqueue expedited WorkManager job
        enqueueUploadWorker(messageId)
        return Result.success(Unit)
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
        val pendingList = smsDao.getMessagesByStatus(QueueStatus.PENDING.name) +
                smsDao.getMessagesByStatus(QueueStatus.FAILED.name)
        for (item in pendingList) {
            uploadPendingMessage(item.messageId)
        }
    }

    /**
     * Reads real SMS messages directly from Android's Telephony ContentProvider (inbox)
     * and forwards them to the paired Host device.
     */
    suspend fun syncRealDeviceInbox(maxCount: Int = 50): Result<Int> {
        return try {
            val contentResolver = context.contentResolver
            val uri = android.provider.Telephony.Sms.Inbox.CONTENT_URI
            val projection = arrayOf(
                android.provider.Telephony.Sms._ID,
                android.provider.Telephony.Sms.ADDRESS,
                android.provider.Telephony.Sms.BODY,
                android.provider.Telephony.Sms.DATE
            )

            val cursor = contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${android.provider.Telephony.Sms.DATE} DESC"
            )

            var importedCount = 0
            cursor?.use { c ->
                val idCol = c.getColumnIndex(android.provider.Telephony.Sms._ID)
                val addressCol = c.getColumnIndex(android.provider.Telephony.Sms.ADDRESS)
                val bodyCol = c.getColumnIndex(android.provider.Telephony.Sms.BODY)
                val dateCol = c.getColumnIndex(android.provider.Telephony.Sms.DATE)

                while (c.moveToNext() && importedCount < maxCount) {
                    val id = if (idCol >= 0) c.getString(idCol) else java.util.UUID.randomUUID().toString()
                    val address = if (addressCol >= 0) c.getString(addressCol) ?: "Unknown" else "Unknown"
                    val body = if (bodyCol >= 0) c.getString(bodyCol) ?: "" else ""
                    val date = if (dateCol >= 0) c.getLong(dateCol) else System.currentTimeMillis()

                    val messageId = com.example.receiver.SmsReceiver.generateMessageId(address, body, date)
                    // Check if already in DB to avoid duplicating
                    val existing = smsDao.getByMessageId(messageId)
                    if (existing == null) {
                        handleIncomingSms(
                            messageId = messageId,
                            sender = address,
                            body = body,
                            receivedAt = date
                        )
                        importedCount++
                    }
                }
            }
            Result.success(importedCount)
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

    fun observeHostSmsList(hostUid: String): Flow<List<SmsMessage>> {
        return firestoreSource.observeSmsMessages(hostUid)
    }

    fun observeConnectedClients(hostCode: String): Flow<List<Map<String, Any>>> {
        return firestoreSource.observeConnectedClients(hostCode)
    }

    suspend fun registerClientLink(hostCode: String): Result<Unit> {
        val clientUid = preferencesRepository.getOrCreateDeviceUid()
        val clientDeviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        return firestoreSource.registerDirectLink(hostCode, clientUid, clientDeviceName)
    }

    suspend fun markAsRead(hostUid: String, messageId: String): Result<Unit> {
        return firestoreSource.markSmsAsRead(hostUid, messageId)
    }

    suspend fun markAllAsRead(hostUid: String): Result<Unit> {
        return firestoreSource.markAllSmsAsRead(hostUid)
    }
}
