package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.PowerManager
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

class PhoneCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PhoneCallReceiver"

        private const val PREFS_NAME = "phone_call_receiver_state"
        private const val KEY_SAVED_NUMBER = "saved_number"
        private const val KEY_START_TIME = "call_start_time"
        private const val KEY_OFFHOOK_TIME = "call_offhook_time"
        private const val KEY_IS_INCOMING = "is_incoming"
        private const val KEY_WAS_ANSWERED = "was_answered"
        private const val KEY_RINGING_CALL_ID = "ringing_call_id"

        // In-memory fallback
        private var lastState = TelephonyManager.EXTRA_STATE_IDLE
        private var callStartTime: Long = 0L
        private var isIncoming: Boolean = false
        private var savedNumber: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)?.trim()

        Log.d(TAG, "Phone state changed: $stateStr, number: ${incomingNumber ?: "[hidden]"}")

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (!incomingNumber.isNullOrBlank()) {
            savedNumber = incomingNumber
            prefs.edit().putString(KEY_SAVED_NUMBER, incomingNumber).apply()
        }

        when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val now = System.currentTimeMillis()
                isIncoming = true
                callStartTime = now
                lastState = TelephonyManager.EXTRA_STATE_RINGING

                prefs.edit()
                    .putLong(KEY_START_TIME, now)
                    .putBoolean(KEY_IS_INCOMING, true)
                    .putBoolean(KEY_WAS_ANSWERED, false)
                    .apply()

                // Immediately dispatch real-time RINGING call event if phone number is known
                val currentNumber = incomingNumber ?: savedNumber ?: prefs.getString(KEY_SAVED_NUMBER, null)
                if (!currentNumber.isNullOrBlank() && isValidPhoneNumber(currentNumber)) {
                    processCallRinging(context, currentNumber, now)
                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val now = System.currentTimeMillis()
                val wasRinging = lastState == TelephonyManager.EXTRA_STATE_RINGING || prefs.getBoolean(KEY_IS_INCOMING, false)
                if (wasRinging) {
                    // Incoming call was answered
                    isIncoming = true
                    prefs.edit()
                        .putBoolean(KEY_WAS_ANSWERED, true)
                        .putLong(KEY_OFFHOOK_TIME, now)
                        .apply()
                } else {
                    // Outgoing call started
                    isIncoming = false
                    callStartTime = now
                    prefs.edit()
                        .putBoolean(KEY_IS_INCOMING, false)
                        .putBoolean(KEY_WAS_ANSWERED, false)
                        .putLong(KEY_START_TIME, now)
                        .putLong(KEY_OFFHOOK_TIME, now)
                        .apply()
                }
                lastState = TelephonyManager.EXTRA_STATE_OFFHOOK
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                val prevState = lastState
                lastState = TelephonyManager.EXTRA_STATE_IDLE

                val persistedStartTime = prefs.getLong(KEY_START_TIME, callStartTime)
                val persistedOffhookTime = prefs.getLong(KEY_OFFHOOK_TIME, 0L)
                val persistedWasAnswered = prefs.getBoolean(KEY_WAS_ANSWERED, false)
                val persistedIsIncoming = prefs.getBoolean(KEY_IS_INCOMING, isIncoming)
                val persistedNumber = prefs.getString(KEY_SAVED_NUMBER, savedNumber)
                val persistedRingingCallId = prefs.getString(KEY_RINGING_CALL_ID, null)

                val effectiveStartTime = if (persistedOffhookTime > 0L) persistedOffhookTime else persistedStartTime
                val fallbackDuration = if (effectiveStartTime > 0L && persistedWasAnswered) {
                    ((System.currentTimeMillis() - effectiveStartTime) / 1000L).toInt().coerceAtLeast(1)
                } else 0

                val fallbackType = if (persistedWasAnswered) {
                    if (persistedIsIncoming) CallType.INCOMING else CallType.OUTGOING
                } else {
                    if (prevState == TelephonyManager.EXTRA_STATE_RINGING || persistedIsIncoming) {
                        CallType.MISSED
                    } else {
                        CallType.MISSED
                    }
                }

                val finalFallbackNumber = incomingNumber ?: savedNumber ?: persistedNumber

                // Process Call Ended asynchronously with WakeLock held
                processCallEnded(
                    context = context,
                    fallbackNumber = finalFallbackNumber,
                    ringingCallId = persistedRingingCallId,
                    timestamp = persistedStartTime,
                    fallbackType = fallbackType,
                    fallbackDuration = fallbackDuration
                )

                // Clear transient call state
                savedNumber = null
                isIncoming = false
                callStartTime = 0L
                prefs.edit().clear().apply()
            }
        }
    }

    /**
     * Dispatches a real-time RINGING call event so the Host device rings immediately.
     */
    private fun processCallRinging(context: Context, phoneNumber: String, timestamp: Long) {
        val pendingResult = goAsync()
        val wakeLock = acquireWakeLock(context, "SmsBridge:CallRingingWakeLock", 25_000L)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val contactName = com.example.util.ContactUtils.resolveContactName(context, phoneNumber)
                val app = context.applicationContext as? SmsBridgeApp
                val callRepo = app?.callRepository ?: SmsBridgeApp.instance.callRepository
                val callId = callRepo.generateCallId(phoneNumber, timestamp, CallType.RINGING)

                // Persist the ringing call ID so processCallEnded updates this exact document
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_RINGING_CALL_ID, callId)
                    .apply()

                Log.i(TAG, "Real-time ringing call alert dispatched: $phoneNumber ($contactName, id=$callId)")

                callRepo.handleIncomingCall(
                    callId = callId,
                    phoneNumber = phoneNumber,
                    contactName = contactName,
                    callType = CallType.RINGING,
                    durationSeconds = 0,
                    timestamp = timestamp,
                    simSlot = 1
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error processing real-time ringing call", e)
            } finally {
                wakeLock?.let { if (it.isHeld) it.release() }
                pendingResult.finish()
            }
        }
    }

    private fun processCallEnded(
        context: Context,
        fallbackNumber: String?,
        ringingCallId: String?,
        timestamp: Long,
        fallbackType: CallType,
        fallbackDuration: Int
    ) {
        val pendingResult = goAsync()
        val wakeLock = acquireWakeLock(context, "SmsBridge:CallEndedWakeLock", 35_000L)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                // Pause to let Android system finish writing to CallLog ContentProvider
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
                    // If not resolved on first check, wait another 1200ms and retry
                    if (callLogInfo == null) {
                        kotlinx.coroutines.delay(1200L)
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

                if (!isValidPhoneNumber(finalNumber)) {
                    Log.w(TAG, "No valid phone number resolved for call event (number='$finalNumber'). Skipping to avoid spurious fake logs.")
                    return@launch
                }

                val app = context.applicationContext as? SmsBridgeApp
                val callRepo = app?.callRepository ?: SmsBridgeApp.instance.callRepository

                // If this call was previously ringing, reuse the ringingCallId so the document/notification updates seamlessly!
                val callId = ringingCallId?.ifBlank { null }
                    ?: callRepo.generateCallId(finalNumber, finalTimestamp, finalCallType)

                Log.i(TAG, "Processed call ended: $finalNumber ($finalCallType, ${finalDuration}s, $finalContactName, id=$callId)")

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
                wakeLock?.let { if (it.isHeld) it.release() }
                pendingResult.finish()
            }
        }
    }

    private fun isValidPhoneNumber(number: String): Boolean {
        if (number.isBlank()) return false
        val trimmed = number.trim()
        if (trimmed.equals("Unknown", ignoreCase = true) ||
            trimmed == "-1" || trimmed == "-2" || trimmed == "-3" ||
            trimmed.equals("private", ignoreCase = true)
        ) {
            return false
        }
        return true
    }

    private fun acquireWakeLock(context: Context, tag: String, timeoutMs: Long): PowerManager.WakeLock? {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wl = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)
            wl?.acquire(timeoutMs)
            wl
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wake lock for $tag", e)
            null
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
                    val callEndTime = date + (duration * 1000L)
                    val isRecent = Math.abs(now - callEndTime) < 300_000L || Math.abs(now - date) < 300_000L

                    if (isRecent && isValidPhoneNumber(number)) {
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
