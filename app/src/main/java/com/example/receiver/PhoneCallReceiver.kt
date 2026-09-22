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

                val fallbackDuration = if (callStartTime > 0L) {
                    ((System.currentTimeMillis() - callStartTime) / 1000L).toInt().coerceAtLeast(1)
                } else 0

                val fallbackType = if (prevState == TelephonyManager.EXTRA_STATE_RINGING) {
                    CallType.MISSED
                } else if (prevState == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                    if (isIncoming) CallType.INCOMING else CallType.OUTGOING
                } else {
                    CallType.MISSED
                }

                // Always invoke processCallEnded to ensure calls are never dropped due to lost static state
                processCallEnded(context, savedNumber, callStartTime, fallbackType, fallbackDuration)

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
                kotlinx.coroutines.delay(1200L)

                var finalNumber = fallbackNumber ?: ""
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
                    var callLogInfo = queryLatestCallLog(context)
                    // If not resolved on first check, wait another 1500ms and retry (for slower OEM skins like MIUI/HyperOS)
                    if (callLogInfo == null) {
                        kotlinx.coroutines.delay(1500L)
                        callLogInfo = queryLatestCallLog(context)
                    }

                    if (callLogInfo != null) {
                        if (callLogInfo.number.isNotBlank()) finalNumber = callLogInfo.number
                        if (callLogInfo.contactName.isNotBlank()) finalContactName = callLogInfo.contactName
                        finalCallType = callLogInfo.callType
                        finalDuration = callLogInfo.duration
                        finalTimestamp = callLogInfo.date
                        simSlot = callLogInfo.simSlot
                    }
                }

                // Resolve contact name if still blank
                if (finalContactName.isBlank() && finalNumber.isNotBlank() && !finalNumber.equals("Unknown", ignoreCase = true)) {
                    finalContactName = com.example.util.ContactUtils.resolveContactName(context, finalNumber)
                }

                // Check if number is valid - NEVER forward blank, unknown, or spurious fake calls
                val isInvalidNumber = finalNumber.isBlank() ||
                        finalNumber.equals("Unknown", ignoreCase = true) ||
                        finalNumber == "-1" || finalNumber == "-2" || finalNumber == "-3" ||
                        finalNumber.equals("private", ignoreCase = true)

                if (isInvalidNumber) {
                    Log.w(TAG, "No valid phone number resolved for call event (number='$finalNumber'). Skipping forwarding to prevent fake/unknown call logs.")
                    return@launch
                }

                val app = context.applicationContext as? SmsBridgeApp
                val callRepo = app?.callRepository ?: SmsBridgeApp.instance.callRepository
                val callId = callRepo.generateCallId(finalNumber, finalTimestamp, finalCallType)

                Log.i(TAG, "Processed call: $finalNumber ($finalCallType, ${finalDuration}s, $finalContactName, id=$callId)")

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

            if (cursor != null) {
                val now = System.currentTimeMillis()
                var scanned = 0
                val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)

                while (cursor.moveToNext() && scanned < 5) {
                    scanned++
                    val number = if (numberIdx >= 0) cursor.getString(numberIdx) ?: "" else ""
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                    val typeInt = if (typeIdx >= 0) cursor.getInt(typeIdx) else CallLog.Calls.MISSED_TYPE
                    val duration = if (durationIdx >= 0) cursor.getInt(durationIdx) else 0
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L

                    // CallLog.Calls.DATE is the call START time on Android.
                    // Call end time is date + duration in milliseconds.
                    val callEndTime = date + (duration * 1000L)
                    val isRecent = Math.abs(now - callEndTime) < 300_000L || Math.abs(now - date) < 300_000L

                    val isValidNumber = number.isNotBlank() &&
                            !number.equals("Unknown", ignoreCase = true) &&
                            number != "-1" && number != "-2" && number != "-3" &&
                            !number.equals("private", ignoreCase = true)

                    if (isRecent && isValidNumber) {
                        val callType = when (typeInt) {
                            CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                            CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                            CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                            CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                            else -> CallType.MISSED
                        }

                        return CallLogEntry(
                            number = number,
                            contactName = name,
                            callType = callType,
                            duration = duration,
                            date = date,
                            simSlot = 1
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query system CallLog", e)
            null
        } finally {
            cursor?.close()
        }
    }
}
