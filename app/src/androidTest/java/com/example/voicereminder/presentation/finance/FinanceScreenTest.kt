package com.example.voicereminder.presentation.finance

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.voicereminder.data.FinanceRepository
import com.example.voicereminder.domain.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for FinanceScreen
 * Tests full screen composition, scrolling behavior, data flow from ViewModel to UI,
 * error state, and empty state rendering.
 * 
 * Requirements tested:
 * - 5.3: Consistent spacing and layout optimization
 * - 5.4: Semi-bold font weight and high contrast for readability
 * - 6.4: Template data structure and display
 */
@RunWith(AndroidJUnit4::class)
class FinanceScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * Test 1: Full screen composition with all components
     * Verifies that all major components are rendered correctly
     */
    @Test
    fun testFullScreenComposition_AllComponentsDisplayed() {
        // Given: FinanceScreen is displayed
        composeTestRule.setContent {
            FinanceScreen()
        }
        
        // Wait for loading to complete
        composeTestRule.waitForIdle()
        
        // Then: All major components should be visible
        // Summary Card
        composeTestRule.onNodeWithText("This Month Overview").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Spent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Received").assertIsDisplayed()
        
        // Quick Stats Row - verify all three cards
        composeTestRule.onNodeWithText("This Week's Spend").assertIsDisplayed()
        composeTestRule.onNodeWithText("Biggest Expense").assertIsDisplayed()
        composeTestRule.onNodeWithText("Most Spent Category").assertIsDisplayed()
        
        // AI Insight Bubble
        composeTestRule.onNodeWithText(
            "You spent 18% more on food this week compared to last week.",
            substring = true
        ).assertIsDisplayed()
        
        // Transaction List Header
        composeTestRule.onNodeWithText("Recent Transactions").assertIsDisplayed()
        
        // Transaction Items - verify at least some are visible
        composeTestRule.onNodeWithText("Bakhtar Bank – ATM Withdrawal", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Food Corner – Lunch", substring = true)
            .assertIsDisplayed()
    }
    
    /**
     * Test 2: Scrolling behavior through all components
     * Verifies that the screen is scrollable and all content is accessible
     */
    @Test
    fun testScrollingBehavior_AllContentAccessible() {
        // Given: FinanceScreen is displayed
        composeTestRule.setContent {
            FinanceScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // Then: Top content should be visible initially
        composeTestRule.onNodeWithText("This Month Overview").assertIsDisplayed()
        
        // When: Scrolling down to bottom transactions
        composeTestRule.onNodeWithText("Recent Transactions")
            .performScrollTo()
        
        // Then: Bottom transactions should become visible
        composeTestRule.onNodeWithText("Market – Groceries", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Salary – Deposit", substring = true)
            .assertIsDisplayed()
        
        // When: Scrolling back to top
        composeTestRule.onNodeWithText("This Month Overview")
            .performScrollTo()
        
        // Then: Top content should be visible again
        composeTestRule.onNodeWithText("This Month Overview").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Spent").assertIsDisplayed()
    }
    
    /**
     * Test 3: Verify correct data flow from ViewModel to UI
     * Tests that template data from repository is correctly displayed
     */
    @Test
    fun testDataFlow_TemplateDataDisplayedCorrectly() {
        // Given: FinanceScreen with default repository
        composeTestRule.setContent {
            FinanceScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // Then: Summary data should match template values
        composeTestRule.onNodeWithText("AFN 12,450", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("AFN 4,200", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("12%", substring = true).assertIsDisplayed()
        
        // Then: Quick stats should match template values
        composeTestRule.onNodeWithText("AFN 3,250").assertIsDisplayed()
        composeTestRule.onNodeWithText("AFN 850").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
        
        // Then: Transaction amounts should be displayed correctly
        composeTestRule.onNodeWithText("-AFN 1,200", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("-AFN 350", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("+AFN 15,000", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("-AFN 200", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("-AFN 720", substring = true).assertIsDisplayed()
    }
    
    /**
     * Test 4: Error state displays correctly with retry button
     * Verifies error handling and retry functionality
     * Requirements: 3.1, 3.2, 3.3
     */
    @Test
    fun testErrorState_DisplaysCorrectlyWithRetryButton() {
        // Given: A repository that returns null to trigger error state
        val errorRepository = object : FinanceRepository() {
            override suspend fun getFinanceData(): FinanceData? {
                return null // This will trigger error state
            }
        }
        
        val viewModel = FinanceViewModel(errorRepository)
        
        // When: FinanceScreen is displayed with error-triggering ViewModel
        composeTestRule.setContent {
            FinanceScreen(viewModel = viewModel)
        }
        
        composeTestRule.waitForIdle()
        
        // Then: Error message should be displayed
        composeTestRule.onNodeWithText("Unable to load financial data")
            .assertIsDisplayed()
        
        // Then: User-friendly error message should be shown (not technical details)
        composeTestRule.onNodeWithText(
            "Unable to load financial data. The data source returned no information.",
            substring = true
        ).assertIsDisplayed()
        
        // Then: Retry button should be visible and clickable
        composeTestRule.onNodeWithText("Retry")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        // Then: Retry button should have minimum touch target size (48.dp)
        composeTestRule.onNodeWithText("Retry")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }
    
    /**
     * Test 5: Retry button functionality
     * Verifies that clicking retry button triggers data reload
     * Requirements: 3.2
     */
    @Test
    fun testErrorState_RetryButtonTriggersReload() {
        var loadAttempts = 0
        
        // Given: A repository that fails first, then succeeds
        val retryRepository = object : FinanceRepository() {
            override suspend fun getFinanceData(): FinanceData? {
                loadAttempts++
                return if (loadAttempts == 1) {
                    null // First attempt fails
                } else {
                    super.getFinanceData() // Second attempt succeeds
                }
            }
        }
        
        val viewModel = FinanceViewModel(retryRepository)
        
        // When: FinanceScreen is displayed
        composeTestRule.setContent {
            FinanceScreen(viewModel = viewModel)
        }
        
        composeTestRule.waitForIdle()
        
        // Then: Error state should be shown initially
        composeTestRule.onNodeWithText("Unable to load financial data")
            .assertIsDisplayed()
        
        // When: Retry button is clicked
        composeTestRule.onNodeWithText("Retry")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then: Success state should be displayed after retry
        composeTestRule.onNodeWithText("This Month Overview")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent Transactions")
            .assertIsDisplayed()
    }
