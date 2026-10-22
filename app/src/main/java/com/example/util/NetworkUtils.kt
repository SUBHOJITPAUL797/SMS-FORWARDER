package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

object NetworkUtils {
    private const val TAG = "NetworkUtils"

    /**
     * Checks if the device has an active, validated internet connection.
     * Uses NET_CAPABILITY_VALIDATED (Android system confirmed internet works) in addition to
     * NET_CAPABILITY_INTERNET to avoid false-positives on captive portals or carrier redirects
     * (common on Jio/Airtel in India when data balance is low or network is transitioning).
     */
    fun isOnline(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val notRestricted = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            // Robust check: when offline (data/wifi off), activeNetwork is null -> returns false immediately.
            // When connected, requires hasInternet and either validated or not-restricted to prevent false
            // negatives on Xiaomi/MIUI, custom ROMs, or private DNS setups.
            val result = hasInternet && (isValidated || notRestricted)
            Log.d(TAG, "isOnline check: hasInternet=$hasInternet, isValidated=$isValidated, notRestricted=$notRestricted => online=$result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check network connectivity", e)
            false
        }
    }
}
