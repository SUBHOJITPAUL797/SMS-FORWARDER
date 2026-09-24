package com.example.ui.client

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmsBridgeApp
import com.example.data.local.QueueStatus
import com.example.data.local.SmsQueueEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.SmsRepository
import com.example.service.SmsBridgeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ClientViewModel(
    private val authRepository: AuthRepository,
    private val smsRepository: SmsRepository,
    private val callRepository: CallRepository = SmsBridgeApp.instance.callRepository
) : ViewModel() {

    private val _isServiceActive = MutableStateFlow(true)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    val linkedHostUid: StateFlow<String?> = authRepository.linkedUidFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalCount: StateFlow<Int> = smsRepository.totalQueueCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val uploadedCount: StateFlow<Int> = smsRepository.uploadedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingCount: StateFlow<Int> = smsRepository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCallsCount: StateFlow<Int> = callRepository.totalQueueCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val uploadedCallsCount: StateFlow<Int> = callRepository.uploadedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingCallsCount: StateFlow<Int> = callRepository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentLocalMessages: StateFlow<List<SmsQueueEntity>> = smsRepository.localQueueMessages
        .map { list ->
            val seen = mutableSetOf<String>()
            val deduped = mutableListOf<SmsQueueEntity>()
            for (msg in list) {
                val timeBucket = msg.receivedAt / 4_000L
                val key = "${msg.sender.trim()}|${msg.body.trim()}|$timeBucket"
                if (seen.add(msg.messageId) && seen.add(key)) {
                    deduped.add(msg)
                }
            }
            deduped
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleService(context: Context, shouldStart: Boolean) {
        _isServiceActive.value = shouldStart
        val serviceIntent = Intent(context, SmsBridgeService::class.java)
        if (shouldStart) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            serviceIntent.action = SmsBridgeService.ACTION_STOP_SERVICE
            context.startService(serviceIntent)
        }
    }

    val isAutoStartConfigured: StateFlow<Boolean> = com.example.SmsBridgeApp.instance.preferencesRepository
        .isAutoStartConfiguredFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAutoStartConfigured(configured: Boolean) {
        viewModelScope.launch {
            com.example.SmsBridgeApp.instance.preferencesRepository.setAutoStartConfigured(configured)
        }
    }

    val isCallForwardingEnabled: StateFlow<Boolean> = com.example.SmsBridgeApp.instance.preferencesRepository
        .isCallForwardingEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setCallForwardingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            com.example.SmsBridgeApp.instance.preferencesRepository.setCallForwardingEnabled(enabled)
        }
    }

    // --- Offline Cellular SMS Fallback StateFlows & Setters ---

    private val prefs = SmsBridgeApp.instance.preferencesRepository

    val isOfflineSmsFallbackEnabled: StateFlow<Boolean> = prefs.isOfflineSmsFallbackEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val fallbackDestinationNumber: StateFlow<String> = prefs.fallbackDestinationNumberFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val preferredSimSlot: StateFlow<Int> = prefs.preferredSimSlotFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dailySmsLimitSim1: StateFlow<Int> = prefs.dailySmsLimitSim1Flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    val dailySmsLimitSim2: StateFlow<Int> = prefs.dailySmsLimitSim2Flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    val dailySmsSentCountSim1: StateFlow<Int> = prefs.dailySmsSentCountSim1Flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dailySmsSentCountSim2: StateFlow<Int> = prefs.dailySmsSentCountSim2Flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isDualSimRolloverEnabled: StateFlow<Boolean> = prefs.isDualSimRolloverEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setOfflineSmsFallbackEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setOfflineSmsFallbackEnabled(enabled) }
    }

    fun setFallbackDestinationNumber(number: String) {
        viewModelScope.launch { prefs.setFallbackDestinationNumber(number) }
    }

    fun setPreferredSimSlot(slot: Int) {
        viewModelScope.launch { prefs.setPreferredSimSlot(slot) }
    }

    fun setDailySmsLimitSim1(limit: Int) {
        viewModelScope.launch { prefs.setDailySmsLimitSim1(limit) }
    }

    fun setDailySmsLimitSim2(limit: Int) {
        viewModelScope.launch { prefs.setDailySmsLimitSim2(limit) }
    }

    fun setDualSimRolloverEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setDualSimRolloverEnabled(enabled) }
    }

    fun sendTestFallbackSms(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val destination = prefs.fallbackDestinationNumberFlow.firstOrNull()?.trim() ?: ""
            if (destination.isBlank()) {
                onResult(false, "Please configure destination mobile number first.")
                return@launch
            }
            val success = smsRepository.trySendOfflineCellularFallback("TEST-SMS", "SMS Bridge offline cellular fallback test message. Forwarding is active!")
            if (success) {
                onResult(true, "Test SMS sent successfully to $destination!")
            } else {
                onResult(false, "Failed to send test SMS. Check SEND_SMS permission or SIM balance.")
            }
        }
    }

    fun syncRealInbox(
        scope: com.example.data.repository.InboxSyncScope = com.example.data.repository.InboxSyncScope.ALL_TIME,
        onResult: (com.example.data.repository.SyncResult?, Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val res = smsRepository.syncRealDeviceInbox(scope)
            if (res.isSuccess) {
                onResult(res.getOrNull(), true)
            } else {
                onResult(null, false)
            }
        }
    }

    fun syncRealCallLog(
        scope: com.example.data.repository.InboxSyncScope = com.example.data.repository.InboxSyncScope.ALL_TIME,
        onResult: (com.example.data.repository.SyncResult?, Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val res = callRepository.syncRealDeviceCallLog(scope)
            if (res.isSuccess) {
                onResult(res.getOrNull(), true)
            } else {
                onResult(null, false)
            }
        }
    }

    fun syncAllPending() {
        viewModelScope.launch {
            smsRepository.syncAllPendingMessages()
            callRepository.syncAllPendingCalls()
        }
    }

    fun updateLinkedHostCode(
        hostCode: String,
        hostPhoneNumber: String? = null,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val cleanCode = hostCode.trim().uppercase()
            authRepository.setUserRoleLocal(com.example.domain.model.UserRole.CLIENT)
            val preferences = com.example.SmsBridgeApp.instance.preferencesRepository
            preferences.setLinkedDevice(cleanCode, "Host ($cleanCode)")

            var phone = hostPhoneNumber?.trim()
            if (phone.isNullOrBlank()) {
                phone = SmsBridgeApp.instance.firestoreSource.getHostPhoneNumber(cleanCode)
            }
            if (!phone.isNullOrBlank()) {
                preferences.setFallbackDestinationNumber(phone)
                preferences.setHostPhoneNumber(phone)
            }

            val res = smsRepository.registerClientLink(cleanCode)
            smsRepository.syncAllPendingMessages()
            if (res.isSuccess) {
                onResult(true, "Connected to Host $cleanCode")
            } else {
                onResult(false, res.exceptionOrNull()?.localizedMessage ?: "Saved locally. Connection pending sync.")
            }
        }
    }

    fun switchRole(onRoleReset: () -> Unit) {
        onRoleReset()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val app = SmsBridgeApp.instance
                val stopIntent = Intent(app, SmsBridgeService::class.java).apply {
                    action = SmsBridgeService.ACTION_STOP_SERVICE
                }
                app.startService(stopIntent)
                app.preferencesRepository.setServiceActive(false)
                authRepository.resetRole()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        onLoggedOut()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val app = SmsBridgeApp.instance
                val stopIntent = Intent(app, SmsBridgeService::class.java).apply {
                    action = SmsBridgeService.ACTION_STOP_SERVICE
                }
                app.startService(stopIntent)
                app.preferencesRepository.setServiceActive(false)
                authRepository.logout()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
