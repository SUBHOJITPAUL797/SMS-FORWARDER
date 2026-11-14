package com.example.ui.host

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.SmsBridgeApp
import com.example.domain.model.CallRecord
import com.example.domain.model.CallType
import com.example.domain.model.ConnectedDevice
import com.example.domain.model.SmsMessage
import com.example.domain.model.UserRole
import com.example.ui.components.PermissionConciergeSheet
import com.example.ui.components.PermissionStatusBanner
import com.example.ui.components.PermissionStepItem
import com.example.ui.pairing.QrScannerOverlay
import com.example.ui.theme.AmberSurface
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralBright
import com.example.ui.theme.CoralSecondary
import com.example.ui.theme.CoralSurface
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CrimsonSurface
import com.example.ui.theme.CyanSurface
import com.example.ui.theme.CyanTertiary
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.update.InAppUpdateDialog
import com.example.util.AutoStartPermissionHelper
import com.example.util.FloatingOtpManager
import com.example.util.OtpExtractor
import com.example.util.QrCodeUtils
import com.example.util.ServiceStarter
import com.example.util.UpdateChecker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HostBottomNavTab(val label: String, val icon: ImageVector) {
    INBOX("Inbox", Icons.Default.Sms),
    CALLS("Calls", Icons.Default.Call),
    DEVICES("Devices", Icons.Default.Devices),
    HUB("Control Hub", Icons.Default.Tune)
}

private enum class InboxFilterMode(val label: String) {
    ALL("All SMS"),
    UNREAD("Unread"),
    OTP_ONLY("OTPs Only")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HostHomeScreen(
    viewModel: HostViewModel,
    onChangeRole: () -> Unit,
    onOpenDeveloperProfile: () -> Unit,
    onOpenWalkieTalkie: () -> Unit = {},
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
    val isCallSelectionMode by viewModel.isCallSelectionMode.collectAsStateWithLifecycle()
    val selectedCallIds by viewModel.selectedCallIds.collectAsStateWithLifecycle()
    val connectedDevices by viewModel.connectedDevices.collectAsStateWithLifecycle()
    val selectedDeviceFilter by viewModel.selectedDeviceFilter.collectAsStateWithLifecycle()

    val rawCalls by viewModel.rawCalls.collectAsStateWithLifecycle()
    val pagedCalls by viewModel.pagedCalls.collectAsStateWithLifecycle()
    val totalCallsCount by viewModel.totalCallsCount.collectAsStateWithLifecycle()
    val hasMoreCalls by viewModel.hasMoreCalls.collectAsStateWithLifecycle()
    val isLoadingOlderCalls by viewModel.isLoadingOlderCalls.collectAsStateWithLifecycle()
    val unreadCallsCount by viewModel.unreadCallsCount.collectAsStateWithLifecycle()
    val hostPhoneNumber by viewModel.hostPhoneNumber.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? ComponentActivity

    var currentBottomTab by rememberSaveable { mutableStateOf(HostBottomNavTab.INBOX) }
    var inboxFilter by rememberSaveable { mutableStateOf(InboxFilterMode.ALL) }

    var availableUpdate by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isBatteryExempt by remember { mutableStateOf(!AutoStartPermissionHelper.isBatteryOptimized(context)) }
    var hasOverlayPermission by remember { mutableStateOf(FloatingOtpManager.canDrawOverlays(context)) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    // Slide-Up Sheets & Dialog States
    var showHostQrSheet by rememberSaveable { mutableStateOf(false) }
    var showPermissionSheet by rememberSaveable { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showBatchDeleteCallsDialog by remember { mutableStateOf(false) }
    var messageDetailTarget by remember { mutableStateOf<SmsMessage?>(null) }
    var callHistoryTarget by remember { mutableStateOf<CallRecord?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            showQrScanner = true
        } else {
            Toast.makeText(context, "Camera permission required to scan QR code", Toast.LENGTH_SHORT).show()
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    BackHandler(enabled = isSelectionMode || isCallSelectionMode || currentBottomTab != HostBottomNavTab.INBOX) {
        when {
            isCallSelectionMode -> viewModel.exitCallSelectionMode()
            isSelectionMode -> viewModel.exitSelectionMode()
            else -> currentBottomTab = HostBottomNavTab.INBOX
        }
    }

    // Safely start Host foreground listener service without crash risk
    LaunchedEffect(Unit) {
        ServiceStarter.startServiceSafely(context, UserRole.HOST)
        val info = UpdateChecker.checkForUpdates(context)
        if (info.hasUpdate) {
            availableUpdate = info
        }
        activity?.intent?.let { intent ->
            if (intent.getStringExtra("selected_tab") == "calls") {
                currentBottomTab = HostBottomNavTab.CALLS
                viewModel.selectTab(HostTab.CALLS)
                intent.removeExtra("selected_tab")
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = FloatingOtpManager.canDrawOverlays(context)
                isBatteryExempt = !AutoStartPermissionHelper.isBatteryOptimized(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
                activity?.intent?.let { intent ->
                    if (intent.getStringExtra("selected_tab") == "calls") {
                        currentBottomTab = HostBottomNavTab.CALLS
                        viewModel.selectTab(HostTab.CALLS)
                        intent.removeExtra("selected_tab")
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hostPermissionSteps = remember(hasNotificationPermission, hasOverlayPermission, isBatteryExempt) {
        listOf(
            PermissionStepItem(
                id = "host_notif",
                title = "Instant OTP & Call Alerts",
                subtitle = "Shows heads-up notifications with 1-tap Copy OTP buttons",
                icon = Icons.Default.Notifications,
                isGranted = hasNotificationPermission,
                isRequired = true,
                actionLabel = "Allow",
                onGrantClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            ),
            PermissionStepItem(
                id = "host_battery",
                title = "Unrestricted Background Sync",
                subtitle = "Keeps live Host stream active even when screen is off",
                icon = Icons.Default.BatteryChargingFull,
                isGranted = isBatteryExempt,
                isRequired = true,
                actionLabel = "Exempt",
                onGrantClick = {
                    AutoStartPermissionHelper.requestIgnoreBatteryOptimizations(context)
                }
            ),
            PermissionStepItem(
                id = "host_overlay",
                title = "Floating OTP Popup Window",
                subtitle = "Truecaller-style OTP card over other apps for instant copy",
                icon = Icons.Default.FlipToFront,
                isGranted = hasOverlayPermission,
                isRequired = false,
                actionLabel = "Enable",
                onGrantClick = {
                    FloatingOtpManager.openOverlaySettings(context)
                }
            )
        )
    }

    // CameraX QR Scanner Overlay
    if (showQrScanner) {
        QrScannerOverlay(
            instructionText = "Point camera at the Client's QR Code",
            onCodeScanned = { rawValue ->
                val parsed = QrCodeUtils.parseScannedQr(rawValue)
                if (parsed != null) {
                    showQrScanner = false
                    scope.launch {
                        val app = context.applicationContext as? SmsBridgeApp
                        val pairingRepo = app?.pairingRepository ?: SmsBridgeApp.instance.pairingRepository
                        val res = pairingRepo.pairAsHost(parsed)
                        if (res.isSuccess) {
                            Toast.makeText(context, "Paired with ${res.getOrNull()}!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Pairing failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    true
                } else {
                    false
                }
            },
            onDismiss = { showQrScanner = false }
        )
        return
    }

    // ── Modals & Slide-Up Sheets ──────────────────────────────────────────────
    if (availableUpdate != null) {
        InAppUpdateDialog(
            updateInfo = availableUpdate!!,
            onDismiss = { availableUpdate = null }
        )
    }

    if (showPermissionSheet) {
        PermissionConciergeSheet(
            title = "Host Permission Concierge",
            subtitle = "Enable instant OTP notifications & floating overlays",
            steps = hostPermissionSteps,
            onGrantAllQuickly = if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            } else null,
            onDismiss = { showPermissionSheet = false }
        )
    }

    if (showHostQrSheet) {
        HostQrModalSheet(
            hostCode = hostCode,
            hostPhoneNumber = hostPhoneNumber,
            onUpdateHostPhoneNumber = { viewModel.updateHostPhoneNumber(it) },
            onCopyCode = {
                if (hostCode.isNotEmpty()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Host Code", hostCode))
                    Toast.makeText(context, "Copied Host Code: $hostCode", Toast.LENGTH_SHORT).show()
                }
            },
            onScanClient = {
                showHostQrSheet = false
                if (hasCameraPermission) {
                    showQrScanner = true
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onDismiss = { showHostQrSheet = false }
        )
    }

    if (messageDetailTarget != null) {
        MessageDetailModalSheet(
            message = messageDetailTarget!!,
            onCopyOtp = { otp ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("OTP", otp))
                Toast.makeText(context, "Copied OTP: $otp", Toast.LENGTH_SHORT).show()
            },
            onCopyFull = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("SMS Content", messageDetailTarget!!.body))
                Toast.makeText(context, "Full message copied", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                val id = messageDetailTarget!!.messageId
                messageDetailTarget = null
                viewModel.deleteSingleMessage(id) {
                    Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { messageDetailTarget = null }
        )
    }

    if (callHistoryTarget != null) {
        CallHistoryModalSheet(
            targetCall = callHistoryTarget!!,
            allCalls = rawCalls,
            onCallBack = { number ->
                try {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${number.trim()}")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(dialIntent)
                } catch (_: Exception) {}
            },
            onCopyNumber = { number ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Phone Number", number))
                Toast.makeText(context, "Copied: $number", Toast.LENGTH_SHORT).show()
            },
            onDeleteSingle = { id ->
                callHistoryTarget = null
                viewModel.deleteSingleCall(id) {
                    Toast.makeText(context, "Call log deleted", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { callHistoryTarget = null }
        )
    }

    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Delete ${selectedMessageIds.size} Messages?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("This permanently deletes the selected messages from this device and cloud storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedMessages {
                            Toast.makeText(context, "Deleted selected messages", Toast.LENGTH_SHORT).show()
                        }
                        showBatchDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError, contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showBatchDeleteCallsDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteCallsDialog = false },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Delete ${selectedCallIds.size} Call Logs?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("This permanently deletes the selected call logs from this device and cloud storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedCalls {
                            Toast.makeText(context, "Deleted selected calls", Toast.LENGTH_SHORT).show()
                        }
                        showBatchDeleteCallsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError, contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteCallsDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // ── Main Obsidian Scaffold ────────────────────────────────────────────────
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = ObsidianBg,
        topBar = {
            if (isSelectionMode || isCallSelectionMode) {
                val selectedCount = if (isCallSelectionMode) selectedCallIds.size else selectedMessageIds.size
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (isCallSelectionMode) viewModel.exitCallSelectionMode()
                                else viewModel.exitSelectionMode()
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = TextPrimary)
                        }
                    },
                    title = {
                        Text(
                            text = "$selectedCount Selected",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (isCallSelectionMode) {
                                    viewModel.selectAllCalls(pagedCalls.map { it.callId })
                                } else {
                                    viewModel.selectAll(pagedMessages.map { it.messageId })
                                }
                            }
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = EmeraldBright)
                        }
                        IconButton(
                            onClick = {
                                if (isCallSelectionMode) showBatchDeleteCallsDialog = true
                                else showBatchDeleteDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = CrimsonError)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianCard)
                )
            } else {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        Brush.linearGradient(listOf(EmeraldPrimary, Color(0xFF059669))),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = ObsidianBg,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Host Hub",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                if (isLiveConnected) EmeraldBright else AmberWarning,
                                                CircleShape
                                            )
                                    )
                                    Text(
                                        text = if (isLiveConnected) "LIVE • $clientDeviceName" else "READY • $clientDeviceName",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLiveConnected) EmeraldBright else TextSecondary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Host Code / QR Quick Pill
                        Surface(
                            onClick = { showHostQrSheet = true },
                            shape = RoundedCornerShape(100.dp),
                            color = ObsidianCard,
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("host_qr_code_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "Host QR Code",
                                    tint = EmeraldBright,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = hostCode.ifBlank { "PAIR" },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Walkie-Talkie Button
                        IconButton(
                            onClick = onOpenWalkieTalkie,
                            modifier = Modifier.testTag("host_walkie_talkie_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(CyanSurface, CircleShape)
                                    .border(1.dp, CyanTertiary.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Walkie-Talkie",
                                    tint = CyanTertiary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // Switch Role Button
                        IconButton(
                            onClick = { viewModel.switchRole(onChangeRole) },
                            modifier = Modifier.testTag("switch_role_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(ObsidianCard, CircleShape)
                                    .border(1.dp, ObsidianBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Switch Role",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Overflow Menu
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextPrimary)
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                containerColor = ObsidianCard
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Mark All Read", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.DoneAll, null, tint = EmeraldBright) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.markAllAsRead()
                                        viewModel.markAllCallsAsRead()
                                        Toast.makeText(context, "All items marked as read", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Permission Concierge", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Shield, null, tint = CoralBright) },
                                    onClick = {
                                        showMoreMenu = false
                                        showPermissionSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Switch to Client Mode", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.SwapHoriz, null, tint = CyanTertiary) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.switchRole(onChangeRole)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isCheckingUpdate) "Checking Update..." else "Check for App Update", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.SystemUpdate, null, tint = EmeraldBright) },
                                    onClick = {
                                        showMoreMenu = false
                                        if (!isCheckingUpdate) {
                                            isCheckingUpdate = true
                                            scope.launch {
                                                val info = UpdateChecker.checkForUpdates(context)
                                                isCheckingUpdate = false
                                                if (info.hasUpdate) {
                                                    availableUpdate = info
                                                } else {
                                                    Toast.makeText(context, "Up to date (${info.currentVersion})", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Developer Profile", color = TextPrimary) },
                                    leadingIcon = {
                                        Image(
                                            painter = painterResource(id = R.drawable.dev_subhojit),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenDeveloperProfile()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = ObsidianSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.border(
                    BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.6f)),
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
            ) {
                HostBottomNavTab.entries.forEach { tab ->
                    val selected = currentBottomTab == tab
                    val badgeCount = when (tab) {
                        HostBottomNavTab.INBOX -> unreadCount
                        HostBottomNavTab.CALLS -> unreadCallsCount
                        HostBottomNavTab.DEVICES -> connectedDevices.count { it.active }
                        HostBottomNavTab.HUB -> 0
                    }

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            currentBottomTab = tab
                            if (tab == HostBottomNavTab.INBOX) viewModel.selectTab(HostTab.MESSAGES)
                            if (tab == HostBottomNavTab.CALLS) viewModel.selectTab(HostTab.CALLS)
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (badgeCount > 0) {
                                        Badge(
                                            containerColor = if (tab == HostBottomNavTab.DEVICES) EmeraldPrimary else CoralSecondary,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ObsidianBg,
                            selectedTextColor = EmeraldBright,
                            indicatorColor = EmeraldPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentBottomTab,
            label = "host_tab_transition",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { tab ->
            when (tab) {
                HostBottomNavTab.INBOX -> HostInboxTab(
                    context = context,
                    messages = pagedMessages,
                    totalMessagesCount = totalMessagesCount,
                    unreadCount = unreadCount,
                    hasMoreMessages = hasMoreMessages,
                    isLoadingOlderMessages = isLoadingOlderMessages,
                    isRefreshing = isRefreshing,
                    searchQuery = searchQuery,
                    inboxFilter = inboxFilter,
                    connectedDevices = connectedDevices,
                    selectedDeviceFilter = selectedDeviceFilter,
                    isSelectionMode = isSelectionMode,
                    selectedMessageIds = selectedMessageIds,
                    permissionSteps = hostPermissionSteps,
                    onOpenPermissionSheet = { showPermissionSheet = true },
                    onOpenQrSheet = { showHostQrSheet = true },
                    onSearchChange = { viewModel.onSearchQueryChanged(it) },
                    onFilterChange = { inboxFilter = it },
                    onSelectDeviceFilter = { viewModel.setSelectedDeviceFilter(it) },
                    onRefresh = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMoreMessages() },
                    onMarkAllRead = { viewModel.markAllAsRead() },
                    onMessageClick = { msg ->
                        if (isSelectionMode) {
                            viewModel.toggleSelection(msg.messageId)
                        } else {
                            if (!msg.read) viewModel.markMessageAsRead(msg.messageId)
                            messageDetailTarget = msg
                        }
                    },
                    onMessageLongClick = { msg ->
                        viewModel.enterSelectionMode(msg.messageId)
                    }
                )

                HostBottomNavTab.CALLS -> HostCallsTab(
                    context = context,
                    calls = pagedCalls,
                    totalCallsCount = totalCallsCount,
                    unreadCallsCount = unreadCallsCount,
                    hasMoreCalls = hasMoreCalls,
                    isLoadingOlderCalls = isLoadingOlderCalls,
                    isRefreshing = isRefreshing,
                    searchQuery = searchQuery,
                    isCallSelectionMode = isCallSelectionMode,
                    selectedCallIds = selectedCallIds,
                    onSearchChange = { viewModel.onSearchQueryChanged(it) },
                    onRefresh = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMoreCalls() },
                    onMarkAllCallsRead = { viewModel.markAllCallsAsRead() },
                    onCallClick = { call ->
                        if (isCallSelectionMode) {
                            viewModel.toggleCallSelection(call.callId)
                        } else {
                            if (!call.read) viewModel.markCallAsRead(call.callId)
                            callHistoryTarget = call
                        }
                    },
                    onCallLongClick = { call ->
                        viewModel.enterCallSelectionMode(call.callId)
                    }
                )

                HostBottomNavTab.DEVICES -> HostDevicesTab(
                    context = context,
                    hostCode = hostCode,
                    connectedDevices = connectedDevices,
                    onOpenQrSheet = { showHostQrSheet = true },
                    onScanClientQr = {
                        if (hasCameraPermission) {
                            showQrScanner = true
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onRequestClientLocation = { clientUid ->
                        viewModel.requestClientLocation(clientUid) { sent ->
                            Toast.makeText(
                                context,
                                if (sent) "Live GPS request sent to client! Watch Inbox for map link." else "Failed to send GPS request.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    onDisconnectClient = { device ->
                        viewModel.disconnectClient(device.clientUid, device.clientDeviceName)
                        Toast.makeText(context, "Unlinked ${device.clientDeviceName}", Toast.LENGTH_SHORT).show()
                    },
                    onOpenWalkieTalkie = onOpenWalkieTalkie
                )

                HostBottomNavTab.HUB -> HostControlHubTab(
                    context = context,
                    hostCode = hostCode,
                    hostPhoneNumber = hostPhoneNumber,
                    isFloatingOtpEnabled = isFloatingOtpEnabled,
                    hasOverlayPermission = hasOverlayPermission,
                    isAutoStartConfigured = isAutoStartConfigured,
                    permissionSteps = hostPermissionSteps,
                    onUpdateHostPhoneNumber = {
                        viewModel.updateHostPhoneNumber(it)
                        Toast.makeText(context, "Host fallback number saved!", Toast.LENGTH_SHORT).show()
                    },
                    onToggleFloatingOtp = { enabled ->
                        if (enabled && !hasOverlayPermission) {
                            FloatingOtpManager.openOverlaySettings(context)
                        } else {
                            viewModel.setFloatingOtpEnabled(enabled)
                        }
                    },
                    onTestFloatingOtp = {
                        if (!FloatingOtpManager.canDrawOverlays(context)) {
                            FloatingOtpManager.openOverlaySettings(context)
                        } else {
                            FloatingOtpManager.showFloatingOtp(
                                context = context,
                                sender = "HDFC Bank",
                                otp = "482910",
                                formattedOtp = "482 910",
                                timeString = "Live Preview · Now"
                            )
                        }
                    },
                    onConfigureAutoStart = {
                        AutoStartPermissionHelper.openAutoStartSettings(context)
                        viewModel.setAutoStartConfigured(true)
                    },
                    onOpenPermissionSheet = { showPermissionSheet = true }
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 1: HOST INBOX (OTP Spotlight, Filter Chips, PullToRefresh Message Feed)
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HostInboxTab(
    context: Context,
    messages: List<SmsMessage>,
    totalMessagesCount: Int,
    unreadCount: Int,
    hasMoreMessages: Boolean,
    isLoadingOlderMessages: Boolean,
    isRefreshing: Boolean,
    searchQuery: String,
    inboxFilter: InboxFilterMode,
    connectedDevices: List<ConnectedDevice>,
    selectedDeviceFilter: String?,
    isSelectionMode: Boolean,
    selectedMessageIds: Set<String>,
    permissionSteps: List<PermissionStepItem>,
    onOpenPermissionSheet: () -> Unit,
    onOpenQrSheet: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterChange: (InboxFilterMode) -> Unit,
    onSelectDeviceFilter: (String?) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onMarkAllRead: () -> Unit,
    onMessageClick: (SmsMessage) -> Unit,
    onMessageLongClick: (SmsMessage) -> Unit
) {
    val pullState = rememberPullToRefreshState()

    val displayedMessages = remember(messages, inboxFilter) {
        when (inboxFilter) {
            InboxFilterMode.ALL -> messages
            InboxFilterMode.UNREAD -> messages.filter { !it.read }
            InboxFilterMode.OTP_ONLY -> messages.filter { OtpExtractor.extractOtp(it.body).isOtp }
        }
    }

    // Latest OTP Spotlight detection
    val latestOtpPair = remember(messages) {
        messages.firstNotNullOfOrNull { msg ->
            val res = OtpExtractor.extractOtp(msg.body)
            if (res.isOtp) msg to res else null
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Show permission banner if any required Host permission is missing
            if (permissionSteps.any { it.isRequired && !it.isGranted }) {
                item {
                    PermissionStatusBanner(
                        steps = permissionSteps,
                        onOpenSheet = onOpenPermissionSheet
                    )
                }
            }

            // Smart OTP Spotlight Hero Card
            if (latestOtpPair != null && searchQuery.isBlank()) {
                val (otpMsg, otpRes) = latestOtpPair
                item(key = "otp_spotlight_${otpMsg.messageId}") {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ObsidianCard,
                        border = BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(listOf(EmeraldPrimary, CyanTertiary))
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            EmeraldSurface.copy(alpha = 0.65f),
                                            CyanSurface.copy(alpha = 0.35f)
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = EmeraldBright,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LATEST OTP • ${otpMsg.sender}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldBright,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = otpRes.formattedOtp.ifBlank { otpRes.otp },
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary,
                                    letterSpacing = 2.sp
                                )
                            }

                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("OTP", otpRes.otp))
                                    Toast.makeText(context, "Copied OTP: ${otpRes.otp}", Toast.LENGTH_SHORT).show()
                                    if (!otpMsg.read) onMessageClick(otpMsg)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = ObsidianBg
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy OTP", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Search & Filter Bar
            item(key = "inbox_search_and_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = {
                            Text("Search sender, OTP, or message text...", fontSize = 13.5.sp, color = TextMuted)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldBright)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ObsidianCard,
                            unfocusedContainerColor = ObsidianCard,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_messages_input")
                    )

                    // Filter Pills Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InboxFilterMode.entries.forEach { mode ->
                            val selected = inboxFilter == mode
                            Surface(
                                onClick = { onFilterChange(mode) },
                                shape = RoundedCornerShape(100.dp),
                                color = if (selected) EmeraldPrimary else ObsidianCard,
                                border = BorderStroke(1.dp, if (selected) EmeraldPrimary else ObsidianBorder)
                            ) {
                                Text(
                                    text = mode.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selected) ObsidianBg else TextSecondary,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }

                        // Multi-device filter chips if clients exist
                        connectedDevices.forEach { device ->
                            val filterKey = device.clientDeviceName.ifBlank { device.clientUid }
                            val selected = selectedDeviceFilter == filterKey || selectedDeviceFilter == device.clientUid
                            Surface(
                                onClick = {
                                    onSelectDeviceFilter(if (selected) null else filterKey)
                                },
                                shape = RoundedCornerShape(100.dp),
                                color = if (selected) CoralSecondary else ObsidianCard,
                                border = BorderStroke(1.dp, if (selected) CoralSecondary else ObsidianBorder)
                            ) {
                                Text(
                                    text = device.clientDeviceName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else TextSecondary,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }

                        if (unreadCount > 0) {
                            Surface(
                                onClick = onMarkAllRead,
                                shape = RoundedCornerShape(100.dp),
                                color = ObsidianElevated,
                                border = BorderStroke(1.dp, ObsidianBorder)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Icon(Icons.Default.DoneAll, null, tint = EmeraldBright, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Mark Read",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldBright,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (displayedMessages.isEmpty()) {
                item(key = "empty_inbox") {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(EmeraldSurface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Sms, null, tint = EmeraldBright, modifier = Modifier.size(30.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No Matching Messages" else "Ready for Incoming SMS",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Link your Client phone using your Host QR code to stream SMS and OTPs here in real time.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onOpenQrSheet,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg)
                            ) {
                                Icon(Icons.Default.QrCode, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Show My Host QR Code", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            } else {
                items(displayedMessages, key = { it.messageId }) { msg ->
                    HostSmsCard(
                        context = context,
                        message = msg,
                        isSelected = selectedMessageIds.contains(msg.messageId),
                        isSelectionMode = isSelectionMode,
                        onClick = { onMessageClick(msg) },
                        onLongClick = { onMessageLongClick(msg) }
                    )
                }

                if (hasMoreMessages) {
                    item(key = "load_more_messages") {
                        OutlinedButton(
                            onClick = onLoadMore,
                            enabled = !isLoadingOlderMessages,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ObsidianBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            if (isLoadingOlderMessages) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = EmeraldBright)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loading older messages...", color = TextSecondary)
                            } else {
                                Text(
                                    text = "Load Older Messages ($totalMessagesCount loaded)",
                                    color = EmeraldBright,
                                    fontWeight = FontWeight.Bold
                                )
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
private fun HostSmsCard(
    context: Context,
    message: SmsMessage,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val otpResult = remember(message.body) { OtpExtractor.extractOtp(message.body) }
    val formattedTime = remember(message.receivedAt) {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(message.receivedAt))
    }
    val initial = remember(message.sender) {
        message.sender.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "S"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = when {
            isSelected -> EmeraldSurface
            !message.read -> ObsidianElevated
            else -> ObsidianCard
        },
        border = BorderStroke(
            width = if (!message.read || isSelected) 1.5.dp else 1.dp,
            color = when {
                isSelected -> EmeraldPrimary
                !message.read -> EmeraldPrimary.copy(alpha = 0.55f)
                otpResult.isOtp -> CyanTertiary.copy(alpha = 0.35f)
                else -> ObsidianBorder
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onClick() },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary, checkmarkColor = ObsidianBg)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (!message.read) EmeraldPrimary.copy(alpha = 0.22f) else ObsidianSurface,
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (!message.read) EmeraldPrimary.copy(alpha = 0.5f) else ObsidianBorder,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (!message.read) EmeraldBright else TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = message.sender,
                                fontSize = 15.sp,
                                fontWeight = if (!message.read) FontWeight.ExtraBold else FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!message.read) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(EmeraldPrimary, CircleShape)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formattedTime,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            if (message.clientDeviceName.isNotBlank()) {
                                Text(
                                    text = " • ${message.clientDeviceName}",
                                    fontSize = 11.sp,
                                    color = EmeraldBright.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 1-Tap Copy OTP Chip if OTP detected
                if (otpResult.isOtp) {
                    Surface(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("OTP", otpResult.otp))
                            Toast.makeText(context, "Copied OTP: ${otpResult.otp}", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = CyanSurface,
                        border = BorderStroke(1.dp, CyanTertiary.copy(alpha = 0.55f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, tint = CyanTertiary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = otpResult.otp,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = CyanTertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = message.body,
                fontSize = 13.5.sp,
                color = if (!message.read) TextPrimary else TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2: HOST CALLS (Live Call Feed, Dialer Callback, Copy Number)
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HostCallsTab(
    context: Context,
    calls: List<CallRecord>,
    totalCallsCount: Int,
    unreadCallsCount: Int,
    hasMoreCalls: Boolean,
    isLoadingOlderCalls: Boolean,
    isRefreshing: Boolean,
    searchQuery: String,
    isCallSelectionMode: Boolean,
    selectedCallIds: Set<String>,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onMarkAllCallsRead: () -> Unit,
    onCallClick: (CallRecord) -> Unit,
    onCallLongClick: (CallRecord) -> Unit
) {
    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "calls_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Search caller name or number...", fontSize = 13.5.sp, color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = CyanTertiary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Default.Clear, null, tint = TextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ObsidianCard,
                            unfocusedContainerColor = ObsidianCard,
                            focusedBorderColor = CyanTertiary,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    if (unreadCallsCount > 0) {
                        IconButton(
                            onClick = onMarkAllCallsRead,
                            modifier = Modifier
                                .size(48.dp)
                                .background(ObsidianCard, RoundedCornerShape(14.dp))
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Mark All Calls Read", tint = CyanTertiary)
                        }
                    }
                }
            }

            if (calls.isEmpty()) {
                item(key = "empty_calls") {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(CyanSurface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Call, null, tint = CyanTertiary, modifier = Modifier.size(30.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Call Events Yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Incoming, missed, and answered calls on your Client phone will stream here automatically.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(calls, key = { it.callId }) { call ->
                    HostCallCard(
                        context = context,
                        call = call,
                        isSelected = selectedCallIds.contains(call.callId),
                        isSelectionMode = isCallSelectionMode,
                        onClick = { onCallClick(call) },
                        onLongClick = { onCallLongClick(call) }
                    )
                }

                if (hasMoreCalls) {
                    item(key = "load_more_calls") {
                        OutlinedButton(
                            onClick = onLoadMore,
                            enabled = !isLoadingOlderCalls,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ObsidianBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isLoadingOlderCalls) "Loading older calls..." else "Load Older Calls ($totalCallsCount loaded)",
                                color = CyanTertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HostCallCard(
    context: Context,
    call: CallRecord,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val (callIcon, callColor, callBg) = when (call.callType) {
        CallType.RINGING -> Triple(Icons.Default.Call, CyanTertiary, CyanSurface)
        CallType.MISSED -> Triple(Icons.Default.CallMissed, CrimsonError, CrimsonSurface)
        CallType.REJECTED -> Triple(Icons.Default.PhoneDisabled, CoralBright, CoralSurface)
        CallType.INCOMING -> Triple(Icons.Default.CallReceived, EmeraldBright, EmeraldSurface)
        CallType.OUTGOING -> Triple(Icons.Default.CallMade, AmberWarning, AmberSurface)
    }

    val formattedTime = remember(call.timestamp) {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(call.timestamp))
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) CyanSurface else ObsidianCard,
        border = BorderStroke(
            width = if (!call.read || isSelected) 1.5.dp else 1.dp,
            color = if (isSelected || !call.read) callColor.copy(alpha = 0.6f) else ObsidianBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        colors = CheckboxDefaults.colors(checkedColor = CyanTertiary, checkmarkColor = ObsidianBg)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(callBg, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(callIcon, contentDescription = null, tint = callColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column {
                    Text(
                        text = call.getDisplayName(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (call.contactName.isNotBlank() && !call.contactName.equals(call.phoneNumber, ignoreCase = true)) {
                        Text(
                            text = call.phoneNumber,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "${call.callType.displayName} • ${call.formattedDuration()} • $formattedTime",
                        fontSize = 11.5.sp,
                        color = callColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${call.phoneNumber.trim()}")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(dialIntent)
                    } catch (_: Exception) {}
                },
                modifier = Modifier
                    .size(38.dp)
                    .background(EmeraldSurface, CircleShape)
            ) {
                Icon(Icons.Default.Phone, contentDescription = "Call Back", tint = EmeraldBright, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 3: HOST DEVICES & RADAR (Pairing Card, Connected Clients, Live GPS Request)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun HostDevicesTab(
    context: Context,
    hostCode: String,
    connectedDevices: List<ConnectedDevice>,
    onOpenQrSheet: () -> Unit,
    onScanClientQr: () -> Unit,
    onRequestClientLocation: (String) -> Unit,
    onDisconnectClient: (ConnectedDevice) -> Unit,
    onOpenWalkieTalkie: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Host Pairing Hero Card
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(listOf(EmeraldPrimary, CyanTertiary))
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(EmeraldSurface.copy(alpha = 0.55f), ObsidianCard)
                        )
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "YOUR PERMANENT HOST CODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldBright,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = hostCode.ifBlank { "LOADING" },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Scan this Host QR from your Client phone or enter the code above to link instantly.",
                    fontSize = 12.5.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenQrSheet,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCode, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Show QR Code", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = onScanClientQr,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, tint = CyanTertiary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Client QR", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        // 2. Connected Client Nodes Header
        Text(
            text = "Linked Client Phones (${connectedDevices.size})",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )

        if (connectedDevices.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Sensors, null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Waiting for Client Connection", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Open SMS Bridge on your secondary phone, select Client mode, and scan your Host QR code.",
                        fontSize = 12.5.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            connectedDevices.forEach { device ->
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ObsidianCard,
                    border = BorderStroke(
                        1.dp,
                        if (device.active) EmeraldPrimary.copy(alpha = 0.45f) else ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(EmeraldSurface, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PhoneAndroid, null, tint = EmeraldBright, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = device.clientDeviceName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val batStr = device.batteryPercentage?.let { " • Battery $it%" } ?: ""
                                    Text(
                                        text = "Last seen: ${device.lastSeenLabel()}$batStr",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = if (device.active) EmeraldSurface else CrimsonSurface
                            ) {
                                Text(
                                    text = if (device.active) "ONLINE" else "OFFLINE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (device.active) EmeraldBright else CrimsonError,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onRequestClientLocation(device.clientUid) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Locate GPS", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                            }

                            if (!device.mapsUrl.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(device.mapsUrl)).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(mapIntent)
                                        } catch (_: Exception) {}
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, CyanTertiary.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Open Map ↗", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanTertiary, maxLines = 1, softWrap = false)
                                }
                            }

                            IconButton(
                                onClick = { onDisconnectClient(device) },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(CrimsonSurface, RoundedCornerShape(12.dp))
                                    .border(1.dp, CrimsonError.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .testTag("unlink_device_${device.clientUid}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Unlink & Delete Device",
                                    tint = CrimsonError,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 4: HOST CONTROL HUB (Floating OTP Popup, Fallback Number, Permissions)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun HostControlHubTab(
    context: Context,
    hostCode: String,
    hostPhoneNumber: String,
    isFloatingOtpEnabled: Boolean,
    hasOverlayPermission: Boolean,
    isAutoStartConfigured: Boolean,
    permissionSteps: List<PermissionStepItem>,
    onUpdateHostPhoneNumber: (String) -> Unit,
    onToggleFloatingOtp: (Boolean) -> Unit,
    onTestFloatingOtp: () -> Unit,
    onConfigureAutoStart: () -> Unit,
    onOpenPermissionSheet: () -> Unit
) {
    var phoneDraft by remember(hostPhoneNumber) { mutableStateOf(hostPhoneNumber) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PermissionStatusBanner(
            steps = permissionSteps,
            onOpenSheet = onOpenPermissionSheet
        )

        // 1. Truecaller-Style Floating OTP Overlay Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(EmeraldSurface, RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FlipToFront, null, tint = EmeraldBright, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Floating OTP Popup Overlay",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Pops up OTP card over any app for instant 1-tap copy",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = isFloatingOtpEnabled && hasOverlayPermission,
                        onCheckedChange = onToggleFloatingOtp,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = EmeraldPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onTestFloatingOtp,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianElevated, contentColor = EmeraldBright),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Bolt, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preview Floating OTP Popup", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // 2. Host Mobile Number for Offline Cellular Fallback
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Host Mobile Number (Offline Fallback)",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Included in your Host QR code so linked Clients automatically text this number when offline.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = phoneDraft,
                        onValueChange = { phoneDraft = it },
                        placeholder = { Text("+91 9876543210", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onUpdateHostPhoneNumber(phoneDraft.trim()) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // 3. OEM Background Survival Shield
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OEM Background Auto-Start",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${AutoStartPermissionHelper.getDeviceBrandName()} background keep-alive settings",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                OutlinedButton(
                    onClick = onConfigureAutoStart,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Text(
                        text = if (isAutoStartConfigured) "Configured ✓" else "Configure",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAutoStartConfigured) EmeraldBright else CoralBright
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SLIDE-UP MODAL BOTTOM SHEETS FOR HOST
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HostQrModalSheet(
    hostCode: String,
    hostPhoneNumber: String,
    onUpdateHostPhoneNumber: (String) -> Unit,
    onCopyCode: () -> Unit,
    onScanClient: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val qrBitmap = remember(hostCode, hostPhoneNumber) {
        val codeToEncode = hostCode.ifBlank { "SMSBRIDGE" }
        QrCodeUtils.generateQrBitmap(
            content = QrCodeUtils.encodeCode(codeToEncode, hostPhoneNumber),
            size = 640
        ).asImageBitmap()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pair Client Phone",
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Scan this QR code from the Client phone for instant 1-tap linking.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(3.dp, EmeraldPrimary),
                modifier = Modifier
                    .sizeIn(maxWidth = 220.dp, maxHeight = 220.dp)
                    .aspectRatio(1f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(14.dp)) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "Host Pairing QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                onClick = onCopyCode,
                shape = RoundedCornerShape(16.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = hostCode.ifBlank { "------" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = EmeraldBright,
                        letterSpacing = 3.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = EmeraldBright, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onScanClient,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralSecondary, contentColor = Color.White),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Or Scan Client's QR Code", fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp)
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageDetailModalSheet(
    message: SmsMessage,
    onCopyOtp: (String) -> Unit,
    onCopyFull: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val otpResult = remember(message.body) { OtpExtractor.extractOtp(message.body) }
    val timeStr = remember(message.receivedAt) {
        SimpleDateFormat("EEEE, MMM d, yyyy • h:mm:ss a", Locale.getDefault()).format(Date(message.receivedAt))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.sender,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "$timeStr • ${message.clientDeviceName.ifBlank { "Client Phone" }}",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (otpResult.isOtp) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = EmeraldSurface,
                    border = BorderStroke(1.dp, EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("DETECTED OTP", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldBright)
                            Text(
                                text = otpResult.formattedOtp.ifBlank { otpResult.otp },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                        }
                        Button(
                            onClick = { onCopyOtp(otpResult.otp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy OTP", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectionContainer {
                    Text(
                        text = message.body,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCopyFull,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianElevated, contentColor = TextPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Full SMS", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, null, tint = CrimsonError, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.Bold, color = CrimsonError)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallHistoryModalSheet(
    targetCall: CallRecord,
    allCalls: List<CallRecord>,
    onCallBack: (String) -> Unit,
    onCopyNumber: (String) -> Unit,
    onDeleteSingle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val matchingCalls = remember(targetCall, allCalls) {
        allCalls.filter { it.phoneNumber == targetCall.phoneNumber }.take(15)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text(
                text = targetCall.getDisplayName(),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "${targetCall.phoneNumber} • ${matchingCalls.size} recent call event(s)",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onCallBack(targetCall.phoneNumber) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call Back", fontWeight = FontWeight.ExtraBold)
                }

                OutlinedButton(
                    onClick = { onCopyNumber(targetCall.phoneNumber) },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Number", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            matchingCalls.forEach { entry ->
                val timeStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(entry.timestamp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${entry.callType.displayName} (${entry.formattedDuration()})",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(text = timeStr, fontSize = 11.5.sp, color = TextSecondary)
                        }
                        IconButton(onClick = { onDeleteSingle(entry.callId) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CrimsonError, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
