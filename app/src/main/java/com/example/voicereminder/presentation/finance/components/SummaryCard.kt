package com.example.voicereminder.presentation.finance.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereminder.domain.models.FinanceSummary
import com.example.voicereminder.domain.models.TrendDirection
import com.example.voicereminder.domain.models.formatCurrency
import com.example.voicereminder.presentation.ui.theme.FinanceGreen
import com.example.voicereminder.presentation.ui.theme.FinanceRed

private const val TAG = "SummaryCard"

/**
 * SummaryCard component displays the monthly financial overview.
 * Shows total spent, total received, and trend indicator with responsive height.
 * 
 * @param summary The financial summary data to display (nullable for safety)
 * @param modifier Optional modifier for the card
 */
@Composable
fun SummaryCard(
    summary: FinanceSummary?,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "SummaryCard composable entered")
    
    // Null safety check - provide fallback UI if summary is null
    if (summary == null) {
        Log.w(TAG, "Summary is null, rendering fallback UI")
        SummaryCardFallback(modifier = modifier)
        return
    }
    
    Log.d(TAG, "Summary data - totalSpent: ${summary.totalSpent}, totalReceived: ${summary.totalReceived}")
    Log.d(TAG, "Summary data - trendPercentage: ${summary.trendPercentage}, trendDirection: ${summary.trendDirection}")
    
    // Cache all responsive values at the top to avoid multiple calls
    // These will automatically recompose when configuration changes
    val heightPercentage = com.example.voicereminder.presentation.finance.ResponsiveUtils.getSummaryCardHeightPercentage()
    val cardPadding = com.example.voicereminder.presentation.finance.ResponsiveUtils.getCardPadding()
    val titleFontSize = com.example.voicereminder.presentation.finance.ResponsiveUtils.getAdjustedFontSize(18.sp)
    val labelFontSize = com.example.voicereminder.presentation.finance.ResponsiveUtils.getAdjustedFontSize(14.sp)
    val amountFontSize = com.example.voicereminder.presentation.finance.ResponsiveUtils.getAdjustedFontSize(24.sp)
    val trendFontSize = com.example.voicereminder.presentation.finance.ResponsiveUtils.getAdjustedFontSize(13.sp)
    val minorSpacing = com.example.voicereminder.presentation.finance.ResponsiveUtils.getMinorSpacing()
    
    Log.d(TAG, "Responsive values - heightPercentage: $heightPercentage, cardPadding: $cardPadding")

    // Calculate responsive height based on a safe fallback height
    // Use remember to avoid recalculating on every recomposition
    val calculatedHeight = androidx.compose.runtime.remember(heightPercentage) {
        (heightPercentage * 100).dp.coerceAtLeast(120.dp).coerceAtMost(200.dp)
    }
    Log.d(TAG, "Calculated card height: $calculatedHeight")

        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(calculatedHeight),
            shape = MaterialTheme.shapes.large.copy(
                topStart = androidx.compose.foundation.shape.CornerSize(28.dp),
                topEnd = androidx.compose.foundation.shape.CornerSize(28.dp),
                bottomStart = androidx.compose.foundation.shape.CornerSize(28.dp),
                bottomEnd = androidx.compose.foundation.shape.CornerSize(28.dp)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    )
                    .padding(cardPadding)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(minorSpacing)
                ) {
                    // Title
                    Text(
                        text = "This Month Overview",
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer, // Using theme color for contrast
                        modifier = Modifier.semantics { heading() }
                    )

                    // Money Out (Withdrawals) and Money In (Deposits) Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Money Out (Withdrawals) Column
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Money Out",
                                    fontSize = labelFontSize,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            // Null safety: validate totalSpent is not NaN or Infinity
                            val totalSpentValue = if (summary.totalSpent.isNaN() || summary.totalSpent.isInfinite()) {
                                Log.w(TAG, "Invalid totalSpent value: ${summary.totalSpent}, using 0.0")
                                0.0
                            } else {
                                summary.totalSpent
                            }
                            Text(
                                text = "AFN ${totalSpentValue.formatCurrency()}",
                                fontSize = amountFontSize,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.semantics {
                                    contentDescription = "${totalSpentValue.formatCurrency()} Afghani withdrawn"
                                }
                            )
                            Text(
                                text = "Withdrawals",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        // Money In (Deposits) Column
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Money In",
                                    fontSize = labelFontSize,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            // Null safety: validate totalReceived is not NaN or Infinity
                            val totalReceivedValue = if (summary.totalReceived.isNaN() || summary.totalReceived.isInfinite()) {
                                Log.w(TAG, "Invalid totalReceived value: ${summary.totalReceived}, using 0.0")
                                0.0
                            } else {
                                summary.totalReceived
                            }
                            Text(
                                text = "AFN ${totalReceivedValue.formatCurrency()}",
                                fontSize = amountFontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.semantics {
                                    contentDescription = "${totalReceivedValue.formatCurrency()} Afghani deposited"
                                }
                            )
                            Text(
                                text = "Deposits",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Trend Indicator Row
                    // Null safety: validate trendPercentage is not NaN or Infinity
                    val trendPercentageValue = if (summary.trendPercentage.isNaN() || summary.trendPercentage.isInfinite()) {
                        Log.w(TAG, "Invalid trendPercentage value: ${summary.trendPercentage}, using 0.0")
                        0.0
                    } else {
                        summary.trendPercentage
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = "Spending trend ${if (trendPercentageValue >= 0) "increased" else "decreased"} by ${kotlin.math.abs(trendPercentageValue).toInt()} percent from last month"
                        }
                    ) {
                        // Null safety: handle null trendDirection with safe fallback
                        val trendDirection = summary.trendDirection ?: TrendDirection.NEUTRAL
                        Icon(
                            imageVector = when (trendDirection) {
                                TrendDirection.UP -> Icons.Default.ArrowUpward
                                TrendDirection.DOWN -> Icons.Default.ArrowDownward
                                TrendDirection.FLAT, TrendDirection.NEUTRAL -> Icons.Default.ArrowUpward // Could use a flat arrow icon instead
                            },
                            contentDescription = null, // Merged into parent
                            tint = if (trendDirection == TrendDirection.UP || trendDirection == TrendDirection.FLAT || trendDirection == TrendDirection.NEUTRAL)
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, // Using theme colors
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Trend: ${if (trendPercentageValue >= 0) "+" else ""}${trendPercentageValue.toInt()}% from last month",
                            fontSize = trendFontSize,
                            color = MaterialTheme.colorScheme.onPrimaryContainer // Using theme color for contrast
                        )
                    }
                }
            }
        }
    Log.d(TAG, "SummaryCard rendered successfully")
}

/**
 * Fallback UI for SummaryCard when data is null
 */
@Composable
private fun SummaryCardFallback(modifier: Modifier = Modifier) {
    val cardPadding = com.example.voicereminder.presentation.finance.ResponsiveUtils.getCardPadding()
    val calculatedHeight = 150.dp
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(calculatedHeight),
        shape = MaterialTheme.shapes.large.copy(
            topStart = androidx.compose.foundation.shape.CornerSize(28.dp),
            topEnd = androidx.compose.foundation.shape.CornerSize(28.dp),
            bottomStart = androidx.compose.foundation.shape.CornerSize(28.dp),
            bottomEnd = androidx.compose.foundation.shape.CornerSize(28.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(cardPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Summary unavailable",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
