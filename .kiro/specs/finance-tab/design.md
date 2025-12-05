# Finance Tab Design Document

## Overview

The Finance Tab is a new navigation destination in the Voice Reminder Assistant app that provides users with a premium financial overview interface. The feature will be implemented using Jetpack Compose following the existing app architecture patterns, with a focus on creating a clean, modern UI that combines design principles from Apple Wallet, Google Material You, and business financial dashboards.

This initial implementation uses hardcoded template data to establish the UI foundation. The design is structured to support future integration with SMS-based transaction parsing and real financial data sources.

## Architecture

### Technology Stack
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Navigation**: Jetpack Navigation Compose
- **State Management**: Compose State and StateFlow
- **Data Layer**: Room Database (for future persistence)
- **Language**: Kotlin

### Component Structure

```
presentation/
├── finance/
│   ├── FinanceScreen.kt          # Main composable screen
│   ├── FinanceViewModel.kt       # ViewModel for state management
│   ├── components/
│   │   ├── SummaryCard.kt        # Monthly overview card
│   │   ├── QuickStatsRow.kt      # Three mini stat cards
│   │   ├── TransactionList.kt    # Scrollable transaction list
│   │   ├── TransactionItem.kt    # Individual transaction row
│   │   └── AIInsightBubble.kt    # Smart insight card
│   └── FinanceViewModelFactory.kt
│
domain/
├── models/
│   ├── FinanceSummary.kt         # Monthly summary data model
│   ├── QuickStat.kt              # Quick stat data model
│   ├── Transaction.kt            # Transaction data model
│   └── AIInsight.kt              # AI insight data model
│
data/
├── FinanceRepository.kt          # Data access layer (template data)
└── FinanceDao.kt                 # Room DAO (for future use)
```

## Components and Interfaces

### 1. FinanceScreen (Main Composable)

**Purpose**: Root composable that orchestrates the Finance Tab UI

**Composition**:
```kotlin
@Composable
fun FinanceScreen(
    viewModel: FinanceViewModel = viewModel()
)
```

**Layout Structure**:
- Column with fillMaxSize modifier
- Vertical scrolling with LazyColumn or Column with verticalScroll
- Padding: 16dp horizontal, 12dp vertical spacing between sections
- Background: MaterialTheme.colorScheme.background

**Child Components** (in order):
1. SummaryCard
2. Spacer (16dp)
3. QuickStatsRow
4. Spacer (20dp)
5. AIInsightBubble
6. Spacer (16dp)
7. TransactionList

### 2. SummaryCard Component

**Purpose**: Display monthly financial overview with trend indicator

**Design Specifications**:
- Height: 20-25% of screen height (use BoxWithConstraints for responsive sizing)
- Rounded corners: 28.dp
- Background: Gradient from Color(0xFF00C853) to Color(0xFF00E676) with 45-degree angle
- Elevation: 6.dp
- Padding: 20.dp

**Content Layout**:
```
Column (verticalArrangement = spacedBy(12.dp)) {
  Text("This Month Overview") - 18.sp, semibold, Color(0xFF212121)
  
  Row (horizontalArrangement = SpaceBetween) {
    Column {
      Text("Total Spent") - 14.sp, Color(0xFF424242)
      Text("AFN 12,450") - 24.sp, bold, Color(0xFFD32F2F) with 0.7 alpha
    }
    Column {
      Text("Total Received") - 14.sp, Color(0xFF424242)
      Text("AFN 4,200") - 24.sp, bold, Color(0xFF00C853)
    }
  }
  
  Row (verticalAlignment = CenterVertically) {
    Icon(ArrowUpward, tint = Color(0xFF00C853), size = 16.dp)
    Text("Trend: +12% from last month") - 13.sp, Color(0xFF424242)
  }
}
```

**Data Model**:
```kotlin
data class FinanceSummary(
    val totalSpent: Double,
    val totalReceived: Double,
    val trendPercentage: Double,
    val trendDirection: TrendDirection
)

enum class TrendDirection { UP, DOWN, NEUTRAL }
```

### 3. QuickStatsRow Component

**Purpose**: Display three key financial metrics horizontally

**Design Specifications**:
- Row with horizontalArrangement = spacedBy(12.dp)
- Each card takes equal weight (1f)
- Card specs:
  - Rounded corners: 20.dp
  - Background: Color.White
  - Elevation: 2.dp
  - Padding: 14.dp

**Individual Stat Card Layout**:
```
Column (verticalArrangement = spacedBy(8.dp)) {
  Icon (size = 24.dp, tint = Color(0xFF00C853))
  Text(value) - 18.sp, bold, Color(0xFF212121)
  Text(label) - 12.sp, Color(0xFF6F6F6F)
}
```

**Data Model**:
```kotlin
data class QuickStat(
    val icon: ImageVector,
    val value: String,
    val label: String
)
```

**Template Data**:
1. Calendar icon, "AFN 3,250", "This Week's Spend"
2. Shopping bag icon, "AFN 850", "Biggest Expense"
3. Restaurant icon, "Food", "Most Spent Category"

### 4. TransactionList Component

**Purpose**: Scrollable list of financial transactions

**Design Specifications**:
- LazyColumn with verticalArrangement = spacedBy(12.dp)
- Content padding: 0.dp (parent handles padding)
- Each item uses TransactionItem composable

**Data Model**:
```kotlin
data class Transaction(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val timestamp: Long
)

enum class TransactionType { SPENT, RECEIVED }

enum class TransactionCategory {
    BANK, FOOD, SALARY, MOBILE, GROCERIES, OTHER
}
```

### 5. TransactionItem Component

**Purpose**: Individual transaction row display

**Design Specifications**:
- Card with:
  - Height: 68.dp
  - Rounded corners: 18.dp
  - Background: Color.White
  - Elevation: 1.dp
  - Padding: 12.dp

**Layout**:
```
Row (
  modifier = fillMaxWidth,
  horizontalArrangement = SpaceBetween,
  verticalAlignment = CenterVertically
) {
  // Left: Icon
  Box (
    size = 40.dp,
    shape = CircleShape,
    background = Color(0xFFE8FDF3)
  ) {
    Icon (category icon, size = 20.dp, tint = Color(0xFF00C853))
  }
  
  // Middle: Text
  Column (modifier = weight(1f), padding = horizontal 12.dp) {
    Text(title) - 16.sp, semibold, Color(0xFF212121)
    Text(subtitle) - 12.sp, Color(0xFF6F6F6F)
  }
  
  // Right: Amount
  Text(
    amount with sign,
    16.sp, bold,
    color = if (type == SPENT) Color(0xFFD32F2F) else Color(0xFF00C853)
  )
}
```

**Category Icon Mapping**:
- BANK → AccountBalance icon
- FOOD → Restaurant icon
- SALARY → AccountBalanceWallet icon
- MOBILE → PhoneAndroid icon
- GROCERIES → ShoppingCart icon
- OTHER → MoreHoriz icon

### 6. AIInsightBubble Component

**Purpose**: Display AI-generated financial insights

**Design Specifications**:
- Card with:
  - Rounded corners: 22.dp
  - Background: Color(0xFFE8FDF3)
  - Elevation: 2.dp
  - Padding: 16.dp

**Layout**:
```
Row (
  horizontalArrangement = spacedBy(12.dp),
  verticalAlignment = CenterVertically
) {
  Icon (
    Lightbulb or AutoAwesome,
    size = 28.dp,
    tint = Color(0xFF00C853)
  )
  
  Text (
    insight text,
    14.sp,
    Color(0xFF212121),
    lineHeight = 20.sp
  )
}
```

**Data Model**:
```kotlin
data class AIInsight(
    val message: String,
    val type: InsightType
)

enum class InsightType {
    SPENDING_INCREASE,
    SPENDING_DECREASE,
    CATEGORY_ALERT,
    SAVINGS_TIP
}
```

## Data Models

### Complete Data Model Definitions

```kotlin
// domain/models/FinanceSummary.kt
data class FinanceSummary(
    val totalSpent: Double,
    val totalReceived: Double,
    val trendPercentage: Double,
    val trendDirection: TrendDirection,
    val month: String
)

enum class TrendDirection {
    UP, DOWN, NEUTRAL
}

// domain/models/QuickStat.kt
data class QuickStat(
    val icon: ImageVector,
    val value: String,
    val label: String,
    val type: StatType
)

enum class StatType {
    WEEKLY_SPEND,
    BIGGEST_EXPENSE,
    TOP_CATEGORY
}

// domain/models/Transaction.kt
data class Transaction(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val timestamp: Long
)

enum class TransactionType {
    SPENT, RECEIVED
}

enum class TransactionCategory {
    BANK, FOOD, SALARY, MOBILE, GROCERIES, OTHER;
    
    fun getIcon(): ImageVector {
        return when (this) {
            BANK -> Icons.Default.AccountBalance
            FOOD -> Icons.Default.Restaurant
            SALARY -> Icons.Default.AccountBalanceWallet
            MOBILE -> Icons.Default.PhoneAndroid
            GROCERIES -> Icons.Default.ShoppingCart
            OTHER -> Icons.Default.MoreHoriz
        }
    }
}

// domain/models/AIInsight.kt
data class AIInsight(
    val message: String,
    val type: InsightType,
    val timestamp: Long
)

enum class InsightType {
    SPENDING_INCREASE,
    SPENDING_DECREASE,
    CATEGORY_ALERT,
    SAVINGS_TIP
}

// domain/models/FinanceData.kt
data class FinanceData(
    val summary: FinanceSummary,
    val quickStats: List<QuickStat>,
    val transactions: List<Transaction>,
    val insight: AIInsight
)
```

## Error Handling

### Error Scenarios

1. **Data Loading Failure**
   - Display error state with retry button
   - Show user-friendly message: "Unable to load financial data"
   - Log error for debugging

2. **Empty State**
   - Display empty state illustration
   - Message: "No transactions yet"
   - Subtitle: "Your financial activity will appear here"

3. **Invalid Data**
   - Gracefully handle null or malformed data
   - Use default values where appropriate
   - Log warnings for debugging

### Error Handling Implementation

```kotlin
sealed class FinanceUiState {
    object Loading : FinanceUiState()
    data class Success(val data: FinanceData) : FinanceUiState()
    data class Error(val message: String) : FinanceUiState()
    object Empty : FinanceUiState()
}
```

## Testing Strategy

### Unit Tests

1. **ViewModel Tests**
   - Test state initialization with template data
   - Test state transitions (Loading → Success → Error)
   - Test data transformation logic
   - Verify correct formatting of currency values

2. **Data Model Tests**
   - Test data class equality and copying
   - Test enum mappings (category to icon)
   - Test currency formatting utilities

3. **Repository Tests**
   - Test template data retrieval
   - Test data validation logic
   - Mock future database operations

### UI Tests (Compose)

1. **Component Tests**
   - Test SummaryCard displays correct values
   - Test QuickStatsRow renders three cards
   - Test TransactionItem shows correct colors for spent/received
   - Test AIInsightBubble displays message

2. **Integration Tests**
   - Test FinanceScreen composition
   - Test scrolling behavior
   - Test responsive layout on different screen sizes

3. **Screenshot Tests** (Optional)
   - Capture reference screenshots for visual regression testing
   - Test light and dark themes

### Test Data

```kotlin
object FinanceTestData {
    val mockSummary = FinanceSummary(
        totalSpent = 12450.0,
        totalReceived = 4200.0,
        trendPercentage = 12.0,
        trendDirection = TrendDirection.UP,
        month = "November 2025"
    )
    
    val mockQuickStats = listOf(
        QuickStat(Icons.Default.CalendarToday, "AFN 3,250", "This Week's Spend", StatType.WEEKLY_SPEND),
        QuickStat(Icons.Default.ShoppingBag, "AFN 850", "Biggest Expense", StatType.BIGGEST_EXPENSE),
        QuickStat(Icons.Default.Restaurant, "Food", "Most Spent Category", StatType.TOP_CATEGORY)
    )
    
    val mockTransactions = listOf(
        Transaction("1", "Bakhtar Bank – ATM Withdrawal", "Yesterday • 3:12 PM", -1200.0, TransactionType.SPENT, TransactionCategory.BANK, System.currentTimeMillis()),
        Transaction("2", "Food Corner – Lunch", "Today • 12:45 PM", -350.0, TransactionType.SPENT, TransactionCategory.FOOD, System.currentTimeMillis()),
        Transaction("3", "Salary – Deposit", "3 days ago • 9:00 AM", 15000.0, TransactionType.RECEIVED, TransactionCategory.SALARY, System.currentTimeMillis()),
        Transaction("4", "Top Up – Mobile Recharge", "Today • 8:30 AM", -200.0, TransactionType.SPENT, TransactionCategory.MOBILE, System.currentTimeMillis()),
        Transaction("5", "Market – Groceries", "2 days ago • 5:15 PM", -720.0, TransactionType.SPENT, TransactionCategory.GROCERIES, System.currentTimeMillis())
    )
    
    val mockInsight = AIInsight(
        "You spent 18% more on food this week compared to last week.",
        InsightType.SPENDING_INCREASE,
        System.currentTimeMillis()
    )
    
    val mockFinanceData = FinanceData(
        summary = mockSummary,
        quickStats = mockQuickStats,
        transactions = mockTransactions,
        insight = mockInsight
    )
}
```

## Design System Integration

### Color Palette

The Finance Tab introduces a new green accent color while maintaining consistency with the existing blue-based theme:

```kotlin
// Add to Color.kt
val FinanceGreen = Color(0xFF00C853)           // Primary green accent
val FinanceGreenLight = Color(0xFFE8FDF3)      // Light green background
val FinanceGreenDark = Color(0xFF00A344)       // Dark green for emphasis
val FinanceRed = Color(0xFFD32F2F)             // Red for spent amounts
val FinanceRedLight = Color(0xFFFFEBEE)        // Light red background
```

### Typography

Follow existing typography scale from Material 3:
- Display: 22.sp, bold (for large numbers)
- Title: 18.sp, semibold (for section headers)
- Body: 16.sp, regular (for transaction titles)
- Label: 14.sp, regular (for descriptions)
- Caption: 12.sp, regular (for timestamps)

### Spacing

Consistent spacing system:
- Extra small: 4.dp
- Small: 8.dp
- Medium: 12.dp
- Large: 16.dp
- Extra large: 20.dp
- XXL: 24.dp

### Elevation

Shadow depths for cards:
- Level 1: 1.dp (transaction items)
- Level 2: 2.dp (quick stat cards, AI insight)
- Level 3: 6.dp (summary card)

## Navigation Integration

### Adding Finance Tab to Bottom Navigation

**Modification to MainActivity.kt**:

1. Add new navigation item to the bottom bar items list:
```kotlin
BottomNavigationItem(
    route = "finance",
    icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
    title = "Finance"
)
```

2. Add composable route to NavHost:
```kotlin
composable("finance") { FinanceScreen() }
```

3. Update string resources (strings.xml):
```xml
<string name="nav_finance">Finance</string>
```

### Navigation Order

Recommended bottom navigation order:
1. Assistant (existing)
2. Calendar (existing)
3. Finance (new)
4. All Events (existing)
5. Map (existing)

Note: Consider removing one tab if 5 tabs is too many for mobile UX. Recommendation: merge "All Events" into "Calendar" to make room for Finance.

## Responsive Design

### Screen Size Adaptations

**Small Screens (< 360dp width)**:
- Reduce padding to 12.dp
- Font sizes: -1sp from base
- Summary card height: 20% of screen

**Medium Screens (360-600dp width)**:
- Standard padding: 16.dp
- Base font sizes
- Summary card height: 22% of screen

**Large Screens (> 600dp width)**:
- Increased padding: 24.dp
- Font sizes: +1sp from base
- Summary card height: 25% of screen
- Consider two-column layout for quick stats

### Orientation Handling

**Portrait Mode**: Standard vertical layout as designed

**Landscape Mode**:
- Reduce summary card height to 30% of screen height
- Consider horizontal scrolling for quick stats
- Maintain vertical scrolling for transactions

## Accessibility

### Compliance Requirements

1. **Color Contrast**:
   - All text meets WCAG AA standards (4.5:1 minimum)
   - Green (#00C853) on white: 3.4:1 (use for decorative only)
   - Dark gray (#212121) on white: 15.8:1 (use for primary text)

2. **Touch Targets**:
   - Minimum 48.dp for interactive elements
   - Transaction items: 68.dp height (exceeds minimum)

3. **Content Descriptions**:
   - All icons have meaningful contentDescription
   - Decorative icons use contentDescription = null

4. **Screen Reader Support**:
   - Semantic ordering of content
   - Proper heading hierarchy
   - Announce currency values with "Afghani" suffix

### Implementation

```kotlin
// Example: Accessible transaction item
Text(
    text = "AFN ${amount.formatCurrency()}",
    modifier = Modifier.semantics {
        contentDescription = "${amount.formatCurrency()} Afghani ${if (type == SPENT) "spent" else "received"}"
    }
)
```

## Performance Considerations

### Optimization Strategies

1. **Lazy Loading**:
   - Use LazyColumn for transaction list
   - Implement pagination for future large datasets

2. **State Management**:
   - Use remember and derivedStateOf for computed values
   - Avoid unnecessary recompositions

3. **Image Loading**:
   - Use vector icons (no bitmap loading)
   - Cache category icons

4. **Memory Management**:
   - Limit transaction list to recent 50 items
   - Implement data cleanup for old transactions

## Future Enhancements

### Phase 2: SMS Integration

1. Add SMS permission handling
2. Implement SMS parsing service
3. Create transaction extraction logic
4. Add database persistence with Room

### Phase 3: Advanced Features

1. Date range filtering
2. Category-based filtering
3. Export to CSV/PDF
4. Budget tracking
5. Spending predictions
6. Multi-currency support

### Phase 4: AI Enhancements

1. Real-time spending alerts
2. Personalized savings recommendations
3. Anomaly detection
4. Spending pattern analysis

## Design Decisions and Rationales

### 1. Green Color Choice
**Decision**: Use #00C853 as primary accent color for Finance Tab
**Rationale**: Green universally represents money and financial growth, distinct from the app's blue theme, creating clear visual separation between features.

### 2. Template Data Approach
**Decision**: Hardcode template data initially
**Rationale**: Allows UI development and testing without SMS integration complexity, enables faster iteration on design, and provides clear examples for future data structure.

### 3. Separate Navigation Tab
**Decision**: Add Finance as a dedicated bottom navigation item
**Rationale**: Finance is a distinct feature domain that deserves prominent access, aligns with user mental models of separate "money" sections in apps.

### 4. Card-Based Layout
**Decision**: Use elevated cards for all major components
**Rationale**: Cards create visual hierarchy, improve scannability, and align with Material Design principles and modern financial app patterns.

### 5. MVVM Architecture
**Decision**: Follow existing app's MVVM pattern
**Rationale**: Maintains architectural consistency, enables testability, and supports future data source changes without UI modifications.
