package com.example.ui.update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.util.UpdateChecker
import kotlinx.coroutines.launch
import java.io.File

private sealed interface UpdateDialogState {
    data object Idle : UpdateDialogState
    data class Downloading(
        val progress: Float,
        val downloadedMb: Double,
        val totalMb: Double
    ) : UpdateDialogState
    data class Downloaded(val apkFile: File) : UpdateDialogState
    data class AwaitingInstallPermission(val apkFile: File) : UpdateDialogState
    data class Error(val message: String) : UpdateDialogState
}

@Composable
fun InAppUpdateDialog(
    updateInfo: UpdateChecker.UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var dialogState by remember { mutableStateOf<UpdateDialogState>(UpdateDialogState.Idle) }

    fun launchApkInstall(apkFile: File) {
        when (UpdateChecker.installApk(context, apkFile)) {
            UpdateChecker.InstallLaunchResult.LAUNCHED -> {
                dialogState = UpdateDialogState.Downloaded(apkFile)
            }
            UpdateChecker.InstallLaunchResult.NEEDS_UNKNOWN_SOURCES_PERMISSION -> {
                dialogState = UpdateDialogState.AwaitingInstallPermission(apkFile)
            }
            UpdateChecker.InstallLaunchResult.FAILED -> {
                dialogState = UpdateDialogState.Error("Unable to launch installer. Please retry.")
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val awaiting = dialogState as? UpdateDialogState.AwaitingInstallPermission
                if (awaiting != null) {
                    val canInstall = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.packageManager.canRequestPackageInstalls()
                    } else true
                    if (canInstall) {
                        launchApkInstall(awaiting.apkFile)
                    } else {
                        dialogState = UpdateDialogState.Downloaded(awaiting.apkFile)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isDownloading = dialogState is UpdateDialogState.Downloading

    Dialog(
        onDismissRequest = {
            if (!updateInfo.isCritical && !isDownloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isCritical && !isDownloading,
            dismissOnClickOutside = !updateInfo.isCritical && !isDownloading
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (updateInfo.isCritical) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (updateInfo.isCritical) Icons.Default.Warning else Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = if (updateInfo.isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title & Version Badge
                Text(
                    text = if (updateInfo.isCritical) "Critical Update Required" else "New Version Available!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Current: v${updateInfo.currentVersion}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = updateInfo.latestVersion,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (updateInfo.apkSizeMb > 0.0) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = String.format("%.1f MB", updateInfo.apkSizeMb),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Release Notes / Changelog
                if (updateInfo.releaseNotes.isNotBlank()) {
                    Text(
                        text = "What's New in this Release:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = updateInfo.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Download Progress / Status Bar
                when (val s = dialogState) {
                    is UpdateDialogState.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Downloading APK...",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${(s.progress * 100).toInt()}% (${String.format("%.1f", s.downloadedMb)} / ${String.format("%.1f", s.totalMb)} MB)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { s.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                    is UpdateDialogState.Downloaded,
                    is UpdateDialogState.AwaitingInstallPermission -> {
                        Surface(
                            color = Color(0xFF062E22),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (s is UpdateDialogState.AwaitingInstallPermission) {
                                        "Allow \"Install unknown apps\" to continue installation."
                                    } else {
                                        "Download complete. Ready to install."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }
                    }
                    is UpdateDialogState.Error -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = s.message.ifBlank { "Download failed. Please try again." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    UpdateDialogState.Idle -> { /* No progress needed */ }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!updateInfo.isCritical && !isDownloading) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Later")
                        }
                    }

                    Button(
                        onClick = {
                            when (val s = dialogState) {
                                UpdateDialogState.Idle, is UpdateDialogState.Error -> {
                                    if (updateInfo.downloadUrl.endsWith(".apk", ignoreCase = true)) {
                                        dialogState = UpdateDialogState.Downloading(0f, 0.0, updateInfo.apkSizeMb)
                                        scope.launch {
                                            val result = UpdateChecker.downloadApk(
                                                context = context,
                                                downloadUrl = updateInfo.downloadUrl,
                                                onProgress = { p, dMb, tMb ->
                                                    val currentTotal = if (tMb > 0) tMb else updateInfo.apkSizeMb
                                                    dialogState = UpdateDialogState.Downloading(p, dMb, currentTotal)
                                                }
                                            )
                                            val file = result.getOrNull()
                                            if (result.isSuccess && file != null) {
                                                launchApkInstall(file)
                                            } else {
                                                dialogState = UpdateDialogState.Error(
                                                    result.exceptionOrNull()?.localizedMessage ?: "Download failed"
                                                )
                                            }
                                        }
                                    } else {
                                        // Fallback to browser if no APK asset is attached
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(updateInfo.browserUrl.ifEmpty { updateInfo.downloadUrl })
                                        )
                                        context.startActivity(intent)
                                    }
                                }
                                is UpdateDialogState.Downloaded -> {
                                    launchApkInstall(s.apkFile)
                                }
                                is UpdateDialogState.AwaitingInstallPermission -> {
                                    launchApkInstall(s.apkFile)
                                }
                                is UpdateDialogState.Downloading -> { /* Do nothing */ }
                            }
                        },
                        enabled = !isDownloading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (updateInfo.isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        when (dialogState) {
                            is UpdateDialogState.Downloading -> Text("Downloading...")
                            is UpdateDialogState.Downloaded -> Text("Install Now")
                            is UpdateDialogState.AwaitingInstallPermission -> Text("Grant & Install")
                            is UpdateDialogState.Error -> Text("Retry")
                            UpdateDialogState.Idle -> {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Update Now")
                            }
                        }
                    }
                }
            }
        }
    }
}
