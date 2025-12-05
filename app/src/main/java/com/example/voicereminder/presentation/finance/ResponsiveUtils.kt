package com.example.voicereminder.presentation.finance

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Utility object for responsive design calculations based on screen size.
 * Provides adaptive padding, font sizes, and layout dimensions with safe defaults.
 * All functions include null checks and fallback values to prevent crashes.
 */
object ResponsiveUtils {
    
    // Safe default values
    private const val DEFAULT_SCREEN_WIDTH = 360
    private const val SMALL_SCREEN_THRESHOLD = 360
    private const val LARGE_SCREEN_THRESHOLD = 600
    
    /**
     * Screen size categories based on width
     */
    enum class ScreenSize {
        SMALL,   // < 360dp
        MEDIUM,  // 360-600dp
        LARGE    // > 600dp
    }
    
    /**
     * Get the current screen size category with safe defaults
     * Returns MEDIUM if configuration is unavailable or invalid
     */
    @Composable
    fun getScreenSize(): ScreenSize {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        
        // Validate screen width is positive
        if (screenWidth <= 0) {
            return ScreenSize.MEDIUM
        }
        
        return when {
            screenWidth < SMALL_SCREEN_THRESHOLD -> ScreenSize.SMALL
            screenWidth <= LARGE_SCREEN_THRESHOLD -> ScreenSize.MEDIUM
            else -> ScreenSize.LARGE
        }
    }
    
    /**
     * Get responsive horizontal padding based on screen size with safe screen width calculations
     * Small: 12.dp, Medium: 16.dp, Large: 24.dp
     */
    @Composable
    fun getHorizontalPadding(): Dp {
        return when (getScreenSize()) {
            ScreenSize.SMALL -> 12.dp
            ScreenSize.MEDIUM -> 16.dp
            ScreenSize.LARGE -> 24.dp
        }
    }
    
    /**
     * Get responsive font size adjustment with safe defaults
     * Small: -1sp, Medium: 0sp, Large: +1sp
     */
    @Composable
    fun getFontSizeAdjustment(): TextUnit {
        return when (getScreenSize()) {
            ScreenSize.SMALL -> (-1).sp
            ScreenSize.MEDIUM -> 0.sp
            ScreenSize.LARGE -> 1.sp
        }
    }
    
    /**
     * Get adjusted font size based on base size and screen size with screen-based font scaling
     * Includes validation and ensures result is always positive
     * Returns baseSize if result would be invalid
     */
    @Composable
    fun getAdjustedFontSize(baseSize: TextUnit): TextUnit {
        val adjustment = getFontSizeAdjustment()
        val adjustedValue = baseSize.value + adjustment.value
        
        // Ensure font size is positive
        return if (adjustedValue <= 0) {
            baseSize
        } else {
            adjustedValue.sp
        }
    }
    
    /**
     * Get SummaryCard height percentage based on screen size with safe percentage calculation
     * Small: 20%, Medium: 22%, Large: 25%
     * Ensures result is always between 0.0 and 1.0
     */
    @Composable
    fun getSummaryCardHeightPercentage(): Float {
        val percentage = when (getScreenSize()) {
            ScreenSize.SMALL -> 0.20f
            ScreenSize.MEDIUM -> 0.22f
            ScreenSize.LARGE -> 0.25f
        }
        
        // Validate percentage is in valid range
        return if (percentage < 0.0f || percentage > 1.0f) {
            0.22f // Safe default
        } else {
            percentage
        }
    }
    
    /**
     * Get responsive spacing between major elements with fallback values
     * Small: 12.dp, Medium: 16.dp, Large: 20.dp
     */
    @Composable
    fun getMajorSpacing(): Dp {
        return when (getScreenSize()) {
            ScreenSize.SMALL -> 12.dp
            ScreenSize.MEDIUM -> 16.dp
            ScreenSize.LARGE -> 20.dp
        }
    }
    
    /**
     * Get responsive spacing between minor elements with fallback values
     * Small: 8.dp, Medium: 12.dp, Large: 14.dp
     */
    @Composable
    fun getMinorSpacing(): Dp {
        return when (getScreenSize()) {
            ScreenSize.SMALL -> 8.dp
            ScreenSize.MEDIUM -> 12.dp
            ScreenSize.LARGE -> 14.dp
        }
    }
    
    /**
     * Get responsive card padding with fallback values
     * Small: 12.dp, Medium: 16.dp, Large: 20.dp
     */
    @Composable
    fun getCardPadding(): Dp {
        return when (getScreenSize()) {
            ScreenSize.SMALL -> 12.dp
            ScreenSize.MEDIUM -> 16.dp
            ScreenSize.LARGE -> 20.dp
        }
    }
}
