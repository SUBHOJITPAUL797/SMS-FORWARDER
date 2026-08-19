package com.example

import com.example.data.repository.InboxSyncScope
import com.example.domain.model.SmsMessage
import com.example.receiver.SmsReceiver
import org.junit.Assert.*
import org.junit.Test

class SmsSyncAndResilienceTest {

    @Test
    fun inboxSyncScope_labelsAndLimitsAreCorrect() {
        assertEquals("Entire Inbox (All Time - No Limit)", InboxSyncScope.ALL_TIME.label)
        assertNull(InboxSyncScope.ALL_TIME.maxCount)
        assertNull(InboxSyncScope.ALL_TIME.daysLimit)

        assertEquals(500, InboxSyncScope.LAST_500.maxCount)
        assertEquals(100, InboxSyncScope.LAST_100.maxCount)
        assertEquals(30, InboxSyncScope.LAST_30_DAYS.daysLimit)
        assertEquals(7, InboxSyncScope.LAST_7_DAYS.daysLimit)
    }

    @Test
    fun smsReceiver_generatesConsistentMessageId() {
        val sender = "+919876543210"
        val body = "Your OTP is 492019"
        val timestamp = 1718000000000L

        val id1 = SmsReceiver.generateMessageId(sender, body, timestamp)
        val id2 = SmsReceiver.generateMessageId(sender, body, timestamp)

        assertNotNull(id1)
        assertEquals(24, id1.length)
        assertEquals(id1, id2)
    }

    @Test
    fun messageDeduplication_filtersDuplicateBucketEntries() {
        val msg1 = SmsMessage(
            messageId = "msg_1",
            sender = "VK-HDFCBK",
            body = "Rs 500 debited",
            receivedAt = 10000L,
            uploadedAt = 10005L,
            clientUid = "dev_1",
            clientDeviceName = "Redmi Note 10",
            read = false
        )
        val msg2 = SmsMessage(
            messageId = "msg_2",
            sender = "VK-HDFCBK",
            body = "Rs 500 debited",
            receivedAt = 11000L, // within 4-second bucket (10000 / 4000 == 2, 11000 / 4000 == 2)
            uploadedAt = 11005L,
            clientUid = "dev_1",
            clientDeviceName = "Redmi Note 10",
            read = false
        )
        val msg3 = SmsMessage(
            messageId = "msg_3",
            sender = "VK-HDFCBK",
            body = "Rs 500 debited",
            receivedAt = 25000L, // different bucket (25000 / 4000 == 6)
            uploadedAt = 25005L,
            clientUid = "dev_1",
            clientDeviceName = "Redmi Note 10",
            read = false
        )

        val list = listOf(msg1, msg2, msg3)
        val seenKeys = mutableSetOf<String>()
        val deduplicated = mutableListOf<SmsMessage>()
        for (msg in list) {
            val timeBucket = msg.receivedAt / 4_000L
            val contentKey = "${msg.sender.trim()}|${msg.body.trim()}|$timeBucket"
            val idKey = msg.messageId

            if (seenKeys.add(idKey) && seenKeys.add(contentKey)) {
                deduplicated.add(msg)
            }
        }

        assertEquals(2, deduplicated.size)
        assertEquals("msg_1", deduplicated[0].messageId)
        assertEquals("msg_3", deduplicated[1].messageId)
    }

    @Test
    fun lazyLoadingPagination_chunksProperly() {
        val allMessages = (1..60).map { i ->
            SmsMessage(
                messageId = "msg_$i",
                sender = "Sender $i",
                body = "Body $i",
                receivedAt = System.currentTimeMillis() - i * 1000L,
                uploadedAt = System.currentTimeMillis(),
                clientUid = "dev_1",
                clientDeviceName = "Redmi",
                read = true
            )
        }

        var displayLimit = 25
        var paged = allMessages.take(displayLimit)
        assertEquals(25, paged.size)
        assertTrue(allMessages.size > displayLimit) // hasMore == true

        displayLimit += 25
        paged = allMessages.take(displayLimit)
        assertEquals(50, paged.size)

        displayLimit += 25
        paged = allMessages.take(displayLimit)
        assertEquals(60, paged.size)
        assertFalse(allMessages.size > displayLimit) // hasMore == false
    }
}
