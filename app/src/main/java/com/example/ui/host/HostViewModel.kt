package com.example.ui.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.SmsRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class HostViewModel(
    private val authRepository: AuthRepository,
    private val smsRepository: SmsRepository
) : ViewModel() {

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

    val isLiveConnected: StateFlow<Boolean> = combine(connectedClients, rawMessages) { clients, messages ->
        clients.isNotEmpty() || messages.isNotEmpty()
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

    val unreadCount: StateFlow<Int> = rawMessages.combine(_searchQuery) { messages, _ ->
        messages.count { !it.read }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isAutoStartConfigured: StateFlow<Boolean> = com.example.SmsBridgeApp.instance.preferencesRepository
        .isAutoStartConfiguredFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAutoStartConfigured(configured: Boolean) {
        viewModelScope.launch {
            com.example.SmsBridgeApp.instance.preferencesRepository.setAutoStartConfigured(configured)
        }
    }

    // Lazy Loading Pagination State
    private val _displayLimit = MutableStateFlow(25)
    val displayLimit: StateFlow<Int> = _displayLimit.asStateFlow()

    val pagedMessages: StateFlow<List<SmsMessage>> = combine(filteredMessages, _displayLimit) { list, limit ->
        list.take(limit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMessagesCount: StateFlow<Int> = filteredMessages.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val hasMoreMessages: StateFlow<Boolean> = combine(filteredMessages, _displayLimit) { list, limit ->
        list.size > limit
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun loadMoreMessages() {
        _displayLimit.value += 25
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
