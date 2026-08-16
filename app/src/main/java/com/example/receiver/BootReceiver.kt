package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i(TAG, "Boot completed event received ($action). Checking role to restart monitoring service...")

            val pendingResult = goAsync()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                try {
                    val app = context.applicationContext as? SmsBridgeApp
                    val userPrefs = app?.preferencesRepository
                    val role = userPrefs?.userRoleFlow?.firstOrNull() ?: UserRole.UNSET
                    val wasActive = userPrefs?.isServiceActiveFlow?.firstOrNull() ?: false

                    if (role == UserRole.CLIENT && wasActive) {
                        Log.i(TAG, "Device is configured as Client with active monitoring. Starting SmsBridgeService...")
                        val serviceIntent = Intent(context, SmsBridgeService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ContextCompat.startForegroundService(context, serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } else {
                        Log.d(TAG, "Device is not in active Client mode (role=$role, wasActive=$wasActive). No service start needed.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in BootReceiver restart flow", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
