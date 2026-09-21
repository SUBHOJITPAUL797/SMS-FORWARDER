package com.example.domain.model

enum class CallType(val displayName: String) {
    MISSED("Missed"),
    INCOMING("Received"),
    OUTGOING("Outgoing"),
    REJECTED("Rejected");

    companion object {
        fun fromString(value: String?): CallType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MISSED
        }
    }
}

data class CallRecord(
    val callId: String = "",
    val phoneNumber: String = "",
    val contactName: String = "",
    val callType: CallType = CallType.MISSED,
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val uploadedAt: Long = System.currentTimeMillis(),
    val clientUid: String = "",
    val clientDeviceName: String = "",
    val simSlot: Int = 1,
    val read: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "callId" to callId,
        "phoneNumber" to phoneNumber,
        "contactName" to contactName,
        "callType" to callType.name,
        "durationSeconds" to durationSeconds,
        "timestamp" to timestamp,
        "uploadedAt" to uploadedAt,
        "clientUid" to clientUid,
        "clientDeviceName" to clientDeviceName,
        "simSlot" to simSlot,
        "read" to read
    )

    fun getDisplayName(): String {
        return if (contactName.isNotBlank()) contactName else phoneNumber.ifBlank { "Unknown Caller" }
    }

    fun formattedDuration(): String {
        return when {
            callType == CallType.MISSED -> "Missed (0s)"
            callType == CallType.REJECTED -> "Rejected (0s)"
            durationSeconds <= 0 -> "0s"
            durationSeconds < 60 -> "${durationSeconds}s"
            else -> {
                val mins = durationSeconds / 60
                val secs = durationSeconds % 60
                if (secs > 0) "${mins}m ${secs}s" else "${mins}m"
            }
        }
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): CallRecord {
            return CallRecord(
                callId = map["callId"] as? String ?: "",
                phoneNumber = map["phoneNumber"] as? String ?: "Unknown",
                contactName = map["contactName"] as? String ?: "",
                callType = CallType.fromString(map["callType"] as? String),
                durationSeconds = (map["durationSeconds"] as? Number)?.toInt() ?: 0,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                uploadedAt = (map["uploadedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                clientUid = map["clientUid"] as? String ?: "",
                clientDeviceName = map["clientDeviceName"] as? String ?: "",
                simSlot = (map["simSlot"] as? Number)?.toInt() ?: 1,
                read = map["read"] as? Boolean ?: false
            )
        }
    }
}
