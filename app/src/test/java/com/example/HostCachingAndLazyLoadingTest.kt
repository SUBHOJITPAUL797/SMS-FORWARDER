package com.example

import com.example.data.local.HostCallEntity
import com.example.data.local.HostMessageEntity
import com.example.domain.model.CallRecord
import com.example.domain.model.CallType
import com.example.domain.model.SmsMessage
import org.junit.Assert.*
import org.junit.Test

class HostCachingAndLazyLoadingTest {

    @Test
    fun hostMessageEntity_mapping_preservesAllProperties() {
        val original = SmsMessage(
            messageId = "msg_12345",
            sender = "HDFC-BANK",
            body = "Your OTP is 987654. Do not share.",
            receivedAt = 1718000000000L,
            uploadedAt = 1718000001000L,
            clientUid = "client_abc",
            clientDeviceName = "Redmi Note 12",
            read = true
        )

        val entity = HostMessageEntity.fromSmsMessage("HOST_CODE_XYZ", original)
        assertEquals("HOST_CODE_XYZ", entity.hostCode)
        assertEquals(original.messageId, entity.messageId)
        assertEquals(original.sender, entity.sender)
        assertEquals(original.body, entity.body)
        assertEquals(original.receivedAt, entity.receivedAt)
        assertEquals(original.uploadedAt, entity.uploadedAt)
        assertEquals(original.clientUid, entity.clientUid)
        assertEquals(original.clientDeviceName, entity.clientDeviceName)
        assertTrue(entity.read)

        val mappedBack = entity.toSmsMessage()
        assertEquals(original, mappedBack)
    }

    @Test
    fun hostCallEntity_mapping_preservesAllProperties() {
        val original = CallRecord(
            callId = "call_998877",
            phoneNumber = "+919876543210",
            contactName = "Work Colleague",
            callType = CallType.INCOMING,
            durationSeconds = 125,
            timestamp = 1718000500000L,
            uploadedAt = 1718000502000L,
            clientUid = "client_abc",
            clientDeviceName = "Pixel 7 Pro",
            simSlot = 2,
            read = false
        )

        val entity = HostCallEntity.fromCallRecord("HOST_CODE_XYZ", original)
        assertEquals("HOST_CODE_XYZ", entity.hostCode)
        assertEquals(original.callId, entity.callId)
        assertEquals(original.phoneNumber, entity.phoneNumber)
        assertEquals(original.contactName, entity.contactName)
        assertEquals("INCOMING", entity.callType)
        assertEquals(original.durationSeconds, entity.durationSeconds)
        assertEquals(original.timestamp, entity.timestamp)
        assertEquals(original.simSlot, 2)
        assertFalse(entity.read)

        val mappedBack = entity.toCallRecord()
        assertEquals(original, mappedBack)
    }

    @Test
    fun lazyLoadingPagination_calculatesLimitsAndOffsetsAccurately() {
        val items = (1..60).map { i ->
            SmsMessage(messageId = "id_$i", sender = "Sender $i", body = "Body $i", receivedAt = 1000L * i)
        }.reversed() // newest first

        var displayLimit = 25
        val page1 = items.take(displayLimit)
        assertEquals(25, page1.size)
        assertEquals("id_60", page1.first().messageId)
        assertTrue(items.size > displayLimit)

        displayLimit += 25
        val page2 = items.take(displayLimit)
        assertEquals(50, page2.size)
        assertTrue(items.size > displayLimit)

        displayLimit += 25
        val page3 = items.take(displayLimit)
        assertEquals(60, page3.size)
        assertFalse(items.size > displayLimit)
    }
}
