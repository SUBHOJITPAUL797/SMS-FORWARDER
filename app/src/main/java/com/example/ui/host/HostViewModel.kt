package com.example.ui.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmsBridgeApp
import com.example.data.repository.AuthRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.SmsRepository
import com.example.domain.model.CallRecord
import com.example.domain.model.ConnectedDevice
import com.example.domain.model.SmsMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HostTab {
    MESSAGES,
    CALLS
}

@OptIn(ExperimentalCoroutinesApi::class)
class HostViewModel(
    private val authRepository: AuthRepository,
    private val smsRepository: SmsRepository,
    private val callRepository: CallRepository = SmsBridgeApp.instance.callRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(HostTab.MESSAGES)
    val selectedTab: StateFlow<HostTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: HostTab) {
        _selectedTab.value = tab
        exitSelectionMode()
        exitCallSelectionMode()
    }

    private val _hostCode = MutableStateFlow("")
    val hostCode: StateFlow<String> = _hostCode.asStateFlow()

    init {
        viewModelScope.launch {
            _hostCode.value = authRepository.getHostCode()
        }
    }

    val hostPhoneNumber: StateFlow<String> = SmsBridgeApp.instance.preferencesRepository
        .hostPhoneNumberFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun updateHostPhoneNumber(phone: String) {
        viewModelScope.launch {
            SmsBridgeApp.instance.preferencesRepository.setHostPhoneNumber(phone)
            val code = _hostCode.value
            if (code.isNotBlank()) {
                SmsBridgeApp.instance.firestoreSource.saveHostPhoneNumber(code, phone)
            }
        }
    }

    val connectedClients: StateFlow<List<Map<String, Any>>> = _hostCode
        .flatMapLatest { code ->
            if (code.isNotEmpty()) {
                smsRepository.observeConnectedClients(code)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Typed connected devices — used for the management panel */
    val connectedDevices: StateFlow<List<ConnectedDevice>> = _hostCode
        .flatMapLatest { code ->
            if (code.isNotEmpty()) {
                smsRepository.observeConnectedDevices(code)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientDeviceName: StateFlow<String> = connectedDevices
        .map { devices ->
            val active = devices.filter { it.active }
            when {
                active.isEmpty() -> "Waiting for Client..."
                active.size == 1 -> active.first().clientDeviceName
                else -> "${active.size} Clients Connected"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Waiting for Client...")

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val rawMessages: StateFlow<List<SmsMessage>> = _hostCode
        .flatMapLatest { code ->
            if (code.isNotEmpty()) {
                smsRepository.observeHostSmsList(code)
            } else {
                flowOf(emptyList())
            }
        }
        .map { list ->
            // Smart Deduplication: deduplicate by messageId or by (sender + body + 5s window)
            val seenIds = mutableSetOf<String>()
            val seenContents = mutableSetOf<String>()
            val deduplicated = mutableListOf<SmsMessage>()
            for (msg in list) {
                val timeBucket = msg.receivedAt / 5_000L
                val cleanSender = msg.sender.filter { it.isDigit() }.takeLast(8).ifEmpty { msg.sender.trim() }
                val contentKey = "$cleanSender|${msg.body.trim()}|$timeBucket"
                val idKey = msg.messageId

                if (!seenIds.contains(idKey) && !seenContents.contains(contentKey)) {
                    seenIds.add(idKey)
                    seenContents.add(contentKey)
                    deduplicated.add(msg)
                }
            }
            deduplicated
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawCalls: StateFlow<List<CallRecord>> = _hostCode
        .flatMapLatest { code ->
            if (code.isNotEmpty()) {
                callRepository.observeHostCallList(code)
            } else {
                flowOf(emptyList())
            }
        }
        .map { list ->
            // Smart Deduplication: deduplicate by callId or by (phone + type + 5s window)
            val seenIds = mutableSetOf<String>()
            val seenContents = mutableSetOf<String>()
            val deduplicated = mutableListOf<CallRecord>()
            for (call in list) {
                val timeBucket = call.timestamp / 5_000L
                val cleanNum = call.phoneNumber.filter { it.isDigit() }.takeLast(8).ifEmpty { call.phoneNumber.trim() }
                val contentKey = "$cleanNum|${call.callType.name}|$timeBucket"
                val idKey = call.callId

                if (!seenIds.contains(idKey) && !seenContents.contains(contentKey)) {
                    seenIds.add(idKey)
                    seenContents.add(contentKey)
                    deduplicated.add(call)
                }
            }
            deduplicated
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLiveConnected: StateFlow<Boolean> = combine(connectedClients, rawMessages, rawCalls) { clients, messages, calls ->
        clients.isNotEmpty() || messages.isNotEmpty() || calls.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _selectedDeviceFilter = MutableStateFlow<String?>(null) // null means All Devices
    val selectedDeviceFilter: StateFlow<String?> = _selectedDeviceFilter.asStateFlow()

    fun setSelectedDeviceFilter(clientUid: String?) {
        _selectedDeviceFilter.value = clientUid
    }

    val filteredMessages: StateFlow<List<SmsMessage>> = combine(rawMessages, _searchQuery, _selectedDeviceFilter) { messages, query, deviceFilter ->
        var list = messages
        if (!deviceFilter.isNullOrEmpty()) {
            list = list.filter {
                it.clientUid == deviceFilter || it.clientDeviceName.equals(deviceFilter, ignoreCase = true)
            }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.sender.contains(query, ignoreCase = true) ||
                        it.body.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCalls: StateFlow<List<CallRecord>> = combine(rawCalls, _searchQuery, _selectedDeviceFilter) { calls, query, deviceFilter ->
        var list = calls
        if (!deviceFilter.isNullOrEmpty()) {
            list = list.filter {
                it.clientUid == deviceFilter || it.clientDeviceName.equals(deviceFilter, ignoreCase = true)
            }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.phoneNumber.contains(query, ignoreCase = true) ||
                        it.contactName.contains(query, ignoreCase = true) ||
                        it.callType.name.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = filteredMessages.map { messages ->
        messages.count { !it.read }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unreadCallsCount: StateFlow<Int> = filteredCalls.map { calls ->
        calls.count { !it.read }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isAutoStartConfigured: StateFlow<Boolean> = com.example.SmsBridgeApp.instance.preferencesRepository
        .isAutoStartConfiguredFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAutoStartConfigured(configured: Boolean) {
        viewModelScope.launch {
            com.example.SmsBridgeApp.instance.preferencesRepository.setAutoStartConfigured(configured)
        }
    }

    val isFloatingOtpEnabled: StateFlow<Boolean> = com.example.SmsBridgeApp.instance.preferencesRepository
        .isFloatingOtpEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setFloatingOtpEnabled(enabled: Boolean) {
        viewModelScope.launch {
            com.example.SmsBridgeApp.instance.preferencesRepository.setFloatingOtpEnabled(enabled)
        }
    }

    // Lazy Loading Pagination State - Messages
    private val _displayLimit = MutableStateFlow(25)
    val displayLimit: StateFlow<Int> = _displayLimit.asStateFlow()

    private val _isLoadingOlderMessages = MutableStateFlow(false)
    val isLoadingOlderMessages: StateFlow<Boolean> = _isLoadingOlderMessages.asStateFlow()

    private val _hasMoreCloudMessages = MutableStateFlow(true)
    val hasMoreCloudMessages: StateFlow<Boolean> = _hasMoreCloudMessages.asStateFlow()

    val pagedMessages: StateFlow<List<SmsMessage>> = combine(filteredMessages, _displayLimit) { list, limit ->
        list.take(limit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMessagesCount: StateFlow<Int> = filteredMessages.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val hasMoreMessages: StateFlow<Boolean> = combine(filteredMessages, _displayLimit, _hasMoreCloudMessages, _searchQuery) { list, limit, cloudMore, query ->
        if (query.isNotBlank()) list.size > limit else (list.size > limit) || cloudMore
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun loadMoreMessages() {
        val currentLimit = _displayLimit.value
        val cachedSize = filteredMessages.value.size
        if (currentLimit < cachedSize) {
            _displayLimit.value = currentLimit + 25
        } else if (_hasMoreCloudMessages.value && !_isLoadingOlderMessages.value) {
            viewModelScope.launch {
                _isLoadingOlderMessages.value = true
                val code = _hostCode.value
                if (code.isNotEmpty()) {
                    val res = smsRepository.loadOlderMessagesFromCloud(code, pageSize = 25)
                    if (res.isSuccess) {
                        val count = res.getOrDefault(0)
                        if (count < 25) {
                            _hasMoreCloudMessages.value = false
                        }
                        _displayLimit.value = currentLimit + 25
                    } else {
                        _hasMoreCloudMessages.value = false
                    }
                }
                _isLoadingOlderMessages.value = false
            }
        }
    }

    // Lazy Loading Pagination State - Calls
    private val _callsDisplayLimit = MutableStateFlow(25)
    val callsDisplayLimit: StateFlow<Int> = _callsDisplayLimit.asStateFlow()

    private val _isLoadingOlderCalls = MutableStateFlow(false)
    val isLoadingOlderCalls: StateFlow<Boolean> = _isLoadingOlderCalls.asStateFlow()

    private val _hasMoreCloudCalls = MutableStateFlow(true)
    val hasMoreCloudCalls: StateFlow<Boolean> = _hasMoreCloudCalls.asStateFlow()

    val pagedCalls: StateFlow<List<CallRecord>> = combine(filteredCalls, _callsDisplayLimit) { list, limit ->
        list.take(limit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCallsCount: StateFlow<Int> = filteredCalls.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val hasMoreCalls: StateFlow<Boolean> = combine(filteredCalls, _callsDisplayLimit, _hasMoreCloudCalls, _searchQuery) { list, limit, cloudMore, query ->
        if (query.isNotBlank()) list.size > limit else (list.size > limit) || cloudMore
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun loadMoreCalls() {
        val currentLimit = _callsDisplayLimit.value
        val cachedSize = filteredCalls.value.size
        if (currentLimit < cachedSize) {
            _callsDisplayLimit.value = currentLimit + 25
        } else if (_hasMoreCloudCalls.value && !_isLoadingOlderCalls.value) {
            viewModelScope.launch {
                _isLoadingOlderCalls.value = true
                val code = _hostCode.value
                if (code.isNotEmpty()) {
                    val res = callRepository.loadOlderCallsFromCloud(code, pageSize = 25)
                    if (res.isSuccess) {
                        val count = res.getOrDefault(0)
                        if (count < 25) {
                            _hasMoreCloudCalls.value = false
                        }
                        _callsDisplayLimit.value = currentLimit + 25
                    } else {
                        _hasMoreCloudCalls.value = false
                    }
                }
                _isLoadingOlderCalls.value = false
            }
        }
    }

    // Multi-Select and Deletion State
    private val _selectedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMessageIds: StateFlow<Set<String>> = _selectedMessageIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    fun enterSelectionMode(initialMessageId: String? = null) {
        _isSelectionMode.value = true
        if (initialMessageId != null) {
            _selectedMessageIds.value = setOf(initialMessageId)
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedMessageIds.value = emptySet()
    }

    fun toggleSelection(messageId: String) {
        val current = _selectedMessageIds.value.toMutableSet()
        if (current.contains(messageId)) {
            current.remove(messageId)
        } else {
            current.add(messageId)
        }
        _selectedMessageIds.value = current
        if (current.isEmpty()) {
            _isSelectionMode.value = false
        } else {
            _isSelectionMode.value = true
        }
    }

    fun selectAll(visibleIds: List<String>) {
        _selectedMessageIds.value = visibleIds.toSet()
        _isSelectionMode.value = true
    }

    fun deleteSingleMessage(messageId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val code = _hostCode.value
            if (code.isNotEmpty()) {
                smsRepository.deleteSms(code, messageId)
            }
            onComplete()
        }
    }

    fun deleteSelectedMessages(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val code = _hostCode.value
            val ids = _selectedMessageIds.value.toList()
            if (code.isNotEmpty() && ids.isNotEmpty()) {
                smsRepository.deleteMultipleSms(code, ids)
            }
            exitSelectionMode()
            onComplete()
        }
    }

    // Multi-Select and Deletion State - Calls
    private val _selectedCallIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedCallIds: StateFlow<Set<String>> = _selectedCallIds.asStateFlow()

    private val _isCallSelectionMode = MutableStateFlow(false)
    val isCallSelectionMode: StateFlow<Boolean> = _isCallSelectionMode.asStateFlow()

    fun enterCallSelectionMode(initialCallId: String? = null) {
        _isCallSelectionMode.value = true
        if (initialCallId != null) {
            _selectedCallIds.value = setOf(initialCallId)
        }
    }

    fun exitCallSelectionMode() {
        _isCallSelectionMode.value = false
        _selectedCallIds.value = emptySet()
    }

    fun toggleCallSelection(callId: String) {
        val current = _selectedCallIds.value.toMutableSet()
        if (current.contains(callId)) {
            current.remove(callId)
        } else {
            current.add(callId)
        }
        _selectedCallIds.value = current
        if (current.isEmpty()) {
            _isCallSelectionMode.value = false
        } else {
            _isCallSelectionMode.value = true
        }
    }

    fun selectAllCalls(visibleIds: List<String>) {
        _selectedCallIds.value = visibleIds.toSet()
        _isCallSelectionMode.value = true
    }

    fun deleteSingleCall(callId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val code = _hostCode.value
            if (code.isNotEmpty()) {
                callRepository.deleteCall(code, callId)
            }
            onComplete()
        }
    }

    fun deleteSelectedCalls(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val code = _hostCode.value
            val ids = _selectedCallIds.value.toList()
            if (code.isNotEmpty() && ids.isNotEmpty()) {
                callRepository.deleteMultipleCalls(code, ids)
            }
            exitCallSelectionMode()
            onComplete()
        }
    }

    fun deleteMultipleCallIds(callIds: List<String>, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val code = _hostCode.value
            if (code.isNotEmpty() && callIds.isNotEmpty()) {
                callRepository.deleteMultipleCalls(code, callIds)
            }
            onComplete()
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            val code = _hostCode.value
            if (code.isNotEmpty()) {
                smsRepository.markAsRead(code, messageId)
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val code = _hostCode.value
            if (code.isNotEmpty()) {
                smsRepository.markAllAsRead(code)
            }
        }
    }


    fun markCallAsRead(callId: String) {
        viewModelScope.launch {
            val code = _hostCode.value
            if (code.isNotEmpty()) {
                callRepository.markCallAsRead(code, callId)
            }
        }
    }

    fun markAllCallsAsRead() {
        viewModelScope.launch {
            val code = _hostCode.value
            if (code.isNotEmpty()) {
                callRepository.markAllCallsAsRead(code)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val code = _hostCode.value
            if (code.isNotEmpty()) {
                callRepository.startHostSync(code)
                smsRepository.startHostSync(code)
                callRepository.loadOlderCallsFromCloud(code, pageSize = 25)
                smsRepository.loadOlderMessagesFromCloud(code, pageSize = 25)
            }
            kotlinx.coroutines.delay(500)
            _isRefreshing.value = false
        }
    }

    fun switchRole(onRoleReset: () -> Unit) {
        onRoleReset()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val app = SmsBridgeApp.instance
                val stopIntent = android.content.Intent(app, com.example.service.SmsBridgeService::class.java).apply {
                    action = com.example.service.SmsBridgeService.ACTION_STOP_SERVICE
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
                val stopIntent = android.content.Intent(app, com.example.service.SmsBridgeService::class.java).apply {
                    action = com.example.service.SmsBridgeService.ACTION_STOP_SERVICE
                }
                app.startService(stopIntent)
                app.preferencesRepository.setServiceActive(false)
                authRepository.logout()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Disconnects a specific client device from this Host.
     * The link is marked inactive in Firestore — the client stops forwarding on next sync.
     */
    fun disconnectClient(clientUid: String) {
        viewModelScope.launch {
            val code = _hostCode.value
            if (code.isNotEmpty()) {
                smsRepository.disconnectClient(code, clientUid)
            }
        }
    }
}
