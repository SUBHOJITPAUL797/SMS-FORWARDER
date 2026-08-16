package com.example.domain.model

data class SmsMessage(
    val messageId: String = "",
    val sender: String = "",
    val body: String = "",
    val receivedAt: Long = System.currentTimeMillis(),
    val uploadedAt: Long = System.currentTimeMillis(),
    val clientUid: String = "",
    val clientDeviceName: String = "",
    val read: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "messageId" to messageId,
        "sender" to sender,
        "body" to body,
        "receivedAt" to receivedAt,
        "uploadedAt" to uploadedAt,
        "clientUid" to clientUid,
        "clientDeviceName" to clientDeviceName,
        "read" to read
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): SmsMessage {
            return SmsMessage(
                messageId = map["messageId"] as? String ?: "",
                sender = map["sender"] as? String ?: "Unknown",
                body = map["body"] as? String ?: "",
                receivedAt = (map["receivedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                uploadedAt = (map["uploadedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                clientUid = map["clientUid"] as? String ?: "",
                clientDeviceName = map["clientDeviceName"] as? String ?: "",
                read = map["read"] as? Boolean ?: false
            )
        }
    }
}
