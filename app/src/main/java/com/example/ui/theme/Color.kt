package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ── Next-Gen Obsidian Palette (Uber / Swiggy / Zomato Inspired) ──────────────
val ObsidianBg = Color(0xFF0A0D12)
val ObsidianSurface = Color(0xFF121721)
val ObsidianCard = Color(0xFF171E2B)
val ObsidianElevated = Color(0xFF1E2738)
val ObsidianBorder = Color(0xFF283347)

val EmeraldPrimary = Color(0xFF10B981)
val EmeraldBright = Color(0xFF34D399)
val EmeraldDim = Color(0xFF064E3B)
val EmeraldSurface = Color(0xFF0D2821)

val CoralSecondary = Color(0xFFFF5A36)
val CoralBright = Color(0xFFFF785A)
val CoralDim = Color(0xFF4C1D13)
val CoralSurface = Color(0xFF2B1615)

val CyanTertiary = Color(0xFF38BDF8)
val CyanDim = Color(0xFF0C4A6E)
val CyanSurface = Color(0xFF0E2436)

val AmberWarning = Color(0xFFF59E0B)
val AmberSurface = Color(0xFF2C200E)

val CrimsonError = Color(0xFFEF4444)
val CrimsonSurface = Color(0xFF2D1418)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Material 3 Scheme Mappings (Defaulting to Sleek Dark Obsidian)
val PolishPrimaryDark = EmeraldPrimary
val PolishOnPrimaryDark = Color(0xFF042F2E)
val PolishPrimaryContainerDark = EmeraldDim
val PolishOnPrimaryContainerDark = Color(0xFFA7F3D0)

val PolishSecondaryDark = CoralSecondary
val PolishOnSecondaryDark = Color(0xFFFFFFFF)
val PolishSecondaryContainerDark = CoralDim
val PolishOnSecondaryContainerDark = Color(0xFFFFDAD2)

val PolishTertiaryDark = CyanTertiary
val PolishOnTertiaryDark = Color(0xFF082F49)
val PolishTertiaryContainerDark = CyanDim
val PolishOnTertiaryContainerDark = Color(0xFFBAE6FD)

val BackgroundDark = ObsidianBg
val SurfaceDark = ObsidianSurface
val SurfaceVariantDark = ObsidianCard
val OnBackgroundDark = TextPrimary
val OnSurfaceDark = TextPrimary
val OnSurfaceVariantDark = TextSecondary
val OutlineDark = ObsidianBorder

// Light fallback tokens aligned with Obsidian brand accents
val PolishPrimaryLight = EmeraldPrimary
val PolishOnPrimaryLight = Color(0xFFFFFFFF)
val PolishPrimaryContainerLight = EmeraldDim
val PolishOnPrimaryContainerLight = Color(0xFFA7F3D0)

val PolishSecondaryLight = CoralSecondary
val PolishOnSecondaryLight = Color(0xFFFFFFFF)
val PolishSecondaryContainerLight = CoralDim
val PolishOnSecondaryContainerLight = Color(0xFFFFDAD2)

val PolishTertiaryLight = CyanTertiary
val PolishOnTertiaryLight = Color(0xFF082F49)
val PolishTertiaryContainerLight = CyanDim
val PolishOnTertiaryContainerLight = Color(0xFFBAE6FD)

val BackgroundLight = ObsidianBg
val SurfaceLight = ObsidianSurface
val SurfaceVariantLight = ObsidianCard
val OnBackgroundLight = TextPrimary
val OnSurfaceLight = TextPrimary
val OnSurfaceVariantLight = TextSecondary
val OutlineLight = ObsidianBorder
val OutlineVariantLight = ObsidianBorder

// Legacy Status Tokens (preserved for full compatibility across screens)
val StatusOnlineBg = EmeraldSurface
val StatusOnlineText = EmeraldBright
val StatusOnlineDot = EmeraldPrimary

val StatusSuccessBg = EmeraldSurface
val StatusSuccessText = EmeraldBright
val StatusSuccessDot = EmeraldPrimary

val StatusOfflineBg = CrimsonSurface
val StatusOfflineText = Color(0xFFFCA5A5)
val StatusOfflineDot = CrimsonError

val StatusPendingBg = AmberSurface
val StatusPendingText = Color(0xFFFCD34D)

val FabAccentBg = EmeraldPrimary
val FabAccentContent = Color(0xFF042F2E)
