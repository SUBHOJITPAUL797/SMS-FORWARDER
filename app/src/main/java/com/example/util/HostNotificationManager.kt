package com.example.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.SmsBridgeApp
import com.example.domain.model.CallRecord
import com.example.domain.model.CallType
import com.example.receiver.NotificationActionReceiver
import com.example.service.SmsBridgeFcmService
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

object HostNotificationManager {

    private const val TAG = "HostNotificationManager"

    // Deduplication cache to prevent duplicate notifications from FCM and Firestore
    private val recentNotifications = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(50, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > 100
            }
        }
    )

    fun showSmsNotification(
        context: Context,
        sender: String,
        body: String,
        messageId: String,
        hostCode: String? = null
    ) {
        val cleanSender = sender.ifBlank { "SMS Bridge" }
        val cleanBody = body.ifBlank { "New SMS received." }
        val cleanMsgId = messageId.ifBlank { "${cleanSender}_${System.currentTimeMillis()}" }

        // Deduplication: if the exact same message was notified in the last 15 seconds, skip
        val dedupKey = "$cleanMsgId|${cleanSender.trim()}|${cleanBody.trim()}"
        val now = System.currentTimeMillis()
        val lastSeen = recentNotifications[dedupKey]
        if (lastSeen != null && (now - lastSeen) < 15_000L) {
            Log.d(TAG, "Notification skipped (already dispatched within last 15s): $dedupKey")
            return
        }
        recentNotifications[dedupKey] = now

        val notifId = (cleanMsgId.hashCode() and 0x3FFFFFFF)
        val otpResult = OtpExtractor.extractOtp(cleanBody)
        val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        val title = if (otpResult.isOtp) {
            "OTP from $cleanSender"
        } else {
            "📩 $cleanSender"
        }

        // Optional Truecaller-style Floating OTP Overlay Window
        if (otpResult.isOtp) {
            try {
                val app = context.applicationContext as? SmsBridgeApp
                val prefs = app?.preferencesRepository ?: SmsBridgeApp.instance.preferencesRepository
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val isFloatingEnabled = prefs.isFloatingOtpEnabledFlow.firstOrNull() ?: false
                    if (isFloatingEnabled && FloatingOtpManager.canDrawOverlays(context)) {
                        FloatingOtpManager.showFloatingOtp(
                            context = context,
                            sender = cleanSender,
                            otp = otpResult.otp,
                            formattedOtp = otpResult.formattedOtp,
                            timeString = timeString
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch floating OTP overlay", e)
            }
        }

        // 1. Content PendingIntent (tapping opens Host screen in MainActivity)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_msg_id", cleanMsgId)
            putExtra("extra_open_host", true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action PendingIntents
        val copyOtpPendingIntent = if (otpResult.isOtp) {
            val copyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_COPY_OTP
                putExtra(NotificationActionReceiver.EXTRA_OTP, otpResult.otp)
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
            }
            PendingIntent.getBroadcast(
                context,
                notifId + 1,
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val copySmsIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COPY_SMS
            putExtra(NotificationActionReceiver.EXTRA_BODY, cleanBody)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val copySmsPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 2,
            copySmsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_READ
            putExtra(NotificationActionReceiver.EXTRA_MESSAGE_ID, cleanMsgId)
            putExtra(NotificationActionReceiver.EXTRA_HOST_CODE, hostCode)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 3,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 4,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Build RemoteViews Layouts
        val packageName = context.packageName
        val remoteCollapsed: RemoteViews
        val remoteExpanded: RemoteViews

        if (otpResult.isOtp) {
            // OTP Collapsed View
            remoteCollapsed = RemoteViews(packageName, R.layout.notification_otp_collapsed).apply {
                setTextViewText(R.id.notif_title, title)
                setTextViewText(R.id.notif_time, timeString)
                setTextViewText(R.id.notif_otp_text, otpResult.formattedOtp)
                setTextViewText(R.id.notif_preview_text, cleanBody)
                copyOtpPendingIntent?.let {
                    setOnClickPendingIntent(R.id.btn_copy_otp, it)
                }
            }

            // OTP Expanded View
            remoteExpanded = RemoteViews(packageName, R.layout.notification_otp_expanded).apply {
                setTextViewText(R.id.notif_title, title)
                setTextViewText(R.id.notif_time, timeString)
                setTextViewText(R.id.notif_otp_text, otpResult.formattedOtp)
                setTextViewText(R.id.notif_full_body, cleanBody)

                copyOtpPendingIntent?.let {
                    setOnClickPendingIntent(R.id.btn_copy_otp, it)
                }
            }
        } else {
            // Normal Collapsed View
            remoteCollapsed = RemoteViews(packageName, R.layout.notification_normal_collapsed).apply {
                setTextViewText(R.id.notif_title, title)
                setTextViewText(R.id.notif_time, timeString)
                setTextViewText(R.id.notif_body, cleanBody)
            }

            // Normal Expanded View
            remoteExpanded = RemoteViews(packageName, R.layout.notification_normal_expanded).apply {
                setTextViewText(R.id.notif_title, title)
                setTextViewText(R.id.notif_time, timeString)
                setTextViewText(R.id.notif_full_body, cleanBody)
            }
        }

        // 4. Build Notification with Custom Views and Standard Actions
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, SmsBridgeFcmService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(if (otpResult.isOtp) "OTP: ${otpResult.otp}" else cleanBody)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteCollapsed)
            .setCustomBigContentView(remoteExpanded)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Add standard action buttons (for lockscreen / WearOS / system shade fallbacks)
        if (otpResult.isOtp && copyOtpPendingIntent != null) {
            notificationBuilder.addAction(R.drawable.ic_copy, "Copy OTP", copyOtpPendingIntent)
        } else {
            notificationBuilder.addAction(R.drawable.ic_copy, "Copy SMS", copySmsPendingIntent)
        }
        notificationBuilder.addAction(R.drawable.ic_check, "Mark as read", markReadPendingIntent)
        notificationBuilder.addAction(R.drawable.ic_close, "Dismiss", dismissPendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notifId, notificationBuilder.build())
        Log.d(TAG, "Notification displayed: id=$notifId, sender=$cleanSender, isOtp=${otpResult.isOtp}")
    }

    /**
     * Displays a rich call forward notification (Missed, Received, Outgoing) on the Host device.
     */
    fun showCallNotification(
        context: Context,
        callRecord: CallRecord,
        hostCode: String? = null
    ) {
        val callerName = callRecord.getDisplayName()
        val phoneNumber = callRecord.phoneNumber
        val callType = callRecord.callType
        val durationStr = callRecord.formattedDuration()
        val cleanCallId = callRecord.callId.ifBlank { "${phoneNumber}_${callRecord.timestamp}" }

        val dedupKey = "call|$cleanCallId|${callRecord.timestamp / 4_000L}"
        val now = System.currentTimeMillis()
        val lastSeen = recentNotifications[dedupKey]
        if (lastSeen != null && (now - lastSeen) < 15_000L) {
            Log.d(TAG, "Call notification skipped (already dispatched within last 15s): $dedupKey")
            return
        }
        recentNotifications[dedupKey] = now

        val notifId = (cleanCallId.hashCode() and 0x3FFFFFFF)
        val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(callRecord.timestamp))
        val hasContactName = callRecord.contactName.isNotBlank() && !callRecord.contactName.equals(phoneNumber, ignoreCase = true)

        val typeEmoji = when (callType) {
            CallType.MISSED -> "📵 Missed Call"
            CallType.INCOMING -> "📞 Received Call"
            CallType.OUTGOING -> "📲 Outgoing Call"
            CallType.REJECTED -> "🚫 Declined Call"
        }

        val title = if (hasContactName) {
            "$typeEmoji: ${callRecord.contactName}"
        } else {
            "$typeEmoji: $phoneNumber"
        }

        val contentText = if (hasContactName) {
            "$phoneNumber · $durationStr · $timeString"
        } else {
            "$durationStr · $timeString"
        }

        val bigText = buildString {
            if (hasContactName) {
                append("👤 Contact: ${callRecord.contactName}\n")
            }
            append("📞 Phone: $phoneNumber\n")
            append("⏱ Duration: $durationStr\n")
            append("🕒 Time: $timeString\n")
            val dev = callRecord.clientDeviceName.ifBlank { "Client Phone" }
            append("📱 Device: $dev (SIM ${callRecord.simSlot})")
        }

        // Tap opens Host screen in MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_call_id", cleanCallId)
            putExtra("extra_open_host", true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: 1-Tap Call Back (opens phone dialer with number)
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = android.net.Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val dialPendingIntent = PendingIntent.getActivity(
            context,
            notifId + 1,
            dialIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: 1-Tap Copy Number
        val copyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COPY_CALL_NUMBER
            putExtra(NotificationActionReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val copyPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 2,
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val builder = NotificationCompat.Builder(context, "call_forward_alerts")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(if (callRecord.clientDeviceName.isNotBlank()) callRecord.clientDeviceName else "Forwarded Call")
                    .bigText(bigText)
            )
            .setColor(0xFF0284C7.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_notification, "Call Back", dialPendingIntent)
            .addAction(R.drawable.ic_copy, "Copy Number", copyPendingIntent)

        notificationManager.notify(notifId, builder.build())
        Log.d(TAG, "Call notification displayed: id=$notifId, caller=$callerName, type=$callType")
    }
}
