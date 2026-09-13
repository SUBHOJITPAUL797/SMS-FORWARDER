package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.SmsBridgeApp
import com.example.util.OtpExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionReceiver"

        const val ACTION_COPY_OTP = "com.example.action.COPY_OTP"
        const val ACTION_COPY_SMS = "com.example.action.COPY_SMS"
        const val ACTION_MARK_READ = "com.example.action.MARK_READ"
        const val ACTION_DISMISS = "com.example.action.DISMISS"

        const val EXTRA_OTP = "extra_otp"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_MESSAGE_ID = "extra_message_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_HOST_CODE = "extra_host_code"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        when (action) {
            ACTION_COPY_OTP -> {
                val otp = intent.getStringExtra(EXTRA_OTP) ?: ""
                if (otp.isNotBlank()) {
                    copyToClipboard(context, "OTP", otp)
                    showToast(context, "Copied OTP: $otp")
                }
            }

            ACTION_COPY_SMS -> {
                val body = intent.getStringExtra(EXTRA_BODY) ?: ""
                if (body.isNotBlank()) {
                    val otpRes = OtpExtractor.extractOtp(body)
                    if (otpRes.isOtp) {
                        copyToClipboard(context, "OTP", otpRes.otp)
                        showToast(context, "Copied OTP: ${otpRes.otp}")
                    } else {
                        copyToClipboard(context, "SMS", body)
                        showToast(context, "Message copied to clipboard")
                    }
                }
            }

            ACTION_MARK_READ -> {
                if (notifId != -1) {
                    notificationManager?.cancel(notifId)
                }
                val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: ""
                val passedHostCode = intent.getStringExtra(EXTRA_HOST_CODE)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val app = context.applicationContext as? SmsBridgeApp
                        val hostCode = if (!passedHostCode.isNullOrEmpty()) {
                            passedHostCode
                        } else {
                            app?.authRepository?.getHostCode() ?: ""
                        }

                        if (hostCode.isNotEmpty() && messageId.isNotEmpty()) {
                            app?.smsRepository?.markAsRead(hostCode, messageId)
                            Log.d(TAG, "Message $messageId successfully marked as read in Firestore.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error marking message $messageId as read", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
                showToast(context, "Marked as read")
            }

            ACTION_DISMISS -> {
                if (notifId != -1) {
                    notificationManager?.cancel(notifId)
                }
            }
        }
    }

    private fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}
