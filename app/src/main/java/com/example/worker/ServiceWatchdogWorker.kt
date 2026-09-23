package com.example.worker

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.SmsBridgeApp
import com.example.domain.model.UserRole
import com.example.service.SmsBridgeService
import kotlinx.coroutines.flow.firstOrNull

class ServiceWatchdogWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "sms_bridge_watchdog_work"
        private const val TAG = "ServiceWatchdogWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? SmsBridgeApp ?: return Result.success()
            val userPrefs = app.preferencesRepository

            val currentRole = userPrefs.userRoleFlow.firstOrNull() ?: UserRole.UNSET
            val isServiceSupposedToBeActive = userPrefs.isServiceActiveFlow.firstOrNull() ?: false

            if ((currentRole == UserRole.CLIENT || currentRole == UserRole.HOST) && isServiceSupposedToBeActive) {
                val isRunning = SmsBridgeService.isRunning || isServiceRunning(applicationContext, SmsBridgeService::class.java)
                Log.d(TAG, "Watchdog check: $currentRole mode active. Is SmsBridgeService running? $isRunning")

                if (!isRunning) {
                    Log.i(TAG, "SmsBridgeService was killed or not running. Restarting service now for $currentRole.")
                    val serviceIntent = Intent(applicationContext, SmsBridgeService::class.java).apply {
                        putExtra("role_key", currentRole.key)
                    }
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ContextCompat.startForegroundService(applicationContext, serviceIntent)
                        } else {
                            applicationContext.startService(serviceIntent)
                        }
                    } catch (fse: Exception) {
                        Log.w(TAG, "Direct startForegroundService restricted in background on Android 12+. Dispatched Alarm fallback.", fse)
                        val restartIntent = Intent(applicationContext, com.example.receiver.BootReceiver::class.java).apply {
                            action = "com.example.action.RESTART_SERVICE"
                        }
                        val pendingIntent = android.app.PendingIntent.getBroadcast(
                            applicationContext,
                            1002,
                            restartIntent,
                            android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                        alarmManager?.set(
                            android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            android.os.SystemClock.elapsedRealtime() + 1000L,
                            pendingIntent
                        )
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in watchdog worker", e)
            Result.success()
        }
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
