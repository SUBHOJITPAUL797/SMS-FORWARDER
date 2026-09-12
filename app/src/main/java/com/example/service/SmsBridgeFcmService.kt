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
import com.example.util.HostNotificationManager
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

        // Show Truecaller-styled rich notification with smart OTP extraction
        serviceScope.launch {
            val app = applicationContext as? SmsBridgeApp
            val hostCode = app?.authRepository?.getHostCode() ?: ""
            HostNotificationManager.showSmsNotification(
                context = this@SmsBridgeFcmService,
                sender = sender,
                body = body,
                messageId = msgId,
                hostCode = hostCode
            )
        }
    }
}
