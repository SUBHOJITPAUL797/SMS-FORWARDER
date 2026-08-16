package com.example.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.SmsBridgeApp
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SmsBridgeFcmService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "sms_notifications"
        private const val TAG = "SmsBridgeFcmService"

        private val _newSmsEvents = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 10)
        val newSmsEvents = _newSmsEvents.asSharedFlow()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
        serviceScope.launch {
            val app = applicationContext as? SmsBridgeApp ?: return@launch
            app.preferencesRepository.setFcmToken(token)
            val user = app.authRepository.currentUser
            if (user != null) {
                app.firestoreSource.updateFcmToken(user.uid, token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val sender = data["sender"] ?: remoteMessage.notification?.title ?: "SMS Bridge"
        val body = data["body"] ?: remoteMessage.notification?.body ?: "New forwarded SMS received."
        val msgId = data["msgId"] ?: ""

        // Emit to local shared flow for in-app reaction
        _newSmsEvents.tryEmit(data)

        // Show push notification
        showNotification(sender, body, msgId)
    }

    private fun showNotification(sender: String, body: String, msgId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_msg_id", msgId)
            putExtra("extra_open_host", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📩 $sender")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
