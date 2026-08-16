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

            if (currentRole == UserRole.CLIENT && isServiceSupposedToBeActive) {
                val isRunning = isServiceRunning(applicationContext, SmsBridgeService::class.java)
                Log.d(TAG, "Watchdog check: Client mode active. Is SmsBridgeService running? $isRunning")

                if (!isRunning) {
                    Log.i(TAG, "SmsBridgeService was killed or not running. Restarting service now.")
                    val serviceIntent = Intent(applicationContext, SmsBridgeService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(applicationContext, serviceIntent)
                    } else {
                        applicationContext.startService(serviceIntent)
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
