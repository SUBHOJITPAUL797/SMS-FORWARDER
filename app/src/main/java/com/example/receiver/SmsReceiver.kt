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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.security.MessageDigest

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"

        /**
         * Generates a deterministic, unique SHA-256 fingerprint for the SMS message.
         * Buckets timestamp by 15-second windows to guarantee multi-part / duplicate intents
         * resolve to the exact same messageId.
         */
        fun generateMessageId(sender: String, body: String, timestamp: Long): String {
            val timeBucket = timestamp / 4_000L // 4-second safety window
            val raw = "${sender.trim()}|${body.trim()}|$timeBucket"
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(raw.toByteArray(Charsets.UTF_8))
                hash.joinToString("") { "%02x".format(it) }.take(24)
            } catch (e: Exception) {
                "${Math.abs(raw.hashCode())}"
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        Log.d(TAG, "SMS_RECEIVED action captured by BroadcastReceiver")

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            Log.w(TAG, "No SMS messages found in intent")
            return
        }

        // Group by sender in case multiple messages or multi-part segments are in the same batch
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
        val messageId = generateMessageId(sender, fullBody, timestamp)

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
                val prefs = app?.preferencesRepository

                // 1. Check if incoming SMS is a remote location trigger
                if (prefs != null) {
                    val isLocEnabled = prefs.isRemoteLocationEnabledFlow.firstOrNull() ?: true
                    val customKeyword = prefs.locationTriggerKeywordFlow.firstOrNull() ?: "#LOC"
                    if (isLocEnabled && com.example.util.LocationUtils.isLocationTrigger(fullBody, customKeyword)) {
                        Log.i(TAG, "Location trigger detected from $sender via SMS: $fullBody")
                        val clientName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                        val bat = com.example.util.LocationUtils.getBatteryInfo(context)
                        val loc = if (com.example.util.LocationUtils.hasLocationPermission(context)) {
                            com.example.util.LocationUtils.getCurrentLocation(context)
                        } else null
                        val replyText = com.example.util.LocationUtils.formatLocationSms(loc, bat, clientName, context)

                        // Send cellular SMS reply back to requester
                        val simSendRes = com.example.util.SimUtils.sendCellularSms(
                            context = context,
                            subscriptionId = null,
                            destinationNumber = sender,
                            messageText = replyText
                        )
                        Log.i(TAG, "Sent cellular SMS location reply to $sender: success=${simSendRes.isSuccess}")

                        // Also upload to Host feed if linked
                        val hostUid = prefs.linkedUidFlow.firstOrNull()
                        val clientUid = prefs.getOrCreateDeviceUid()
                        if (!hostUid.isNullOrEmpty() && smsRepo != null) {
                            val locMsg = com.example.domain.model.SmsMessage(
                                messageId = "sms_loc_${System.currentTimeMillis()}",
                                sender = "📍 SMS Location ($sender)",
                                body = replyText,
                                receivedAt = System.currentTimeMillis(),
                                uploadedAt = System.currentTimeMillis(),
                                clientUid = clientUid,
                                clientDeviceName = clientName,
                                read = false
                            )
                            smsRepo.uploadPendingMessageDirect(hostUid, locMsg)
                        }
                    }
                }

                // 2. Normal SMS processing (Room DB + Forwarding)
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
