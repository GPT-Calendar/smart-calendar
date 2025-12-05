# Design Document

## Overview

This design document outlines the implementation of a minimalist, modern UI redesign for all five screens of the Mobile AI Assistant application: Assistant (Chat), Calendar, Finance, All Events (Timeline), and Map. The design follows a strict 60-30-10 color strategy where white dominates the content area (60%), deep blue provides branding and navigation (30%), and light blue serves as accent for interactive elements (10%).

The redesign will update existing screen implementations to follow consistent design patterns, typography, spacing, and color usage across the entire application. All screens will share common global elements (header and bottom navigation) while maintaining screen-specific functionality.

## Architecture

### Global Component Structure

```
App Root
├── MinimalHeader (30% Deep Blue)
│   ├── Screen Title
│   └── Action Icons
├── Screen Content (60% White)
│   ├── AssistantScreen
│   ├── CalendarScreen
│   ├── FinanceScreen
│   ├── AllEventsScreen
│   └── MapScreen
└── MinimalBottomNavigation (30% Deep Blue)
    ├── Assistant Tab
    ├── Calendar Tab
    ├── Finance Tab
    ├── All Events Tab
    └── Map Tab
```

### State Management

Each screen maintains its existing ViewModel architecture:
- **ChatViewModel**: Manages chat messages and AI interactions
- **CalendarViewModel**: Manages calendar events and date selection
- **FinanceViewModel**: Manages financial data and transactions
- **MapViewModel**: Manages location data and map state

### Navigation State

```kotlin
enum class NavigationTab {
    ASSISTANT,
    CALENDAR,
    FINANCE,
    ALL_EVENTS,
    MAP
}

data class NavigationState(
    val currentTab: NavigationTab = NavigationTab.ASSISTANT
)
```

## Components and Interfaces

### 1. MinimalHeader (Global)

**Purpose**: Provide consistent branding and navigation across all screens

**Interface**:
```kotlin
@Composable
fun MinimalHeader(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
)
```

**Styling**:
- Background: MinimalDeepBlue (#305CDE)
- Height: 64.dp
- Elevation: 4.dp shadow
- Title: Roboto Medium, 18sp, white color
- Icons: 24dp, white color

### 2. MinimalBottomNavigation (Global)

**Purpose**: Provide navigation between all five screens

**Interface**:
```kotlin
@Composable
fun MinimalBottomNavigation(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
)
```

**Styling**:
- Background: MinimalDeepBlue (#305CDE)
- Height: 64.dp
- Elevation: 8.dp upward shadow
- Inactive items: White with 0.7 opacity, Roboto Regular 11sp
- Active item: MinimalAccentBlue (#5D83FF), Roboto Medium 11sp, 3dp indicator line
- Touch targets: 56dp × 56dp

### 3. AssistantScreen Components

**MinimalChatBubble (AI)**:
```kotlin
@Composable
fun MinimalAIBubble(
    message: String,
    chartData: ChartData? = null,
    summaryData: SummaryData? = null,
    modifier: Modifier = Modifier
)
```

**Styling**:
- Background: Very light grey (#F0F0F0)
- Corner radius: 16.dp (4.dp top-left pointer)
- Max width: 75% screen width
- Text: Roboto Regular 15sp, dark grey (#333333)
- Padding: 12dp horizontal, 10dp vertical

**MinimalChatBubble (User)**:
```kotlin
@Composable
fun MinimalUserBubble(
    message: String,
    modifier: Modifier = Modifier
)
```

**Styling**:
- Background: Plain white (#FFFFFF)
- Corner radius: 16.dp (4.dp top-right pointer)
- Max width: 75% screen width
- Text: Roboto Regular 15sp, dark grey (#333333)
- Padding: 12dp horizontal, 10dp vertical

**MinimalInputField**:
```kotlin
@Composable
fun MinimalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onVoiceClick: () -> Unit,
    isListening: Boolean,
    modifier: Modifier = Modifier
)
```

**Styling**:
- Background: White (#FFFFFF)
- Corner radius: 24.dp (pill shape)
- Height: 48dp minimum
- Voice button: Circular, MinimalAccentBlue (#5D83FF), 48dp × 48dp

### 4. CalendarScreen Components

**MinimalCalendarGrid**:
```kotlin
@Composable
fun MinimalCalendarGrid(
    selectedMonth: YearMonth,
    selectedDate: LocalDate?,
    eventCounts: Map<LocalDate, Int>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
)
```

**Styling**:
- Background: White (#FFFFFF)
- Date text: Dark grey, Roboto Regular 14sp
- Selected date: Circled with MinimalAccentBlue (#5D83FF)
- Event indicator: MinimalAccentBlue (#5D83FF) dot or number badge
- Touch targets: 48dp × 48dp

**MinimalEventCard**:
```kotlin
@Composable
fun MinimalEventCard(
    time: String,
    message: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Styling**:
- Background: White (#FFFFFF)
- Corner radius: 8dp
- Padding: 12-16dp
- Time text: MinimalAccentBlue (#5D83FF), Roboto Regular 14sp
- Message text: Dark grey, Roboto Regular 15sp

**MinimalFloatingActionButton**:
```kotlin
@Composable
fun MinimalFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Styling**:
- Background: MinimalAccentBlue (#5D83FF)
- Icon: White, 24dp
- Size: 56dp × 56dp
- Elevation: 6.dp

### 5. FinanceScreen Components

**MinimalSummaryCard**:
```kotlin
@Composable
fun MinimalSummaryCard(
    totalSpent: String,
    period: String,
    modifier: Modifier = Modifier
)
```

**Styling**:
- Background: MinimalDeepBlue (#305CDE)
- Corner radius: 12dp
- Padding: 20dp
- Text: White, Roboto Medium 18sp (amount), Roboto Regular 14sp (period)
- Elevation: 4.dp

**MinimalFinanceChart**:
```kotlin
@Composable
fun MinimalFinanceChart(
    data: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier
)
```

**Styling**:
- Background: White (#FFFFFF)
- Chart line: MinimalAccentBlue (#5D83FF), 2.dp width
- Grid lines: Light grey (#E0E0E0), 1.dp width
- Labels: Dark grey, Roboto Regular 12sp

**MinimalTransactionItem**:
```kotlin
@Composable
fun MinimalTransactionItem(
    description: String,
    amount: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
)
```

**Styling**:
- Background: White (#FFFFFF)
- Padding: 12dp vertical
- Description: Dark grey, Roboto Regular 15sp
- Positive amount: MinimalAccentBlue (#5D83FF), Roboto Medium 15sp
- Negative amount: Dark red (#D32F2F), Roboto Medium 15sp

### 6. AllEventsScreen Components

**MinimalTimelineItem**:
```kotlin
@Composable
fun MinimalTimelineItem(
    time: String,
    message: String,
    isLastItem: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Styling**:
- Background: White (#FFFFFF)
- Corner radius: 8dp
- Timeline line: MinimalAccentBlue (#5D83FF), 2.dp width, vertical
- Time text: MinimalAccentBlue (#5D83FF), Roboto Regular 14sp
- Message text: Dark grey, Roboto Regular 15sp
- Padding: 12-16dp

### 7. MapScreen Components

**MinimalSearchBar**:
```kotlin
@Composable
fun MinimalSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

**Styling**:
- Background: MinimalDeepBlue (#305CDE)
- Corner radius: 24dp (pill shape)
- Height: 48dp
- Text: White, Roboto Regular 15sp
- Icons: White, 24dp
- Elevation: 4.dp

**MinimalNavigateButton**:
```kotlin
@Composable
fun MinimalNavigateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Styling**:
- Background: MinimalAccentBlue (#5D83FF)
- Shape: Circular
- Size: 56dp × 56dp
- Icon: White, 24dp
- Elevation: 6.dp
- Position: Bottom corner with 16dp margin

## Data Models

### ChartData
```kotlin
data class ChartData(
    val values: List<Float>,
    val labels: List<String>
)
```

### SummaryData
```kotlin
data class SummaryData(
    val items: List<SummaryItem>
)

data class SummaryItem(
    val label: String,
    val value: String,
    val isAccent: Boolean = false
)
```

### NavigationItem
```kotlin
data class NavigationItem(
    val tab: NavigationTab,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
)
```

## Color Strategy Implementation

### 60% White Allocation
- Main content backgrounds for all screens
- Chat bubbles (with slight grey tints)
- Event cards
- Transaction list backgrounds
- Map background (light-themed map tiles)

### 30% Deep Blue Allocation
- Top header on all screens
- Bottom navigation bar
- Finance summary card background
- Map search bar background

### 10% Light Blue Allocation
- Active navigation indicator
- Selected calendar dates
- Event timestamps
- Chart lines and data visualization
- Positive transaction amounts
- Timeline connector line
- Floating action buttons
- Navigate button on map
- Microphone button in chat

## Error Handling

### Network Errors
1. **Connection Loss**: Display banner with error message and retry button
2. **API Timeout**: Show timeout message with retry option
3. **Data Sync Failure**: Indicate sync status and allow manual refresh

### UI Error States
1. **Empty States**: Show friendly message with action suggestions
2. **Loading States**: Display progress indicators with descriptive text
3. **Permission Errors**: Show clear permission request dialogs

### Data Validation
1. **Invalid Input**: Prevent submission and show inline validation errors
2. **Missing Data**: Handle gracefully with placeholder content
3. **Malformed Data**: Log errors and show fallback UI

## Testing Strategy

### Unit Testing
- Test individual component rendering
- Test color application correctness
- Test typography consistency
- Test spacing calculations
- Test touch target sizes

### Integration Testing
- Test navigation between screens
- Test state preservation across navigation
- Test data flow from ViewModels to UI
- Test error handling flows

### Visual Regression Testing
- Screenshot tests for each screen
- Component snapshot tests
- Color usage verification
- Layout consistency checks

### Accessibility Testing
- Contrast ratio verification (automated)
- Touch target size verification (automated)
- Screen reader testing with TalkBack (manual)
- Content description audit (automated)

## Implementation Phases

### Phase 1: Global Components
1. Create MinimalHeader component
2. Create MinimalBottomNavigation component
3. Update MainActivity to use new navigation
4. Implement navigation state management

### Phase 2: Assistant Screen Redesign
1. Update ChatScreen layout
2. Redesign chat bubbles (AI and User)
3. Update input field styling
4. Add embedded chart support
5. Update suggestion chips

### Phase 3: Calendar Screen Redesign
1. Update CalendarScreen layout
2. Redesign calendar grid with new colors
3. Update event cards styling
4. Add floating action button
5. Update date selection indicators

### Phase 4: Finance Screen Redesign
1. Update FinanceScreen layout
2. Redesign summary card with deep blue background
3. Update chart styling with light blue lines
4. Redesign transaction list items
5. Update positive/negative amount colors

### Phase 5: All Events Screen Redesign
1. Update AllEventsScreen layout
2. Redesign timeline with light blue connector
3. Update event cards styling
4. Update timestamp colors
5. Improve spacing and padding

### Phase 6: Map Screen Redesign
1. Update MapScreen layout
2. Add floating search bar with deep blue background
3. Add navigate button with light blue accent
4. Update map markers and circles
5. Improve info card styling

### Phase 7: Polish and Testing
1. Verify color strategy compliance across all screens
2. Test navigation flow
3. Verify typography consistency
4. Test accessibility features
5. Conduct visual regression testing

## Dependencies

### External Libraries
- Jetpack Compose (already in project)
- Material3 (already in project)
- OSMDroid for maps (already in project)

### Internal Dependencies
- Existing ViewModels (ChatViewModel, CalendarViewModel, FinanceViewModel, MapViewModel)
- Existing data repositories
- Existing domain models

## Performance Considerations

### Rendering Optimization
1. Use LazyColumn/LazyRow for scrollable lists
2. Provide stable keys for list items
3. Use remember for expensive computations
4. Use derivedStateOf for computed values

### Memory Management
1. Properly dispose resources in DisposableEffect
2. Cache images and icons
3. Implement pagination for large lists

### Animation Performance
1. Use hardware acceleration
2. Target 60fps for all animations
3. Debounce rapid user inputs

## Accessibility Implementation

### Screen Reader Support
1. Content descriptions for all icons
2. Semantic roles for interactive elements
3. State announcements for dynamic content
4. Navigation hints for complex interactions

### Visual Accessibility
1. Minimum 4.5:1 contrast ratio for all text
2. Minimum 48dp × 48dp touch targets
3. Clear focus indicators
4. Support for dynamic text sizing

### Motor Accessibility
1. Large touch targets (56dp for navigation)
2. Adequate spacing between interactive elements
3. No time-based interactions required
4. Alternative input methods supported

## Security Considerations

1. Sanitize user input before processing
2. Request permissions only when needed
3. Don't log sensitive data
4. Secure storage for financial data

## Future Enhancements

1. Dark mode support
2. Custom theme colors
3. Animated transitions between screens
4. Gesture navigation
5. Widget support
6. Tablet-optimized layouts
7. Landscape mode optimization
8. Offline mode indicators
9. Sync status indicators
10. Advanced filtering and search
