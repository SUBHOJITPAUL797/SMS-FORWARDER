package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object AutoStartPermissionHelper {

    private const val TAG = "AutoStartHelper"

    fun isXiaomiOrRedmi(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("xiaomi") ||
                manufacturer.contains("redmi") ||
                brand.contains("xiaomi") ||
                brand.contains("redmi") ||
                brand.contains("poco")
    }

    fun isBatteryOptimized(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) != true
    }

    /**
     * Attempts to open the OEM-specific AutoStart permission screen (especially for Xiaomi/Redmi MIUI & HyperOS).
     * Falls back gracefully to App Info or Battery Optimization settings.
     */
    fun openAutoStartSettings(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()

        val intents = mutableListOf<Intent>()

        // 1. Xiaomi / Redmi / Poco (MIUI & HyperOS)
        if (isXiaomiOrRedmi()) {
            intents.add(
                Intent().setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                )
            )
            intents.add(
                Intent().setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.securityscan.MainActivity"
                    )
                )
            )
            intents.add(
                Intent().setComponent(
                    ComponentName(
                        "com.miui.powerkeeper",
                        "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                    )
                ).apply {
                    putExtra("package_name", context.packageName)
                    putExtra("package_label", "SMS Forwarder")
                }
            )
        }

        // 2. Oppo / Realme / ColorOS
        if (manufacturer.contains("oppo") || brand.contains("oppo") || manufacturer.contains("realme") || brand.contains("realme")) {
            intents.add(
                Intent().setComponent(
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                )
            )
            intents.add(
                Intent().setComponent(
                    ComponentName(
                        "com.oppo.safe",
                        "com.oppo.safe.permission.startup.StartupAppListActivity"
                    )
                )
            )
            intents.add(
                Intent().setComponent(
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.startupapp.StartupAppListActivity"
                    )
                )
            )
        }

        // 3. Vivo / iQOO / FuntouchOS
        if (manufacturer.contains("vivo") || brand.contains("vivo") || brand.contains("iqoo")) {
            intents.add(
                Intent().setComponent(
                    ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                )
            )
            intents.add(
                Intent().setComponent(
                    ComponentName(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
                    )
                )
            )
        }

        // 4. Samsung
        if (manufacturer.contains("samsung") || brand.contains("samsung")) {
            intents.add(
                Intent().setComponent(
                    ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.battery.ui.BatteryActivity"
                    )
                )
            )
        }

        // 5. Huawei / Honor
        if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            intents.add(
                Intent().setComponent(
                    ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                )
            )
            intents.add(
                Intent().setComponent(
                    ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                    )
                )
            )
        }

        // 6. Generic Battery Optimization Intent
        intents.add(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )

        // 7. Generic App Settings Fallback
        intents.add(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.i(TAG, "Successfully launched AutoStart intent: ${intent.component ?: intent.action}")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch intent: ${intent.component ?: intent.action}, trying fallback...")
            }
        }

        return false
    }

    /**
     * Directly requests battery optimization exemption if needed
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to open battery optimization settings", ex)
            }
        }
    }
}
