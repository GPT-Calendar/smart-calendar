# Finance System Enhancements - Implementation Tasks

## Phase 1: Data Layer Foundation

### Task 1.1: Create Budget Database Entities and DAO
- [x] Create `BudgetEntity.kt` in `data/entity/`
- [x] Create `BudgetDao.kt` in `data/dao/`
- [x] Add Budget table to `FinanceDatabase.kt`
- [x] Create database migration for new table
- [x] Create `Budget.kt` domain model in `domain/models/`

### Task 1.2: Create Savings Goal Database Entities and DAO
- [x] Create `SavingsGoalEntity.kt` in `data/entity/`
- [x] Create `SavingsGoalDao.kt` in `data/dao/`
- [x] Add SavingsGoal table to `FinanceDatabase.kt`
- [x] Create `SavingsGoal.kt` domain model in `domain/models/`

### Task 1.3: Create Custom Category Database Entities and DAO
- [ ] Create `CustomCategoryEntity.kt` in `data/entity/`
- [ ] Create `CustomCategoryDao.kt` in `data/dao/`
- [ ] Add CustomCategory table to `FinanceDatabase.kt`
- [ ] Create `CustomCategory.kt` domain model in `domain/models/`

### Task 1.4: Enhance Transaction Entity
- [x] Add `isManual`, `isEdited`, `notes` fields to `FinanceTransaction` entity
- [x] Create database migration for new fields
- [x] Update `Transaction.kt` domain model
- [x] Update `FinanceRepository` mapping functions

## Phase 2: Domain Layer - Business Logic

### Task 2.1: Create BudgetManager
- [ ] Create `BudgetManager.kt` in `domain/`
- [ ] Implement `createBudget(category, limit, month)`
- [ ] Implement `updateBudget(budget)`
- [ ] Implement `deleteBudget(budgetId)`
- [ ] Implement `getBudgetsForMonth(month)`
- [ ] Implement `calculateBudgetProgress(budget, transactions)`

### Task 2.2: Create SavingsGoalManager
- [ ] Create `SavingsGoalManager.kt` in `domain/`
- [ ] Implement `createGoal(name, targetAmount, deadline)`
- [ ] Implement `updateGoal(goal)`
- [ ] Implement `deleteGoal(goalId)`
- [ ] Implement `contributeToGoal(goalId, amount)`
- [ ] Implement `getActiveGoals()`
- [ ] Implement `getCompletedGoals()`

### Task 2.3: Create TransactionManager
- [ ] Create `TransactionManager.kt` in `domain/`
- [ ] Implement `addManualTransaction(transaction)`
- [ ] Implement `editTransaction(transactionId, updates)`
- [ ] Implement `deleteTransaction(transactionId)`
- [ ] Implement `filterTransactions(filter)`
- [ ] Implement `searchTransactions(query)`

### Task 2.4: Create ChartDataCalculator
- [ ] Create `ChartDataCalculator.kt` in `domain/`
- [ ] Implement `calculateWeeklySpending(transactions)`
- [ ] Implement `calculateCategoryBreakdown(transactions)`
- [ ] Implement `calculateMonthlyTrend(transactions)`

### Task 2.5: Enhance InsightGenerator
- [ ] Update existing insight generation in `FinanceRepository`
- [ ] Add category-specific insights
- [ ] Add budget-based insights
- [ ] Add unusual spending pattern detection

### Task 2.6: Create ExportService
- [ ] Create `ExportService.kt` in `domain/`
- [ ] Implement `exportToCSV(transactions, dateRange)`
- [ ] Implement file saving to Downloads folder
- [ ] Handle permissions for file access

## Phase 3: Presentation Layer - UI Components

### Task 3.1: Create Transaction Filter Components
- [x] Create `TransactionFilter.kt` data class in `domain/models/`
- [x] Create `TransactionFilterBar.kt` composable
- [x] Implement date range chip selector
- [x] Implement category filter chips
- [x] Implement type filter toggle
- [x] Implement search field expansion

### Task 3.2: Create Add Transaction Dialog
- [x] Create `AddTransactionDialog.kt` in `presentation/finance/components/`
- [x] Implement amount input with numeric keyboard
- [x] Implement type toggle (Income/Expense)
- [x] Implement category dropdown
- [x] Implement description text field
- [x] Implement date picker
- [x] Add validation and error states

### Task 3.3: Create Edit Transaction Dialog
- [x] Create `EditTransactionDialog.kt` in `presentation/finance/components/`
- [x] Pre-populate form with existing transaction data
- [x] Implement save and cancel actions
- [x] Show "manual" vs "auto-parsed" indicator

### Task 3.4: Enhance Transaction Item with Swipe Actions
- [x] Add swipe-to-delete gesture to `TransactionItem.kt`
- [x] Add swipe-to-edit gesture
- [x] Add delete confirmation dialog
- [x] Add "manual" badge for manual transactions

### Task 3.5: Create Budget Overview Card
- [x] Create `BudgetOverviewCard.kt` in `presentation/finance/components/`
- [x] Implement horizontal scrollable budget list
- [x] Implement budget progress bar with color coding
- [x] Implement tap-to-edit functionality

### Task 3.6: Create Set Budget Dialog
- [x] Create `SetBudgetDialog.kt` in `presentation/finance/components/`
- [x] Implement category selector
- [x] Implement amount input
- [x] Implement month selector
- [x] Add validation

### Task 3.7: Create Savings Goals Section
- [ ] Create `SavingsGoalsSection.kt` in `presentation/finance/components/`
- [ ] Implement collapsible section
- [ ] Implement goal progress cards
- [ ] Implement "Add Goal" button

### Task 3.8: Create Savings Goal Dialog
- [ ] Create `CreateSavingsGoalDialog.kt` in `presentation/finance/components/`
- [ ] Implement name input
- [ ] Implement target amount input
- [ ] Implement optional deadline picker

### Task 3.9: Enhance Spending Chart with Real Data
- [ ] Update `FinanceChart` in `FinanceScreen.kt`
- [ ] Connect to `ChartDataCalculator`
- [ ] Implement time period toggle (Week/Month/Year)
- [ ] Add touch interaction for data points

### Task 3.10: Create Category Breakdown Chart
- [ ] Create `CategoryBreakdownChart.kt` in `presentation/finance/components/`
- [ ] Implement donut/pie chart
- [ ] Implement legend with percentages
- [ ] Implement tap-to-filter interaction

### Task 3.11: Create Export Dialog
- [ ] Create `ExportDialog.kt` in `presentation/finance/components/`
- [ ] Implement date range selector
- [ ] Implement export button with progress indicator
- [ ] Show success/error feedback

### Task 3.12: Create Category Management Dialog
- [ ] Create `CategoryManagementDialog.kt` in `presentation/finance/components/`
- [ ] Implement category list with edit/delete
- [ ] Implement add custom category form
- [ ] Implement icon picker
- [ ] Implement color picker

## Phase 4: ViewModel Integration

### Task 4.1: Enhance FinanceViewModel
- [x] Add `budgets: StateFlow<List<Budget>>`
- [ ] Add `savingsGoals: StateFlow<List<SavingsGoal>>`
- [x] Add `filteredTransactions: StateFlow<List<Transaction>>`
- [x] Add `filterState: StateFlow<TransactionFilter>`
- [ ] Add `chartData: StateFlow<ChartData>`
- [ ] Add `customCategories: StateFlow<List<CustomCategory>>`

### Task 4.2: Implement Budget Operations in ViewModel
- [x] Add `createBudget()` function (placeholder)
- [x] Add `updateBudget()` function (placeholder)
- [x] Add `deleteBudget()` function (placeholder)
- [x] Add `loadBudgets()` function (placeholder)

### Task 4.3: Implement Savings Goal Operations in ViewModel
- [ ] Add `createSavingsGoal()` function
- [ ] Add `updateSavingsGoal()` function
- [ ] Add `deleteSavingsGoal()` function
- [ ] Add `contributeToGoal()` function
- [ ] Add `loadSavingsGoals()` function

### Task 4.4: Implement Transaction Operations in ViewModel
- [x] Add `addManualTransaction()` function
- [x] Add `editTransaction()` function (placeholder)
- [x] Add `deleteTransaction()` function (placeholder)
- [x] Add `updateFilter()` function
- [x] Add `searchTransactions()` function (via filter)

### Task 4.5: Implement Chart Data Loading
- [ ] Add `loadChartData()` function
- [ ] Add `updateChartPeriod()` function

### Task 4.6: Implement Export Functionality
- [ ] Add `exportTransactions()` function
- [ ] Handle export state (loading, success, error)

## Phase 5: Screen Integration

### Task 5.1: Update FinanceScreen Layout
- [x] Add Budget Overview section after Summary Card
- [ ] Add Savings Goals section (collapsible)
- [x] Add Filter Bar before Transaction List
- [x] Add FAB for adding transactions
- [ ] Update tab structure (add Budgets, Goals tabs)

### Task 5.2: Add New Tabs to Finance Screen
- [ ] Create Budgets tab content
- [ ] Create Goals tab content
- [ ] Update TabRow with new tabs

### Task 5.3: Wire Up All Dialogs
- [x] Connect Add Transaction dialog to FAB
- [x] Connect Edit Transaction dialog to swipe action
- [x] Connect Set Budget dialog to budget cards
- [ ] Connect Savings Goal dialog to goals section
- [ ] Connect Export dialog to menu action

## Phase 6: Testing & Polish

### Task 6.1: Unit Tests
- [ ] Test BudgetManager calculations
- [ ] Test SavingsGoalManager operations
- [ ] Test TransactionManager filtering
- [ ] Test ChartDataCalculator accuracy
- [ ] Test ExportService CSV generation

### Task 6.2: UI Testing
- [ ] Test filter interactions
- [ ] Test swipe gestures
- [ ] Test dialog flows
- [ ] Test chart interactions

### Task 6.3: Performance Optimization
- [ ] Optimize transaction list with large datasets
- [ ] Implement pagination if needed
- [ ] Cache chart calculations

### Task 6.4: Accessibility
- [ ] Add content descriptions to all interactive elements
- [ ] Ensure proper focus order
- [ ] Test with TalkBack

---

## Implementation Priority

**High Priority (Core Features):**
1. Task 1.4 - Enhance Transaction Entity
2. Task 2.3 - TransactionManager
3. Task 3.1 - Transaction Filter
4. Task 3.2 - Add Transaction Dialog
5. Task 3.4 - Swipe Actions

**Medium Priority (Budget & Goals):**
1. Task 1.1 - Budget Entities
2. Task 2.1 - BudgetManager
3. Task 3.5 - Budget Overview
4. Task 1.2 - Savings Goal Entities
5. Task 2.2 - SavingsGoalManager

**Lower Priority (Analytics & Export):**
1. Task 2.4 - ChartDataCalculator
2. Task 3.9 - Enhanced Charts
3. Task 3.10 - Category Breakdown
4. Task 2.6 - ExportService
5. Task 3.11 - Export Dialog
