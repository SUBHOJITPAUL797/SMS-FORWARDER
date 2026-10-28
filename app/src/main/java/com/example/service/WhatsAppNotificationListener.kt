package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.SmsBridgeApp
import com.example.domain.model.SmsMessage
import com.example.domain.model.UserRole
import com.example.util.LocationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class WhatsAppNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "WhatsAppNotifListener"
        private const val WHATSAPP_PKG = "com.whatsapp"
        private const val WHATSAPP_BIZ_PKG = "com.whatsapp.w4b"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg != WHATSAPP_PKG && pkg != WHATSAPP_BIZ_PKG) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        if (text.isBlank()) return

        serviceScope.launch {
            try {
                val app = applicationContext as? SmsBridgeApp ?: return@launch
                val userRole = app.preferencesRepository.userRoleFlow.firstOrNull() ?: UserRole.CLIENT
                if (userRole != UserRole.CLIENT) return@launch

                val isLocationEnabled = app.preferencesRepository.isRemoteLocationEnabledFlow.firstOrNull() ?: true
                if (!isLocationEnabled) return@launch

                val customKeyword = app.preferencesRepository.locationTriggerKeywordFlow.firstOrNull() ?: "#LOC"
                if (LocationUtils.isLocationTrigger(text, customKeyword)) {
                    Log.i(TAG, "WhatsApp location trigger received from '$title': '$text'")

                    val clientName = "${Build.MANUFACTURER} ${Build.MODEL}"
                    val bat = LocationUtils.getBatteryInfo(applicationContext)

                    // 1. Fetch current GPS location if permission granted
                    val loc = if (LocationUtils.hasLocationPermission(applicationContext)) {
                        LocationUtils.getCurrentLocation(applicationContext)
                    } else null

                    val replyMessage = LocationUtils.formatLocationSms(loc, bat, clientName, applicationContext)

                    // 2. Try Direct Reply via WhatsApp Notification Action
                    val actions = sbn.notification?.actions
                    if (actions != null) {
                        for (action in actions) {
                            val remoteInputs = action.remoteInputs
                            if (remoteInputs != null && remoteInputs.isNotEmpty()) {
                                for (remoteInput in remoteInputs) {
                                    val replyBundle = Bundle().apply {
                                        putCharSequence(remoteInput.resultKey, replyMessage)
                                    }
                                    val replyIntent = Intent()
                                    RemoteInput.addResultsToIntent(arrayOf(remoteInput), replyIntent, replyBundle)
                                    try {
                                        action.actionIntent.send(applicationContext, 0, replyIntent)
                                        Log.i(TAG, "Successfully dispatched WhatsApp location reply to '$title'!")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed sending WhatsApp reply action", e)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Also upload to Firestore / Host feed
                    val linkedHostCode = app.preferencesRepository.linkedUidFlow.firstOrNull()
                    val clientUid = app.preferencesRepository.getOrCreateDeviceUid()
                    if (!linkedHostCode.isNullOrEmpty()) {
                        val msg = SmsMessage(
                            messageId = "wa_loc_${System.currentTimeMillis()}",
                            sender = "WhatsApp: $title",
                            body = replyMessage,
                            receivedAt = System.currentTimeMillis(),
                            uploadedAt = System.currentTimeMillis(),
                            clientUid = clientUid,
                            clientDeviceName = clientName,
                            read = false
                        )
                        app.smsRepository.uploadPendingMessageDirect(linkedHostCode, msg)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in WhatsApp notification processing", e)
            }
        }
    }
}
