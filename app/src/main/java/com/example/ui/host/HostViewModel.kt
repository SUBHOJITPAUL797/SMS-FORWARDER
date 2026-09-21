package com.example.ui.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmsBridgeApp
import com.example.data.repository.AuthRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.SmsRepository
import com.example.domain.model.CallRecord
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
    }

    private val _hostCode = MutableStateFlow("")
    val hostCode: StateFlow<String> = _hostCode.asStateFlow()

    init {
        viewModelScope.launch {
            _hostCode.value = authRepository.getHostCode()
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

    val clientDeviceName: StateFlow<String> = connectedClients
        .map { clients ->
            if (clients.isNotEmpty()) {
                val names = clients.mapNotNull { it["clientDeviceName"] as? String }.filter { it.isNotBlank() }.distinct()
                if (names.isNotEmpty()) names.joinToString(", ") else "Client Connected"
            } else {
                "Waiting for Client..."
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
            // Smart Deduplication: deduplicate by messageId or by (sender + body + 15s window)
            val seenKeys = mutableSetOf<String>()
            val deduplicated = mutableListOf<SmsMessage>()
            for (msg in list) {
                val timeBucket = msg.receivedAt / 4_000L
                val contentKey = "${msg.sender.trim()}|${msg.body.trim()}|$timeBucket"
                val idKey = msg.messageId

                if (seenKeys.add(idKey) && seenKeys.add(contentKey)) {
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
            val seenKeys = mutableSetOf<String>()
            val deduplicated = mutableListOf<CallRecord>()
            for (call in list) {
                val timeBucket = call.timestamp / 4_000L
                val contentKey = "${call.phoneNumber.trim()}|${call.callType.name}|$timeBucket"
                val idKey = call.callId

                if (seenKeys.add(idKey) && seenKeys.add(contentKey)) {
                    deduplicated.add(call)
                }
            }
            deduplicated
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLiveConnected: StateFlow<Boolean> = combine(connectedClients, rawMessages, rawCalls) { clients, messages, calls ->
        clients.isNotEmpty() || messages.isNotEmpty() || calls.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val filteredMessages: StateFlow<List<SmsMessage>> = combine(rawMessages, _searchQuery) { messages, query ->
        if (query.isBlank()) {
            messages
        } else {
            messages.filter {
                it.sender.contains(query, ignoreCase = true) ||
                        it.body.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCalls: StateFlow<List<CallRecord>> = combine(rawCalls, _searchQuery) { calls, query ->
        if (query.isBlank()) {
            calls
        } else {
            calls.filter {
                it.phoneNumber.contains(query, ignoreCase = true) ||
                        it.contactName.contains(query, ignoreCase = true) ||
                        it.callType.name.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = rawMessages.combine(_searchQuery) { messages, _ ->
        messages.count { !it.read }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unreadCallsCount: StateFlow<Int> = rawCalls.map { calls ->
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

    fun deleteSingleCall(callId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val code = _hostCode.value
            if (code.isNotEmpty()) {
                callRepository.deleteCall(code, callId)
            }
            onComplete()
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
            kotlinx.coroutines.delay(800)
            _isRefreshing.value = false
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
