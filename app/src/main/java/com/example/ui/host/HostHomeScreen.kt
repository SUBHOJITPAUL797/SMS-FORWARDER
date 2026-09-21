package com.example.ui.host

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.SimCard
import com.example.domain.model.CallRecord
import com.example.domain.model.CallType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import com.example.util.OtpExtractor
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.SmsMessage
import com.example.ui.theme.FabAccentBg
import com.example.ui.theme.FabAccentContent
import com.example.ui.theme.StatusOfflineBg
import com.example.ui.theme.StatusOfflineDot
import com.example.ui.theme.StatusOfflineText
import com.example.ui.theme.StatusOnlineBg
import com.example.ui.theme.StatusOnlineDot
import com.example.ui.theme.StatusOnlineText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.SystemUpdate
import com.example.ui.update.InAppUpdateDialog
import com.example.util.UpdateChecker
import kotlinx.coroutines.launch

import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.util.AutoStartPermissionHelper
import com.example.util.FloatingOtpManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HostHomeScreen(
    viewModel: HostViewModel,
    onChangeRole: () -> Unit,
    onOpenDeveloperProfile: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val pagedMessages by viewModel.pagedMessages.collectAsStateWithLifecycle()
    val totalMessagesCount by viewModel.totalMessagesCount.collectAsStateWithLifecycle()
    val hasMoreMessages by viewModel.hasMoreMessages.collectAsStateWithLifecycle()
    val isLoadingOlderMessages by viewModel.isLoadingOlderMessages.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
    val clientDeviceName by viewModel.clientDeviceName.collectAsStateWithLifecycle()
    val hostCode by viewModel.hostCode.collectAsStateWithLifecycle()
    val isLiveConnected by viewModel.isLiveConnected.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isAutoStartConfigured by viewModel.isAutoStartConfigured.collectAsStateWithLifecycle()
    val isFloatingOtpEnabled by viewModel.isFloatingOtpEnabled.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedMessageIds by viewModel.selectedMessageIds.collectAsStateWithLifecycle()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val rawCalls by viewModel.rawCalls.collectAsStateWithLifecycle()
    val filteredCalls by viewModel.filteredCalls.collectAsStateWithLifecycle()
    val pagedCalls by viewModel.pagedCalls.collectAsStateWithLifecycle()
    val totalCallsCount by viewModel.totalCallsCount.collectAsStateWithLifecycle()
    val hasMoreCalls by viewModel.hasMoreCalls.collectAsStateWithLifecycle()
    val isLoadingOlderCalls by viewModel.isLoadingOlderCalls.collectAsStateWithLifecycle()
    val unreadCallsCount by viewModel.unreadCallsCount.collectAsStateWithLifecycle()

    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var availableUpdate by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isBatteryOptimized by remember { mutableStateOf(AutoStartPermissionHelper.isBatteryOptimized(context)) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var messageToDeleteSingle by remember { mutableStateOf<String?>(null) }
    var callToDeleteSingle by remember { mutableStateOf<String?>(null) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(FloatingOtpManager.canDrawOverlays(context)) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notifPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = FloatingOtpManager.canDrawOverlays(context)
                if (hasOverlayPermission && showOverlayPermissionDialog) {
                    showOverlayPermissionDialog = false
                    viewModel.setFloatingOtpEnabled(true)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Automatic update check in background on launch
    LaunchedEffect(Unit) {
        val info = UpdateChecker.checkForUpdates(context)
        if (info.hasUpdate) {
            availableUpdate = info
        }
    }

    // Ensure persistent real-time listener service is active on Host
    LaunchedEffect(Unit) {
        try {
            val serviceIntent = android.content.Intent(context, com.example.service.SmsBridgeService::class.java).apply {
                putExtra("role_key", com.example.domain.model.UserRole.HOST.key)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("HostHomeScreen", "Failed to start SmsBridgeService for Host", e)
        }
    }

    if (availableUpdate != null) {
        InAppUpdateDialog(
            updateInfo = availableUpdate!!,
            onDismiss = { availableUpdate = null }
        )
    }

    // Confirmation dialog for batch delete
    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("Delete Selected Messages?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete ${selectedMessageIds.size} selected message(s)? This will permanently remove them from both this device and the cloud database.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedMessages {
                            Toast.makeText(context, "Deleted selected messages", Toast.LENGTH_SHORT).show()
                        }
                        showBatchDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog for single delete
    if (messageToDeleteSingle != null) {
        AlertDialog(
            onDismissRequest = { messageToDeleteSingle = null },
            title = { Text("Delete Message?", fontWeight = FontWeight.Bold) },
            text = { Text("Delete this message permanently from device and cloud database?", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        messageToDeleteSingle?.let { msgId ->
                            viewModel.deleteSingleMessage(msgId) {
                                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                        messageToDeleteSingle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDeleteSingle = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog for single call delete
    if (callToDeleteSingle != null) {
        AlertDialog(
            onDismissRequest = { callToDeleteSingle = null },
            title = { Text("Delete Call Record?", fontWeight = FontWeight.Bold) },
            text = { Text("Delete this call record permanently from device and cloud database?", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        callToDeleteSingle?.let { callId ->
                            viewModel.deleteSingleCall(callId) {
                                Toast.makeText(context, "Call record deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                        callToDeleteSingle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { callToDeleteSingle = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Permission Dialog for Display Over Other Apps
    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            title = { Text("Display Over Other Apps", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "To show Truecaller-style floating OTP popups over other apps when SMS arrives, Android requires the 'Display over other apps' permission. Tap 'Open Settings' to grant it.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverlayPermissionDialog = false
                        FloatingOtpManager.openOverlaySettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Open Settings", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                if (isSelectionMode) {
                    // Contextual Multi-Select Top App Bar
                    TopAppBar(
                        title = {
                            Text(
                                text = "${selectedMessageIds.size} Selected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.exitSelectionMode() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Exit Selection",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    viewModel.selectAll(pagedMessages.map { it.messageId })
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "Select All",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (selectedMessageIds.isNotEmpty()) {
                                        showBatchDeleteDialog = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Selected",
                                    tint = Color(0xFFDC2626)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                } else {
                    // Standard Top App Bar
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "SMS Bridge",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    letterSpacing = (-0.3).sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (selectedTab == HostTab.MESSAGES && unreadCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "$unreadCount new",
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (selectedTab == HostTab.CALLS && unreadCallsCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = Color(0xFFFEE2E2)
                                    ) {
                                        Text(
                                            text = "$unreadCallsCount new",
                                            color = Color(0xFFDC2626),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            if (selectedTab == HostTab.MESSAGES && unreadCount > 0) {
                                IconButton(
                                    onClick = { viewModel.markAllAsRead() },
                                    modifier = Modifier.testTag("mark_all_read_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Mark all messages as read",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else if (selectedTab == HostTab.CALLS && unreadCallsCount > 0) {
                                IconButton(
                                    onClick = { viewModel.markAllCallsAsRead() },
                                    modifier = Modifier.testTag("mark_all_calls_read_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Mark all calls as read",
                                        tint = Color(0xFFDC2626)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        isCheckingUpdate = true
                                        Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
                                        val info = UpdateChecker.checkForUpdates(context)
                                        isCheckingUpdate = false
                                        if (info.hasUpdate) {
                                            availableUpdate = info
                                        } else {
                                            Toast.makeText(context, "You are using the latest version (v${info.currentVersion})", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = "Check for Updates",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.switchRole(onChangeRole) },
                                modifier = Modifier.testTag("host_switch_role_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Switch Role",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = onOpenDeveloperProfile,
                                modifier = Modifier.testTag("host_developer_profile_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            Brush.sweepGradient(
                                                listOf(
                                                    Color(0xFF9333EA),
                                                    Color(0xFFC084FC),
                                                    Color(0xFF6750A4),
                                                    Color(0xFF9333EA)
                                                )
                                            ),
                                            CircleShape
                                        )
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.dev_subhojit),
                                        contentDescription = "Subhojit Paul - Developer Profile",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    thickness = 1.dp
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.refresh() },
                containerColor = FabAccentBg,
                contentColor = FabAccentContent,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier.testTag("host_refresh_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh SMS",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Notification Permission Warning Banner (Android 13+)
                if (!hasNotificationPermission) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Notifications",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Tap to allow notifications so you receive instant OTP alerts & full messages.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }

                // Host Code & Connection Banner
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "THIS HOST CODE (SHARE WITH CLIENT)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        if (hostCode.isNotEmpty()) {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Host Code", hostCode))
                                            Toast.makeText(context, "Host Code copied: $hostCode", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text(
                                        text = hostCode.ifEmpty { "Generating..." },
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Host Code",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Live status indicator chip
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = if (isLiveConnected) StatusOnlineBg else StatusOfflineBg
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = if (isLiveConnected) StatusOnlineDot else StatusOfflineDot,
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isLiveConnected) "LIVE" else "OFFLINE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp,
                                        color = if (isLiveConnected) StatusOnlineText else StatusOfflineText
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = if (isLiveConnected) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Client: $clientDeviceName",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLiveConnected) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Xiaomi / Redmi AutoStart and Background Persistence Warning Card
                if (!isAutoStartConfigured && (isBatteryOptimized || AutoStartPermissionHelper.isXiaomiOrRedmi())) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFFEF3C7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (AutoStartPermissionHelper.isXiaomiOrRedmi()) "Xiaomi/Redmi Auto-Start Setup" else "Background Battery Exemption",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Enable Auto-Start & set 'No restrictions' so the service runs reliably after reboot.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309),
                                    lineHeight = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    AutoStartPermissionHelper.openAutoStartSettings(context)
                                    viewModel.setAutoStartConfigured(true)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            IconButton(
                                onClick = { viewModel.setAutoStartConfigured(true) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Truecaller-Style Floating OTP Overlay Option Card
                val isDarkTheme = isSystemInDarkTheme()
                val floatingCardBg = if (isFloatingOtpEnabled) {
                    if (isDarkTheme) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
                val floatingBorderColor = if (isFloatingOtpEnabled) {
                    if (isDarkTheme) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF10B981).copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                }
                val floatingTitleColor = if (isFloatingOtpEnabled) {
                    if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                val floatingSubtitleColor = if (isFloatingOtpEnabled) {
                    if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF047857)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                val floatingIconBg = if (isFloatingOtpEnabled) {
                    if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                val floatingIconTint = if (isFloatingOtpEnabled) {
                    if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF059669)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = floatingCardBg),
                    border = BorderStroke(1.dp, floatingBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(floatingIconBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipToFront,
                                    contentDescription = null,
                                    tint = floatingIconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Floating OTP Popup (Truecaller Style)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = floatingTitleColor
                                )
                                Text(
                                    text = "Pop up a 1-tap copy card on your screen when an OTP arrives.",
                                    fontSize = 11.5.sp,
                                    color = floatingSubtitleColor,
                                    lineHeight = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isFloatingOtpEnabled,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        if (!FloatingOtpManager.canDrawOverlays(context)) {
                                            showOverlayPermissionDialog = true
                                        } else {
                                            viewModel.setFloatingOtpEnabled(true)
                                            Toast.makeText(context, "Floating OTP overlay enabled", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        viewModel.setFloatingOtpEnabled(false)
                                        Toast.makeText(context, "Floating OTP overlay disabled", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF10B981)
                                )
                            )
                        }

                        if (isFloatingOtpEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (hasOverlayPermission) "Active · Displays over all apps" else "⚠️ Permission required",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (hasOverlayPermission) {
                                        if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF059669)
                                    } else {
                                        Color(0xFFF87171)
                                    }
                                )

                                OutlinedButton(
                                    onClick = {
                                        if (!FloatingOtpManager.canDrawOverlays(context)) {
                                            showOverlayPermissionDialog = true
                                        } else {
                                            FloatingOtpManager.showFloatingOtp(
                                                context = context,
                                                sender = "Centru Bank",
                                                otp = "881231",
                                                formattedOtp = "881 231",
                                                timeString = "12:30 PM"
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF059669)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Test Popup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    thickness = 1.dp
                )

                // Segmented Tab Bar: [ 📩 Messages (N) | 📞 Calls (N) ]
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Messages Tab
                        val isMessagesSelected = selectedTab == HostTab.MESSAGES
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isMessagesSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (isMessagesSelected) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectTab(HostTab.MESSAGES) }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = if (isMessagesSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Messages",
                                    fontSize = 13.sp,
                                    fontWeight = if (isMessagesSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isMessagesSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (unreadCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "$unreadCount",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                } else if (totalMessagesCount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "($totalMessagesCount)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        // Calls Tab
                        val isCallsSelected = selectedTab == HostTab.CALLS
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCallsSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (isCallsSelected) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectTab(HostTab.CALLS) }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = if (isCallsSelected) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Calls",
                                    fontSize = 13.sp,
                                    fontWeight = if (isCallsSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCallsSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (unreadCallsCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFDC2626)
                                    ) {
                                        Text(
                                            text = "$unreadCallsCount",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                } else if (rawCalls.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(${rawCalls.size})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = if (selectedTab == HostTab.MESSAGES) "Search sender or message content..." else "Search number, caller name or call type...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("host_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                if (selectedTab == HostTab.MESSAGES) {
                    // Message List or Empty State
                    if (pagedMessages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sms,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No matching messages found" else "Waiting for SMS from Client…",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Try searching for a different sender number or keyword." else "When an SMS is received on your linked Client device, it will automatically show up here in real time.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("host_sms_list"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pagedMessages, key = { it.messageId }) { msg ->
                                val isSelected = selectedMessageIds.contains(msg.messageId)
                                SmsCardItem(
                                    message = msg,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    onToggleSelect = { viewModel.toggleSelection(msg.messageId) },
                                    onLongClick = { viewModel.enterSelectionMode(msg.messageId) },
                                    onMarkAsRead = { viewModel.markMessageAsRead(msg.messageId) },
                                    onDelete = { messageToDeleteSingle = msg.messageId },
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val otpResult = OtpExtractor.extractOtp(msg.body)
                                        if (otpResult.isOtp) {
                                            val clip = ClipData.newPlainText("OTP", otpResult.otp)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Copied OTP: ${otpResult.otp}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val clip = ClipData.newPlainText("SMS Body", msg.body)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Copied SMS to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }

                            // Lazy Loading Footer / Pagination Info
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Showing ${pagedMessages.size} of $totalMessagesCount messages (Cached in Encrypted Local Storage)",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (isLoadingOlderMessages) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Fetching older messages from cloud...",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    } else if (hasMoreMessages) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { viewModel.loadMoreMessages() },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth(0.65f)
                                        ) {
                                            Text("Load Older Messages", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(72.dp)) // Padding for FAB
                            }
                        }
                    }
                } else {
                    // CALLS TAB CONTENT
                    if (filteredCalls.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .background(
                                            color = Color(0xFFE0F2FE),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(38.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No matching calls found" else "Waiting for Calls from Client…",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Try searching for a different number or caller name." else "When an incoming, missed, or outgoing call occurs on your linked Client device, its details will appear here automatically in real time.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("host_calls_list"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Quick Call Metrics Chips
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val missedCount = rawCalls.count { it.callType == CallType.MISSED || it.callType == CallType.REJECTED }
                                    val incomingCount = rawCalls.count { it.callType == CallType.INCOMING }
                                    val outgoingCount = rawCalls.count { it.callType == CallType.OUTGOING }

                                    CallMetricPill(
                                        label = "Total",
                                        count = rawCalls.size,
                                        color = Color(0xFF0284C7),
                                        bgColor = Color(0xFFE0F2FE)
                                    )
                                    CallMetricPill(
                                        label = "Missed",
                                        count = missedCount,
                                        color = Color(0xFFDC2626),
                                        bgColor = Color(0xFFFEE2E2)
                                    )
                                    CallMetricPill(
                                        label = "Received",
                                        count = incomingCount,
                                        color = Color(0xFF16A34A),
                                        bgColor = Color(0xFFDCFCE7)
                                    )
                                    if (outgoingCount > 0) {
                                        CallMetricPill(
                                            label = "Outgoing",
                                            count = outgoingCount,
                                            color = Color(0xFF7C3AED),
                                            bgColor = Color(0xFFEDE9FE)
                                        )
                                    }
                                }
                            }

                            items(pagedCalls, key = { it.callId }) { call ->
                                CallCardItem(
                                    call = call,
                                    onMarkAsRead = { viewModel.markCallAsRead(call.callId) },
                                    onDelete = { callToDeleteSingle = call.callId },
                                    onCallBack = {
                                        try {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${call.phoneNumber.trim()}")).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(dialIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onCopyNumber = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Phone Number", call.phoneNumber))
                                        Toast.makeText(context, "Copied: ${call.phoneNumber}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            // Lazy Loading Footer / Pagination Info for Calls
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Showing ${pagedCalls.size} of $totalCallsCount calls (Cached in Encrypted Local Storage)",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (isLoadingOlderCalls) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF0284C7)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Fetching older calls from cloud...",
                                                fontSize = 12.sp,
                                                color = Color(0xFF0284C7),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    } else if (hasMoreCalls) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { viewModel.loadMoreCalls() },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth(0.65f)
                                        ) {
                                            Text("Load Older Calls", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(72.dp)) // Padding for FAB
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SmsCardItem(
    message: SmsMessage,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val formattedTime = remember(message.receivedAt) {
        val now = System.currentTimeMillis()
        if (now - message.receivedAt < DateUtils.DAY_IN_MILLIS) {
            DateUtils.getRelativeTimeSpanString(
                message.receivedAt,
                now,
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        } else {
            SimpleDateFormat("MMM d, yyyy · hh:mm a", Locale.getDefault()).format(Date(message.receivedAt))
        }
    }

    val isUnread = !message.read

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> Color(0xFFD0BCFF).copy(alpha = 0.6f)
                isUnread -> Color(0xFFEADDFF)
                else -> Color(0xFFFFFFFF)
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isUnread -> Color(0xFFD0BCFF)
                else -> Color(0xFFCAC4D0)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnread || isSelected) 2.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        expanded = !expanded
                        if (isUnread) {
                            onMarkAsRead()
                        }
                    }
                },
                onLongClick = onLongClick
            )
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Header Row: Sender + Relative Time + Unread indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message.sender,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                        color = if (isUnread) Color(0xFF21005D) else Color(0xFF1D1B20)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formattedTime,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF49454F)
                        )
                        if (isUnread) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF6750A4), shape = CircleShape)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Body text
                Text(
                    text = message.body,
                    fontSize = 14.sp,
                    fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                    color = if (isUnread) Color(0xFF21005D) else Color(0xFF49454F),
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                // Prominent OTP Container if message contains OTP / verification code
                val itemContext = androidx.compose.ui.platform.LocalContext.current
                val otpResult = remember(message.body) { OtpExtractor.extractOtp(message.body) }
                if (otpResult.isOtp) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF7C3AED).copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "OTP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7C3AED)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = otpResult.formattedOtp,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp,
                                    color = Color(0xFF6D28D9)
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    val clipboard = itemContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("OTP", otpResult.otp)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(itemContext, "Copied OTP: ${otpResult.otp}", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF7C3AED),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy OTP",
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy OTP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom action row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (expanded) "Tap to collapse" else "Tap to expand",
                        fontSize = 11.sp,
                        color = if (isUnread) Color(0xFF21005D).copy(alpha = 0.7f) else Color(0xFF49454F).copy(alpha = 0.7f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete SMS",
                                tint = Color(0xFFDC2626).copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = if (isUnread) Color(0xFF21005D) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallMetricPill(
    label: String,
    count: Int,
    color: Color,
    bgColor: Color
) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = bgColor,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
private fun CallCardItem(
    call: CallRecord,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    onCallBack: () -> Unit,
    onCopyNumber: () -> Unit
) {
    val formattedTime = remember(call.timestamp) {
        val now = System.currentTimeMillis()
        if (now - call.timestamp < DateUtils.DAY_IN_MILLIS) {
            DateUtils.getRelativeTimeSpanString(
                call.timestamp,
                now,
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        } else {
            SimpleDateFormat("MMM d, yyyy · hh:mm a", Locale.getDefault()).format(Date(call.timestamp))
        }
    }

    val isUnread = !call.read

    // Type styling
    val (typeLabel, typeColor, typeBg, typeIcon) = when (call.callType) {
        CallType.MISSED -> Quadruple("Missed Call", Color(0xFFDC2626), Color(0xFFFEE2E2), Icons.Default.CallMissed)
        CallType.INCOMING -> Quadruple("Incoming", Color(0xFF16A34A), Color(0xFFDCFCE7), Icons.Default.CallReceived)
        CallType.OUTGOING -> Quadruple("Outgoing", Color(0xFF2563EB), Color(0xFFDBEAFE), Icons.Default.CallMade)
        CallType.REJECTED -> Quadruple("Declined", Color(0xFFD97706), Color(0xFFFEF3C7), Icons.Default.PhoneDisabled)
    }

    val displayName = call.contactName.trim().ifEmpty { call.phoneNumber.trim() }
    val displayInitial = if (call.contactName.isNotBlank()) {
        call.contactName.trim().first().uppercaseChar().toString()
    } else {
        "#"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) Color(0xFFEFF6FF) else Color.White
        ),
        border = BorderStroke(
            width = if (isUnread) 1.5.dp else 1.dp,
            color = if (isUnread) Color(0xFF93C5FD) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnread) 2.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isUnread) onMarkAsRead()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Caller Initial Avatar with Type Badge
                Box(modifier = Modifier.size(46.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = typeBg,
                        border = BorderStroke(1.dp, typeColor.copy(alpha = 0.4f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = displayInitial,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = typeColor
                            )
                        }
                    }

                    // Mini Call Type Badge in bottom right
                    Surface(
                        shape = CircleShape,
                        color = typeColor,
                        border = BorderStroke(1.5.dp, Color.White),
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name / Phone / Badges
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = displayName,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formattedTime,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            if (isUnread) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF2563EB), CircleShape)
                                )
                            }
                        }
                    }

                    if (call.contactName.isNotBlank() && call.phoneNumber.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = call.phoneNumber,
                            fontSize = 12.5.sp,
                            color = Color(0xFF475569),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Tags: Call Type, Duration, SIM
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = typeBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = typeLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = typeColor
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = call.formattedDuration(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "SIM ${call.simSlot}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = Color(0xFFE2E8F0).copy(alpha = 0.7f),
                thickness = 0.8.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Row: 1-Tap Call Back, Copy Number, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onCallBack,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call Back", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = onCopyNumber,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Call",
                        tint = Color(0xFFDC2626).copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)


