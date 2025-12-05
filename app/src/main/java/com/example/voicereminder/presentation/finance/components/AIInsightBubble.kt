package com.example.voicereminder.presentation.finance.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereminder.domain.models.AIInsight
import com.example.voicereminder.presentation.ui.theme.FinanceGreen
import com.example.voicereminder.presentation.ui.theme.FinanceGreenLight

private const val TAG = "AIInsightBubble"

/**
 * AIInsightBubble component displays AI-generated financial insights.
 * Shows an icon and contextual spending insight message.
 * 
 * @param insight The AI insight data to display (nullable for safety)
 * @param modifier Optional modifier for the card
 */
@Composable
fun AIInsightBubble(
    insight: AIInsight?,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "AIInsightBubble composable entered")
    
    // Null safety check - provide fallback UI if insight is null
    if (insight == null) {
        Log.w(TAG, "Insight is null, rendering fallback UI")
        AIInsightBubbleFallback(modifier = modifier)
        return
    }
    
    Log.d(TAG, "Insight message: ${insight.message}")
    
    // Cache responsive values to avoid multiple calls
    val cardPadding = com.example.voicereminder.presentation.finance.ResponsiveUtils.getCardPadding()
    val messageFontSize = com.example.voicereminder.presentation.finance.ResponsiveUtils.getAdjustedFontSize(14.sp)
    val minorSpacing = com.example.voicereminder.presentation.finance.ResponsiveUtils.getMinorSpacing()
    
    // Null safety: validate message is not blank
    val message = insight.message.ifBlank { 
        Log.w(TAG, "Insight message is blank, using fallback")
        "No insights available at this time." 
    }
    
    // Theme-aware card with primary container background
    Card(
            modifier = modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "AI Insight: $message"
                },
            shape = MaterialTheme.shapes.large.copy(
                topStart = androidx.compose.foundation.shape.CornerSize(22.dp),
                topEnd = androidx.compose.foundation.shape.CornerSize(22.dp),
                bottomStart = androidx.compose.foundation.shape.CornerSize(22.dp),
                bottomEnd = androidx.compose.foundation.shape.CornerSize(22.dp)
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding)
                    .heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(minorSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI Icon - theme-aware
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                
                // Insight Message - theme-aware text
                Text(
                    text = message,
                    fontSize = messageFontSize,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = (messageFontSize.value + 6).sp
                )
            }
        }
    Log.d(TAG, "AIInsightBubble rendered successfully")
}

/**
 * Fallback UI for AIInsightBubble when data is null
 */
@Composable
private fun AIInsightBubbleFallback(modifier: Modifier = Modifier) {
    val cardPadding = com.example.voicereminder.presentation.finance.ResponsiveUtils.getCardPadding()
    val messageFontSize = com.example.voicereminder.presentation.finance.ResponsiveUtils.getAdjustedFontSize(14.sp)
    val minorSpacing = com.example.voicereminder.presentation.finance.ResponsiveUtils.getMinorSpacing()
    
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large.copy(
            topStart = androidx.compose.foundation.shape.CornerSize(22.dp),
            topEnd = androidx.compose.foundation.shape.CornerSize(22.dp),
            bottomStart = androidx.compose.foundation.shape.CornerSize(22.dp),
            bottomEnd = androidx.compose.foundation.shape.CornerSize(22.dp)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding)
                .heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(minorSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            
            Text(
                text = "No insights available",
                fontSize = messageFontSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = (messageFontSize.value + 6).sp
            )
        }
    }
}
