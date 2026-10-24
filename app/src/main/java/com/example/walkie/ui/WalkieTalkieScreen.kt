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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val context = LocalContext.current
    val callState by viewModel.callState.collectAsState()
    val hasMicPermission by viewModel.hasMicPermission.collectAsState()
    val isTalking by viewModel.isTalking.collectAsState()
    val peerName by viewModel.peerName.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

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
                            tint = accentGreen,
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ── Security Badge ──────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0D2818)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = accentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "End-to-end encrypted · P2P audio · No server relay",
                            style = MaterialTheme.typography.bodySmall,
                            color = accentGreen
                        )
                    }
                }

                // ── Connection Status Card ──────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = surfaceGlass.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Peer avatar ring
                        Box(contentAlignment = Alignment.Center) {
                            // Outer pulse ring (only when active/talking)
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
                                            else -> Color.Gray
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
                                WtCallState.CALLING -> "📞 Calling…"
                                WtCallState.RINGING -> "📲 Incoming call"
                                WtCallState.ACTIVE -> if (isTalking) "🎙 Transmitting…" else "🔊 Listening…"
                                WtCallState.ENDED -> "Call ended"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (callState) {
                                WtCallState.ACTIVE -> accentGreen
                                WtCallState.CALLING, WtCallState.RINGING -> Color(0xFFFFA000)
                                else -> Color.Gray
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
                    }
                }

                // ── PTT Button (main interaction) ───────────────────
                AnimatedVisibility(
                    visible = callState == WtCallState.ACTIVE,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "HOLD TO TALK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        // Phase 3 will upgrade this to a proper press-and-hold detector
                        Surface(
                            modifier = Modifier
                                .size(if (isTalking) 140.dp else 120.dp)
                                .clip(CircleShape),
                            color = if (isTalking) accentGreen else deepPurple,
                            shadowElevation = if (isTalking) 16.dp else 4.dp,
                            onClick = { /* Phase 3: pointerInput holdToTalk */ }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Push to Talk",
                                    tint = Color.White,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isTalking) "Speaking…" else "Press & hold mic to talk",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isTalking) accentGreen else Color.Gray
                        )
                    }
                }

                // ── Call / End Call Controls ────────────────────────
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
                                onClick = { viewModel.answerCall("") },
                                modifier = Modifier.weight(1f).height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentGreen),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = Color.Black)
                                Spacer(Modifier.width(6.dp))
                                Text("Answer", color = Color.Black)
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

                    else -> {}
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
                            Icon(Icons.Default.Wifi, contentDescription = null,
                                modifier = Modifier.size(16.dp), tint = Color(0xFF90CAF9))
                            Spacer(Modifier.width(8.dp))
                            Text("P2P via WebRTC · DTLS-SRTP encrypted",
                                style = MaterialTheme.typography.bodySmall, color = Color(0xFF90CAF9))
                        }
                        Text(
                            "Audio travels directly between devices — no audio data is stored on any server.",
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
}
