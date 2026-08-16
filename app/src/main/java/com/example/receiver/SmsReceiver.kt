package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Telephony
import android.util.Log
import com.example.SmsBridgeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        Log.d(TAG, "SMS_RECEIVED action captured by BroadcastReceiver")

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            Log.w(TAG, "No SMS messages found in intent")
            return
        }

        // Group by sender in case multiple messages are in the same batch
        val sender = messages[0].displayOriginatingAddress ?: "Unknown"
        val bodyBuilder = StringBuilder()
        var timestamp = messages[0].timestampMillis
        if (timestamp <= 0L) {
            timestamp = System.currentTimeMillis()
        }

        for (sms in messages) {
            bodyBuilder.append(sms.displayMessageBody ?: "")
        }

        val fullBody = bodyBuilder.toString()
        val messageId = UUID.randomUUID().toString()

        Log.i(TAG, "Parsed incoming SMS from $sender (id: $messageId, length: ${fullBody.length})")

        val pendingResult = goAsync()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SmsBridge:ReceiverWakeLock"
        )
        wakeLock?.acquire(30_000L) // 30 sec hold

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val app = context.applicationContext as? SmsBridgeApp
                val smsRepo = app?.smsRepository
                if (smsRepo != null) {
                    smsRepo.handleIncomingSms(
                        messageId = messageId,
                        sender = sender,
                        body = fullBody,
                        receivedAt = timestamp
                    )
                } else {
                    Log.e(TAG, "SmsRepository was null in SmsReceiver")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling incoming SMS", e)
            } finally {
                wakeLock?.let { if (it.isHeld) it.release() }
                pendingResult.finish()
            }
        }
    }
}
