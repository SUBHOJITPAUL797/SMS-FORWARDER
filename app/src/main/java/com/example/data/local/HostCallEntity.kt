package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.CallRecord
import com.example.domain.model.CallType

@Entity(
    tableName = "host_calls",
    indices = [
        Index(value = ["hostCode", "timestamp"]),
        Index(value = ["timestamp"])
    ]
)
data class HostCallEntity(
    @PrimaryKey
    val callId: String,
    val hostCode: String,
    val phoneNumber: String,
    val contactName: String,
    val callType: String,
    val durationSeconds: Int,
    val timestamp: Long,
    val uploadedAt: Long,
    val clientUid: String,
    val clientDeviceName: String,
    val simSlot: Int,
    val read: Boolean
) {
    fun toCallRecord(): CallRecord {
        return CallRecord(
            callId = callId,
            phoneNumber = phoneNumber,
            contactName = contactName,
            callType = CallType.fromString(callType),
            durationSeconds = durationSeconds,
            timestamp = timestamp,
            uploadedAt = uploadedAt,
            clientUid = clientUid,
            clientDeviceName = clientDeviceName,
            simSlot = simSlot,
            read = read
        )
    }

    companion object {
        fun fromCallRecord(hostCode: String, call: CallRecord): HostCallEntity {
            return HostCallEntity(
                callId = call.callId,
                hostCode = hostCode,
                phoneNumber = call.phoneNumber,
                contactName = call.contactName,
                callType = call.callType.name,
                durationSeconds = call.durationSeconds,
                timestamp = call.timestamp,
                uploadedAt = call.uploadedAt,
                clientUid = call.clientUid,
                clientDeviceName = call.clientDeviceName,
                simSlot = call.simSlot,
                read = call.read
            )
        }
    }
}
