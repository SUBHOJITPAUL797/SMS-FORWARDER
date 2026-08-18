package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.SmsBridgeApp
import com.example.domain.model.UserRole
import com.example.service.SmsBridgeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Device Boot/Restart event received ($action). Initializing auto-start sequence...")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SmsBridge:BootReceiverWakeLock"
        )
        wakeLock?.acquire(45_000L) // Hold CPU awake for 45s while starting foreground service

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val app = context.applicationContext as? SmsBridgeApp
                val userPrefs = app?.preferencesRepository
                val role = userPrefs?.userRoleFlow?.firstOrNull() ?: UserRole.UNSET
                val wasActive = userPrefs?.isServiceActiveFlow?.firstOrNull() ?: false

                Log.i(TAG, "Boot state inspection: role=$role, wasActive=$wasActive")

                // Auto-start for any configured role (Client or Host) that was active or paired
                if (role != UserRole.UNSET) {
                    Log.i(TAG, "Device has configured role ($role). Starting SmsBridgeService immediately...")
                    val serviceIntent = Intent(context, SmsBridgeService::class.java).apply {
                        putExtra("auto_started_from_boot", true)
                        putExtra("role_key", role.key)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    userPrefs?.setServiceActive(true)
                    app?.smsRepository?.syncAllPendingMessages()
                    Log.i(TAG, "SmsBridgeService successfully dispatched on boot for role $role.")
                } else {
                    Log.d(TAG, "Device role is UNSET ($role). Skipping automatic background service start.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in BootReceiver restart flow", e)
            } finally {
                wakeLock?.let { if (it.isHeld) it.release() }
                pendingResult.finish()
            }
        }
    }
}
