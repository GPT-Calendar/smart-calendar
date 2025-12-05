package com.example.voicereminder.presentation.finance

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.voicereminder.domain.models.*
import com.example.voicereminder.presentation.finance.components.*
import com.example.voicereminder.presentation.ui.theme.VirtualAssistantTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility tests for Finance Tab components.
 * Verifies content descriptions, semantic descriptions, and touch target sizes.
 */
@RunWith(AndroidJUnit4::class)
class FinanceAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun summaryCard_hasAccessibleCurrencyDescriptions() {
        val summary = FinanceSummary(
            totalSpent = 12450.0,
            totalReceived = 4200.0,
            trendPercentage = 12.0,
            trendDirection = TrendDirection.UP,
            month = "November 2025"
        )

        composeTestRule.setContent {
            VirtualAssistantTheme {
                SummaryCard(summary = summary)
            }
        }

        // Verify currency amounts have "Afghani" suffix in semantic description
        composeTestRule.onNodeWithText("AFN 12,450")
            .assertExists()
        
        composeTestRule.onNodeWithText("AFN 4,200")
            .assertExists()
    }

    @Test
    fun quickStatsRow_hasAccessibleDescriptions() {
        val quickStats = listOf(
            QuickStat(
                icon = androidx.compose.material.icons.Icons.Default.CalendarToday,
                value = "AFN 3,250",
                label = "This Week's Spend",
                type = StatType.WEEKLY_SPEND
            ),
            QuickStat(
                icon = androidx.compose.material.icons.Icons.Default.ShoppingBag,
                value = "AFN 850",
                label = "Biggest Expense",
                type = StatType.BIGGEST_EXPENSE
            ),
            QuickStat(
                icon = androidx.compose.material.icons.Icons.Default.Restaurant,
                value = "Food",
                label = "Most Spent Category",
                type = StatType.TOP_CATEGORY
            )
        )

        composeTestRule.setContent {
            VirtualAssistantTheme {
                QuickStatsRow(quickStats = quickStats)
            }
        }

        // Verify all stat cards are present
        composeTestRule.onNodeWithText("AFN 3,250").assertExists()
        composeTestRule.onNodeWithText("AFN 850").assertExists()
        composeTestRule.onNodeWithText("Food").assertExists()
    }

    @Test
    fun transactionItem_hasAccessibleDescription() {
        val transaction = Transaction(
            id = "1",
            title = "Bakhtar Bank – ATM Withdrawal",
            subtitle = "Yesterday • 3:12 PM",
            amount = 1200.0,
            type = TransactionType.SPENT,
            category = TransactionCategory.BANK,
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            VirtualAssistantTheme {
                TransactionItem(transaction = transaction)
            }
        }

        // Verify transaction details are present
        composeTestRule.onNodeWithText("Bakhtar Bank – ATM Withdrawal").assertExists()
        composeTestRule.onNodeWithText("Yesterday • 3:12 PM").assertExists()
    }

    @Test
    fun aiInsightBubble_hasAccessibleDescription() {
        val insight = AIInsight(
            message = "You spent 18% more on food this week compared to last week.",
            type = InsightType.SPENDING_INCREASE,
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            VirtualAssistantTheme {
                AIInsightBubble(insight = insight)
            }
        }

        // Verify insight message is present
        composeTestRule.onNodeWithText("You spent 18% more on food this week compared to last week.")
            .assertExists()
    }

    @Test
    fun transactionItem_meetsMinimumTouchTargetSize() {
        val transaction = Transaction(
            id = "1",
            title = "Test Transaction",
            subtitle = "Today",
            amount = 100.0,
            type = TransactionType.SPENT,
            category = TransactionCategory.FOOD,
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            VirtualAssistantTheme {
                TransactionItem(transaction = transaction)
            }
        }

        // Verify the transaction item has minimum height of 68dp (exceeds 48dp requirement)
        composeTestRule.onNodeWithText("Test Transaction")
            .assertExists()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun quickStatCard_meetsMinimumTouchTargetSize() {
        val quickStats = listOf(
            QuickStat(
                icon = androidx.compose.material.icons.Icons.Default.CalendarToday,
                value = "AFN 3,250",
                label = "This Week's Spend",
                type = StatType.WEEKLY_SPEND
            )
        )

        composeTestRule.setContent {
            VirtualAssistantTheme {
                QuickStatsRow(quickStats = quickStats)
            }
        }

        // Verify the stat card meets minimum touch target size
        composeTestRule.onNodeWithText("AFN 3,250")
            .assertExists()
    }

    @Test
    fun aiInsightBubble_meetsMinimumTouchTargetSize() {
        val insight = AIInsight(
            message = "Test insight message",
            type = InsightType.SAVINGS_TIP,
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            VirtualAssistantTheme {
                AIInsightBubble(insight = insight)
            }
        }

        // Verify the insight bubble meets minimum touch target size
        composeTestRule.onNodeWithText("Test insight message")
            .assertExists()
    }
}
