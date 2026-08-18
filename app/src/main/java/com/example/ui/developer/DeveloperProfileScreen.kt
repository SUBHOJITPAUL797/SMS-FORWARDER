package com.example.ui.developer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import com.example.util.AutoStartPermissionHelper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.update.InAppUpdateDialog
import com.example.util.UpdateChecker
import kotlinx.coroutines.launch

// Primary Rich Purple Color Tokens
private val DeepPurpleHeader = Color(0xFF2E1065)
private val PurplePrimary = Color(0xFF6750A4)
private val PurpleAccent = Color(0xFF9333EA)
private val PurpleContainer = Color(0xFFF3E8FF)
private val PurpleCardBg = Color(0xFFFAF5FF)
private val PurpleBorder = Color(0xFFE9D5FF)
private val DarkText = Color(0xFF1E1B4B)
private val SubtitleText = Color(0xFF581C87)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperProfileScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showTermsDetails by remember { mutableStateOf(false) }
    var showPrivacyDetails by remember { mutableStateOf(false) }

    val currentVersion = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.4"
    } catch (e: Exception) {
        "1.0.4"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Developer Profile",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Check out SMS Forwarder created by Subhojit Paul!\nPortfolio: https://subhojit-paul.pages.dev/\nGitHub: https://github.com/SUBHOJITPAUL797/SMS-FORWARDER"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Developer Profile"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Profile",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepPurpleHeader
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DeepPurpleHeader,
                            Color(0xFF3B0764),
                            Color(0xFF581C87),
                            Color(0xFFFAF5FF)
                        ),
                        startY = 0f,
                        endY = 1200f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. HERO DEVELOPER CARD
                HeroDeveloperCard(
                    context = context,
                    onOpenWebsite = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://subhojit-paul.pages.dev/"))
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 2. WHY THIS APP WAS CREATED (NOTE FROM SUBHOJIT)
                WhyAppCreatedCard()

                Spacer(modifier = Modifier.height(18.dp))

                // 3. IN-APP UPDATER & VERSION CARD
                AppUpdaterCard(
                    currentVersion = currentVersion,
                    isCheckingUpdate = isCheckingUpdate,
                    onCheckUpdate = {
                        isCheckingUpdate = true
                        scope.launch {
                            val info = UpdateChecker.checkForUpdates(context)
                            isCheckingUpdate = false
                            if (info.hasUpdate) {
                                updateInfo = info
                                showUpdateDialog = true
                            } else {
                                Toast.makeText(
                                    context,
                                    "✨ You are running the latest version ($currentVersion)!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 4. AUTOSTART & BACKGROUND PERSISTENCE SETUP
                AutoStartSettingsCard(
                    context = context
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 5. TERMS & CONDITIONS (DISCLAIMER) CARD
                TermsAndConditionsCard(
                    isExpanded = showTermsDetails,
                    onToggleExpand = { showTermsDetails = !showTermsDetails }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 5. PRIVACY POLICY CARD
                PrivacyPolicyCard(
                    isExpanded = showPrivacyDetails,
                    onToggleExpand = { showPrivacyDetails = !showPrivacyDetails }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 6. FOOTER BRANDING
                FooterSection(
                    onOpenWebsite = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://subhojit-paul.pages.dev/"))
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // In-App Update Dialog Trigger
    if (showUpdateDialog && updateInfo != null) {
        InAppUpdateDialog(
            updateInfo = updateInfo!!,
            onDismiss = { showUpdateDialog = false }
        )
    }
}

@Composable
private fun HeroDeveloperCard(
    context: Context,
    onOpenWebsite: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(2.dp, PurpleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture with glowing purple border
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(12.dp, CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                PurpleAccent,
                                Color(0xFFC084FC),
                                PurplePrimary,
                                PurpleAccent
                            )
                        ),
                        CircleShape
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dev_subhojit),
                    contentDescription = "Subhojit Paul - Developer",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Developer Name
            Text(
                text = "Subhojit Paul",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DarkText,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Role Badge
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = PurpleContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lead Developer & Creator",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SubtitleText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Building modern, intuitive Android utilities and web software.",
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Portfolio Website Button
            Button(
                onClick = onOpenWebsite,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurplePrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "subhojit-paul.pages.dev",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WhyAppCreatedCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.5.dp, PurpleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PurpleContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "A Note from Subhojit",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Text(
                        text = "Why SMS Forwarder was created",
                        fontSize = 12.sp,
                        color = SubtitleText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "I created SMS Forwarder to solve a real everyday challenge: managing two or more physical phones or separate SIM cards without the hassle of carrying all devices simultaneously.\n\nWhether you have a secondary backup phone at home, distinct business and personal SIMs, or need important banking OTPs and urgent messages forwarded directly to your primary phone in real time, this app delivers an instant, private bridge between your devices.",
                fontSize = 13.sp,
                color = Color(0xFF374151),
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun AppUpdaterCard(
    currentVersion: String,
    isCheckingUpdate: Boolean,
    onCheckUpdate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.5.dp, PurpleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFE0E7FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = Color(0xFF4338CA),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "App Updates & Releases",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        Text(
                            text = "Installed: v$currentVersion",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4B5563)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Official Release",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "SMS Forwarder features an in-app GitHub auto-updater that seamlessly downloads and installs new releases directly on your device.",
                fontSize = 13.sp,
                color = Color(0xFF4B5563),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCheckUpdate,
                enabled = !isCheckingUpdate,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, PurplePrimary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PurplePrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = PurplePrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking GitHub Releases...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check for Updates Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun AutoStartSettingsCard(
    context: Context
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.5.dp, PurpleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFFEF3C7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Auto-Start & Restart Settings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Text(
                        text = "Xiaomi, Redmi, Vivo, Oppo & Samsung",
                        fontSize = 12.sp,
                        color = Color(0xFFB45309),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "On Xiaomi/Redmi (MIUI & HyperOS) and other custom Android skins, you must enable 'AutoStart' in app settings and set Battery Saver to 'No restrictions' so SMS Forwarder can restart automatically after your phone reboots.",
                fontSize = 13.sp,
                color = Color(0xFF374151),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        AutoStartPermissionHelper.openAutoStartSettings(context)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD97706)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("AutoStart Settings", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        AutoStartPermissionHelper.requestIgnoreBatteryOptimizations(context)
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFD97706)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD97706)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Battery Saver", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TermsAndConditionsCard(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.5.dp, PurpleBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFEF3C7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Terms & Conditions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        Text(
                            text = "Disclaimer & Usage Terms",
                            fontSize = 12.sp,
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color(0xFF6B7280)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        text = "1. Legitimate Personal Use Only",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DarkText
                    )
                    Text(
                        text = "This application is developed strictly for personal multi-device management, dual-phone convenience, and self-forwarding utilities between devices owned and authorized by the same user.",
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563),
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    Text(
                        text = "2. Developer Non-Liability & Misuse Disclaimer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFB91C1C)
                    )
                    Text(
                        text = "Subhojit Paul and the developers of SMS Forwarder assume NO responsibility, liability, or legal accountability for any misuse, unauthorized message forwarding, interception, fraudulent actions, or violation of third-party privacy conducted using this software. Users bear 100% legal responsibility for how this tool is utilized on their devices.",
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563),
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    Text(
                        text = "3. Telecommunication & Privacy Compliance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DarkText
                    )
                    Text(
                        text = "Users must ensure their usage conforms with local laws, telecom regulations, and carrier terms. The developer provides no warranty regarding message delivery guarantees in scenarios of carrier blocking or power-off.",
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563),
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (!isExpanded) {
                Text(
                    text = "Tap to read full Terms of Use & Non-Liability Disclaimer.",
                    fontSize = 12.sp,
                    color = PurpleAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PrivacyPolicyCard(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.5.dp, PurpleBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFECFDF5), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Privacy Policy",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        Text(
                            text = "Your Data & Privacy Protection",
                            fontSize = 12.sp,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color(0xFF6B7280)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        text = "1. Zero Third-Party Monetization",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DarkText
                    )
                    Text(
                        text = "SMS Forwarder does NOT sell, rent, monetize, or harvest user data, phone numbers, or SMS message content. All data remains exclusively between your connected devices.",
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563),
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    Text(
                        text = "2. Secure Cloud Synchronization",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DarkText
                    )
                    Text(
                        text = "Messages are transmitted over encrypted HTTPS/gRPC protocols directly to your private Firebase Firestore channels, protected by role-based Cloud Security Rules.",
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563),
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (!isExpanded) {
                Text(
                    text = "Tap to review our full Privacy Commitment.",
                    fontSize = 12.sp,
                    color = Color(0xFF059669),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun FooterSection(
    onOpenWebsite: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Designed & Built with ❤️ by Subhojit Paul",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "https://subhojit-paul.pages.dev/",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE9D5FF),
            modifier = Modifier.clickable { onOpenWebsite() }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "© 2026 Subhojit Paul. All Rights Reserved.",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
