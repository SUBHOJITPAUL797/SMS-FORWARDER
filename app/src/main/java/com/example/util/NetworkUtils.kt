package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

object NetworkUtils {
    private const val TAG = "NetworkUtils"

    /**
     * Checks if the device has an active internet connection.
     * Validates that an active network exists and has the NET_CAPABILITY_INTERNET capability.
     */
    fun isOnline(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            Log.d(TAG, "isOnline check: hasInternet=$hasInternet")
            hasInternet
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check network connectivity", e)
            false
        }
    }
}
