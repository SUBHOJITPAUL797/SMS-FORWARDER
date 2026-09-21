package com.example

import com.example.domain.model.CallRecord
import com.example.domain.model.CallType
import org.junit.Assert.*
import org.junit.Test

class CallForwardingTest {

    @Test
    fun callType_fromString_parsesCorrectly() {
        assertEquals(CallType.MISSED, CallType.fromString("MISSED"))
        assertEquals(CallType.INCOMING, CallType.fromString("incoming"))
        assertEquals(CallType.OUTGOING, CallType.fromString("OUTGOING"))
        assertEquals(CallType.REJECTED, CallType.fromString("rejected"))
        assertEquals(CallType.MISSED, CallType.fromString("UNKNOWN_VALUE"))
        assertEquals(CallType.MISSED, CallType.fromString(null))
    }

    @Test
    fun callRecord_formattedDuration_formatsProperly() {
        val missedCall = CallRecord(
            callType = CallType.MISSED,
            durationSeconds = 0
        )
        assertEquals("Missed (0s)", missedCall.formattedDuration())

        val shortCall = CallRecord(
            callType = CallType.INCOMING,
            durationSeconds = 45
        )
        assertEquals("45s", shortCall.formattedDuration())

        val minuteCall = CallRecord(
            callType = CallType.OUTGOING,
            durationSeconds = 120
        )
        assertEquals("2m", minuteCall.formattedDuration())

        val complexCall = CallRecord(
            callType = CallType.INCOMING,
            durationSeconds = 145
        )
        assertEquals("2m 25s", complexCall.formattedDuration())
    }

    @Test
    fun callRecord_getDisplayName_prefersContactName() {
        val withName = CallRecord(
            phoneNumber = "+919876543210",
            contactName = "Mom"
        )
        assertEquals("Mom", withName.getDisplayName())

        val withoutName = CallRecord(
            phoneNumber = "+919876543210",
            contactName = ""
        )
        assertEquals("+919876543210", withoutName.getDisplayName())

        val emptyRecord = CallRecord(
            phoneNumber = "",
            contactName = ""
        )
        assertEquals("Unknown Caller", emptyRecord.getDisplayName())
    }

    @Test
    fun callRecord_serialization_toMapAndFromMap() {
        val record = CallRecord(
            callId = "call_abc_123",
            phoneNumber = "+919876543210",
            contactName = "Subhojit",
            callType = CallType.INCOMING,
            durationSeconds = 85,
            timestamp = 1718000000000L,
            uploadedAt = 1718000005000L,
            clientUid = "client_x",
            clientDeviceName = "Redmi Note 12",
            simSlot = 1,
            read = true
        )

        val map = record.toMap()
        val deserialized = CallRecord.fromMap(map)

        assertEquals(record.callId, deserialized.callId)
        assertEquals(record.phoneNumber, deserialized.phoneNumber)
        assertEquals(record.contactName, deserialized.contactName)
        assertEquals(record.callType, deserialized.callType)
        assertEquals(record.durationSeconds, deserialized.durationSeconds)
        assertEquals(record.timestamp, deserialized.timestamp)
        assertEquals(record.simSlot, deserialized.simSlot)
        assertEquals(record.read, deserialized.read)
    }

    @Test
    fun callDeduplication_filtersDuplicateBucketEntries() {
        val call1 = CallRecord(
            callId = "call_1",
            phoneNumber = "+919876543210",
            callType = CallType.MISSED,
            timestamp = 10000L
        )
        val call2 = CallRecord(
            callId = "call_2",
            phoneNumber = "+919876543210",
            callType = CallType.MISSED,
            timestamp = 11000L // same 4-second bucket (10000 / 4000 == 2, 11000 / 4000 == 2)
        )
        val call3 = CallRecord(
            callId = "call_3",
            phoneNumber = "+919876543210",
            callType = CallType.MISSED,
            timestamp = 25000L // different bucket (25000 / 4000 == 6)
        )

        val list = listOf(call1, call2, call3)
        val seenKeys = mutableSetOf<String>()
        val deduplicated = mutableListOf<CallRecord>()
        for (call in list) {
            val timeBucket = call.timestamp / 4_000L
            val contentKey = "${call.phoneNumber.trim()}|${call.callType.name}|$timeBucket"
            val idKey = call.callId

            if (seenKeys.add(idKey) && seenKeys.add(contentKey)) {
                deduplicated.add(call)
            }
        }

        assertEquals(2, deduplicated.size)
        assertEquals("call_1", deduplicated[0].callId)
        assertEquals("call_3", deduplicated[1].callId)
    }
}
