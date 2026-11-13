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
        if (!com.example.util.ServiceStarter.isUserUnlocked(context)) {
            Log.w(TAG, "Device still in Direct Boot locked state ($action); skipping service init.")
            return
        }
        Log.i(TAG, "Device Boot/Restart event received ($action). Initializing auto-start sequence...")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = try {
            powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SmsBridge:BootReceiverWakeLock"
            )?.apply { acquire(45_000L) }
        } catch (e: Exception) {
            null
        }

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val app = context.applicationContext as? SmsBridgeApp
                val userPrefs = app?.preferencesRepository
                val role = userPrefs?.userRoleFlow?.firstOrNull() ?: UserRole.UNSET
                val wasActive = userPrefs?.isServiceActiveFlow?.firstOrNull() ?: false

                Log.i(TAG, "Boot state inspection: role=$role, wasActive=$wasActive")

                if (role != UserRole.UNSET) {
                    Log.i(TAG, "Device has configured role ($role). Starting SmsBridgeService safely...")
                    com.example.util.ServiceStarter.startServiceSafely(context, role, fromBoot = true)
                    userPrefs?.setServiceActive(true)
                    app?.smsRepository?.syncAllPendingMessages()
                    app?.callRepository?.syncAllPendingCalls()
                    Log.i(TAG, "SmsBridgeService successfully dispatched on boot for role $role.")
                } else {
                    Log.d(TAG, "Device role is UNSET ($role). Skipping automatic background service start.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in BootReceiver restart flow", e)
            } finally {
                try {
                    wakeLock?.let { if (it.isHeld) it.release() }
                } catch (_: Exception) {}
                pendingResult.finish()
            }
        }
    }
}
