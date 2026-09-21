package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CallQueueDao {

    @Query("SELECT * FROM call_queue ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallQueueEntity>>

    @Query("SELECT * FROM call_queue WHERE status = :status ORDER BY timestamp ASC")
    suspend fun getCallsByStatus(status: String): List<CallQueueEntity>

    @Query("SELECT * FROM call_queue WHERE callId = :callId LIMIT 1")
    suspend fun getByCallId(callId: String): CallQueueEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: CallQueueEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<CallQueueEntity>)

    @Query("DELETE FROM call_queue WHERE callId = :callId")
    suspend fun deleteByCallId(callId: String)

    @Query("DELETE FROM call_queue WHERE callId IN (:callIds)")
    suspend fun deleteMultiple(callIds: List<String>)

    @Update
    suspend fun update(entity: CallQueueEntity)

    @Query("UPDATE call_queue SET status = :status WHERE callId = :callId")
    suspend fun updateStatus(callId: String, status: String)

    @Query("UPDATE call_queue SET retryCount = retryCount + 1 WHERE callId = :callId")
    suspend fun incrementRetryCount(callId: String)

    @Query("SELECT COUNT(*) FROM call_queue")
    fun getTotalCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM call_queue WHERE status = 'UPLOADED'")
    fun getUploadedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM call_queue WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>
}
