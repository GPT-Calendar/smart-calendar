package com.example.voicereminder.presentation.finance.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereminder.domain.models.Transaction
import com.example.voicereminder.domain.models.TransactionType
import com.example.voicereminder.domain.models.formatCurrency
import com.example.voicereminder.presentation.ui.theme.FinanceGreen
import com.example.voicereminder.presentation.ui.theme.FinanceGreenLight
import com.example.voicereminder.presentation.ui.theme.FinanceRed

// Transaction amount colors - these are semantic colors that stay consistent
private val DepositGreen = Color(0xFF4CAF50)   // Green for deposits/income
private val WithdrawalRed = Color(0xFFD32F2F)  // Red for withdrawals/expenses

/**
 * TransactionItem component displays an individual transaction row.
 * Shows category icon, transaction details, and amount with color coding.
 * Supports swipe actions for edit and delete.
 * 
 * @param transaction The transaction data to display (nullable for safety)
 * @param onEdit Callback when edit action is triggered
 * @param onDelete Callback when delete action is triggered
 * @param modifier Optional modifier for the card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(
    transaction: Transaction?,
    onEdit: ((Transaction) -> Unit)? = null,
    onDelete: ((Transaction) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Null safety check - provide fallback UI if transaction is null
    if (transaction == null) {
        TransactionItemFallback(modifier = modifier)
        return
    }
    
    val cardPadding = com.example.voicereminder.presentation.finance.ResponsiveUtils.getCardPadding()
    val titleFontSize = com.example.voicereminder.presentation.finance.ResponsiveUtils.getAdjustedFontSize(16.sp)
    val subtitleFontSize = com.example.voicereminder.presentation.finance.ResponsiveUtils.getAdjustedFontSize(12.sp)
    val amountFontSize = com.example.voicereminder.presentation.finance.ResponsiveUtils.getAdjustedFontSize(16.sp)
    val minorSpacing = com.example.voicereminder.presentation.finance.ResponsiveUtils.getMinorSpacing()
    
    // Null safety: validate transaction properties
    val title = transaction.title.ifBlank { "Unknown Transaction" }
    val subtitle = transaction.subtitle.ifBlank { "No details" }
    val amount = if (transaction.amount.isNaN() || transaction.amount.isInfinite()) {
        0.0
    } else {
        transaction.amount
    }
    val type = transaction.type ?: TransactionType.SPENT
    
    // Check if transaction is manual or edited
    val isManual = transaction.isManual
    val isEdited = transaction.isEdited
    
    // Create semantic description for the transaction
    val transactionDescription = buildString {
        append(title)
        append(", ")
        append(subtitle)
        append(", ")
        append(amount.formatCurrency())
        append(" Afghani ")
        append(type.getDisplayName().lowercase())
        if (isManual) append(", manually entered")
        if (isEdited) append(", edited")
    }
    
    // Swipe to dismiss state for delete action
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // Delete action
                    onDelete?.invoke(transaction)
                    false // Don't dismiss, let parent handle removal
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Edit action
                    onEdit?.invoke(transaction)
                    false // Don't dismiss
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )
    
    // Only enable swipe if callbacks are provided
    val enableSwipe = onEdit != null || onDelete != null
    
    if (enableSwipe) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val color by animateColorAsState(
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                        SwipeToDismissBoxValue.Settled -> Color.Transparent
                    },
                    label = "swipe_color"
                )
                val alignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    SwipeToDismissBoxValue.Settled -> Alignment.Center
                }
                val icon = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                    SwipeToDismissBoxValue.Settled -> Icons.Default.Delete
                }
                val iconTint = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = if (direction == SwipeToDismissBoxValue.StartToEnd) "Edit" else "Delete",
                        tint = iconTint
                    )
                }
            },
            content = {
                TransactionItemContent(
                    transaction = transaction,
                    title = title,
                    subtitle = subtitle,
                    amount = amount,
                    type = type,
                    isManual = isManual,
                    isEdited = isEdited,
                    transactionDescription = transactionDescription,
                    cardPadding = cardPadding,
                    titleFontSize = titleFontSize,
                    subtitleFontSize = subtitleFontSize,
                    amountFontSize = amountFontSize,
                    minorSpacing = minorSpacing,
                    onClick = if (onEdit != null) { { onEdit(transaction) } } else null,
                    modifier = modifier
                )
            },
            enableDismissFromStartToEnd = onEdit != null,
            enableDismissFromEndToStart = onDelete != null
        )
    } else {
        TransactionItemContent(
            transaction = transaction,
            title = title,
            subtitle = subtitle,
            amount = amount,
            type = type,
            isManual = isManual,
            isEdited = isEdited,
            transactionDescription = transactionDescription,
            cardPadding = cardPadding,
            titleFontSize = titleFontSize,
            subtitleFontSize = subtitleFontSize,
            amountFontSize = amountFontSize,
            minorSpacing = minorSpacing,
            onClick = null,
            modifier = modifier
        )
    }

    // Subtle divider at the bottom
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp
    )
}

@Composable
private fun TransactionItemContent(
    transaction: Transaction,
    title: String,
    subtitle: String,
    amount: Double,
    type: TransactionType,
    isManual: Boolean,
    isEdited: Boolean,
    transactionDescription: String,
    cardPadding: androidx.compose.ui.unit.Dp,
    titleFontSize: androidx.compose.ui.unit.TextUnit,
    subtitleFontSize: androidx.compose.ui.unit.TextUnit,
    amountFontSize: androidx.compose.ui.unit.TextUnit,
    minorSpacing: androidx.compose.ui.unit.Dp,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .semantics(mergeDescendants = true) {
                contentDescription = transactionDescription
            },
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = cardPadding, vertical = cardPadding/2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Category Icon in circular container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                val category = transaction.category ?: com.example.voicereminder.domain.models.TransactionCategory.OTHER
                Icon(
                    imageVector = category.getIcon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Middle: Transaction details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = minorSpacing),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Manual badge
                    if (isManual) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Manual",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    // Edited badge
                    if (isEdited && !isManual) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Edited",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = subtitleFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Right: Amount with type indicator
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${if (type == TransactionType.SPENT) "−" else "+"}AFN ${amount.formatCurrency()}",
                    fontSize = amountFontSize,
                    fontWeight = FontWeight.Bold,
                    color = if (type == TransactionType.SPENT) WithdrawalRed else DepositGreen
                )
                Text(
                    text = type.getDisplayName(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (type == TransactionType.SPENT) 
                        WithdrawalRed.copy(alpha = 0.7f) 
                    else 
                        DepositGreen.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Fallback UI for TransactionItem when data is null
 */
@Composable
private fun TransactionItemFallback(modifier: Modifier = Modifier) {
    val cardPadding = com.example.voicereminder.presentation.finance.ResponsiveUtils.getCardPadding()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = cardPadding, vertical = cardPadding/2),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Transaction unavailable",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp
    )
}
