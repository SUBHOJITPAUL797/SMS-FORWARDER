package com.example.ui.client

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.QueueStatus
import com.example.data.local.SmsQueueEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.SmsRepository
import com.example.service.SmsBridgeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ClientViewModel(
    private val authRepository: AuthRepository,
    private val smsRepository: SmsRepository
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

    fun syncRealInbox(onResult: (Int, Boolean) -> Unit) {
        viewModelScope.launch {
            val res = smsRepository.syncRealDeviceInbox(maxCount = 100)
            if (res.isSuccess) {
                onResult(res.getOrDefault(0), true)
            } else {
                onResult(0, false)
            }
        }
    }

    fun syncAllPending() {
        viewModelScope.launch {
            smsRepository.syncAllPendingMessages()
        }
    }

    fun updateLinkedHostCode(hostCode: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val cleanCode = hostCode.trim().uppercase()
            authRepository.setUserRoleLocal(com.example.domain.model.UserRole.CLIENT)
            val preferences = com.example.SmsBridgeApp.instance.preferencesRepository
            preferences.setLinkedDevice(cleanCode, "Host ($cleanCode)")
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
                authRepository.logout()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
