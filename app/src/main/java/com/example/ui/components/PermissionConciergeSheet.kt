package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralBright
import com.example.ui.theme.CoralSecondary
import com.example.ui.theme.CoralSurface
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

data class PermissionStepItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val isRequired: Boolean = true,
    val actionLabel: String = "Allow",
    val onGrantClick: () -> Unit
)

/**
 * Compact, high-visibility Permission Readiness Banner placed at the top of Host or Client screens.
 * Tapping opens the interactive step-by-step PermissionConciergeSheet.
 */
@Composable
fun PermissionStatusBanner(
    steps: List<PermissionStepItem>,
    onOpenSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val grantedCount = steps.count { it.isGranted }
    val totalCount = steps.size
    val allGranted = grantedCount == totalCount
    val nextPending = steps.firstOrNull { !it.isGranted }

    Surface(
        onClick = onOpenSheet,
        shape = RoundedCornerShape(20.dp),
        color = if (allGranted) EmeraldSurface.copy(alpha = 0.7f) else CoralSurface.copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            if (allGranted) EmeraldPrimary.copy(alpha = 0.45f) else CoralSecondary.copy(alpha = 0.55f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("permission_status_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (allGranted) EmeraldPrimary.copy(alpha = 0.2f) else CoralSecondary.copy(alpha = 0.22f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (allGranted) Icons.Default.CheckCircle else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (allGranted) EmeraldBright else CoralBright,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (allGranted) "All Permissions Active" else "Permissions ($grantedCount/$totalCount)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = if (allGranted) EmeraldPrimary else CoralSecondary
                        ) {
                            Text(
                                text = if (allGranted) "READY" else "SETUP",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (allGranted) ObsidianBg else Color.White,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (allGranted) {
                            "Background engine & alerts have full system access"
                        } else {
                            "Next: ${nextPending?.title ?: "Tap to review"} — tap to grant"
                        },
                        fontSize = 12.sp,
                        color = if (allGranted) EmeraldBright.copy(alpha = 0.9f) else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open Permissions",
                tint = if (allGranted) EmeraldBright else CoralBright,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Interactive Step-by-Step Permission Concierge Bottom Sheet.
 * Lets users understand and grant each permission individually or all at once with live animated checkmarks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionConciergeSheet(
    title: String,
    subtitle: String,
    steps: List<PermissionStepItem>,
    onGrantAllQuickly: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val grantedCount = steps.count { it.isGranted }
    val totalCount = steps.size.coerceAtLeast(1)
    val progress by animateFloatAsState(
        targetValue = grantedCount.toFloat() / totalCount.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "perm_progress"
    )
    val allGranted = grantedCount == steps.size
    val allRequiredGranted = steps.filter { it.isRequired }.all { it.isGranted }

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
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            // Pinned Header Row (never clips when scrolling)
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
                            .size(44.dp)
                            .background(
                                Brush.linearGradient(listOf(EmeraldPrimary, CoralSecondary)),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 18.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(ObsidianCard, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pinned Animated Progress Bar Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (allGranted) "100% Ready — All Set!" else "$grantedCount of ${steps.size} Permissions Active",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (allGranted) EmeraldBright else TextPrimary
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (allGranted) EmeraldBright else CoralBright
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(ObsidianElevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0.05f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(100.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        if (allGranted) listOf(EmeraldPrimary, EmeraldBright)
                                        else listOf(CoralSecondary, EmeraldPrimary)
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Step Cards List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                steps.forEachIndexed { index, step ->
                    PermissionStepCard(
                        stepNumber = index + 1,
                        step = step
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Primary CTA
            if (!allGranted && onGrantAllQuickly != null) {
                Button(
                    onClick = onGrantAllQuickly,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralSecondary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_all_permissions_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (allRequiredGranted) {
                            "One-Tap Grant Remaining Permissions"
                        } else {
                            "One-Tap Grant Core Permissions"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (allGranted || allRequiredGranted) EmeraldPrimary else ObsidianCard,
                    contentColor = if (allGranted || allRequiredGranted) ObsidianBg else TextPrimary
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (allGranted) "Done — Everything is Active" else "Continue to Dashboard",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PermissionStepCard(
    stepNumber: Int,
    step: PermissionStepItem
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (step.isGranted) EmeraldSurface.copy(alpha = 0.45f) else ObsidianCard,
        border = BorderStroke(
            1.dp,
            if (step.isGranted) EmeraldPrimary.copy(alpha = 0.5f) else ObsidianBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !step.isGranted) { step.onGrantClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Step Icon or Animated Checkmark
                AnimatedContent(
                    targetState = step.isGranted,
                    label = "step_check_${step.id}"
                ) { granted ->
                    if (granted) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(EmeraldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Granted",
                                tint = ObsidianBg,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(ObsidianElevated, RoundedCornerShape(12.dp))
                                .border(1.dp, CoralSecondary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = CoralBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$stepNumber. ${step.title}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!step.isRequired) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ObsidianElevated,
                            border = BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Text(
                                text = "OPTIONAL FEATURE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMuted,
                                maxLines = 1,
                                softWrap = false,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = step.subtitle,
                        fontSize = 12.sp,
                        color = if (step.isGranted) EmeraldBright.copy(alpha = 0.85f) else TextSecondary,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (step.isGranted) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = EmeraldPrimary.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "Active ✓",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldBright,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            } else {
                Button(
                    onClick = step.onGrantClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralSecondary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = step.actionLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
