package com.example.walkie.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkieTalkieScreen(
    viewModel: WalkieTalkieViewModel,
    onNavigateBack: () -> Unit
) {
    val callState by viewModel.callState.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val hasMicPermission by viewModel.hasMicPermission.collectAsState()
    val isTalking by viewModel.isTalking.collectAsState()
    val isFullDuplex by viewModel.isFullDuplex.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val peerName by viewModel.peerName.collectAsState()
    val hostCode by viewModel.hostCode.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val selectedTargetDevice by viewModel.selectedTargetDevice.collectAsState()

    var showRecordingConsentDialog by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onMicPermissionResult(granted) }

    LaunchedEffect(Unit) {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Pulse animation for the PTT button when active
    val infiniteTransition = rememberInfiniteTransition(label = "ptt_pulse")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha"
    )

    val deepPurple = Color(0xFF6A0DAD)
    val accentGreen = Color(0xFF00E676)
    val accentRed = Color(0xFFFF1744)
    val surfaceGlass = Color(0xFF1A1A2E)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.RadioButtonChecked,
                            contentDescription = null,
                            tint = if (callState == WtCallState.ACTIVE) accentGreen else Color(0xFFFFA000),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Walkie-Talkie",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (callState == WtCallState.ACTIVE) {
                        IconButton(onClick = {
                            if (isRecording) {
                                viewModel.toggleRecording()
                            } else {
                                showRecordingConsentDialog = true
                            }
                        }) {
                            Icon(
                                Icons.Default.FiberManualRecord,
                                contentDescription = "Recording",
                                tint = if (isRecording) accentRed else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            Color(0xFF0D0D1A)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Channel Badge ──────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = surfaceGlass,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E4A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF90CAF9),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (hostCode.isNotBlank()) "Channel: $hostCode" else "Standalone Channel",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF90CAF9)
                            )
                        )
                    }
                }

                // ── Multi-Device Target Selector ───────────────────
                if (connectedDevices.size > 1) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SELECT TARGET DEVICE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedTargetDevice == null,
                                onClick = { viewModel.selectTargetDevice(null) },
                                label = { Text("All Devices (Broadcast)", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(15.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00E5FF),
                                    selectedLabelColor = Color.Black
                                )
                            )
                            connectedDevices.forEach { dev ->
                                Spacer(Modifier.width(8.dp))
                                FilterChip(
                                    selected = selectedTargetDevice?.clientUid == dev.clientUid,
                                    onClick = { viewModel.selectTargetDevice(dev) },
                                    label = { Text(dev.clientDeviceName, fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(15.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = deepPurple,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // ── Status & Device Card ───────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceGlass),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Profile Avatar with dynamic state rings
                        Box(contentAlignment = Alignment.Center) {
                            if (callState == WtCallState.ACTIVE && isTalking) {
                                Box(
                                    modifier = Modifier
                                        .size(112.dp)
                                        .scale(pulseFraction)
                                        .clip(CircleShape)
                                        .background(accentGreen.copy(alpha = pulseRingAlpha))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(deepPurple, Color(0xFF1A0030))
                                        )
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = when (callState) {
                                            WtCallState.ACTIVE -> accentGreen
                                            WtCallState.CALLING -> Color(0xFFFFA000)
                                            WtCallState.RINGING -> accentGreen
                                            WtCallState.IDLE, WtCallState.ENDED -> Color.Gray
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isTalking) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = peerName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )

                        Text(
                            text = when (callState) {
                                WtCallState.IDLE -> "Ready to connect"
                                WtCallState.CALLING -> "📞 Calling paired device…"
                                WtCallState.RINGING -> "📲 Incoming call!"
                                WtCallState.ACTIVE -> if (isTalking) "🎙 Transmitting…" else "🔊 Listening…"
                                WtCallState.ENDED -> "Call ended"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (callState) {
                                WtCallState.ACTIVE -> accentGreen
                                WtCallState.CALLING, WtCallState.RINGING -> Color(0xFFFFA000)
                                WtCallState.IDLE, WtCallState.ENDED -> Color.Gray
                            },
                            fontWeight = FontWeight.Medium
                        )

                        if (statusMessage.isNotBlank()) {
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (isRecording) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = accentRed.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, accentRed.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = accentRed, modifier = Modifier.size(10.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Recording Active (Local)", style = MaterialTheme.typography.labelSmall, color = accentRed)
                                }
                            }
                        }
                    }
                }

                // ── Mode Switcher (Full-Duplex vs Half-Duplex PTT) ──
                if (callState == WtCallState.ACTIVE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = !isFullDuplex,
                            onClick = { if (isFullDuplex) viewModel.toggleFullDuplex() },
                            label = { Text("Push-to-Talk (Hold)") },
                            leadingIcon = {
                                Icon(Icons.Default.Hearing, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = deepPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        FilterChip(
                            selected = isFullDuplex,
                            onClick = { if (!isFullDuplex) viewModel.toggleFullDuplex() },
                            label = { Text("Hands-Free Mode") },
                            leadingIcon = {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentGreen,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                // ── Push-To-Talk Button ─────────────────────────────
                AnimatedVisibility(
                    visible = callState == WtCallState.ACTIVE,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (isFullDuplex) "HANDS-FREE TWO-WAY AUDIO" else "HOLD TO TALK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.sp,
                                color = if (isTalking) accentGreen else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .size(if (isTalking) 140.dp else 120.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isTalking) accentGreen else deepPurple
                                )
                                .border(
                                    width = 3.dp,
                                    color = if (isTalking) Color.White else deepPurple.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .pointerInput(isFullDuplex) {
                                    if (!isFullDuplex) {
                                        detectTapGestures(
                                            onPress = {
                                                viewModel.onPttDown()
                                                tryAwaitRelease()
                                                viewModel.onPttUp()
                                            }
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isTalking) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Push to Talk Button",
                                tint = if (isTalking) Color.Black else Color.White,
                                modifier = Modifier.size(52.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = when {
                                isFullDuplex -> "Mic open continuously (two-way)"
                                isTalking -> "Transmitting speech…"
                                else -> "Hold mic button to speak"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isTalking) accentGreen else Color.Gray
                        )
                    }
                }

                // ── Call / Answer / End Call Controls ──────────────
                when (callState) {
                    WtCallState.IDLE, WtCallState.ENDED -> {
                        if (!hasMicPermission) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2A1500)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.MicOff,
                                        contentDescription = null,
                                        tint = Color(0xFFFFA000),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Microphone permission required",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFFFA000),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    ) {
                                        Text("Grant Permission")
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.initiateCall() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = deepPurple
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Start Walkie-Talkie",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    WtCallState.CALLING -> {
                        FilledTonalButton(
                            onClick = { viewModel.endCall() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = accentRed.copy(alpha = 0.15f),
                                contentColor = accentRed
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Cancel Call")
                        }
                    }

                    WtCallState.RINGING -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    activeSession?.let { viewModel.answerCall(it.sessionId) }
                                },
                                modifier = Modifier.weight(1f).height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentGreen),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = Color.Black)
                                Spacer(Modifier.width(6.dp))
                                Text("Answer", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { viewModel.endCall() },
                                modifier = Modifier.weight(1f).height(56.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = accentRed.copy(alpha = 0.15f),
                                    contentColor = accentRed
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Decline")
                            }
                        }
                    }

                    WtCallState.ACTIVE -> {
                        FilledTonalButton(
                            onClick = { viewModel.endCall() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = accentRed.copy(alpha = 0.15f),
                                contentColor = accentRed
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("End Call", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // ── Info Card ───────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = surfaceGlass.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF90CAF9)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Direct P2P WebRTC · DTLS-SRTP Media Transport",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF90CAF9),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            "Audio streams directly peer-to-peer over your local network or internet with end-to-end encryption. No audio data ever touches Firebase.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // ── Explicit Audio Recording Consent Dialog ─────────────
    if (showRecordingConsentDialog) {
        AlertDialog(
            onDismissRequest = { showRecordingConsentDialog = false },
            title = { Text("Consent to Record Audio") },
            text = {
                Text(
                    "Do you consent to record this Walkie-Talkie session? " +
                    "Audio will be saved locally on your device in standard M4A format. " +
                    "Both parties must be aware when recording is active."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRecordingConsentDialog = false
                        viewModel.toggleRecording()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = deepPurple)
                ) {
                    Text("I Consent & Start Recording")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordingConsentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
