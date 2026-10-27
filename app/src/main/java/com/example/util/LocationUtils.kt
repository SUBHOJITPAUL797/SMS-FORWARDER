package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val provider: String
) {
    val mapsUrl: String
        get() = "https://maps.google.com/?q=${latitude},${longitude}"
}

data class BatteryInfo(
    val percentage: Int,
    val isCharging: Boolean
)

object LocationUtils {
    private const val TAG = "LocationUtils"

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isLocationServiceEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val gps = try { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false }
        val net = try { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false }
        return gps || net
    }

    fun getBatteryInfo(context: Context): BatteryInfo {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            BatteryInfo(pct, isCharging)
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching battery info", e)
            BatteryInfo(-1, false)
        }
    }

    /**
     * Checks if the incoming message body is a location trigger command.
     * Matches keywords like "#LOC", "#LOCATION", "LOCATION", or any message starting with "#LOC".
     */
    fun isLocationTrigger(body: String, customKeyword: String = "#LOC"): Boolean {
        val clean = body.trim()
        val custom = customKeyword.trim()
        if (clean.equals(custom, ignoreCase = true) || clean.startsWith("$custom ", ignoreCase = true)) {
            return true
        }
        if (clean.equals("#LOC", ignoreCase = true) || clean.startsWith("#LOC ", ignoreCase = true)) {
            return true
        }
        if (clean.equals("#LOCATION", ignoreCase = true) || clean.startsWith("#LOCATION ", ignoreCase = true)) {
            return true
        }
        return false
    }

    /**
     * Retrieves the current device location.
     * First checks for a recent lastKnownLocation.
     * If not recent (< 60s), requests a fresh high-accuracy GPS / Network fix with a timeout.
     */
    suspend fun getCurrentLocation(context: Context, timeoutMs: Long = 12_000L): DeviceLocation? {
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "Cannot get location: Location permission not granted")
            return null
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        // 1. Check last known location
        var bestLastKnown: Location? = null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        for (provider in providers) {
            try {
                if (lm.isProviderEnabled(provider)) {
                    val loc = lm.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLastKnown == null || loc.time > bestLastKnown.time) {
                            bestLastKnown = loc
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking lastKnownLocation for $provider: ${e.message}")
            }
        }

        val now = System.currentTimeMillis()
        if (bestLastKnown != null && (now - bestLastKnown.time) < 45_000L && bestLastKnown.accuracy <= 40f) {
            Log.d(TAG, "Using fresh lastKnownLocation (age: ${(now - bestLastKnown.time) / 1000}s, acc: ${bestLastKnown.accuracy}m)")
            return DeviceLocation(
                latitude = bestLastKnown.latitude,
                longitude = bestLastKnown.longitude,
                accuracy = bestLastKnown.accuracy,
                timestamp = bestLastKnown.time,
                provider = bestLastKnown.provider ?: "Cached"
            )
        }

        // 2. Request a fresh fix within timeout
        val freshFix = withTimeoutOrNull(timeoutMs) {
            requestFreshLocationFix(lm, context)
        }

        if (freshFix != null) {
            return DeviceLocation(
                latitude = freshFix.latitude,
                longitude = freshFix.longitude,
                accuracy = freshFix.accuracy,
                timestamp = freshFix.time,
                provider = freshFix.provider ?: "GPS"
            )
        }

        // 3. Fallback to best known if fresh fix timed out
        return bestLastKnown?.let {
            DeviceLocation(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracy = it.accuracy,
                timestamp = it.time,
                provider = "${it.provider ?: "Cached"} (Fallback)"
            )
        }
    }

    private suspend fun requestFreshLocationFix(lm: LocationManager, context: Context): Location? {
        return suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    try { lm.removeUpdates(this) } catch (_: Exception) {}
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            continuation.invokeOnCancellation {
                try { lm.removeUpdates(listener) } catch (_: Exception) {}
            }

            var requested = false
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
                try {
                    if (lm.isProviderEnabled(provider)) {
                        lm.requestLocationUpdates(
                            provider,
                            500L,
                            0f,
                            listener,
                            context.mainLooper
                        )
                        requested = true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed requesting updates from $provider", e)
                }
            }

            if (!requested && continuation.isActive) {
                continuation.resume(null)
            }
        }
    }

    fun formatLocationSms(
        location: DeviceLocation?,
        batteryInfo: BatteryInfo,
        clientName: String = ""
    ): String {
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val batStr = if (batteryInfo.percentage >= 0) {
            val bolt = if (batteryInfo.isCharging) " ⚡" else ""
            "${batteryInfo.percentage}%$bolt"
        } else "N/A"

        return if (location != null) {
            val devicePrefix = if (clientName.isNotBlank()) "[$clientName] " else ""
            "${devicePrefix}📍 Device Location:\n" +
                    "${location.mapsUrl}\n" +
                    "Accuracy: ±${location.accuracy.toInt()}m | Battery: $batStr\n" +
                    "Time: $timeStr"
        } else {
            val devicePrefix = if (clientName.isNotBlank()) "[$clientName] " else ""
            "${devicePrefix}⚠️ Location Request Received, but GPS is currently disabled or unavailable on this device.\n" +
                    "Battery: $batStr | Time: $timeStr"
        }
    }
}
