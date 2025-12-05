package com.example.voicereminder.presentation.finance.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereminder.domain.models.QuickStat
import com.example.voicereminder.presentation.ui.theme.FinanceGreen

private const val TAG = "QuickStatsRow"

@Composable
fun QuickStatsRow(
    quickStats: List<QuickStat>?,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "QuickStatsRow composable entered with ${quickStats?.size ?: 0} stats")
    
    // Null safety check - provide fallback UI if quickStats is null or empty
    if (quickStats == null) {
        Log.w(TAG, "QuickStats is null, rendering fallback UI")
        QuickStatsRowFallback(modifier = modifier)
        return
    }
    
    if (quickStats.isEmpty()) {
        Log.w(TAG, "QuickStats is empty, rendering fallback UI")
        QuickStatsRowFallback(modifier = modifier)
        return
    }
    
    val minorSpacing = com.example.voicereminder.presentation.finance.ResponsiveUtils.getMinorSpacing()
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(minorSpacing)
    ) {
        quickStats.forEachIndexed { index, stat ->
            Log.d(TAG, "Rendering stat $index: ${stat.label} = ${stat.value}")
            StatCard(
                quickStat = stat,
                modifier = Modifier.weight(1f)
            )
        }
    }
    Log.d(TAG, "QuickStatsRow rendered successfully")
}

@Composable
private fun StatCard(
    quickStat: QuickStat?,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "StatCard composable entered for: ${quickStat?.label ?: "null"}")
    
    // Null safety check - provide fallback if quickStat is null
    if (quickStat == null) {
        Log.w(TAG, "QuickStat is null, rendering fallback")
        StatCardFallback(modifier = modifier)
        return
    }
    
    // Cache responsive values to avoid multiple calls
    val cardPadding = com.example.voicereminder.presentation.finance.ResponsiveUtils.getCardPadding()
    val valueFontSize = com.example.voicereminder.presentation.finance.ResponsiveUtils.getAdjustedFontSize(18.sp)
    val labelFontSize = com.example.voicereminder.presentation.finance.ResponsiveUtils.getAdjustedFontSize(12.sp)
    
    // Null safety: validate label and value are not blank
    val label = quickStat.label.ifBlank { 
        Log.w(TAG, "QuickStat label is blank, using fallback")
        "N/A" 
    }
    val value = quickStat.value.ifBlank { 
        Log.w(TAG, "QuickStat value is blank, using fallback")
        "0" 
    }
    
    // Create semantic description for the stat card
    // Use remember to avoid rebuilding string on every recomposition
    val semanticDescription = androidx.compose.runtime.remember(label, value) {
        buildString {
            append(label)
            append(": ")
            append(value)
            // Add "Afghani" suffix if value contains "AFN"
            if (value.contains("AFN")) {
                append(" Afghani")
            }
        }
    }
    
    Card(
            modifier = modifier
                .semantics(mergeDescendants = true) {
                    contentDescription = semanticDescription
                },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding)
                    .heightIn(min = 48.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val icon = quickStat.icon ?: Icons.Default.MoreHoriz
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = value,
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = label,
                    fontSize = labelFontSize,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    Log.d(TAG, "StatCard rendered successfully for: $label")
}

/**
 * Fallback UI for QuickStatsRow when data is null or empty
 */
@Composable
private fun QuickStatsRowFallback(modifier: Modifier = Modifier) {
    val minorSpacing = com.example.voicereminder.presentation.finance.ResponsiveUtils.getMinorSpacing()
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(minorSpacing)
    ) {
        repeat(3) {
            StatCardFallback(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Fallback UI for StatCard when data is null
 */
@Composable
private fun StatCardFallback(modifier: Modifier = Modifier) {
    val cardPadding = com.example.voicereminder.presentation.finance.ResponsiveUtils.getCardPadding()
    
    Card(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding)
                .heightIn(min = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "N/A",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
