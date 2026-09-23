package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

data class SimSlotInfo(
    val slotIndex: Int,          // 0 = SIM 1, 1 = SIM 2
    val subscriptionId: Int,
    val displayName: String,     // e.g. "Jio 4G", "Airtel"
    val carrierName: String,
    val phoneNumber: String? = null
) {
    fun userLabel(): String = "SIM ${slotIndex + 1}: $displayName"
}

object SimUtils {
    private const val TAG = "SimUtils"

    /**
     * Inspects device SIM slots and returns details on active SIM cards.
     * Requires READ_PHONE_STATE permission.
     */
    fun getAvailableSims(context: Context): List<SimSlotInfo> {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "READ_PHONE_STATE permission not granted, cannot enumerate SIMs")
            return emptyList()
        }

        try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val subList = sm?.activeSubscriptionInfoList

            if (!subList.isNullOrEmpty()) {
                return subList.map { info ->
                    val slot = info.simSlotIndex.coerceAtLeast(0)
                    val disp = info.displayName?.toString()?.ifBlank { null }
                        ?: info.carrierName?.toString()?.ifBlank { null }
                        ?: "SIM ${slot + 1}"
                    val carrier = info.carrierName?.toString()?.ifBlank { null } ?: "Carrier"
                    
                    var phoneNum: String? = null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        try {
                            phoneNum = sm.getPhoneNumber(info.subscriptionId).ifBlank { null }
                        } catch (_: Exception) {}
                    }
                    if (phoneNum == null) {
                        phoneNum = info.number?.ifBlank { null }
                    }

                    SimSlotInfo(
                        slotIndex = slot,
                        subscriptionId = info.subscriptionId,
                        displayName = disp,
                        carrierName = carrier,
                        phoneNumber = phoneNum
                    )
                }.sortedBy { it.slotIndex }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enumerating active subscription list", e)
        }

        // Single SIM fallback if SubscriptionManager returns empty
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val opName = tm?.networkOperatorName?.ifBlank { null } ?: "SIM 1"
        return listOf(
            SimSlotInfo(
                slotIndex = 0,
                subscriptionId = -1,
                displayName = opName,
                carrierName = opName,
                phoneNumber = null
            )
        )
    }

    /**
     * Sends a real cellular SMS directly through the device's selected SIM card.
     * @param subscriptionId Sub ID of the SIM to send from, or -1 for default.
     * @param destinationNumber Target mobile number to receive the SMS.
     * @param messageText SMS body text to send.
     * @return Result containing the number of message parts (1 or more).
     */
    fun sendCellularSms(
        context: Context,
        subscriptionId: Int?,
        destinationNumber: String,
        messageText: String
    ): Result<Int> {
        val cleanNumber = destinationNumber.filter { it.isDigit() || it == '+' }.trim()
        if (cleanNumber.length < 5) {
            return Result.failure(IllegalArgumentException("Invalid destination phone number: $destinationNumber"))
        }

        if (messageText.isBlank()) {
            return Result.failure(IllegalArgumentException("SMS text is empty"))
        }

        val hasSendPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasSendPermission) {
            return Result.failure(SecurityException("SEND_SMS permission not granted"))
        }

        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (subscriptionId != null && subscriptionId != -1) {
                    context.getSystemService(SmsManager::class.java).createForSubscriptionId(subscriptionId)
                } else {
                    context.getSystemService(SmsManager::class.java)
                }
            } else {
                @Suppress("DEPRECATION")
                if (subscriptionId != null && subscriptionId != -1) {
                    SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                } else {
                    SmsManager.getDefault()
                }
            }

            val parts = smsManager.divideMessage(messageText)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(cleanNumber, null, parts, null, null)
                Log.i(TAG, "Sent multipart cellular SMS (${parts.size} parts) to $cleanNumber via subId $subscriptionId")
            } else {
                smsManager.sendTextMessage(cleanNumber, null, messageText, null, null)
                Log.i(TAG, "Sent single cellular SMS to $cleanNumber via subId $subscriptionId")
            }
            Result.success(parts.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send cellular SMS to $cleanNumber", e)
            Result.failure(e)
        }
    }

    /**
     * Compacts the incoming SMS to maximize chances of fitting in a single 160-char SMS part,
     * protecting the user's daily 100 free SMS allowance.
     */
    fun formatForwardedSms(sender: String, body: String): String {
        val cleanSender = sender.trim().take(16)
        val cleanBody = body.trim()
        val header = "[FWD:$cleanSender]\n"
        val maxBodyLen = 160 - header.length
        return if (cleanBody.length <= maxBodyLen) {
            "$header$cleanBody"
        } else {
            // For longer messages, still send full text; SmsManager will divide into 2 parts
            "$header$cleanBody"
        }
    }
}
