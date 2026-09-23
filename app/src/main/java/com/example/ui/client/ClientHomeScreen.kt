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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import com.example.ui.pairing.QrScannerOverlay
import com.example.util.QrCodeUtils
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SmsQueueEntity
import com.example.ui.theme.StatusOfflineBg
import com.example.ui.theme.StatusOfflineDot
import com.example.ui.theme.StatusOfflineText
import com.example.ui.theme.StatusOnlineBg
import com.example.ui.theme.StatusOnlineDot
import com.example.ui.theme.StatusOnlineText
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingText
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusSuccessDot
import com.example.ui.theme.StatusSuccessText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ui.update.InAppUpdateDialog
import com.example.util.UpdateChecker
import kotlinx.coroutines.launch

import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextButton
import com.example.R
import com.example.data.repository.InboxSyncScope
import com.example.util.AutoStartPermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientHomeScreen(
    viewModel: ClientViewModel,
    onChangeRole: () -> Unit,
    onOpenDeveloperProfile: () -> Unit,
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

    var showDestinationNumberDialog by remember { mutableStateOf(false) }
    var showQuotaSettingsDialog by remember { mutableStateOf(false) }

    var isServiceRunning by remember { mutableStateOf(true) }
    var availableUpdate by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var showSyncScopeDialog by remember { mutableStateOf(false) }
    var selectedSyncScope by remember { mutableStateOf(InboxSyncScope.ALL_TIME) }
    var isSyncingInbox by remember { mutableStateOf(false) }

    var showCallSyncScopeDialog by remember { mutableStateOf(false) }
    var selectedCallSyncScope by remember { mutableStateOf(InboxSyncScope.ALL_TIME) }
    var isSyncingCalls by remember { mutableStateOf(false) }

    var showHostQrScanner by remember { mutableStateOf(false) }
    var hasCameraPermissionForHost by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

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

    // CameraX QR Scanner overlay to scan Host's QR code
    if (showHostQrScanner) {
        QrScannerOverlay(
            onCodeScanned = { rawValue ->
                val qrData = QrCodeUtils.parseScannedQrData(rawValue)
                if (qrData != null) {
                    showHostQrScanner = false
                    val phoneMsg = if (!qrData.phoneNumber.isNullOrBlank()) " (SMS Fallback: ${qrData.phoneNumber})" else ""
                    Toast.makeText(context, "Scanned Host Code: ${qrData.code}$phoneMsg. Connecting...", Toast.LENGTH_SHORT).show()
                    viewModel.updateLinkedHostCode(qrData.code, qrData.phoneNumber) { success, msg ->
                        Toast.makeText(context, if (success) "Connected to Host (${qrData.code})!" else msg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showHostQrScanner = false }
        )
    }

    // Automatic update check in background on launch
    LaunchedEffect(Unit) {
        val info = UpdateChecker.checkForUpdates(context)
        if (info.hasUpdate) {
            availableUpdate = info
        }
    }



    if (availableUpdate != null) {
        InAppUpdateDialog(
            updateInfo = availableUpdate!!,
            onDismiss = { availableUpdate = null }
        )
    }

    // Sync Scope Selection Dialog
    if (showSyncScopeDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSyncingInbox) showSyncScopeDialog = false },
            title = { Text("Sync Real SMS Inbox", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Choose the date range or count of SMS to import from your device's SMS inbox and forward to your Host:",
                        fontSize = 13.sp,
                        color = Color(0xFF49454F)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    InboxSyncScope.entries.forEach { scopeOption ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedSyncScope = scopeOption }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedSyncScope == scopeOption),
                                onClick = { selectedSyncScope = scopeOption },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = scopeOption.label,
                                fontSize = 13.sp,
                                fontWeight = if (selectedSyncScope == scopeOption) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedSyncScope == scopeOption) MaterialTheme.colorScheme.primary else Color(0xFF1D1B20)
                            )
                        }
                    }

                    if (isSyncingInbox) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Importing & forwarding SMS...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSyncingInbox = true
                        viewModel.syncRealInbox(selectedSyncScope) { result, success ->
                            isSyncingInbox = false
                            showSyncScopeDialog = false
                            if (success && result != null) {
                                Toast.makeText(
                                    context,
                                    "✅ Inbox Scanned: ${result.totalFound} found, ${result.newImported} newly queued, ${result.alreadyExisted} already in DB",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, "Failed to read device SMS inbox", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isSyncingInbox
                ) {
                    Text("Start Sync", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isSyncingInbox) {
                    TextButton(onClick = { showSyncScopeDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Call Log Sync Scope Selection Dialog
    if (showCallSyncScopeDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSyncingCalls) showCallSyncScopeDialog = false },
            title = { Text("Sync Real Call Log History", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Choose the date range or count of calls to import from your device's Call History and forward to your Host:",
                        fontSize = 13.sp,
                        color = Color(0xFF49454F)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    InboxSyncScope.entries.forEach { scopeOption ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedCallSyncScope = scopeOption }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedCallSyncScope == scopeOption),
                                onClick = { selectedCallSyncScope = scopeOption },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = scopeOption.label,
                                fontSize = 13.sp,
                                fontWeight = if (selectedCallSyncScope == scopeOption) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCallSyncScope == scopeOption) Color(0xFF0284C7) else Color(0xFF1D1B20)
                            )
                        }
                    }

                    if (isSyncingCalls) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF0284C7))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Importing & forwarding call records...", fontSize = 12.sp, color = Color(0xFF0284C7))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSyncingCalls = true
                        viewModel.syncRealCallLog(selectedCallSyncScope) { result, success ->
                            isSyncingCalls = false
                            showCallSyncScopeDialog = false
                            if (success && result != null) {
                                Toast.makeText(
                                    context,
                                    "✅ Call Log Scanned: ${result.totalFound} found, ${result.newImported} newly queued, ${result.alreadyExisted} already in DB",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, "Failed to read device Call Log", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    enabled = !isSyncingCalls
                ) {
                    Text("Start Sync", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isSyncingCalls) {
                    TextButton(onClick = { showCallSyncScopeDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Fallback Destination Mobile Number Dialog
    if (showDestinationNumberDialog) {
        var tempNumber by remember(fallbackDestinationNumber) { mutableStateOf(fallbackDestinationNumber) }
        AlertDialog(
            onDismissRequest = { showDestinationNumberDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fallback Host Mobile Number", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "When offline or without internet, incoming SMS will be forwarded directly via cellular SMS to this mobile number.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = tempNumber,
                        onValueChange = { tempNumber = it },
                        label = { Text("Host Mobile Number") },
                        placeholder = { Text("+91 9876543210") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setFallbackDestinationNumber(tempNumber.trim())
                    showDestinationNumberDialog = false
                    Toast.makeText(context, "Fallback number updated!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDestinationNumberDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Daily SMS Quota & Dual-SIM Rollover Settings Dialog
    if (showQuotaSettingsDialog) {
        var tempLimit1 by remember(dailySmsLimitSim1) { mutableStateOf(dailySmsLimitSim1.toString()) }
        var tempLimit2 by remember(dailySmsLimitSim2) { mutableStateOf(dailySmsLimitSim2.toString()) }
        var tempRollover by remember(isDualSimRolloverEnabled) { mutableStateOf(isDualSimRolloverEnabled) }

        AlertDialog(
            onDismissRequest = { showQuotaSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SMS Quotas & Dual-SIM", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Configure your daily free SMS limits (Default: 100 for Jio/Airtel plans). To protect against extra carrier balance deductions, cellular SMS stops when limits are reached.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = tempLimit1,
                        onValueChange = { tempLimit1 = it.filter { ch -> ch.isDigit() } },
                        label = { Text("SIM 1 Daily Limit (e.g. 100)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = tempLimit2,
                        onValueChange = { tempLimit2 = it.filter { ch -> ch.isDigit() } },
                        label = { Text("SIM 2 Daily Limit (e.g. 100)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Smart Dual-SIM Rollover", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text(
                                "When SIM 1 quota is used up, automatically use SIM 2 to continue forwarding.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = tempRollover,
                            onCheckedChange = { tempRollover = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val l1 = tempLimit1.toIntOrNull()?.coerceAtLeast(1) ?: 100
                    val l2 = tempLimit2.toIntOrNull()?.coerceAtLeast(1) ?: 100
                    viewModel.setDailySmsLimitSim1(l1)
                    viewModel.setDailySmsLimitSim2(l2)
                    viewModel.setDualSimRolloverEnabled(tempRollover)
                    showQuotaSettingsDialog = false
                    Toast.makeText(context, "Quota & Rollover settings saved!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuotaSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Permission states
    var hasSendSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val sendSmsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSendSmsPermission = isGranted
        if (isGranted) {
            viewModel.setOfflineSmsFallbackEnabled(true)
            Toast.makeText(context, "SEND_SMS permission granted! Offline fallback enabled.", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setOfflineSmsFallbackEnabled(false)
            Toast.makeText(context, "SEND_SMS permission required for offline cellular fallback.", Toast.LENGTH_LONG).show()
        }
    }

    val availableSims = remember(context) {
        com.example.util.SimUtils.getAvailableSims(context)
    }

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
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

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val hasState = perms[Manifest.permission.READ_PHONE_STATE] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val hasLog = perms[Manifest.permission.READ_CALL_LOG] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        val hasContacts = perms[Manifest.permission.READ_CONTACTS] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

        hasCallPermissions = hasState && hasLog && hasContacts
        if (hasCallPermissions) {
            viewModel.setCallForwardingEnabled(true)
            Toast.makeText(context, "Call Forwarding & Contact Names Enabled", Toast.LENGTH_SHORT).show()
            viewModel.syncRealCallLog(InboxSyncScope.LAST_30_DAYS) { result, success ->
                if (success && result != null && result.newImported > 0) {
                    Toast.makeText(context, "✅ Synced ${result.newImported} calls to Host", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Phone, Call Log & Contacts permissions are needed to detect numbers and caller names.", Toast.LENGTH_LONG).show()
        }
    }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    var isBatteryExempt by remember {
        mutableStateOf(
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasSmsPermission = (perms[Manifest.permission.RECEIVE_SMS] == true) && (perms[Manifest.permission.READ_SMS] == true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = perms[Manifest.permission.POST_NOTIFICATIONS] == true
        }
        val hasState = perms[Manifest.permission.READ_PHONE_STATE] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val hasLog = perms[Manifest.permission.READ_CALL_LOG] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        val hasContacts = perms[Manifest.permission.READ_CONTACTS] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        hasCallPermissions = hasState && hasLog && hasContacts

        if (hasSmsPermission) {
            viewModel.toggleService(context, true)
            isServiceRunning = true
            Toast.makeText(context, "Permissions Updated. Service Active.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "SMS permissions are required to forward messages.", Toast.LENGTH_LONG).show()
        }
    }

    fun requestAllPermissions() {
        val perms = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }
                hasCallPermissions = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                isBatteryExempt = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
                if (hasSmsPermission) {
                    viewModel.toggleService(context, true)
                    isServiceRunning = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasSmsPermission || !hasNotificationPermission) {
            requestAllPermissions()
        } else {
            viewModel.toggleService(context, true)
            isServiceRunning = true
        }
    }

    // Automatic initial call sync for legacy users when Call Forwarding is enabled and permission granted
    LaunchedEffect(isCallForwardingEnabled, hasCallPermissions) {
        if (isCallForwardingEnabled && hasCallPermissions && totalCallsCount == 0 && !isSyncingCalls) {
            viewModel.syncRealCallLog(InboxSyncScope.LAST_30_DAYS) { _, _ -> }
        }
    }

    Scaffold(
        topBar = {
            Column {
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = Color(0xFFC2E7FF)
                            ) {
                                Text(
                                    text = "CLIENT",
                                    color = Color(0xFF001D35),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    actions = {
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
                            modifier = Modifier.testTag("client_switch_role_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Switch Role",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onOpenDeveloperProfile,
                            modifier = Modifier.testTag("client_developer_profile_button")
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    thickness = 1.dp
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("client_home_scroll_container"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Alert Warning Banner if SMS permission is missing
            if (!hasSmsPermission) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFDC2626), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "SMS Permissions Required",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = "Forwarding cannot work without SMS access",
                                        fontSize = 12.sp,
                                        color = Color(0xFFB91C1C)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "This device needs permission to receive and read SMS messages in order to automatically forward OTPs and texts to your receiving Host phone.",
                                fontSize = 13.sp,
                                color = Color(0xFF450A0A),
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { requestAllPermissions() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Grant Permissions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    },
                                    border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Settings", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Xiaomi / Redmi AutoStart and Background Persistence Warning Card
            if ((!isBatteryExempt || AutoStartPermissionHelper.isXiaomiOrRedmi()) && !isAutoStartConfigured) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.5.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
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
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (AutoStartPermissionHelper.isXiaomiOrRedmi()) "Xiaomi/Redmi Auto-Start Setup" else "Background Battery Exemption",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF92400E)
                                        )
                                        Text(
                                            text = "Allow app to run automatically after reboot",
                                            fontSize = 12.sp,
                                            color = Color(0xFFB45309)
                                        )
                                    }
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

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Xiaomi/Redmi devices require enabling 'AutoStart' and setting Battery Saver to 'No restrictions' so SMS Forwarder can restart and forward SMS without touching your phone.",
                                fontSize = 13.sp,
                                color = Color(0xFF78350F),
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        AutoStartPermissionHelper.openAutoStartSettings(context)
                                        viewModel.setAutoStartConfigured(true)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Enable Auto-Start", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = {
                                        AutoStartPermissionHelper.requestIgnoreBatteryOptimizations(context)
                                        viewModel.setAutoStartConfigured(true)
                                    },
                                    border = BorderStroke(1.dp, Color(0xFFD97706)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Battery Saver", color = Color(0xFFD97706), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 0. Linked Host Channel Card
            item {
                var showChangeDialog by remember { mutableStateOf(false) }
                var newHostCodeInput by remember { mutableStateOf("") }
                var newHostPhoneInput by remember { mutableStateOf("") }
                var isConnecting by remember { mutableStateOf(false) }

                val isHostLinked = !linkedHostUid.isNullOrEmpty()

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHostLinked) Color.White else Color(0xFFFFFBEB)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (isHostLinked) Color(0xFFCAC4D0) else Color(0xFFF59E0B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FORWARDING DESTINATION (HOST)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = if (isHostLinked) MaterialTheme.colorScheme.primary else Color(0xFFB45309)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isHostLinked) "Host Channel: $linkedHostUid" else "No Host Linked (Tap to set)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHostLinked) Color(0xFF1D1B20) else Color(0xFF92400E)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (hasCameraPermissionForHost) {
                                        showHostQrScanner = true
                                    } else {
                                        cameraPermissionLauncherForHost.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = {
                                    newHostCodeInput = linkedHostUid ?: ""
                                    showChangeDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isHostLinked) "Change" else "Enter Code",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (showChangeDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = {
                            if (!isConnecting) showChangeDialog = false
                        },
                        title = { Text("Set Host Channel Code", fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Button(
                                    onClick = {
                                        showChangeDialog = false
                                        if (hasCameraPermissionForHost) {
                                            showHostQrScanner = true
                                        } else {
                                            cameraPermissionLauncherForHost.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00668B)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan Host QR with Camera", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    "Or enter the 6-character Host Code displayed at the top of your Host phone's screen:",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                androidx.compose.material3.OutlinedTextField(
                                    value = newHostCodeInput,
                                    onValueChange = {
                                        if (it.length <= 8) {
                                            newHostCodeInput = it.uppercase()
                                        }
                                    },
                                    label = { Text("Host Code (e.g. 6 chars)") },
                                    placeholder = { Text("e.g. R44MQG") },
                                    singleLine = true,
                                    enabled = !isConnecting,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.material3.OutlinedTextField(
                                    value = newHostPhoneInput,
                                    onValueChange = { newHostPhoneInput = it },
                                    label = { Text("Host Mobile (Optional - for SMS fallback)") },
                                    placeholder = { Text("e.g. +91 9876543210") },
                                    singleLine = true,
                                    enabled = !isConnecting,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val clean = newHostCodeInput.trim().uppercase()
                                    if (clean.isNotBlank()) {
                                        isConnecting = true
                                        viewModel.updateLinkedHostCode(clean, newHostPhoneInput.ifBlank { null }) { success, msg ->
                                            isConnecting = false
                                            showChangeDialog = false
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = newHostCodeInput.isNotBlank() && !isConnecting
                            ) {
                                if (isConnecting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Save & Connect")
                                }
                            }
                        },
                        dismissButton = {
                            if (!isConnecting) {
                                androidx.compose.material3.TextButton(onClick = { showChangeDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        }
                    )
                }
            }
            // 1. Service Status Card
            item {
                val isActive = isServiceRunning && hasSmsPermission
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) Color(0xFFEADDFF) else Color(0xFFFEE2E2)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isActive) Color(0xFFD0BCFF) else Color(0xFFFCA5A5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = if (isActive) Color(0xFF6750A4) else Color(0xFFDC2626),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isActive) Icons.Default.Sensors else Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (isActive) "Monitoring Active" else "Forwarding Paused",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color(0xFF21005D) else Color(0xFF7F1D1D)
                                )
                                Text(
                                    text = if (isActive) "Background SMS listener running" else "Enable switch to resume forwarding",
                                    fontSize = 12.sp,
                                    color = if (isActive) Color(0xFF49454F) else Color(0xFF991B1B)
                                )
                            }
                        }

                        Switch(
                            checked = isServiceRunning,
                            onCheckedChange = { checked ->
                                if (checked && !hasSmsPermission) {
                                    val list = mutableListOf(
                                        Manifest.permission.RECEIVE_SMS,
                                        Manifest.permission.READ_SMS
                                    )
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        list.add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    permissionLauncher.launch(list.toTypedArray())
                                } else {
                                    isServiceRunning = checked
                                    viewModel.toggleService(context, checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6750A4)
                            ),
                            modifier = Modifier.testTag("service_toggle_switch")
                        )
                    }
                }
            }

            // 1.5 Offline Cellular SMS Fallback Card (Smart Hybrid / Jio 100 Quota)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOfflineSmsFallbackEnabled) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isOfflineSmsFallbackEnabled) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header: Title + Toggle Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = if (isOfflineSmsFallbackEnabled) Color(0xFF16A34A) else Color(0xFF94A3B8),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SimCard,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Offline SMS Fallback",
                                            fontSize = 15.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOfflineSmsFallbackEnabled) Color(0xFF14532D) else Color(0xFF334155)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isOfflineSmsFallbackEnabled) Color(0xFFDCFCE7) else Color(0xFFE2E8F0)
                                        ) {
                                            Text(
                                                text = "HYBRID",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isOfflineSmsFallbackEnabled) Color(0xFF15803D) else Color(0xFF64748B),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isOfflineSmsFallbackEnabled)
                                            "Forwards via SIM SMS when offline"
                                        else
                                            "Offline SMS fallback paused",
                                        fontSize = 12.sp,
                                        color = if (isOfflineSmsFallbackEnabled) Color(0xFF15803D) else Color(0xFF64748B)
                                    )
                                }
                            }

                            Switch(
                                checked = isOfflineSmsFallbackEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!hasSendSmsPermission) {
                                            sendSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                                        } else {
                                            viewModel.setOfflineSmsFallbackEnabled(true)
                                        }
                                    } else {
                                        viewModel.setOfflineSmsFallbackEnabled(false)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF16A34A)
                                )
                            )
                        }

                        AnimatedVisibility(visible = isOfflineSmsFallbackEnabled) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                HorizontalDivider(color = Color(0xFFBBF7D0))
                                Spacer(modifier = Modifier.height(14.dp))

                                // Destination Number Display & Change
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "FORWARDING TO MOBILE NUMBER",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B),
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (fallbackDestinationNumber.isNotBlank())
                                                    fallbackDestinationNumber
                                                else
                                                    "Not Configured (Required)",
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (fallbackDestinationNumber.isNotBlank())
                                                    Color(0xFF0F172A)
                                                else
                                                    Color(0xFFDC2626)
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { showDestinationNumberDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (fallbackDestinationNumber.isNotBlank()) "Edit" else "Set",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // SIM Card Selector & Detection
                                Text(
                                    text = "SELECT SENDING SIM (RECEIVER DEVICE)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF14532D),
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val sim1 = availableSims.find { it.slotIndex == 0 } ?: availableSims.firstOrNull()
                                    val sim2 = availableSims.find { it.slotIndex == 1 }

                                    // Option 0: Auto (SIM 1 with Smart Rollover)
                                    val isAutoSelected = preferredSimSlot == 0
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isAutoSelected) Color(0xFF16A34A) else Color.White,
                                        border = BorderStroke(1.dp, if (isAutoSelected) Color(0xFF16A34A) else Color(0xFFCBD5E1)),
                                        modifier = Modifier.clickable { viewModel.setPreferredSimSlot(0) }
                                    ) {
                                        Text(
                                            text = "Auto (Smart Fallback)",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isAutoSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isAutoSelected) Color.White else Color(0xFF334155),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }

                                    // Option 1: Force SIM 1
                                    val isSim1Selected = preferredSimSlot == 1
                                    val sim1Label = "SIM 1: ${sim1?.displayName ?: "SIM 1"}"
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSim1Selected) Color(0xFF16A34A) else Color.White,
                                        border = BorderStroke(1.dp, if (isSim1Selected) Color(0xFF16A34A) else Color(0xFFCBD5E1)),
                                        modifier = Modifier.clickable { viewModel.setPreferredSimSlot(1) }
                                    ) {
                                        Text(
                                            text = sim1Label,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSim1Selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSim1Selected) Color.White else Color(0xFF334155),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }

                                    // Option 2: Force SIM 2 (if present)
                                    if (sim2 != null) {
                                        val isSim2Selected = preferredSimSlot == 2
                                        val sim2Label = "SIM 2: ${sim2.displayName}"
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSim2Selected) Color(0xFF16A34A) else Color.White,
                                            border = BorderStroke(1.dp, if (isSim2Selected) Color(0xFF16A34A) else Color(0xFFCBD5E1)),
                                            modifier = Modifier.clickable { viewModel.setPreferredSimSlot(2) }
                                        ) {
                                            Text(
                                                text = sim2Label,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSim2Selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSim2Selected) Color.White else Color(0xFF334155),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Daily Quota Tracking (Jio 100 Free SMS Plan)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "DAILY FREE SMS QUOTA TRACKER",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B),
                                                letterSpacing = 0.5.sp
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFF1F5F9)
                                            ) {
                                                Text(
                                                    text = "Resets 00:00 midnight",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF64748B),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // SIM 1 Usage Bar
                                        val sim1Progress = (dailySmsSentCountSim1.toFloat() / dailySmsLimitSim1.toFloat()).coerceIn(0f, 1f)
                                        val isSim1Exhausted = dailySmsSentCountSim1 >= dailySmsLimitSim1
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "SIM 1 Usage",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                            Text(
                                                text = "$dailySmsSentCountSim1 / $dailySmsLimitSim1 SMS" + if (isSim1Exhausted) " (EXHAUSTED)" else "",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSim1Exhausted) Color(0xFFDC2626) else Color(0xFF16A34A)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { sim1Progress },
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                            color = if (isSim1Exhausted) Color(0xFFDC2626) else if (sim1Progress > 0.8f) Color(0xFFF59E0B) else Color(0xFF16A34A),
                                            trackColor = Color(0xFFE2E8F0)
                                        )

                                        // SIM 2 Usage Bar (if available)
                                        val hasSim2 = availableSims.any { it.slotIndex == 1 }
                                        if (hasSim2) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            val sim2Progress = (dailySmsSentCountSim2.toFloat() / dailySmsLimitSim2.toFloat()).coerceIn(0f, 1f)
                                            val isSim2Exhausted = dailySmsSentCountSim2 >= dailySmsLimitSim2
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "SIM 2 Usage",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF334155)
                                                )
                                                Text(
                                                    text = "$dailySmsSentCountSim2 / $dailySmsLimitSim2 SMS" + if (isSim2Exhausted) " (EXHAUSTED)" else "",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSim2Exhausted) Color(0xFFDC2626) else Color(0xFF16A34A)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { sim2Progress },
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                                color = if (isSim2Exhausted) Color(0xFFDC2626) else if (sim2Progress > 0.8f) Color(0xFFF59E0B) else Color(0xFF16A34A),
                                                trackColor = Color(0xFFE2E8F0)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Dual SIM Rollover Badge & Customize Button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = if (isDualSimRolloverEnabled) Color(0xFF16A34A) else Color(0xFF94A3B8),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isDualSimRolloverEnabled)
                                                        "Dual-SIM Rollover: Active"
                                                    else
                                                        "Dual-SIM Rollover: Off",
                                                    fontSize = 11.sp,
                                                    color = if (isDualSimRolloverEnabled) Color(0xFF15803D) else Color(0xFF64748B),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            FilledTonalButton(
                                                onClick = { showQuotaSettingsDialog = true },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Limits & Rollover", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Hybrid Note
                                Text(
                                    text = "💡 Hybrid Mode sends over Firebase internet first. If offline, SMS fallback sends to Host without extra recharge cost within daily 100 free SMS limits.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF166534),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Call Details Forwarding Toggle Card
            item {
                val isCallActive = isCallForwardingEnabled && hasCallPermissions
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCallActive) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isCallActive) Color(0xFF93C5FD) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = if (isCallActive) Color(0xFF0284C7) else Color(0xFF94A3B8),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Call Details Forwarding",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCallActive) Color(0xFF0369A1) else Color(0xFF334155)
                                    )
                                    Text(
                                        text = if (isCallActive) {
                                            "Forwarding incoming, missed & call logs"
                                        } else if (!hasCallPermissions) {
                                            "⚠️ Permission required to detect calls"
                                        } else {
                                            "Enable switch to forward call logs"
                                        },
                                        fontSize = 12.sp,
                                        color = if (isCallActive) Color(0xFF0284C7) else Color(0xFF64748B)
                                    )
                                }
                            }

                            Switch(
                                checked = isCallForwardingEnabled && hasCallPermissions,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!hasCallPermissions) {
                                            callPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.READ_PHONE_STATE,
                                                    Manifest.permission.READ_CALL_LOG,
                                                    Manifest.permission.READ_CONTACTS
                                                )
                                            )
                                        } else {
                                            viewModel.setCallForwardingEnabled(true)
                                            Toast.makeText(context, "Call Forwarding Enabled", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        viewModel.setCallForwardingEnabled(false)
                                        Toast.makeText(context, "Call Forwarding Disabled", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF0284C7)
                                )
                            )
                        }

                        if (!hasCallPermissions && isCallForwardingEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Call Log & Contacts Permission Needed",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = Color(0xFF991B1B)
                                        )
                                        Text(
                                            text = "Required by Android to detect phone numbers and show saved contact names.",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFFB91C1C)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            callPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.READ_PHONE_STATE,
                                                    Manifest.permission.READ_CALL_LOG,
                                                    Manifest.permission.READ_CONTACTS
                                                )
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (hasCallPermissions && isCallForwardingEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = Color(0xFFF0F9FF),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Call History Forwarding",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = Color(0xFF0369A1)
                                            )
                                            Text(
                                                text = "Uploaded: $uploadedCallsCount · Pending: $pendingCallsCount",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF0284C7)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = { showCallSyncScopeDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp),
                                            enabled = !isSyncingCalls
                                        ) {
                                            if (isSyncingCalls) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 1.5.dp,
                                                    color = Color.White
                                                )
                                            } else {
                                                Text("Sync Calls", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Permission Checklist Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Device Permissions & Diagnostics",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B20)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        PermissionRow(
                            label = "SMS Reception & Read",
                            granted = hasSmsPermission,
                            icon = Icons.Default.Message
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PermissionRow(
                            label = "Push Notifications",
                            granted = hasNotificationPermission,
                            icon = Icons.Default.Notifications
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PermissionRow(
                            label = "Phone & Call Log Detection",
                            granted = hasCallPermissions,
                            icon = Icons.Default.Phone
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PermissionRow(
                            label = "Battery Optimization Disabled",
                            granted = isBatteryExempt,
                            icon = Icons.Default.BatteryAlert
                        )

                        if (!hasSmsPermission || !hasNotificationPermission || !isBatteryExempt || !hasCallPermissions) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    if (!hasSmsPermission || !hasNotificationPermission) {
                                        val list = mutableListOf(
                                            Manifest.permission.RECEIVE_SMS,
                                            Manifest.permission.READ_SMS
                                        )
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            list.add(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        permissionLauncher.launch(list.toTypedArray())
                                    } else if (!hasCallPermissions) {
                                        callPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_PHONE_STATE,
                                                Manifest.permission.READ_CALL_LOG,
                                                Manifest.permission.READ_CONTACTS
                                            )
                                        )
                                    } else if (!isBatteryExempt) {
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("grant_permissions_button")
                            ) {
                                Text("Fix Permissions & Exempt Battery", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Stats Metric Tiles
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Uploaded tile
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                "Forwarded",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF49454F)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$uploadedCount",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6750A4)
                            )
                        }
                    }

                    // Pending sync tile
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                "Pending Queue",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF49454F)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$pendingCount",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingCount > 0) Color(0xFF92400E) else Color(0xFF1D1B20)
                            )
                        }
                    }
                }
            }

            // 4. Quick Action Controls (Real Device SMS Sync + Force Resync)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Real SMS Telephony Pipeline",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20)
                            )
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = Color(0xFFEADDFF)
                            ) {
                                Text(
                                    text = "LIVE TELEPHONY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF21005D),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Every real incoming SMS (bank OTPs, personal texts, carrier alerts) is automatically captured in real time by the background listener and forwarded to your paired Host device.",
                            fontSize = 12.sp,
                            color = Color(0xFF49454F),
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!hasSmsPermission) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.RECEIVE_SMS,
                                                Manifest.permission.READ_SMS
                                            )
                                        )
                                    } else {
                                        showSyncScopeDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("sync_real_inbox_button")
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Real Inbox", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.syncAllPending()
                                    Toast.makeText(context, "Syncing pending queue...", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("force_sync_button")
                            ) {
                                Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Queue", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 5. Local Event Log Header
            item {
                Text(
                    text = "Recent Forwarding History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (recentMessages.isEmpty()) {
                item {
                    Text(
                        text = "No SMS forwarded yet. Trigger a Test SMS above to verify.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recentMessages.take(15), key = { it.messageId }) { item ->
                    ClientMessageLogItem(item)
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF49454F),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1D1B20),
                fontWeight = FontWeight.Medium
            )
        }

        Surface(
            shape = RoundedCornerShape(100.dp),
            color = if (granted) StatusSuccessBg else StatusOfflineBg
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (granted) StatusSuccessDot else StatusOfflineDot,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (granted) "OK" else "Missing",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (granted) StatusSuccessText else StatusOfflineText
                )
            }
        }
    }
}

@Composable
private fun ClientMessageLogItem(entity: SmsQueueEntity) {
    val dateStr = remember(entity.receivedAt) {
        SimpleDateFormat("hh:mm:ss a · MMM d", Locale.getDefault()).format(Date(entity.receivedAt))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.sender,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
                Text(
                    text = entity.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF49454F),
                    maxLines = 1
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF79747E),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (entity.status == "UPLOADED") StatusSuccessBg else StatusPendingBg
            ) {
                Text(
                    text = entity.status,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (entity.status == "UPLOADED") StatusSuccessText else StatusPendingText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
