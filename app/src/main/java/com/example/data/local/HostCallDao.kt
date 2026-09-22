package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HostCallDao {
    @Query("SELECT * FROM host_calls WHERE hostCode = :hostCode ORDER BY timestamp DESC")
    fun observeCalls(hostCode: String): Flow<List<HostCallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<HostCallEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HostCallEntity)

    @Query("DELETE FROM host_calls WHERE callId = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM host_calls WHERE callId IN (:ids)")
    suspend fun deleteMultiple(ids: List<String>)

    @Query("SELECT * FROM host_calls WHERE callId = :id LIMIT 1")
    suspend fun getById(id: String): HostCallEntity?

    @Query("SELECT callId FROM host_calls WHERE hostCode = :hostCode AND phoneNumber = :phoneNumber AND timestamp BETWEEN :minTs AND :maxTs")
    suspend fun findMatchingCallIds(hostCode: String, phoneNumber: String, minTs: Long, maxTs: Long): List<String>

    @Query("UPDATE host_calls SET `read` = 1 WHERE callId = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE host_calls SET `read` = 1 WHERE hostCode = :hostCode")
    suspend fun markAllAsRead(hostCode: String)

    @Query("SELECT MIN(timestamp) FROM host_calls WHERE hostCode = :hostCode")
    suspend fun getOldestTimestamp(hostCode: String): Long?

    @Query("DELETE FROM host_calls WHERE hostCode = :hostCode")
    suspend fun clearAll(hostCode: String)
}
