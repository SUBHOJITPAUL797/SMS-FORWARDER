package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_queue",
    indices = [Index(value = ["callId"], unique = true)]
)
data class CallQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callId: String,
    val phoneNumber: String,
    val contactName: String = "",
    val callType: String,
    val durationSeconds: Int = 0,
    val timestamp: Long,
    val simSlot: Int = 1,
    val status: String = QueueStatus.PENDING.name,
    val retryCount: Int = 0
)
