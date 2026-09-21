package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HostMessageDao {
    @Query("SELECT * FROM host_messages WHERE hostCode = :hostCode ORDER BY receivedAt DESC")
    fun observeMessages(hostCode: String): Flow<List<HostMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<HostMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HostMessageEntity)

    @Query("DELETE FROM host_messages WHERE messageId = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM host_messages WHERE messageId IN (:ids)")
    suspend fun deleteMultiple(ids: List<String>)

    @Query("UPDATE host_messages SET `read` = 1 WHERE messageId = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE host_messages SET `read` = 1 WHERE hostCode = :hostCode")
    suspend fun markAllAsRead(hostCode: String)

    @Query("SELECT MIN(receivedAt) FROM host_messages WHERE hostCode = :hostCode")
    suspend fun getOldestTimestamp(hostCode: String): Long?

    @Query("DELETE FROM host_messages WHERE hostCode = :hostCode")
    suspend fun clearAll(hostCode: String)
}
