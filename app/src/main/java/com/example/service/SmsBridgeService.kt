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
import kotlinx.coroutines.flow.firstOrNull
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
        @Volatile
        var isRunning: Boolean = false
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private var hostListenerJob: kotlinx.coroutines.Job? = null
    private var heartbeatJob: kotlinx.coroutines.Job? = null
    private var clientLinkObserverJob: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
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

        // 3. Record service active in preferences and handle role-specific monitoring
        serviceScope.launch {
            val app = applicationContext as? SmsBridgeApp ?: return@launch
            app.preferencesRepository.setServiceActive(true)

            app.preferencesRepository.userRoleFlow.collect { role ->
                if (role == com.example.domain.model.UserRole.HOST) {
                    updateServiceNotification("SMS Bridge Host Active", "Listening for incoming forwarded SMS in real time")
                    clientLinkObserverJob?.cancel()
                    clientLinkObserverJob = null
                    hostListenerJob?.cancel()
                    hostListenerJob = launch {
                        val hostCode = app.authRepository.getHostCode()
                        if (hostCode.isNotEmpty()) {
                            Log.i(TAG, "Host mode active ($hostCode). Starting real-time Firestore SMS and Call listeners...")
                            
                            // 1. SMS Listener
                            launch {
                                var initialLoadComplete = false
                                val knownMessageIds = mutableSetOf<String>()

                                app.smsRepository.observeHostSmsList(hostCode).collect { messages ->
                                    if (!initialLoadComplete) {
                                        messages.forEach { knownMessageIds.add(it.messageId) }
                                        initialLoadComplete = true
                                        Log.d(TAG, "Host listener initialized with ${messages.size} existing messages.")
                                    } else {
                                        val now = System.currentTimeMillis()
                                        for (msg in messages) {
                                            if (knownMessageIds.add(msg.messageId)) {
                                                val isRecent = Math.abs(now - msg.receivedAt) < 15 * 60 * 1000L ||
                                                        (msg.uploadedAt > 0 && Math.abs(now - msg.uploadedAt) < 2 * 60 * 1000L)
                                                if (isRecent && !msg.read) {
                                                    Log.i(TAG, "New unread SMS detected on Host: ${msg.messageId} from ${msg.sender}")
                                                    com.example.util.HostNotificationManager.showSmsNotification(
                                                        context = this@SmsBridgeService,
                                                        sender = msg.sender,
                                                        body = msg.body,
                                                        messageId = msg.messageId,
                                                        hostCode = hostCode
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Calls Listener
                            launch {
                                var initialCallsLoadComplete = false
                                val knownCallIds = mutableSetOf<String>()

                                app.callRepository.observeHostCallList(hostCode).collect { calls ->
                                    if (!initialCallsLoadComplete) {
                                        calls.forEach { knownCallIds.add(it.callId) }
                                        initialCallsLoadComplete = true
                                        Log.d(TAG, "Host call listener initialized with ${calls.size} existing calls.")
                                    } else {
                                        val now = System.currentTimeMillis()
                                        for (call in calls) {
                                            if (knownCallIds.add(call.callId)) {
                                                val isRecent = Math.abs(now - call.timestamp) < 15 * 60 * 1000L ||
                                                        (call.uploadedAt > 0 && Math.abs(now - call.uploadedAt) < 2 * 60 * 1000L)
                                                if (isRecent && !call.read) {
                                                    Log.i(TAG, "New call event detected on Host: ${call.callId} (${call.callType}, ${call.phoneNumber})")
                                                    com.example.util.HostNotificationManager.showCallNotification(
                                                        context = this@SmsBridgeService,
                                                        callRecord = call,
                                                        hostCode = hostCode
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (role == com.example.domain.model.UserRole.CLIENT) {
                    updateServiceNotification("SMS & Call Bridge Client Active", "Monitoring incoming SMS and Calls in real time")
                    hostListenerJob?.cancel()
                    hostListenerJob = null
                    app.smsRepository.syncAllPendingMessages()
                    app.callRepository.syncAllPendingCalls()
                    registerCallLogObserver()
                    // Start 5-minute heartbeat to update lastSeenAt on the link doc
                    heartbeatJob?.cancel()
                    heartbeatJob = launch {
                        val clientUid = app.preferencesRepository.getOrCreateDeviceUid()
                        val linkedHostCode = app.preferencesRepository.linkedUidFlow.firstOrNull() ?: ""
                        if (clientUid.isNotEmpty() && linkedHostCode.isNotEmpty()) {
                            while (true) {
                                try {
                                    app.smsRepository.updateClientLastSeen(linkedHostCode, clientUid)
                                    Log.d(TAG, "Client heartbeat: updated lastSeenAt for $clientUid")
                                } catch (e: Exception) {
                                    Log.w(TAG, "Heartbeat failed", e)
                                }
                                kotlinx.coroutines.delay(5 * 60 * 1000L) // 5 minutes
                            }
                        }
                    }

                    // Real-time link monitor — triggers heads-up alert if Host disconnects this device
                    clientLinkObserverJob?.cancel()
                    clientLinkObserverJob = launch {
                        val clientUid = app.preferencesRepository.getOrCreateDeviceUid()
                        val linkedHostCode = app.preferencesRepository.linkedUidFlow.firstOrNull() ?: ""
                        if (clientUid.isNotEmpty() && linkedHostCode.isNotEmpty()) {
                            app.smsRepository.observeClientLink(linkedHostCode, clientUid).collect { doc ->
                                if (doc != null) {
                                    val active = doc["active"] as? Boolean ?: true
                                    if (!active) {
                                        Log.i(TAG, "Client device disconnected by host ($linkedHostCode). Alerting user.")
                                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                                        val disconnectNotif = NotificationCompat.Builder(this@SmsBridgeService, SmsBridgeFcmService.CHANNEL_ID)
                                            .setSmallIcon(R.drawable.ic_notification)
                                            .setContentTitle("⚠️ Disconnected by Host")
                                            .setContentText("Your device was unlinked by Host ($linkedHostCode). Forwarding paused.")
                                            .setStyle(NotificationCompat.BigTextStyle().bigText("Your device was unlinked by Host ($linkedHostCode). SMS and Call forwarding has been paused."))
                                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                                            .setAutoCancel(true)
                                            .build()
                                        notificationManager?.notify(7771, disconnectNotif)

                                        app.preferencesRepository.clearLinkedDevice()
                                        updateServiceNotification("SMS Bridge: Unlinked", "Disconnected by Host ($linkedHostCode)")
                                        heartbeatJob?.cancel()
                                        clientLinkObserverJob?.cancel()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private var callLogObserver: android.database.ContentObserver? = null

    private fun registerCallLogObserver() {
        if (callLogObserver != null) return
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return
        }
        try {
            val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
                    super.onChange(selfChange, uri)
                    serviceScope.launch {
                        try {
                            val app = applicationContext as? SmsBridgeApp ?: return@launch
                            val isEnabled = app.preferencesRepository.isCallForwardingEnabledFlow.firstOrNull() ?: true
                            if (isEnabled) {
                                kotlinx.coroutines.delay(1200L) // Brief delay for system CallLog write completion
                                app.callRepository.syncLatestRecentCalls(limit = 3)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in CallLogObserver onChange", e)
                        }
                    }
                }
            }
            contentResolver.registerContentObserver(android.provider.CallLog.Calls.CONTENT_URI, true, observer)
            callLogObserver = observer
            Log.i(TAG, "Registered real-time CallLogObserver on Client")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register CallLogObserver", e)
        }
    }

    private fun unregisterCallLogObserver() {
        callLogObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                // Ignore
            }
            callLogObserver = null
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager?.canScheduleExactAlarms() == true) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerTime,
                            restartPendingIntent
                        )
                    } catch (se: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerTime,
                            restartPendingIntent
                        )
                    }
                } else {
                    alarmManager?.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerTime,
                        restartPendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        isRunning = false
        unregisterCallLogObserver()
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

    private fun updateServiceNotification(title: String, text: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val updated = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, updated)
    }
}
