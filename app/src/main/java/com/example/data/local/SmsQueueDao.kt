package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsQueueDao {

    @Query("SELECT * FROM sms_queue ORDER BY receivedAt DESC")
    fun getAllMessages(): Flow<List<SmsQueueEntity>>

    @Query("SELECT * FROM sms_queue WHERE status = :status ORDER BY receivedAt ASC")
    suspend fun getMessagesByStatus(status: String): List<SmsQueueEntity>

    @Query("SELECT * FROM sms_queue WHERE messageId = :messageId LIMIT 1")
    suspend fun getByMessageId(messageId: String): SmsQueueEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: SmsQueueEntity): Long

    @Update
    suspend fun update(entity: SmsQueueEntity)

    @Query("UPDATE sms_queue SET status = :status WHERE messageId = :messageId")
    suspend fun updateStatus(messageId: String, status: String)

    @Query("UPDATE sms_queue SET retryCount = retryCount + 1 WHERE messageId = :messageId")
    suspend fun incrementRetryCount(messageId: String)

    @Query("SELECT COUNT(*) FROM sms_queue")
    fun getTotalCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_queue WHERE status = 'UPLOADED'")
    fun getUploadedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_queue WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("DELETE FROM sms_queue WHERE status = 'UPLOADED' AND receivedAt < :beforeTimestamp")
    suspend fun clearOldUploaded(beforeTimestamp: Long)
}
