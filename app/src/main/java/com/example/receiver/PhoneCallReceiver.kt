package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.provider.CallLog
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.SmsBridgeApp
import com.example.domain.model.CallType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class PhoneCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PhoneCallReceiver"

        // Track state across broadcasts
        private var lastState = TelephonyManager.EXTRA_STATE_IDLE
        private var callStartTime: Long = 0L
        private var isIncoming: Boolean = false
        private var savedNumber: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d(TAG, "Phone state changed: $stateStr, number: ${incomingNumber ?: "[hidden]"}")

        if (!incomingNumber.isNullOrBlank()) {
            savedNumber = incomingNumber
        }

        when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                isIncoming = true
                callStartTime = System.currentTimeMillis()
                lastState = TelephonyManager.EXTRA_STATE_RINGING
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (lastState == TelephonyManager.EXTRA_STATE_RINGING) {
                    // Incoming call was answered
                    isIncoming = true
                    callStartTime = System.currentTimeMillis()
                } else {
                    // Outgoing call started
                    isIncoming = false
                    callStartTime = System.currentTimeMillis()
                }
                lastState = TelephonyManager.EXTRA_STATE_OFFHOOK
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                val prevState = lastState
                lastState = TelephonyManager.EXTRA_STATE_IDLE

                if (prevState == TelephonyManager.EXTRA_STATE_RINGING) {
                    // Ring to Idle without Offhook -> Missed or Rejected call
                    processCallEnded(context, savedNumber, callStartTime, CallType.MISSED, 0)
                } else if (prevState == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                    // Offhook to Idle -> Call ended
                    val duration = ((System.currentTimeMillis() - callStartTime) / 1000L).toInt().coerceAtLeast(1)
                    val callType = if (isIncoming) CallType.INCOMING else CallType.OUTGOING
                    processCallEnded(context, savedNumber, callStartTime, callType, duration)
                }

                // Reset state
                savedNumber = null
                isIncoming = false
                callStartTime = 0L
            }
        }
    }

    private fun processCallEnded(
        context: Context,
        fallbackNumber: String?,
        timestamp: Long,
        fallbackType: CallType,
        fallbackDuration: Int
    ) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                // Short pause to let Android system finish writing to CallLog
                kotlinx.coroutines.delay(1000L)

                var finalNumber = fallbackNumber ?: "Unknown"
                var finalContactName = ""
                var finalCallType = fallbackType
                var finalDuration = fallbackDuration
                var simSlot = 1
                var finalTimestamp = if (timestamp > 0L) timestamp else System.currentTimeMillis()

                // Attempt to read official Android CallLog entry if permission granted
                val hasCallLogPermission = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_CALL_LOG
                ) == PackageManager.PERMISSION_GRANTED

                if (hasCallLogPermission) {
                    val callLogInfo = queryLatestCallLog(context)
                    if (callLogInfo != null) {
                        if (callLogInfo.number.isNotBlank()) finalNumber = callLogInfo.number
                        if (callLogInfo.contactName.isNotBlank()) finalContactName = callLogInfo.contactName
                        finalCallType = callLogInfo.callType
                        finalDuration = callLogInfo.duration
                        finalTimestamp = callLogInfo.date
                        simSlot = callLogInfo.simSlot
                    }
                }

                val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"

                Log.i(TAG, "Processed call: $finalNumber ($finalCallType, ${finalDuration}s, $finalContactName)")

                val app = context.applicationContext as? SmsBridgeApp
                val callRepo = app?.callRepository ?: SmsBridgeApp.instance.callRepository
                callRepo.handleIncomingCall(
                    callId = callId,
                    phoneNumber = finalNumber,
                    contactName = finalContactName,
                    callType = finalCallType,
                    durationSeconds = finalDuration,
                    timestamp = finalTimestamp,
                    simSlot = simSlot
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error processing call ended event", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private data class CallLogEntry(
        val number: String,
        val contactName: String,
        val callType: CallType,
        val duration: Int,
        val date: Long,
        val simSlot: Int
    )

    private fun queryLatestCallLog(context: Context): CallLogEntry? {
        var cursor: Cursor? = null
        return try {
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION,
                CallLog.Calls.DATE
            )

            cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            if (cursor != null && cursor.moveToFirst()) {
                val number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: ""
                val name = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)) ?: ""
                val typeInt = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                val duration = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                val date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE))

                val callType = when (typeInt) {
                    CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                    CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                    CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                    CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                    else -> CallType.MISSED
                }

                // Check if this log entry occurred within the last 30 seconds
                val now = System.currentTimeMillis()
                if (Math.abs(now - date) < 30_000L) {
                    CallLogEntry(
                        number = number,
                        contactName = name,
                        callType = callType,
                        duration = duration,
                        date = date,
                        simSlot = 1
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query system CallLog", e)
            null
        } finally {
            cursor?.close()
        }
    }
}
