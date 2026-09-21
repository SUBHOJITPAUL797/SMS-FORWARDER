package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.SmsMessage

@Entity(tableName = "host_messages")
data class HostMessageEntity(
    @PrimaryKey
    val messageId: String,
    val hostCode: String,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val uploadedAt: Long,
    val clientUid: String,
    val clientDeviceName: String,
    val read: Boolean
) {
    fun toSmsMessage(): SmsMessage {
        return SmsMessage(
            messageId = messageId,
            sender = sender,
            body = body,
            receivedAt = receivedAt,
            uploadedAt = uploadedAt,
            clientUid = clientUid,
            clientDeviceName = clientDeviceName,
            read = read
        )
    }

    companion object {
        fun fromSmsMessage(hostCode: String, message: SmsMessage): HostMessageEntity {
            return HostMessageEntity(
                messageId = message.messageId,
                hostCode = hostCode,
                sender = message.sender,
                body = message.body,
                receivedAt = message.receivedAt,
                uploadedAt = message.uploadedAt,
                clientUid = message.clientUid,
                clientDeviceName = message.clientDeviceName,
                read = message.read
            )
        }
    }
}
