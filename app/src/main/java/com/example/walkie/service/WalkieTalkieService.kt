package com.example.walkie.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.SmsBridgeApp

/**
 * Foreground Service for the P2P Walkie-Talkie audio feature.
 * Declared with foregroundServiceType="microphone" in AndroidManifest.xml
 * so Android 10+ allows microphone access while the app is backgrounded.
 *
 * Phase 1 (Foundation): Service skeleton — starts, shows persistent notification,
 * and stops cleanly. WebRTC PeerConnectionEngine will be wired in Phase 2.
 */
class WalkieTalkieService : Service() {

    companion object {
        private const val TAG = "WalkieTalkieService"
        const val CHANNEL_ID = "walkie_talkie_channel"
        const val CALL_ALERT_CHANNEL_ID = "walkie_call_alerts"
        const val NOTIFICATION_ID = 1002
        const val INCOMING_CALL_NOTIF_ID = 1003

        const val ACTION_START = "com.example.walkie.ACTION_START"
        const val ACTION_STOP = "com.example.walkie.ACTION_STOP"
        const val ACTION_ANSWER = "com.example.walkie.ACTION_ANSWER"
        const val ACTION_DECLINE = "com.example.walkie.ACTION_DECLINE"

        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_PEER_NAME = "extra_peer_name"

        fun startIntent(context: Context, sessionId: String, peerName: String): Intent =
            Intent(context, WalkieTalkieService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_PEER_NAME, peerName)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, WalkieTalkieService::class.java).apply {
                action = ACTION_STOP
            }

        fun showIncomingCallNotification(context: Context, sessionId: String, callerName: String) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CALL_ALERT_CHANNEL_ID,
                    "Walkie-Talkie Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts for incoming walkie-talkie audio calls"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notif = NotificationCompat.Builder(context, CALL_ALERT_CHANNEL_ID)
                .setContentTitle("📲 Incoming Walkie-Talkie Call")
                .setContentText("Call from $callerName — tap to answer")
                .setSmallIcon(com.example.R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setContentIntent(openPendingIntent)
                .build()

            manager.notify(INCOMING_CALL_NOTIF_ID, notif)
        }

        fun cancelIncomingCallNotification(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(INCOMING_CALL_NOTIF_ID)
        }
    }

    private var currentSessionId: String? = null
    private var peerName: String = "Unknown"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "WalkieTalkieService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentSessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                peerName = intent.getStringExtra(EXTRA_PEER_NAME) ?: "Unknown"
                startServiceForeground(buildActiveNotification(peerName))
                Log.d(TAG, "Walkie-Talkie session started: $currentSessionId with $peerName")
                // Phase 2: PeerConnectionEngine will be initialized here
            }
            ACTION_STOP -> {
                Log.d(TAG, "Walkie-Talkie session stopped")
                stopForegroundSession()
            }
            else -> {
                // Service restarted by system — re-attach to notification if session active
                if (currentSessionId != null) {
                    startServiceForeground(buildActiveNotification(peerName))
                } else {
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startServiceForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        currentSessionId = null
        Log.d(TAG, "WalkieTalkieService destroyed")
    }

    private fun stopForegroundSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun buildActiveNotification(peer: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎙 Walkie-Talkie Active")
            .setContentText("Connected with $peer — tap to open")
            .setSmallIcon(com.example.R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Call",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Walkie-Talkie Audio Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of an active P2P audio session"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
