package com.example.voicereminder.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// MODERN DARK THEME - UNIFIED DESIGN SYSTEM
// ============================================

// Primary Colors - Teal/Ocean Blue (for active states and primary actions)
val PrimaryTeal = Color(0xFF14B8A6)           // Modern teal - primary brand color
val PrimaryTealLight = Color(0xFF5EEAD4)      // Light teal for highlights
val PrimaryTealDark = Color(0xFF0D9488)       // Dark teal for pressed states
val OnPrimaryTeal = Color(0xFF000000)         // Black text on teal

// Secondary Colors - Soft Coral (for secondary actions and accents)
val SecondaryCoral = Color(0xFFFF8A80)        // Soft coral accent
val SecondaryCoralLight = Color(0xFFFFB3AB)   // Light coral
val SecondaryCoralDark = Color(0xFFFF5252)    // Vibrant coral
val OnSecondaryCoral = Color(0xFF000000)      // Black text on coral

// Tertiary Colors - Sage Green (for success states)
val TertiarySage = Color(0xFF86EFAC)          // Sage green
val TertiarySageLight = Color(0xFFBBF7D0)     // Light sage
val TertiarySageDark = Color(0xFF4ADE80)      // Vibrant sage
val OnTertiarySage = Color(0xFF000000)        // Black text on sage

// Background Colors - Deep Charcoal/Slate
val BackgroundDark = Color(0xFF121212)        // Deep charcoal background
val BackgroundDarkElevated = Color(0xFF1E1E1E) // Slightly elevated surfaces
val BackgroundDarkCard = Color(0xFF2C2C2C)    // Card backgrounds
val OnBackgroundDark = Color(0xFFE5E5E5)      // Light text on dark background

// Surface Colors
val SurfaceDark = Color(0xFF1E1E1E)           // Primary surface
val SurfaceDarkVariant = Color(0xFF2C2C2C)    // Variant surface (cards)
val SurfaceDarkElevated = Color(0xFF383838)   // Elevated surface
val OnSurfaceDark = Color(0xFFE5E5E5)         // Text on surface
val OnSurfaceDarkSecondary = Color(0xFFB0B0B0) // Secondary text

// Outline Colors
val OutlineDark = Color(0xFF404040)           // Subtle dividers
val OutlineDarkVariant = Color(0xFF2C2C2C)    // Very subtle dividers

// Finance Colors - Redesigned for better contrast
val FinanceMintGreen = Color(0xFF10B981)      // Mint green for income/positive
val FinanceMintGreenLight = Color(0xFF34D399) // Light mint
val FinanceMintGreenDark = Color(0xFF059669)  // Dark mint for gradients
val FinanceGoldOrange = Color(0xFFFBBF24)     // Soft gold for alerts/warnings
val FinancePastelRed = Color(0xFFEF4444)      // Pastel red for expenses
val FinancePastelRedLight = Color(0xFFF87171) // Light red
val OnFinanceGreen = Color(0xFFFFFFFF)        // White text on green
val OnFinanceRed = Color(0xFFFFFFFF)          // White text on red

// Error Colors
val ErrorDark = Color(0xFFEF4444)             // Modern red error
val OnErrorDark = Color(0xFFFFFFFF)           // White text on error
val ErrorContainerDark = Color(0xFF7F1D1D)    // Dark red container
val OnErrorContainerDark = Color(0xFFFEE2E2) // Light red text

// Success Colors
val SuccessDark = Color(0xFF10B981)           // Green success
val OnSuccessDark = Color(0xFFFFFFFF)         // White text on success

// Warning Colors
val WarningDark = Color(0xFFFBBF24)           // Gold warning
val OnWarningDark = Color(0xFF000000)         // Black text on warning

// Other Colors
val Scrim = Color(0xCC000000)                 // Semi-transparent overlay
val InverseSurface = Color(0xFFE5E5E5)        // Light inverse surface
val InverseOnSurface = Color(0xFF1E1E1E)      // Dark text on light surface
val InversePrimary = Color(0xFF0D9488)        // Inverse primary

// Chat Bubble Colors
val ChatUserBubble = Color(0xFF14B8A6)        // Teal for user messages
val ChatAIBubble = Color(0xFF2C2C2C)          // Dark gray for AI messages
val OnChatUserBubble = Color(0xFF000000)      // Black text on user bubble
val OnChatAIBubble = Color(0xFFE5E5E5)        // Light text on AI bubble

// Calendar Colors
val CalendarSelectedDay = Color(0xFF14B8A6)   // Teal for selected date
val CalendarEventIndicator = Color(0xFF10B981) // Mint green dot for events
val CalendarToday = Color(0xFF5EEAD4)         // Light teal for today

// 60-30-10 Light Theme Colors (Blue and White only)
val PrimaryBlue = Color(0xFF305CDE)           // Deep Blue - 30% allocation
val OnPrimaryBlue = Color(0xFFFFFFFF)         // White text on primary
val PrimaryContainerBlue = Color(0xFFE8EEFF)  // Very light blue container
val OnPrimaryContainerBlue = Color(0xFF1A237E) // Dark blue text on primary container
val SecondaryBlue = Color(0xFF5D83FF)         // Light Blue - 10% accent
val OnSecondaryBlue = Color(0xFFFFFFFF)       // White text on secondary
val SecondaryContainerBlue = Color(0xFFE3F2FD) // Light blue secondary container
val OnSecondaryContainerBlue = Color(0xFF1A237E) // Dark blue text on secondary container
val TertiaryBlue = Color(0xFF5D83FF)          // Light Blue - 10% accent (same as secondary)
val OnTertiaryBlue = Color(0xFFFFFFFF)        // White text on tertiary
val TertiaryContainerBlue = Color(0xFFE8EEFF) // Very light blue tertiary container
val OnTertiaryContainerBlue = Color(0xFF1A237E) // Dark blue text on tertiary container
val BackgroundLight = Color(0xFFFFFFFF)        // Pure white background
val OnBackgroundLight = Color(0xFF212121)     // Dark gray text on background (WCAG AA compliant)
val SurfaceLight = Color(0xFFFFFFFF)          // Pure white surface
val OnSurfaceLight = Color(0xFF212121)        // Dark gray text on surface (WCAG AA compliant)
val SurfaceVariantLight = Color(0xFFF5F5F5)   // Light gray surface variant
val OnSurfaceVariantLight = Color(0xFF757575) // Medium gray text on surface variant
val Outline = Color(0xFFE0E0E0)               // Light gray outline
val OutlineVariant = Color(0xFFE0E0E0)        // Light gray outline variant
val Error = Color(0xFFD32F2F)                 // Vibrant red for errors
val OnError = Color(0xFFFFFFFF)               // White text on error
val ErrorContainer = Color(0xFFFCE4EC)        // Light red error container
val OnErrorContainer = Color(0xFF8C001A)      // Dark red text on error container

// Backward compatibility aliases for Finance colors
val FinanceGreen = FinanceMintGreen           // Alias for old code
val FinanceGreenLight = FinanceMintGreenLight // Alias for old code
val FinanceGreenDark = FinanceMintGreenDark   // Alias for old code
val FinanceRed = FinancePastelRed             // Alias for old code
val FinanceRedLight = FinancePastelRedLight   // Alias for old code

// ============================================
// MINIMALIST CHAT UI - 60-30-10 COLOR STRATEGY
// ============================================

// 30% Deep Blue - Primary branding (headers, navigation)
val MinimalDeepBlue = Color(0xFF305CDE)       // Deep blue for headers and navigation
val OnMinimalDeepBlue = Color(0xFFFFFFFF)     // White text on deep blue

// 10% Light Blue - Accents (active states, highlights)
val MinimalAccentBlue = Color(0xFF5D83FF)     // Light blue for accents and active states
val OnMinimalAccentBlue = Color(0xFFFFFFFF)   // White text on accent blue

// 60% White - Main content area
val MinimalWhite = Color(0xFFFFFFFF)          // Pure white for main content
val MinimalOffWhite = Color(0xFFF5F5F5)       // Very light grey for subtle backgrounds

// Chat Bubble Colors (Minimalist)
val MinimalChatBubbleAI = Color(0xFFF0F0F0)   // Very light grey for AI messages
val MinimalChatBubbleUser = Color(0xFFE8E8E8) // Off-white for user messages
val MinimalChatTextDark = Color(0xFF333333)   // Dark grey for text on light bubbles

// UI Element Colors (Minimalist)
val MinimalBorderGrey = Color(0xFFE0E0E0)     // Light grey for borders
val MinimalTextGrey = Color(0xFF666666)       // Medium grey for secondary text
val MinimalPlaceholderGrey = Color(0xFFAAAAAA) // Light grey for placeholders
val MinimalIconGrey = Color(0xFF999999)       // Grey for inactive icons