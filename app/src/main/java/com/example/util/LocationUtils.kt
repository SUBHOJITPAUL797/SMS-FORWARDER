package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Log
import androidx.annotation.RequiresApi
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
        val fused = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try { lm.isProviderEnabled(LocationManager.FUSED_PROVIDER) } catch (_: Exception) { false }
        } else false
        return gps || net || fused
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
        if (clean.equals("LOCATION", ignoreCase = true) || clean.startsWith("LOCATION ", ignoreCase = true)) {
            return true
        }
        return false
    }

    /**
     * Retrieves current device location with multi-stage fallback:
     * 1. Check all available providers for a recent cached location (< 60s, good accuracy).
     * 2. On Android 11+ (API 30+), try the fast system Fused/Current location API.
     * 3. Request fresh GPS/Network updates within timeoutMs.
     * 4. Fallback to the latest known cached location (even if older or cell-based) so the user never gets an empty response.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context, timeoutMs: Long = 14_000L): DeviceLocation? {
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "Cannot get location: Location permission not granted")
            return null
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        // 1. Check last known location across all available providers
        var bestLastKnown: Location? = null
        val candidateProviders = mutableListOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            candidateProviders.add(0, LocationManager.FUSED_PROVIDER)
        }

        for (provider in candidateProviders) {
            try {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null) {
                    if (bestLastKnown == null || loc.time > bestLastKnown.time) {
                        bestLastKnown = loc
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking lastKnownLocation for $provider: ${e.message}")
            }
        }

        val now = System.currentTimeMillis()
        if (bestLastKnown != null && (now - bestLastKnown.time) < 60_000L && bestLastKnown.accuracy <= 40f) {
            Log.d(TAG, "Using fresh lastKnownLocation (age: ${(now - bestLastKnown.time) / 1000}s, acc: ${bestLastKnown.accuracy}m)")
            return DeviceLocation(
                latitude = bestLastKnown.latitude,
                longitude = bestLastKnown.longitude,
                accuracy = bestLastKnown.accuracy,
                timestamp = bestLastKnown.time,
                provider = bestLastKnown.provider ?: "Cached"
            )
        }

        // 2. On Android 11+ (API 30+), use system getCurrentLocation API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val api30Fix = withTimeoutOrNull(timeoutMs / 2) {
                getCurrentLocationApi30(lm, context)
            }
            if (api30Fix != null) {
                Log.d(TAG, "Acquired fix via Android 11+ getCurrentLocation: ${api30Fix.latitude}, ${api30Fix.longitude} (acc: ${api30Fix.accuracy}m)")
                return DeviceLocation(
                    latitude = api30Fix.latitude,
                    longitude = api30Fix.longitude,
                    accuracy = api30Fix.accuracy,
                    timestamp = api30Fix.time,
                    provider = api30Fix.provider ?: "Fused"
                )
            }
        }

        // 3. Request fresh fix via LocationListener
        val freshFix = withTimeoutOrNull(timeoutMs) {
            requestFreshLocationFix(lm, context)
        }

        if (freshFix != null) {
            Log.d(TAG, "Acquired fresh fix via LocationListener: ${freshFix.latitude}, ${freshFix.longitude} (acc: ${freshFix.accuracy}m)")
            return DeviceLocation(
                latitude = freshFix.latitude,
                longitude = freshFix.longitude,
                accuracy = freshFix.accuracy,
                timestamp = freshFix.time,
                provider = freshFix.provider ?: "GPS"
            )
        }

        // 4. Reliable Fallback: return best known location even if older or cell-tower based
        return bestLastKnown?.let {
            val ageMin = ((now - it.time) / 60000L).coerceAtLeast(0)
            val prov = it.provider ?: "Cached"
            Log.d(TAG, "Falling back to cached location from $ageMin mins ago (acc: ${it.accuracy}m)")
            DeviceLocation(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracy = it.accuracy,
                timestamp = it.time,
                provider = "$prov (${ageMin}m ago)"
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocationApi30(lm: LocationManager, context: Context): Location? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation {
                cancellationSignal.cancel()
            }

            val provider = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && lm.hasProvider(LocationManager.FUSED_PROVIDER) ->
                    LocationManager.FUSED_PROVIDER
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                    LocationManager.NETWORK_PROVIDER
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                    LocationManager.GPS_PROVIDER
                else -> LocationManager.PASSIVE_PROVIDER
            }

            try {
                lm.getCurrentLocation(
                    provider,
                    cancellationSignal,
                    ContextCompat.getMainExecutor(context),
                    { loc ->
                        if (continuation.isActive) {
                            continuation.resume(loc)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.w(TAG, "getCurrentLocationApi30 failed for $provider", e)
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
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
            val providersToTry = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && lm.hasProvider(LocationManager.FUSED_PROVIDER)) {
                providersToTry.add(LocationManager.FUSED_PROVIDER)
            }
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                providersToTry.add(LocationManager.GPS_PROVIDER)
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                providersToTry.add(LocationManager.NETWORK_PROVIDER)
            }

            for (provider in providersToTry) {
                try {
                    lm.requestLocationUpdates(
                        provider,
                        500L,
                        0f,
                        listener,
                        context.mainLooper
                    )
                    requested = true
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
        clientName: String = "",
        context: Context? = null
    ): String {
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val batStr = if (batteryInfo.percentage >= 0) {
            val bolt = if (batteryInfo.isCharging) " ⚡" else ""
            "${batteryInfo.percentage}%$bolt"
        } else "N/A"

        val devicePrefix = if (clientName.isNotBlank()) "[$clientName] " else ""

        if (location != null) {
            val ageSec = (System.currentTimeMillis() - location.timestamp) / 1000
            val ageTag = if (ageSec > 120) " (${ageSec / 60}m ago)" else ""
            return "${devicePrefix}📍 Device Location:\n" +
                    "${location.mapsUrl}\n" +
                    "Accuracy: ±${location.accuracy.toInt()}m$ageTag | Battery: $batStr\n" +
                    "Time: $timeStr"
        }

        // Pinpoint the exact reason when location is unavailable
        val reason = when {
            context != null && !hasLocationPermission(context) ->
                "⚠️ Location permission is NOT granted on this device.\nOpen SMS Bridge on this phone & tap 'Grant Permission'."
            context != null && !isLocationServiceEnabled(context) ->
                "⚠️ Device GPS / Location toggle is turned OFF in phone Quick Settings.\nPlease turn ON Location on the device."
            else ->
                "⚠️ GPS fix timed out (device may be indoors with no satellite signal)."
        }

        return "${devicePrefix}$reason\nBattery: $batStr | Time: $timeStr"
    }
}
