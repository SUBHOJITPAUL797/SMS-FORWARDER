package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.SmsBridgeApp
import com.example.domain.model.UserRole
import com.example.service.SmsBridgeService
import kotlinx.coroutines.launch

/**
 * Crash-safe Foreground Service starter & stopper for Android 8–15+.
 *
 * Prevents:
 * 1. ForegroundServiceDidNotStartInTimeException (by throttling redundant starts and ensuring
 *    SmsBridgeService always calls startForeground() in both onCreate() and onStartCommand()).
 * 2. ForegroundServiceStartNotAllowedException on Android 12+ when invoked from background.
 * 3. Direct Boot IllegalStateException when device storage is still locked.
 * 4. BackgroundServiceStartNotAllowedException when stopping the service from background coroutines.
 */
object ServiceStarter {

    private const val TAG = "ServiceStarter"
    @Volatile
    private var lastStartAttemptMs: Long = 0L

    fun isUserUnlocked(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val um = context.getSystemService(Context.USER_SERVICE) as? UserManager
                um?.isUserUnlocked ?: true
            } else {
                true
            }
        } catch (e: Exception) {
            true
        }
    }

    fun startServiceSafely(
        context: Context,
        role: UserRole? = null,
        fromBoot: Boolean = false
    ): Boolean {
        if (!isUserUnlocked(context)) {
            Log.w(TAG, "Skipping SmsBridgeService start: device user storage is still locked (Direct Boot).")
            return false
        }

        val now = System.currentTimeMillis()
        // If service is already running and we just started it within the last 2.5 seconds, skip redundant startForegroundService call
        if (SmsBridgeService.isRunning && (now - lastStartAttemptMs) < 2500L && role == null) {
            Log.d(TAG, "SmsBridgeService is already running; skipping duplicate start call.")
            return true
        }
        lastStartAttemptMs = now

        return try {
            val appContext = context.applicationContext
            val serviceIntent = Intent(appContext, SmsBridgeService::class.java).apply {
                if (fromBoot) putExtra("auto_started_from_boot", true)
                if (role != null) putExtra("role_key", role.key)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(appContext, serviceIntent)
            } else {
                appContext.startService(serviceIntent)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not start SmsBridgeService in current state: ${e.message}")
            false
        }
    }

    fun stopServiceSafely(context: Context) {
        try {
            val appContext = context.applicationContext
            val serviceIntent = Intent(appContext, SmsBridgeService::class.java)
            appContext.stopService(serviceIntent)
            SmsBridgeService.isRunning = false
            val app = appContext as? SmsBridgeApp
            if (app != null) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        app.preferencesRepository.setServiceActive(false)
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping SmsBridgeService safely", e)
        }
    }
}
