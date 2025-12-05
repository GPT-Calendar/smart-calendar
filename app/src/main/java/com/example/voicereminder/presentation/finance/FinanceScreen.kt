package com.example.voicereminder.presentation.finance

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicereminder.data.FinanceRepository
import com.example.voicereminder.domain.models.Budget
import com.example.voicereminder.domain.models.Transaction
import com.example.voicereminder.domain.models.TransactionFilter

import com.example.voicereminder.presentation.finance.components.*

private const val TAG = "FinanceScreen"

/**
 * Main Finance Screen composable that displays the complete financial overview.
 * Handles different UI states: Loading, Success, Error, and Empty.
 * 
 * @param viewModel The FinanceViewModel instance (provided by default)
 * @param modifier Optional modifier for the screen
 */
@Composable
fun FinanceScreen(
    modifier: Modifier = Modifier,
    viewModel: FinanceViewModel = viewModel(
        factory = FinanceViewModelFactory(
            com.example.voicereminder.data.FinanceRepository(LocalContext.current.applicationContext),
            LocalContext.current.applicationContext
        )
    )
) {
    Log.d(TAG, "FinanceScreen composable entered")

    // Add tab state to manage financial data vs pattern management
    val tabIndex = remember { mutableIntStateOf(0) }

    // Load data lazily when screen becomes visible
    // This prevents unnecessary database operations on app startup
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.onScreenVisible()
    }

    Log.d(TAG, "Collecting UI state from ViewModel")
    val uiState by viewModel.uiState.collectAsState()
    val smsSources by viewModel.smsSources.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val showAddTransactionDialog by viewModel.showAddTransactionDialog.collectAsState()
    val editingTransaction by viewModel.editingTransaction.collectAsState()
    val showBudgetDialog by viewModel.showBudgetDialog.collectAsState()
    val editingBudget by viewModel.editingBudget.collectAsState()
    Log.d(TAG, "Current UI state: ${uiState.javaClass.simpleName}")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab selection - theme-aware colors
        TabRow(
            selectedTabIndex = tabIndex.intValue,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                if (tabIndex.intValue < tabPositions.size) {
                    val currentTab = tabPositions[tabIndex.intValue]
                    Box(
                        modifier = Modifier
                            .offset(x = currentTab.left)
                            .width(currentTab.width)
                            .fillMaxHeight()
                    ) {
                        // Bottom indicator line - Light Blue accent (10%)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            },
            divider = {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            }
        ) {
            // Selected tab: primary color text, bold
            // Unselected tab: muted text, normal weight
            Tab(
                selected = tabIndex.intValue == 0,
                onClick = { tabIndex.intValue = 0 },
                text = {
                    Text(
                        "Financial Data",
                        color = if (tabIndex.intValue == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (tabIndex.intValue == 0) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = tabIndex.intValue == 1,
                onClick = { tabIndex.intValue = 1 },
                text = {
                    Text(
                        "SMS Sources",
                        color = if (tabIndex.intValue == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (tabIndex.intValue == 1) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                    )
                }
            )
        }

        // Content based on selected tab
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (tabIndex.intValue) {
                0 -> {  // Financial Data Tab
                    when (val state = uiState) {
                        is FinanceUiState.Loading -> {
                            Log.d(TAG, "Rendering LoadingState")
                            LoadingState()
                        }

                        is FinanceUiState.Success -> {
                            Log.d(TAG, "Rendering SuccessState with data")
                            Log.d(TAG, "Success data - transactions: ${state.data.transactions?.size ?: 0}")
                            SuccessState(
                                data = state.data,
                                budgets = budgets,
                                filterState = filterState,
                                filteredTransactions = filteredTransactions,
                                onFilterChange = { viewModel.updateFilter(it) },
                                onAddBudgetClick = { viewModel.showCreateBudget() },
                                onBudgetClick = { viewModel.showEditBudget(it) },
                                onEditTransaction = { viewModel.showEditTransaction(it) },
                                onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                onAddTransactionClick = { viewModel.showAddTransaction() }
                            )
                        }

                        is FinanceUiState.Error -> {
                            Log.d(TAG, "Rendering ErrorState with message: ${state.message}")
                            ErrorState(
                                message = state.message,
                                onRetry = { viewModel.retry() }
                            )
                        }

                        is FinanceUiState.Empty -> {
                            Log.d(TAG, "Rendering EmptyState")
                            EmptyState()
                        }
                    }
                }
                1 -> {  // SMS Sources Tab
                    SmsSourceManagement(
                        sources = smsSources,
                        onAddSource = { name, phoneNumber, description ->
                            viewModel.addSmsSource(name, phoneNumber, description)
                        },
                        onUpdateSource = { source ->
                            viewModel.updateSmsSource(source)
                        },
                        onDeleteSource = { source ->
                            viewModel.deleteSmsSource(source)
                        }
                    )
                }
            }
        }
    }
    
    // Dialogs
    if (showAddTransactionDialog) {
        AddTransactionDialog(
            onDismiss = { viewModel.hideAddTransaction() },
            onConfirm = { amount, type, category, description, date, notes ->
                viewModel.addManualTransaction(amount, type, category, description, date, notes)
            }
        )
    }
    
    editingTransaction?.let { transaction ->
        EditTransactionDialog(
            transaction = transaction,
            onDismiss = { viewModel.hideEditTransaction() },
            onSave = { id, amount, type, category, description, notes ->
                viewModel.editTransaction(id, amount, type, category, description, notes)
            },
            onDelete = { viewModel.deleteTransaction(it) }
        )
    }
    
    if (showBudgetDialog) {
        SetBudgetDialog(
            existingBudget = editingBudget,
            onDismiss = { viewModel.hideBudgetDialog() },
            onConfirm = { category, limit, month ->
                viewModel.saveBudget(category, limit, month)
            },
            onDelete = editingBudget?.let { budget -> { viewModel.deleteBudget(budget) } }
        )
    }
}

/**
 * Loading state UI - displays a circular progress indicator
 */
@Composable
private fun LoadingState() {
    Log.d(TAG, "LoadingState composable entered")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { 
                heading()
            },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Loading financial data",
            modifier = Modifier
                .padding(top = 80.dp)
                .semantics { heading() }
        )
    }
    Log.d(TAG, "LoadingState rendered successfully")
}

/**
 * Success state UI - displays all financial components
 * Enhanced with budget overview, filter bar, and FAB for adding transactions
 */
@Composable
private fun SuccessState(
    data: com.example.voicereminder.domain.models.FinanceData,
    budgets: List<Budget>,
    filterState: TransactionFilter,
    filteredTransactions: List<Transaction>,
    onFilterChange: (TransactionFilter) -> Unit,
    onAddBudgetClick: () -> Unit,
    onBudgetClick: (Budget) -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onAddTransactionClick: () -> Unit
) {
    Log.d(TAG, "SuccessState composable entered")

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .semantics { heading() },
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
        ) {
            item {
                // Summary Card (30% allocation) - Deep Blue background
                Log.d(TAG, "Rendering SummaryCard")
                SummaryCard(summary = data.summary)
            }
            
            item {
                // Budget Overview Card (NEW)
                Log.d(TAG, "Rendering BudgetOverviewCard with ${budgets.size} budgets")
                BudgetOverviewCard(
                    budgets = budgets,
                    onBudgetClick = onBudgetClick,
                    onAddBudgetClick = onAddBudgetClick
                )
            }

            item {
                // Quick Stats Row
                Log.d(TAG, "Rendering QuickStatsRow with ${data.quickStats?.size ?: 0} stats")
                QuickStatsRow(quickStats = data.quickStats)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Chart (on white background using 10% Light Blue accent for data)
                Log.d(TAG, "Rendering Finance Chart")
                FinanceChart()
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                // AI Insight Bubble
                Log.d(TAG, "Rendering AIInsightBubble")
                AIInsightBubble(insight = data.insight)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Transaction Filter Bar (NEW)
                TransactionFilterBar(
                    filter = filterState,
                    onFilterChange = onFilterChange
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Transaction List Header with legend
                Column {
                    Text(
                        text = if (filterState.hasActiveFilters()) 
                            "Filtered Activity (${filteredTransactions.size})" 
                        else 
                            "Recent Activity",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .semantics { heading() }
                    )
                    // Legend for users to understand the colors
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(MaterialTheme.colorScheme.error, shape = RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = "Withdrawal (Money Out)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(MaterialTheme.colorScheme.tertiary, shape = RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = "Deposit (Money In)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

            item {
                // Transaction List - use filtered transactions if filter is active
                val transactionsToShow = if (filterState.hasActiveFilters()) {
                    filteredTransactions
                } else {
                    data.transactions
                }
                Log.d(TAG, "Rendering TransactionList with ${transactionsToShow?.size ?: 0} transactions")
                TransactionList(
                    transactions = transactionsToShow,
                    onEditTransaction = onEditTransaction,
                    onDeleteTransaction = onDeleteTransaction
                )
            }

            item {
                // Bottom padding for better scrolling experience
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Floating Action Button for adding transactions
        FloatingActionButton(
            onClick = onAddTransactionClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add transaction",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
    Log.d(TAG, "SuccessState rendered successfully")
}

@Composable
fun SummaryCard(summary: com.example.voicereminder.domain.models.FinanceSummary?) {
    val totalSpent = summary?.totalSpent ?: 0.0
    val totalReceived = summary?.totalReceived ?: 0.0
    val netBalance = totalReceived - totalSpent
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        val errorColor = MaterialTheme.colorScheme.error

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Net Balance at top - shows overall financial position
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Balance",
                    fontSize = 12.sp,
                    color = onPrimaryColor.copy(alpha = 0.7f)
                )
                Text(
                    text = "AFN ${String.format("%,.2f", netBalance)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (netBalance >= 0) tertiaryColor else errorColor
                )
            }
            
            // Money In and Money Out row - clear visual separation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Money Out (Withdrawals) - what you spent
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "↑", fontSize = 14.sp, color = errorColor)
                        Text(
                            text = "Money Out",
                            fontSize = 11.sp,
                            color = onPrimaryColor.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "AFN ${String.format("%,.2f", totalSpent)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = errorColor
                    )
                    Text(
                        text = "Withdrawals",
                        fontSize = 10.sp,
                        color = onPrimaryColor.copy(alpha = 0.5f)
                    )
                }
                
                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(50.dp)
                        .background(onPrimaryColor.copy(alpha = 0.25f))
                )
                
                // Money In (Deposits) - what you received
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "↓", fontSize = 14.sp, color = tertiaryColor)
                        Text(
                            text = "Money In",
                            fontSize = 11.sp,
                            color = onPrimaryColor.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "AFN ${String.format("%,.2f", totalReceived)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tertiaryColor
                    )
                    Text(
                        text = "Deposits",
                        fontSize = 10.sp,
                        color = onPrimaryColor.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun FinanceChart() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Your Weekly Activity",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 16f
                val graphWidth = width - 2 * padding
                val graphHeight = height - 2 * padding

                // Draw grid lines
                drawLine(
                    color = outlineColor,
                    start = androidx.compose.ui.geometry.Offset(padding, height / 2),
                    end = androidx.compose.ui.geometry.Offset(width - padding, height / 2),
                    strokeWidth = 1f
                )

                // Draw chart line - theme-aware
                val data = listOf(20f, 40f, 30f, 45f, 35f, 50f, 40f) // Sample data
                val maxValue = data.maxOrNull() ?: 1f
                val segmentWidth = graphWidth / (data.size - 1)

                for (i in data.indices) {
                    val x = padding + i * segmentWidth
                    val y = height - padding - (data[i] / maxValue) * graphHeight

                    if (i > 0) {
                        val prevX = padding + (i - 1) * segmentWidth
                        val prevY = height - padding - (data[i - 1] / maxValue) * graphHeight

                        drawLine(
                            color = primaryColor,
                            start = androidx.compose.ui.geometry.Offset(prevX, prevY),
                            end = androidx.compose.ui.geometry.Offset(x, y),
                            strokeWidth = 3f
                        )
                    }

                    // Draw circle for each point
                    drawCircle(
                        color = primaryColor,
                        radius = 4f,
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
            }
        }
    }
}

/**
 * Error state UI - displays error message with retry button
 * Handles all error scenarios with proper error handling and accessibility
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Log.d(TAG, "ErrorState composable entered with message: $message")
    
    // Validate message is not empty, provide fallback
    val displayMessage = message.ifBlank { 
        Log.w(TAG, "Empty error message provided, using fallback")
        "An unexpected error occurred. Please try again."
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { heading() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Unable to load financial data",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            
            Text(
                text = displayMessage,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Button(
                onClick = {
                    try {
                        Log.d(TAG, "Retry button clicked")
                        onRetry()
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during retry button click", e)
                        // Don't crash - the ViewModel will handle the error
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .widthIn(min = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Retry")
            }
        }
    }
    Log.d(TAG, "ErrorState rendered successfully")
}

/**
 * Empty state UI - displays placeholder message when no transactions exist
 */
@Composable
private fun EmptyState() {
    Log.d(TAG, "EmptyState composable entered")
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Wallet icon placeholder
            Text(
                text = "💰",
                fontSize = 48.sp
            )
            
            Text(
                text = "No Money Activity Yet",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            
            Text(
                text = "Your deposits and withdrawals will show up here once you start tracking your finances",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
    Log.d(TAG, "EmptyState rendered successfully")
}
