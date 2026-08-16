package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.local.AppDatabase
import com.example.data.remote.AuthSource
import com.example.data.remote.FirestoreSource
import com.example.data.repository.AuthRepository
import com.example.data.repository.PairingRepository
import com.example.data.repository.SmsRepository
import com.example.service.SmsBridgeFcmService
import com.example.service.SmsBridgeService
import com.example.worker.ServiceWatchdogWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.util.concurrent.TimeUnit

class SmsBridgeApp : Application() {

    companion object {
        private const val TAG = "SmsBridgeApp"
        lateinit var instance: SmsBridgeApp
            private set
    }

    lateinit var database: AppDatabase
        private set
    lateinit var preferencesRepository: UserPreferencesRepository
        private set
    lateinit var authSource: AuthSource
        private set
    lateinit var firestoreSource: FirestoreSource
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var pairingRepository: PairingRepository
        private set
    lateinit var smsRepository: SmsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Initialize Firebase if needed
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.smsbridge.v8k2p")
                    .setApiKey("AIzaSyDjVqas7AANFiZLpVUUuxqBXAPwRIdAQzM")
                    .setProjectId("dasmo-scanner-android")
                    .setStorageBucket("dasmo-scanner-android.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
            val firestore = FirebaseFirestore.getInstance()
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization check", e)
        }

        // 2. Initialize Singletons & Dependencies
        database = AppDatabase.getInstance(this)
        preferencesRepository = UserPreferencesRepository(this)
        authSource = AuthSource(FirebaseAuth.getInstance())
        firestoreSource = FirestoreSource(FirebaseFirestore.getInstance())

        authRepository = AuthRepository(authSource, firestoreSource, preferencesRepository)
        pairingRepository = PairingRepository(authSource, firestoreSource, preferencesRepository)
        smsRepository = SmsRepository(this, database, firestoreSource, authSource, preferencesRepository)

        // 3. Create Notification Channels
        createNotificationChannels()

        // 4. Enqueue Periodic Watchdog Worker for Service Resiliency
        scheduleWatchdog()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Foreground Service Channel (Low importance)
            val serviceChannel = NotificationChannel(
                SmsBridgeService.CHANNEL_ID,
                "SMS Bridge Service Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows continuous monitoring status of SMS Bridge on Client devices"
                setShowBadge(false)
            }

            // SMS Push Alert Channel (High importance)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val alertChannel = NotificationChannel(
                SmsBridgeFcmService.CHANNEL_ID,
                "SMS Forward Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant notifications when new SMS is received from Client phone"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
                setSound(defaultSoundUri, audioAttributes)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    private fun scheduleWatchdog() {
        try {
            val watchdogRequest = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                ServiceWatchdogWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                watchdogRequest
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule watchdog worker", e)
        }
    }
}
