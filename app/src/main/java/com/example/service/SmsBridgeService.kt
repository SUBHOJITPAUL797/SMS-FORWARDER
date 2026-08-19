package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.SmsBridgeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

import android.app.AlarmManager
import android.os.SystemClock
import com.example.receiver.BootReceiver

class SmsBridgeService : Service() {

    companion object {
        const val CHANNEL_ID = "sms_bridge_service"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "SmsBridgeService"
        const val ACTION_STOP_SERVICE = "com.example.service.STOP_SERVICE"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SmsBridgeService onCreate()")

        // 1. Acquire partial wake lock to keep background processing responsive
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SmsBridge:ServiceWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 min fallback

        // 2. Build foreground notification
        val notification = createServiceNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 3. Record service active in preferences and sync pending queue
        serviceScope.launch {
            val app = applicationContext as? SmsBridgeApp
            app?.preferencesRepository?.setServiceActive(true)
            app?.smsRepository?.syncAllPendingMessages()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            serviceScope.launch {
                val app = applicationContext as? SmsBridgeApp
                app?.preferencesRepository?.setServiceActive(false)
            }
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(TAG, "SmsBridgeService onTaskRemoved() triggered (e.g. app cleared from Recents). Arming resurrection alarm...")
        try {
            val restartServiceIntent = Intent(applicationContext, BootReceiver::class.java).apply {
                action = "com.example.action.RESTART_SERVICE"
            }
            val restartPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                1001,
                restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val triggerTime = SystemClock.elapsedRealtime() + 1500L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    restartPendingIntent
                )
            } else {
                alarmManager?.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    restartPendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule resurrection alarm onTaskRemoved", e)
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.d(TAG, "SmsBridgeService onDestroy()")
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createServiceNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Bridge Active")
            .setContentText("Monitoring incoming SMS and forwarding in real time")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
