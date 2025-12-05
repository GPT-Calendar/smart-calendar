# Finance System Enhancements - Design

## Architecture Overview

The enhanced finance system extends the existing architecture with new data models, UI components, and business logic while maintaining backward compatibility.

```
┌─────────────────────────────────────────────────────────────────┐
│                     Presentation Layer                          │
├─────────────────────────────────────────────────────────────────┤
│  FinanceScreen (Enhanced)                                       │
│  ├── SummaryCard (existing)                                     │
│  ├── BudgetOverviewCard (NEW)                                   │
│  ├── QuickStatsRow (existing)                                   │
│  ├── SpendingChart (ENHANCED - real data)                       │
│  ├── CategoryBreakdownChart (NEW)                               │
│  ├── SavingsGoalsSection (NEW)                                  │
│  ├── AIInsightBubble (ENHANCED)                                 │
│  ├── TransactionFilterBar (NEW)                                 │
│  └── TransactionList (ENHANCED - editable)                      │
│                                                                 │
│  Dialogs:                                                       │
│  ├── AddTransactionDialog (NEW)                                 │
│  ├── EditTransactionDialog (NEW)                                │
│  ├── SetBudgetDialog (NEW)                                      │
│  ├── CreateSavingsGoalDialog (NEW)                              │
│  ├── ExportDialog (NEW)                                         │
│  └── CategoryManagementDialog (NEW)                             │
├─────────────────────────────────────────────────────────────────┤
│                     ViewModel Layer                             │
├─────────────────────────────────────────────────────────────────┤
│  FinanceViewModel (ENHANCED)                                    │
│  ├── uiState: StateFlow<FinanceUiState>                         │
│  ├── budgets: StateFlow<List<Budget>>                           │
│  ├── savingsGoals: StateFlow<List<SavingsGoal>>                 │
│  ├── filteredTransactions: StateFlow<List<Transaction>>         │
│  ├── filterState: StateFlow<TransactionFilter>                  │
│  ├── chartData: StateFlow<ChartData>                            │
│  └── customCategories: StateFlow<List<CustomCategory>>          │
├─────────────────────────────────────────────────────────────────┤
│                     Domain Layer                                │
├─────────────────────────────────────────────────────────────────┤
│  BudgetManager (NEW)                                            │
│  SavingsGoalManager (NEW)                                       │
│  TransactionManager (NEW)                                       │
│  ChartDataCalculator (NEW)                                      │
│  InsightGenerator (ENHANCED)                                    │
│  ExportService (NEW)                                            │
├─────────────────────────────────────────────────────────────────┤
│                     Data Layer                                  │
├─────────────────────────────────────────────────────────────────┤
│  Database Entities:                                             │
│  ├── FinanceTransaction (existing)                              │
│  ├── BudgetEntity (NEW)                                         │
│  ├── SavingsGoalEntity (NEW)                                    │
│  └── CustomCategoryEntity (NEW)                                 │
│                                                                 │
│  DAOs:                                                          │
│  ├── FinanceTransactionDao (ENHANCED)                           │
│  ├── BudgetDao (NEW)                                            │
│  ├── SavingsGoalDao (NEW)                                       │
│  └── CustomCategoryDao (NEW)                                    │
└─────────────────────────────────────────────────────────────────┘
```

## Data Models

### Budget Entity
```kotlin
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,           // Category name
    val monthlyLimit: Double,       // Budget limit in AFN
    val month: String,              // "2025-11" format
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
```

### Savings Goal Entity
```kotlin
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,               // Goal name
    val targetAmount: Double,       // Target in AFN
    val currentAmount: Double = 0.0,// Current saved amount
    val deadline: Long?,            // Optional deadline timestamp
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

### Custom Category Entity
```kotlin
@Entity(tableName = "custom_categories")
data class CustomCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,           // Material icon name
    val color: String,              // Hex color code
    val isActive: Boolean = true
)
```

### Enhanced Transaction (Domain Model)
```kotlin
data class Transaction(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val timestamp: Long,
    val isManual: Boolean = false,  // NEW: distinguish manual vs SMS
    val isEdited: Boolean = false,  // NEW: track if edited
    val notes: String? = null       // NEW: optional notes
)
```

### Transaction Filter
```kotlin
data class TransactionFilter(
    val dateRange: DateRange = DateRange.THIS_MONTH,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val categories: Set<TransactionCategory> = emptySet(), // empty = all
    val types: Set<TransactionType> = emptySet(),          // empty = all
    val searchQuery: String = ""
)

enum class DateRange {
    TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, CUSTOM, ALL
}
```

### Chart Data
```kotlin
data class ChartData(
    val weeklyData: List<DailySpending>,
    val categoryBreakdown: List<CategorySpending>,
    val monthlyTrend: List<MonthlySpending>
)

data class DailySpending(
    val date: Long,
    val dayLabel: String,  // "Mon", "Tue", etc.
    val amount: Double
)

data class CategorySpending(
    val category: TransactionCategory,
    val amount: Double,
    val percentage: Float
)
```

## UI Components Design

### 1. Budget Overview Card
- Horizontal scrollable list of budget cards
- Each card shows: category icon, category name, spent/budget, progress bar
- Progress bar color: green (<60%), yellow (60-80%), red (>80%)
- Tap to edit budget

### 2. Transaction Filter Bar
- Horizontal chip row for quick filters
- Date range dropdown
- Category multi-select
- Search icon that expands to search field

### 3. Add Transaction FAB
- Floating action button at bottom right
- Opens bottom sheet with transaction form
- Form fields: Amount (numeric keyboard), Type toggle, Category dropdown, Description, Date picker

### 4. Enhanced Transaction Item
- Swipe left to delete (with confirmation)
- Swipe right to edit
- Long press for context menu
- Manual transactions show small "manual" badge

### 5. Spending Chart (Enhanced)
- Line chart with real weekly data
- Tap on data points to see daily breakdown
- Toggle between Week/Month/Year views

### 6. Category Breakdown Chart
- Donut chart showing spending by category
- Legend with percentages
- Tap on segment to filter transactions

### 7. Savings Goals Section
- Collapsible section below quick stats
- Each goal shows: name, progress bar, current/target amount
- "Add Goal" button

## Screen Flow

```
Finance Tab
├── [Tab: Financial Data]
│   ├── Summary Card
│   ├── Budget Overview (horizontal scroll)
│   ├── Quick Stats Row
│   ├── Savings Goals (collapsible)
│   ├── Spending Chart
│   ├── Category Breakdown
│   ├── AI Insight
│   ├── Filter Bar
│   └── Transaction List
│       └── [Swipe/Tap] → Edit/Delete
│
├── [Tab: Bank Patterns] (existing)
│
├── [Tab: Budgets] (NEW)
│   ├── Budget List by Category
│   └── [+] Add Budget
│
├── [Tab: Goals] (NEW)
│   ├── Active Goals
│   ├── Completed Goals
│   └── [+] Add Goal
│
└── [FAB: +] → Add Transaction Dialog
```

## Correctness Properties

### P1: Budget Calculations
- Budget spent amount = sum of transactions in category for current month
- Budget remaining = monthlyLimit - spent
- Budget percentage = (spent / monthlyLimit) * 100

### P2: Transaction Filtering
- Filtered list contains only transactions matching ALL active filters
- Empty filter set returns all transactions
- Search is case-insensitive and matches title/description

### P3: Chart Data Accuracy
- Weekly chart data = actual transactions grouped by day
- Category breakdown percentages sum to 100%
- All amounts are positive (absolute values for display)

### P4: Savings Goal Progress
- Goal progress = (currentAmount / targetAmount) * 100
- Goal is complete when currentAmount >= targetAmount
- Contributions cannot exceed remaining amount needed

### P5: Data Persistence
- All user-created data (budgets, goals, manual transactions) persists after app restart
- Deleted items are permanently removed from database
- Edited transactions maintain original creation timestamp

### P6: Export Accuracy
- Exported CSV contains all transactions in selected date range
- CSV format is compatible with Excel/Google Sheets
- All monetary values are formatted consistently
