package com.example.voicereminder.presentation.finance.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereminder.domain.models.Transaction

private const val TAG = "TransactionList"

/**
 * TransactionList component displays a list of transactions.
 * Designed to be used inside a parent scrolling container (like LazyColumn).
 * For efficiency when inside a parent scrolling container, this just renders a Column of items.
 *
 * @param transactions List of transactions to display (nullable for safety)
 * @param onEditTransaction Callback when user wants to edit a transaction
 * @param onDeleteTransaction Callback when user wants to delete a transaction
 * @param modifier Optional modifier for the list
 */
@Composable
fun TransactionList(
    transactions: List<Transaction>?,
    onEditTransaction: ((Transaction) -> Unit)? = null,
    onDeleteTransaction: ((Transaction) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "TransactionList composable entered with ${transactions?.size ?: 0} transactions")

    // Cache responsive values to avoid multiple calls
    val minorSpacing = com.example.voicereminder.presentation.finance.ResponsiveUtils.getMinorSpacing()
    val emptyTitleFontSize = com.example.voicereminder.presentation.finance.ResponsiveUtils.getAdjustedFontSize(16.sp)
    val emptySubtitleFontSize = com.example.voicereminder.presentation.finance.ResponsiveUtils.getAdjustedFontSize(14.sp)

    // Null safety check - treat null as empty list
    if (transactions == null || transactions.isEmpty()) {
        Log.d(TAG, "Rendering empty state")
        // Empty state
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "No activity yet",
                    fontSize = emptyTitleFontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Your deposits and withdrawals will appear here",
                    fontSize = emptySubtitleFontSize,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Log.d(TAG, "Rendering transaction list with ${transactions.size} items")
        // When inside a parent scrolling container like LazyColumn,
        // use Column instead of LazyColumn to avoid nested scrolling containers
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(minorSpacing)
        ) {
            transactions.forEach { transaction ->
                Log.d(TAG, "Rendering transaction: ${transaction.title}")
                TransactionItem(
                    transaction = transaction,
                    onEdit = onEditTransaction,
                    onDelete = onDeleteTransaction
                )
            }
        }
    }
    Log.d(TAG, "TransactionList rendered successfully")
}
