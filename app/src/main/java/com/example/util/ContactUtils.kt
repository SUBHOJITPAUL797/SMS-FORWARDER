package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

object ContactUtils {
    private const val TAG = "ContactUtils"

    /**
     * Resolves the saved contact name from the user's phonebook using ContactsContract.PhoneLookup.
     * Returns the contact display name if found, or empty string if not found or permission not granted.
     */
    fun resolveContactName(context: Context, phoneNumber: String): String {
        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.isBlank() || cleanNumber.equals("Unknown", ignoreCase = true)) return ""

        // Check if READ_CONTACTS permission is granted
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return ""
        }

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(cleanNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (idx >= 0) {
                        cursor.getString(idx) ?: ""
                    } else ""
                } else ""
            } ?: ""
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve contact name for $cleanNumber", e)
            ""
        }
    }
}
