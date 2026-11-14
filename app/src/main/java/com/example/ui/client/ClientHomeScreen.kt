package com.example.ui.client

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.QueueStatus
import com.example.data.local.SmsQueueEntity
import com.example.data.repository.InboxSyncScope
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
import com.example.util.LocationUtils
import com.example.util.OtpExtractor
import com.example.util.QrCodeUtils
import com.example.util.SimUtils
import com.example.util.UpdateChecker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ClientNavTab(val label: String, val icon: ImageVector) {
    BRIDGE("Live Bridge", Icons.Default.Sensors),
    ACTIVITY("Sync Queue", Icons.Default.History),
    TOOLS("Smart Tools", Icons.Default.Tune)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientHomeScreen(
    viewModel: ClientViewModel,
    onChangeRole: () -> Unit,
    onOpenDeveloperProfile: () -> Unit,
    onOpenWalkieTalkie: () -> Unit = {},
    onOpenClientPairing: () -> Unit = {},
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val uploadedCount by viewModel.uploadedCount.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val recentMessages by viewModel.recentLocalMessages.collectAsStateWithLifecycle()
    val linkedHostUid by viewModel.linkedHostUid.collectAsStateWithLifecycle()
    val isAutoStartConfigured by viewModel.isAutoStartConfigured.collectAsStateWithLifecycle()
    val isCallForwardingEnabled by viewModel.isCallForwardingEnabled.collectAsStateWithLifecycle()
    val totalCallsCount by viewModel.totalCallsCount.collectAsStateWithLifecycle()
    val uploadedCallsCount by viewModel.uploadedCallsCount.collectAsStateWithLifecycle()
    val pendingCallsCount by viewModel.pendingCallsCount.collectAsStateWithLifecycle()

    val isOfflineSmsFallbackEnabled by viewModel.isOfflineSmsFallbackEnabled.collectAsStateWithLifecycle()
    val fallbackDestinationNumber by viewModel.fallbackDestinationNumber.collectAsStateWithLifecycle()
    val preferredSimSlot by viewModel.preferredSimSlot.collectAsStateWithLifecycle()
    val dailySmsLimitSim1 by viewModel.dailySmsLimitSim1.collectAsStateWithLifecycle()
    val dailySmsLimitSim2 by viewModel.dailySmsLimitSim2.collectAsStateWithLifecycle()
    val dailySmsSentCountSim1 by viewModel.dailySmsSentCountSim1.collectAsStateWithLifecycle()
    val dailySmsSentCountSim2 by viewModel.dailySmsSentCountSim2.collectAsStateWithLifecycle()
    val isDualSimRolloverEnabled by viewModel.isDualSimRolloverEnabled.collectAsStateWithLifecycle()

    val isRemoteLocationEnabled by viewModel.isRemoteLocationEnabled.collectAsStateWithLifecycle()
    val locationTriggerKeyword by viewModel.locationTriggerKeyword.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(ClientNavTab.BRIDGE) }
    var isServiceRunning by remember { mutableStateOf(true) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Bottom sheets & dialogs state
    var showPermissionSheet by rememberSaveable { mutableStateOf(false) }
    var showHostLinkSheet by rememberSaveable { mutableStateOf(false) }
    var showSyncScopeSheet by rememberSaveable { mutableStateOf(false) }
    var isCallSyncSheetMode by rememberSaveable { mutableStateOf(false) }
    var showDestinationNumberSheet by rememberSaveable { mutableStateOf(false) }
    var showQuotaSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showLocationKeywordSheet by rememberSaveable { mutableStateOf(false) }
    var testGpsResultDialog by remember { mutableStateOf<String?>(null) }

    var selectedSyncScope by remember { mutableStateOf(InboxSyncScope.LAST_7_DAYS) }
    var isSyncingHistory by remember { mutableStateOf(false) }
    var isTestingGps by remember { mutableStateOf(false) }

    var availableUpdate by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    // Live permission states
    var availableSims by remember { mutableStateOf(SimUtils.getAvailableSims(context)) }
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasSendSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var hasCallPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(LocationUtils.hasLocationPermission(context))
    }
    var hasNotificationListenerAccess by remember {
        mutableStateOf(NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName))
    }
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    var isBatteryExempt by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true)
    }

    var showHostQrScanner by remember { mutableStateOf(false) }
    var hasCameraPermissionForHost by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    BackHandler(enabled = currentTab != ClientNavTab.BRIDGE) {
        currentTab = ClientNavTab.BRIDGE
    }

    // ── Permission Launchers ──────────────────────────────────────────────────
    val cameraPermissionLauncherForHost = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermissionForHost = isGranted
        if (isGranted) {
            showHostQrScanner = true
        } else {
            Toast.makeText(context, "Camera permission needed to scan Host QR code", Toast.LENGTH_SHORT).show()
        }
    }

    val smsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasSmsPermission = (perms[Manifest.permission.RECEIVE_SMS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) &&
            (perms[Manifest.permission.READ_SMS] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)
        if (hasSmsPermission) {
            viewModel.toggleService(context, true)
            isServiceRunning = true
            Toast.makeText(context, "SMS Sync Permission Active ✓", Toast.LENGTH_SHORT).show()
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasCallPermissions = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        availableSims = SimUtils.getAvailableSims(context)
        if (hasCallPermissions) {
            viewModel.setCallForwardingEnabled(true)
            Toast.makeText(context, "Call & Contact ID Active ✓", Toast.LENGTH_SHORT).show()
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val sendSmsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSendSmsPermission = isGranted
        if (isGranted) {
            viewModel.setOfflineSmsFallbackEnabled(true)
            Toast.makeText(context, "Offline SIM Fallback Active ✓", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasLocationPermission = LocationUtils.hasLocationPermission(context)
        if (hasLocationPermission) {
            viewModel.setRemoteLocationEnabled(true)
            Toast.makeText(context, "Remote GPS Responder Active ✓", Toast.LENGTH_SHORT).show()
        }
    }

    val allCorePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        hasSendSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        hasCallPermissions = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = LocationUtils.hasLocationPermission(context)
        availableSims = SimUtils.getAvailableSims(context)

        if (hasSmsPermission) {
            viewModel.toggleService(context, true)
            isServiceRunning = true
        }
    }

    fun requestAllCorePermissions() {
        val perms = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        allCorePermissionsLauncher.launch(perms.toTypedArray())
    }

    // Refresh permission states smoothly on resume without duplicate foreground service collisions
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                hasSendSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                availableSims = SimUtils.getAvailableSims(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }
                hasCallPermissions = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                hasLocationPermission = LocationUtils.hasLocationPermission(context)
                hasNotificationListenerAccess = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
                isBatteryExempt = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
                if (hasSmsPermission && isServiceRunning) {
                    viewModel.toggleService(context, true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Start foreground service safely if permissions are already granted; otherwise open guided permission sheet
    LaunchedEffect(Unit) {
        if (hasSmsPermission) {
            viewModel.toggleService(context, true)
            isServiceRunning = true
        } else {
            showPermissionSheet = true
        }
        val info = UpdateChecker.checkForUpdates(context)
        if (info.hasUpdate) {
            availableUpdate = info
        }
    }

    // Build interactive permission steps list
    val permissionSteps = remember(
        hasSmsPermission,
        hasCallPermissions,
        hasNotificationPermission,
        isBatteryExempt,
        hasSendSmsPermission,
        hasLocationPermission
    ) {
        listOf(
            PermissionStepItem(
                id = "sms",
                title = "SMS Capture & Inbox Read",
                subtitle = "Detects incoming OTPs and text messages in real time",
                icon = Icons.Default.Sms,
                isGranted = hasSmsPermission,
                isRequired = true,
                actionLabel = "Grant",
                onGrantClick = {
                    smsPermLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
                }
            ),
            PermissionStepItem(
                id = "calls",
                title = "Call Log & Caller Names",
                subtitle = "Forwards incoming/missed call alerts with contact names",
                icon = Icons.Default.Call,
                isGranted = hasCallPermissions,
                isRequired = true,
                actionLabel = "Grant",
                onGrantClick = {
                    callPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.READ_CALL_LOG,
                            Manifest.permission.READ_CONTACTS
                        )
                    )
                }
            ),
            PermissionStepItem(
                id = "notif",
                title = "Persistent Bridge Status",
                subtitle = "Keeps the live bridge notification visible and active",
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
                id = "battery",
                title = "Unrestricted Battery Shield",
                subtitle = "Prevents Android from putting SMS Bridge to sleep",
                icon = Icons.Default.BatteryChargingFull,
                isGranted = isBatteryExempt,
                isRequired = true,
                actionLabel = "Exempt",
                onGrantClick = {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        } catch (_: Exception) {}
                    }
                }
            ),
            PermissionStepItem(
                id = "send_sms",
                title = "Offline Cellular SMS Fallback",
                subtitle = "Forwards SMS via SIM card when internet is unavailable",
                icon = Icons.Default.SimCard,
                isGranted = hasSendSmsPermission,
                isRequired = false,
                actionLabel = "Enable",
                onGrantClick = {
                    sendSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                }
            ),
            PermissionStepItem(
                id = "location",
                title = "Remote GPS Responder",
                subtitle = "Replies with a live Google Maps link when Host requests location",
                icon = Icons.Default.LocationOn,
                isGranted = hasLocationPermission,
                isRequired = false,
                actionLabel = "Allow",
                onGrantClick = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
        )
    }

    // Full-screen CameraX QR Scanner overlay to scan Host's QR code
    if (showHostQrScanner) {
        QrScannerOverlay(
            instructionText = "Point camera at the Host's QR Code",
            onCodeScanned = { rawValue ->
                val qrData = QrCodeUtils.parseScannedQrData(rawValue)
                if (qrData != null) {
                    showHostQrScanner = false
                    Toast.makeText(context, "Linking to Host ${qrData.code}...", Toast.LENGTH_SHORT).show()
                    viewModel.updateLinkedHostCode(qrData.code, qrData.phoneNumber) { success, msg ->
                        Toast.makeText(
                            context,
                            if (success) "Connected to Host (${qrData.code})!" else msg,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    true
                } else {
                    false
                }
            },
            onDismiss = { showHostQrScanner = false }
        )
        return
    }

    // ── Modals & Bottom Sheets ────────────────────────────────────────────────
    if (availableUpdate != null) {
        InAppUpdateDialog(
            updateInfo = availableUpdate!!,
            onDismiss = { availableUpdate = null }
        )
    }

    if (showPermissionSheet) {
        PermissionConciergeSheet(
            title = "Client Permission Hub",
            subtitle = "Grant access below with 1-tap live verification",
            steps = permissionSteps,
            onGrantAllQuickly = { requestAllCorePermissions() },
            onDismiss = { showPermissionSheet = false }
        )
    }

    if (showHostLinkSheet) {
        HostLinkModalSheet(
            currentHostCode = linkedHostUid ?: "",
            onConnectCode = { code, phone ->
                viewModel.updateLinkedHostCode(code, phone) { success, msg ->
                    Toast.makeText(
                        context,
                        if (success) "Connected to Host ($code)!" else msg,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showHostLinkSheet = false
            },
            onScanQr = {
                showHostLinkSheet = false
                if (hasCameraPermissionForHost) {
                    showHostQrScanner = true
                } else {
                    cameraPermissionLauncherForHost.launch(Manifest.permission.CAMERA)
                }
            },
            onShowMyClientQr = {
                showHostLinkSheet = false
                onOpenClientPairing()
            },
            onDismiss = { showHostLinkSheet = false }
        )
    }

    if (showSyncScopeSheet) {
        HistoryImportModalSheet(
            isCallMode = isCallSyncSheetMode,
            selectedScope = selectedSyncScope,
            isSyncing = isSyncingHistory,
            onSelectScope = { selectedSyncScope = it },
            onStartSync = {
                isSyncingHistory = true
                if (isCallSyncSheetMode) {
                    viewModel.syncRealCallLog(selectedSyncScope) { result, success ->
                        isSyncingHistory = false
                        showSyncScopeSheet = false
                        if (success && result != null) {
                            Toast.makeText(
                                context,
                                "Call Log Synced: ${result.newImported} newly queued (${result.totalFound} scanned)",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(context, "Grant Call Log permission first", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    viewModel.syncRealInbox(selectedSyncScope) { result, success ->
                        isSyncingHistory = false
                        showSyncScopeSheet = false
                        if (success && result != null) {
                            Toast.makeText(
                                context,
                                "SMS Inbox Synced: ${result.newImported} newly queued (${result.totalFound} scanned)",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(context, "Grant SMS permission first", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onDismiss = { if (!isSyncingHistory) showSyncScopeSheet = false }
        )
    }

    if (showDestinationNumberSheet) {
        DestinationNumberModalSheet(
            initialNumber = fallbackDestinationNumber,
            onSave = { cleanNum ->
                viewModel.setFallbackDestinationNumber(cleanNum)
                showDestinationNumberSheet = false
                Toast.makeText(context, "Fallback number saved!", Toast.LENGTH_SHORT).show()
                if (!hasSendSmsPermission) {
                    sendSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                }
            },
            onDismiss = { showDestinationNumberSheet = false }
        )
    }

    if (showQuotaSettingsSheet) {
        SimQuotaModalSheet(
            limit1 = dailySmsLimitSim1,
            limit2 = dailySmsLimitSim2,
            rollover = isDualSimRolloverEnabled,
            onSave = { l1, l2, roll ->
                viewModel.setDailySmsLimitSim1(l1)
                viewModel.setDailySmsLimitSim2(l2)
                viewModel.setDualSimRolloverEnabled(roll)
                showQuotaSettingsSheet = false
                Toast.makeText(context, "SIM Quotas & Rollover saved!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showQuotaSettingsSheet = false }
        )
    }

    if (showLocationKeywordSheet) {
        LocationKeywordModalSheet(
            initialKeyword = locationTriggerKeyword,
            onSave = { kw ->
                viewModel.setLocationTriggerKeyword(kw)
                showLocationKeywordSheet = false
                Toast.makeText(context, "Trigger keyword set to $kw", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showLocationKeywordSheet = false }
        )
    }

    if (testGpsResultDialog != null) {
        val dialogMessage = testGpsResultDialog!!
        val isSuccess = dialogMessage.startsWith("✅")
        val googleMapsUrl = if (dialogMessage.contains("https://maps.google.com")) {
            dialogMessage.substringAfter("Map Link:\n").trim()
        } else null

        AlertDialog(
            onDismissRequest = { testGpsResultDialog = null },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isSuccess) EmeraldBright else CrimsonError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSuccess) "Live GPS Fix Acquired" else "GPS Diagnostic Report",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = { Text(text = dialogMessage, fontSize = 13.sp, color = TextPrimary) },
            confirmButton = {
                if (googleMapsUrl != null) {
                    Button(
                        onClick = {
                            try {
                                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(googleMapsUrl)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(mapIntent)
                            } catch (_: Exception) {}
                            testGpsResultDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg)
                    ) {
                        Text("Open Google Maps ↗", fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    Button(onClick = { testGpsResultDialog = null }) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (googleMapsUrl != null) {
                    TextButton(onClick = { testGpsResultDialog = null }) {
                        Text("Close", color = TextSecondary)
                    }
                }
            }
        )
    }

    // ── Main Obsidian Scaffold with Bottom Navigation ─────────────────────────
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = ObsidianBg,
        topBar = {
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
                                    Brush.linearGradient(listOf(CoralSecondary, Color(0xFFE11D48))),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Client Node",
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
                                            if (isServiceRunning) EmeraldBright else CrimsonError,
                                            CircleShape
                                        )
                                )
                                val statusText = if (isServiceRunning) "ACTIVE" else "PAUSED"
                                val hostLabel = if (!linkedHostUid.isNullOrBlank()) "Host: $linkedHostUid" else "Tap below to link Host"
                                Text(
                                    text = "$statusText • $hostLabel",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!linkedHostUid.isNullOrBlank() && isServiceRunning) EmeraldBright else TextSecondary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Quick Walkie-Talkie Button
                    IconButton(
                        onClick = onOpenWalkieTalkie,
                        modifier = Modifier.testTag("walkie_talkie_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(CyanSurface, CircleShape)
                                .border(1.dp, CyanTertiary.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Walkie-Talkie",
                                tint = CyanTertiary,
                                modifier = Modifier.size(18.dp)
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
                                .size(36.dp)
                                .background(ObsidianCard, CircleShape)
                                .border(1.dp, ObsidianBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Switch Role",
                                tint = TextPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Overflow Menu
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = TextPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            containerColor = ObsidianCard
                        ) {
                            DropdownMenuItem(
                                text = { Text("Permission Concierge", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Shield, null, tint = EmeraldBright) },
                                onClick = {
                                    showMoreMenu = false
                                    showPermissionSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Switch to Host Mode", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.SwapHoriz, null, tint = CoralBright) },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.switchRole(onChangeRole)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isCheckingUpdate) "Checking Update..." else "Check for App Update", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.SystemUpdate, null, tint = CyanTertiary) },
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
                                                Toast.makeText(context, "You are on the latest version (${info.currentVersion})", Toast.LENGTH_SHORT).show()
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
                ClientNavTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(22.dp)
                            )
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
                            selectedTextColor = CoralBright,
                            indicatorColor = CoralSecondary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentTab,
            label = "client_tab_transition",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { tab ->
            when (tab) {
                ClientNavTab.BRIDGE -> ClientBridgeTab(
                    permissionSteps = permissionSteps,
                    isServiceRunning = isServiceRunning,
                    linkedHostUid = linkedHostUid,
                    uploadedCount = uploadedCount,
                    uploadedCallsCount = uploadedCallsCount,
                    pendingCount = pendingCount + pendingCallsCount,
                    onOpenPermissionSheet = { showPermissionSheet = true },
                    onToggleService = { enabled ->
                        isServiceRunning = enabled
                        viewModel.toggleService(context, enabled)
                    },
                    onScanHostQr = {
                        if (hasCameraPermissionForHost) {
                            showHostQrScanner = true
                        } else {
                            cameraPermissionLauncherForHost.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onOpenHostCodeSheet = { showHostLinkSheet = true },
                    onFlushPending = {
                        viewModel.syncAllPending()
                        Toast.makeText(context, "Syncing queued messages & calls...", Toast.LENGTH_SHORT).show()
                    },
                    onOpenImportSms = {
                        isCallSyncSheetMode = false
                        showSyncScopeSheet = true
                    },
                    onOpenImportCalls = {
                        isCallSyncSheetMode = true
                        showSyncScopeSheet = true
                    },
                    onOpenWalkieTalkie = onOpenWalkieTalkie
                )

                ClientNavTab.ACTIVITY -> ClientActivityQueueTab(
                    recentMessages = recentMessages,
                    totalCount = totalCount,
                    uploadedCount = uploadedCount,
                    pendingCount = pendingCount,
                    onOpenImportSms = {
                        isCallSyncSheetMode = false
                        showSyncScopeSheet = true
                    },
                    onOpenImportCalls = {
                        isCallSyncSheetMode = true
                        showSyncScopeSheet = true
                    },
                    onRetryPending = {
                        viewModel.syncAllPending()
                        Toast.makeText(context, "Retrying pending queue...", Toast.LENGTH_SHORT).show()
                    }
                )

                ClientNavTab.TOOLS -> ClientSmartToolsTab(
                    context = context,
                    isOfflineSmsFallbackEnabled = isOfflineSmsFallbackEnabled,
                    fallbackDestinationNumber = fallbackDestinationNumber,
                    availableSims = availableSims,
                    preferredSimSlot = preferredSimSlot,
                    dailySmsSentCountSim1 = dailySmsSentCountSim1,
                    dailySmsLimitSim1 = dailySmsLimitSim1,
                    dailySmsSentCountSim2 = dailySmsSentCountSim2,
                    dailySmsLimitSim2 = dailySmsLimitSim2,
                    isDualSimRolloverEnabled = isDualSimRolloverEnabled,
                    hasSendSmsPermission = hasSendSmsPermission,
                    isCallForwardingEnabled = isCallForwardingEnabled,
                    hasCallPermissions = hasCallPermissions,
                    isRemoteLocationEnabled = isRemoteLocationEnabled,
                    hasLocationPermission = hasLocationPermission,
                    locationTriggerKeyword = locationTriggerKeyword,
                    hasNotificationListenerAccess = hasNotificationListenerAccess,
                    isTestingGps = isTestingGps,
                    isAutoStartConfigured = isAutoStartConfigured,
                    onToggleOfflineFallback = { enabled ->
                        if (enabled && !hasSendSmsPermission) {
                            sendSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                        } else {
                            viewModel.setOfflineSmsFallbackEnabled(enabled)
                        }
                    },
                    onOpenNumberSheet = { showDestinationNumberSheet = true },
                    onSelectSimSlot = { viewModel.setPreferredSimSlot(it) },
                    onOpenQuotaSheet = { showQuotaSettingsSheet = true },
                    onSendTestFallbackSms = {
                        viewModel.sendTestFallbackSms { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    onToggleCallForwarding = { enabled ->
                        if (enabled && !hasCallPermissions) {
                            callPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.READ_CONTACTS
                                )
                            )
                        } else {
                            viewModel.setCallForwardingEnabled(enabled)
                        }
                    },
                    onToggleRemoteLocation = { enabled ->
                        if (enabled && !hasLocationPermission) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            viewModel.setRemoteLocationEnabled(enabled)
                        }
                    },
                    onOpenLocationKeywordSheet = { showLocationKeywordSheet = true },
                    onTestGpsFix = {
                        if (!hasLocationPermission) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else if (!isTestingGps) {
                            isTestingGps = true
                            scope.launch {
                                val loc = LocationUtils.getCurrentLocation(context)
                                val bat = LocationUtils.getBatteryInfo(context)
                                isTestingGps = false
                                testGpsResultDialog = if (loc != null) {
                                    "✅ Live GPS Fix Acquired!\n\n" +
                                        "Coordinates: ${loc.latitude}, ${loc.longitude}\n" +
                                        "Accuracy: ±${loc.accuracy.toInt()}m (${loc.provider})\n" +
                                        "Battery: ${bat.percentage}%\n\n" +
                                        "Map Link:\n${loc.mapsUrl}"
                                } else {
                                    "⚠️ Could not acquire GPS fix.\nEnsure device Location / GPS is switched ON in quick settings."
                                }
                            }
                        }
                    },
                    onOpenNotificationListenerSettings = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        } catch (_: Exception) {}
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
// TAB 1: LIVE BRIDGE (Radar Hero, Host Link Card, Telemetry Grid, Quick Actions)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun ClientBridgeTab(
    permissionSteps: List<PermissionStepItem>,
    isServiceRunning: Boolean,
    linkedHostUid: String?,
    uploadedCount: Int,
    uploadedCallsCount: Int,
    pendingCount: Int,
    onOpenPermissionSheet: () -> Unit,
    onToggleService: (Boolean) -> Unit,
    onScanHostQr: () -> Unit,
    onOpenHostCodeSheet: () -> Unit,
    onFlushPending: () -> Unit,
    onOpenImportSms: () -> Unit,
    onOpenImportCalls: () -> Unit,
    onOpenWalkieTalkie: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bridge_radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Interactive Permission Concierge Banner
        PermissionStatusBanner(
            steps = permissionSteps,
            onOpenSheet = onOpenPermissionSheet
        )

        // 2. Active Radar & Host Connection Hero Card
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    if (isServiceRunning) listOf(EmeraldPrimary.copy(alpha = 0.65f), CoralSecondary.copy(alpha = 0.45f))
                    else listOf(CrimsonError.copy(alpha = 0.5f), ObsidianBorder)
                )
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (isServiceRunning) EmeraldSurface.copy(alpha = 0.5f) else CrimsonSurface.copy(alpha = 0.35f),
                                ObsidianCard
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isServiceRunning) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .scale(pulseScale)
                                        .alpha(pulseAlpha)
                                        .border(2.dp, EmeraldPrimary, CircleShape)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (isServiceRunning) EmeraldPrimary.copy(alpha = 0.2f) else CrimsonError.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                                    .border(
                                        1.dp,
                                        if (isServiceRunning) EmeraldBright else CrimsonError,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (isServiceRunning) EmeraldBright else CrimsonError,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = if (isServiceRunning) "Background Bridge Live" else "Bridge Paused",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isServiceRunning) "Listening 24/7 for incoming SMS & Calls" else "Tap switch to resume real-time sync",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = onToggleService,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = EmeraldPrimary,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = ObsidianElevated
                        ),
                        modifier = Modifier.testTag("service_toggle_switch")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(16.dp))

                // Linked Host Destination Box
                val hasHost = !linkedHostUid.isNullOrBlank()
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(
                        1.dp,
                        if (hasHost) EmeraldPrimary.copy(alpha = 0.45f) else CoralSecondary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "DESTINATION HOST DEVICE",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextMuted,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (hasHost) "Host Code: $linkedHostUid" else "No Host Linked Yet",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = if (hasHost) FontFamily.Monospace else FontFamily.Default,
                                    color = if (hasHost) EmeraldBright else CoralBright
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = if (hasHost) EmeraldPrimary.copy(alpha = 0.16f) else CoralSecondary.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = if (hasHost) "LINKED ✓" else "UNLINKED",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (hasHost) EmeraldBright else CoralBright,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onScanHostQr,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CoralSecondary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scan_host_qr_button")
                            ) {
                                Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Host QR", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = onOpenHostCodeSheet,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, ObsidianBorder),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("enter_host_code_button")
                            ) {
                                Icon(Icons.Default.Keyboard, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasHost) "Change Code" else "Enter Code",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Telemetry Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryStatCard(
                label = "SMS Synced",
                value = uploadedCount.toString(),
                accent = EmeraldBright,
                icon = Icons.Default.CloudDone,
                modifier = Modifier.weight(1f)
            )
            TelemetryStatCard(
                label = "Calls Synced",
                value = uploadedCallsCount.toString(),
                accent = CyanTertiary,
                icon = Icons.Default.Call,
                modifier = Modifier.weight(1f)
            )
            TelemetryStatCard(
                label = "In Queue",
                value = pendingCount.toString(),
                accent = if (pendingCount > 0) AmberWarning else TextSecondary,
                icon = Icons.Default.CloudSync,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onFlushPending() }
            )
        }

        // 4. Quick Action Bento Cards
        Text(
            text = "Quick Actions",
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionTile(
                title = "Import SMS",
                subtitle = "Sync existing inbox",
                icon = Icons.Default.Sms,
                accent = EmeraldBright,
                onClick = onOpenImportSms,
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                title = "Import Calls",
                subtitle = "Sync call history",
                icon = Icons.Default.Phone,
                accent = CyanTertiary,
                onClick = onOpenImportCalls,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionTile(
                title = "Walkie-Talkie",
                subtitle = "Live P2P voice push",
                icon = Icons.Default.Mic,
                accent = CoralBright,
                onClick = onOpenWalkieTalkie,
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                title = "Flush Queue",
                subtitle = "Force cloud upload",
                icon = Icons.Default.Sync,
                accent = AmberWarning,
                onClick = onFlushPending,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun TelemetryStatCard(
    label: String,
    value: String,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2: ACTIVITY QUEUE (Recent Captured SMS & History Sync)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun ClientActivityQueueTab(
    recentMessages: List<SmsQueueEntity>,
    totalCount: Int,
    uploadedCount: Int,
    pendingCount: Int,
    onOpenImportSms: () -> Unit,
    onOpenImportCalls: () -> Unit,
    onRetryPending: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Captured Activity Feed",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "$uploadedCount synced • $pendingCount pending ($totalCount total)",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        if (pendingCount > 0) {
                            Button(
                                onClick = onRetryPending,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CoralSecondary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Sync, null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenImportSms,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sms, null, tint = EmeraldBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import SMS", fontSize = 12.sp, color = EmeraldBright, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onOpenImportCalls,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CyanTertiary.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Call, null, tint = CyanTertiary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Calls", fontSize = 12.sp, color = CyanTertiary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (recentMessages.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(EmeraldSurface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = EmeraldBright,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Messages Captured Yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "New incoming SMS will appear here automatically, or tap 'Import SMS' above to sync existing messages.",
                            fontSize = 12.5.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(recentMessages, key = { it.messageId }) { item ->
                ClientQueueMessageCard(item = item)
            }
        }
    }
}

@Composable
private fun ClientQueueMessageCard(item: SmsQueueEntity) {
    val otpResult = remember(item.body) { OtpExtractor.extractOtp(item.body) }
    val timeFormatted = remember(item.receivedAt) {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(item.receivedAt))
    }
    val isUploaded = item.status == QueueStatus.UPLOADED.name

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ObsidianCard,
        border = BorderStroke(
            1.dp,
            if (otpResult.isOtp) CyanTertiary.copy(alpha = 0.45f) else ObsidianBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.sender,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (otpResult.isOtp) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = CyanSurface,
                            border = BorderStroke(1.dp, CyanTertiary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "OTP ${otpResult.otp}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyanTertiary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (isUploaded) EmeraldSurface else AmberSurface
                ) {
                    Text(
                        text = if (isUploaded) "SYNCED ✓" else item.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isUploaded) EmeraldBright else AmberWarning,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.body,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = timeFormatted,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 3: SMART TOOLS (Offline SIM Fallback, Dual-SIM Quotas, Remote GPS, Shield)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun ClientSmartToolsTab(
    context: Context,
    isOfflineSmsFallbackEnabled: Boolean,
    fallbackDestinationNumber: String,
    availableSims: List<com.example.util.SimSlotInfo>,
    preferredSimSlot: Int,
    dailySmsSentCountSim1: Int,
    dailySmsLimitSim1: Int,
    dailySmsSentCountSim2: Int,
    dailySmsLimitSim2: Int,
    isDualSimRolloverEnabled: Boolean,
    hasSendSmsPermission: Boolean,
    isCallForwardingEnabled: Boolean,
    hasCallPermissions: Boolean,
    isRemoteLocationEnabled: Boolean,
    hasLocationPermission: Boolean,
    locationTriggerKeyword: String,
    hasNotificationListenerAccess: Boolean,
    isTestingGps: Boolean,
    isAutoStartConfigured: Boolean,
    onToggleOfflineFallback: (Boolean) -> Unit,
    onOpenNumberSheet: () -> Unit,
    onSelectSimSlot: (Int) -> Unit,
    onOpenQuotaSheet: () -> Unit,
    onSendTestFallbackSms: () -> Unit,
    onToggleCallForwarding: (Boolean) -> Unit,
    onToggleRemoteLocation: (Boolean) -> Unit,
    onOpenLocationKeywordSheet: () -> Unit,
    onTestGpsFix: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit,
    onConfigureAutoStart: () -> Unit,
    onOpenPermissionSheet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Offline Cellular SMS Fallback & Dual-SIM Quota Card
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
                                .background(CoralSurface, RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SimCard, null, tint = CoralBright, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Offline Cellular SMS Fallback",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Forwards via SIM card when internet is down",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = isOfflineSmsFallbackEnabled && hasSendSmsPermission,
                        onCheckedChange = onToggleOfflineFallback,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CoralSecondary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Destination Mobile Number Row
                Surface(
                    onClick = onOpenNumberSheet,
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Host Mobile Number", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text(
                                text = fallbackDestinationNumber.ifBlank { "Tap to set Host phone number" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (fallbackDestinationNumber.isBlank()) CoralBright else TextPrimary
                            )
                        }
                        Icon(Icons.Default.Edit, contentDescription = "Edit Number", tint = CoralBright, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SIM Selector Pills
                if (availableSims.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableSims.forEach { sim ->
                            val selected = preferredSimSlot == sim.slotIndex
                            Surface(
                                onClick = { onSelectSimSlot(sim.slotIndex) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) CoralSecondary.copy(alpha = 0.2f) else ObsidianSurface,
                                border = BorderStroke(1.dp, if (selected) CoralSecondary else ObsidianBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = sim.userLabel(),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) CoralBright else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Quota Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SIM 1: $dailySmsSentCountSim1/$dailySmsLimitSim1 • SIM 2: $dailySmsSentCountSim2/$dailySmsLimitSim2" +
                            if (isDualSimRolloverEnabled) " (Rollover ON)" else "",
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onOpenQuotaSheet) {
                        Text("Edit Quotas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CoralBright)
                    }
                }

                Button(
                    onClick = onSendTestFallbackSms,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianElevated, contentColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, null, tint = CoralBright, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Test Cellular SMS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // 2. Remote GPS Location Responder (#LOC) Card
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
                            Icon(Icons.Default.LocationOn, null, tint = EmeraldBright, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Remote GPS Responder",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Replies with live Google Maps link on '$locationTriggerKeyword'",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = isRemoteLocationEnabled && hasLocationPermission,
                        onCheckedChange = onToggleRemoteLocation,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = EmeraldPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenLocationKeywordSheet,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Keyword: $locationTriggerKeyword", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Button(
                        onClick = onTestGpsFix,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTestingGps) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ObsidianBg)
                        } else {
                            Text("Test Live GPS", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasNotificationListenerAccess) "WhatsApp #LOC Trigger: Active ✓" else "Optional: Allow WhatsApp #LOC Trigger",
                        fontSize = 11.5.sp,
                        color = if (hasNotificationListenerAccess) EmeraldBright else TextSecondary
                    )
                    if (!hasNotificationListenerAccess) {
                        TextButton(onClick = onOpenNotificationListenerSettings) {
                            Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldBright)
                        }
                    }
                }
            }
        }

        // 3. Call Forwarding & OEM Auto-Start Protection Card
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
                                .background(CyanSurface, RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Call, null, tint = CyanTertiary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Live Call Alert Forwarding",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Streams ringing, missed & answered calls to Host",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = isCallForwardingEnabled && hasCallPermissions,
                        onCheckedChange = onToggleCallForwarding,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = CyanTertiary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = ObsidianBorder)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OEM Background Auto-Start Shield",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${AutoStartPermissionHelper.getDeviceBrandName()} background survival settings",
                            fontSize = 12.sp,
                            color = if (isAutoStartConfigured) EmeraldBright else TextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = onConfigureAutoStart,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Text(
                            text = if (isAutoStartConfigured) "Configured ✓" else "Open Settings",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAutoStartConfigured) EmeraldBright else CoralBright
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onOpenPermissionSheet,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianElevated, contentColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Shield, null, tint = EmeraldBright, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Interactive Permission Concierge", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SLIDE-UP MODAL BOTTOM SHEETS FOR CLIENT
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HostLinkModalSheet(
    currentHostCode: String,
    onConnectCode: (String, String?) -> Unit,
    onScanQr: () -> Unit,
    onShowMyClientQr: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var codeInput by remember(currentHostCode) { mutableStateOf(currentHostCode) }

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
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Link to Host Device",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter the 6-character Host Code displayed on your Host phone, or scan its QR code.",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = codeInput,
                onValueChange = { if (it.length <= 8) codeInput = it.uppercase() },
                label = { Text("6-Character Host Code") },
                placeholder = { Text("e.g. R44MQG") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = EmeraldBright,
                    unfocusedLabelColor = TextSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val clean = codeInput.trim().uppercase()
                    if (clean.length >= 4) {
                        onConnectCode(clean, null)
                    }
                },
                enabled = codeInput.trim().length >= 4,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connect to Host Now", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onScanQr,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CoralSecondary.copy(alpha = 0.6f)),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.QrCodeScanner, null, tint = CoralBright, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Host QR Code Instead", fontWeight = FontWeight.Bold, color = CoralBright)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onShowMyClientQr,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Or Show My Client QR Code for Host to Scan →",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanTertiary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryImportModalSheet(
    isCallMode: Boolean,
    selectedScope: InboxSyncScope,
    isSyncing: Boolean,
    onSelectScope: (InboxSyncScope) -> Unit,
    onStartSync: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (isCallMode) "Import Device Call History" else "Import Device SMS Inbox",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select how much history to import and forward to your linked Host device:",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            InboxSyncScope.entries.forEach { option ->
                val selected = selectedScope == option
                Surface(
                    onClick = { onSelectScope(option) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) EmeraldSurface else ObsidianCard,
                    border = BorderStroke(1.dp, if (selected) EmeraldPrimary else ObsidianBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectScope(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = EmeraldBright,
                                unselectedColor = TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = option.label,
                            fontSize = 13.5.sp,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (selected) EmeraldBright else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onStartSync,
                enabled = !isSyncing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ObsidianBg)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importing & Syncing...", fontWeight = FontWeight.ExtraBold)
                } else {
                    Text("Start Import Now", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationNumberModalSheet(
    initialNumber: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempNumber by remember(initialNumber) { mutableStateOf(initialNumber) }

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
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text("Fallback Host Mobile Number", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "When internet is offline, incoming SMS will be forwarded via cellular SIM directly to this number.",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = tempNumber,
                onValueChange = { tempNumber = it },
                label = { Text("Host Mobile Number") },
                placeholder = { Text("+91 9876543210") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CoralSecondary,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onSave(tempNumber.trim()) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralSecondary, contentColor = Color.White),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Fallback Number", fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimQuotaModalSheet(
    limit1: Int,
    limit2: Int,
    rollover: Boolean,
    onSave: (Int, Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempLimit1 by remember(limit1) { mutableStateOf(limit1.toString()) }
    var tempLimit2 by remember(limit2) { mutableStateOf(limit2.toString()) }
    var tempRollover by remember(rollover) { mutableStateOf(rollover) }

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
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text("SIM Daily Quotas & Rollover", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Protect your carrier balance by setting daily free SMS limits (e.g. 100/day).",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = tempLimit1,
                onValueChange = { tempLimit1 = it.filter { ch -> ch.isDigit() } },
                label = { Text("SIM 1 Daily Limit") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = tempLimit2,
                onValueChange = { tempLimit2 = it.filter { ch -> ch.isDigit() } },
                label = { Text("SIM 2 Daily Limit") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Smart Dual-SIM Rollover", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Text("Automatically switch to SIM 2 when SIM 1 reaches its daily limit", fontSize = 12.sp, color = TextSecondary)
                }
                Switch(checked = tempRollover, onCheckedChange = { tempRollover = it })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val l1 = tempLimit1.toIntOrNull()?.coerceAtLeast(1) ?: 100
                    val l2 = tempLimit2.toIntOrNull()?.coerceAtLeast(1) ?: 100
                    onSave(l1, l2, tempRollover)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save SIM Quota Settings", fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationKeywordModalSheet(
    initialKeyword: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempKeyword by remember(initialKeyword) { mutableStateOf(initialKeyword) }

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
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text("Location Trigger Keyword", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "When an SMS or WhatsApp message starting with this keyword arrives, this phone replies with a live Google Maps link.",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = tempKeyword,
                onValueChange = { tempKeyword = it.uppercase() },
                label = { Text("Trigger Keyword (Default: #LOC)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onSave(tempKeyword.trim().ifBlank { "#LOC" }) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Keyword", fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
