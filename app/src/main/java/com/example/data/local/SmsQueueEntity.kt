package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class QueueStatus {
    PENDING,
    UPLOADED,
    FAILED
}

@Entity(
    tableName = "sms_queue",
    indices = [Index(value = ["messageId"], unique = true)]
)
data class SmsQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val status: String = QueueStatus.PENDING.name,
    val retryCount: Int = 0
)
