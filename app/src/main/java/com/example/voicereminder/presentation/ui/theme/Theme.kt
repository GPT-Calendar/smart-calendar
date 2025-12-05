package com.example.voicereminder.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryBlue,
    primaryContainer = PrimaryContainerBlue,
    onPrimaryContainer = OnPrimaryContainerBlue,
    
    secondary = SecondaryBlue,
    onSecondary = OnSecondaryBlue,
    secondaryContainer = SecondaryContainerBlue,
    onSecondaryContainer = OnSecondaryContainerBlue,
    
    tertiary = TertiaryBlue,
    onTertiary = OnTertiaryBlue,
    tertiaryContainer = TertiaryContainerBlue,
    onTertiaryContainer = OnTertiaryContainerBlue,
    
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = Outline,
    outlineVariant = OutlineVariant,
    
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    
    scrim = Scrim,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    inversePrimary = InversePrimary,
)

private val DarkColorScheme = darkColorScheme(
    // Primary - Modern Teal
    primary = PrimaryTeal,
    onPrimary = OnPrimaryTeal,
    primaryContainer = PrimaryTealDark,
    onPrimaryContainer = PrimaryTealLight,
    
    // Secondary - Soft Coral
    secondary = SecondaryCoral,
    onSecondary = OnSecondaryCoral,
    secondaryContainer = SecondaryCoralDark,
    onSecondaryContainer = SecondaryCoralLight,
    
    // Tertiary - Sage Green
    tertiary = TertiarySage,
    onTertiary = OnTertiarySage,
    tertiaryContainer = TertiarySageDark,
    onTertiaryContainer = TertiarySageLight,
    
    // Background - Deep Charcoal
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    
    // Surface - Elevated Dark
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDarkVariant,
    onSurfaceVariant = OnSurfaceDarkSecondary,
    
    // Outlines
    outline = OutlineDark,
    outlineVariant = OutlineDarkVariant,
    
    // Error
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    
    // Other
    scrim = Scrim,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    inversePrimary = InversePrimary,
)

@Composable
fun SmartCalendarTheme(
    darkTheme: Boolean = false, // Default to light theme for professional look
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            // Use dynamic colors for Android 12+
            if (darkTheme) {
                androidx.compose.material3.dynamicDarkColorScheme(LocalContext.current)
            } else {
                androidx.compose.material3.dynamicLightColorScheme(LocalContext.current)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}