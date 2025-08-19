package fi.darklake.wallet.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.darklake.wallet.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import androidx.test.platform.app.InstrumentationRegistry
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.crypto.SolanaWallet

/**
 * End-to-end test for swap functionality
 */
@RunWith(AndroidJUnit4::class)
class SwapE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        // Set up a test wallet
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storageManager = WalletStorageManager(context)
        
        storageManager.clearAllData()
        
        val testMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val wallet = SolanaWallet.fromMnemonic(testMnemonic)
        storageManager.saveWallet("Test Wallet", wallet.mnemonic, wallet.privateKey, wallet.publicKey)
    }

    @Test
    fun testSwapScreenNavigation() {
        composeTestRule.waitForIdle()

        // Navigate to Swap tab
        composeTestRule.onNodeWithText("Swap")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Verify swap screen elements are displayed
        composeTestRule.onNodeWithText("Swap", ignoreCase = true)
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("From")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("To")
            .assertIsDisplayed()
    }

    @Test
    fun testTokenSelection() {
        // Navigate to Swap
        composeTestRule.onNodeWithText("Swap")
            .performClick()

        composeTestRule.waitForIdle()

        // Click on "From" token selector
        composeTestRule.onAllNodesWithText("Select")
            .filterToOne(hasAnyAncestor(hasText("From")))
            .performClick()

        composeTestRule.waitForIdle()

        // Verify token selection sheet is displayed
        composeTestRule.onNodeWithText("Select Token", substring = true)
            .assertIsDisplayed()

        // Look for SOL in the token list
        composeTestRule.onNodeWithText("SOL", substring = true)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Verify SOL is selected as "From" token
        composeTestRule.onAllNodesWithText("SOL")
            .filterToOne(hasAnyAncestor(hasText("From")))
            .assertIsDisplayed()
    }

    @Test
    fun testAmountInput() {
        composeTestRule.onNodeWithText("Swap")
            .performClick()

        composeTestRule.waitForIdle()

        // Find the amount input field
        composeTestRule.onAllNodesWithContentDescription("Amount", substring = true)
            .onFirst()
            .performTextInput("1.5")

        // Verify the input is displayed
        composeTestRule.onNodeWithText("1.5")
            .assertIsDisplayed()

        // Test MAX button if balance is available
        composeTestRule.onAllNodesWithText("MAX")
            .onFirst()
            .performScrollTo()
            .performClick()

        // The amount field should be updated with max balance
        composeTestRule.waitForIdle()
    }

    @Test
    fun testSwapTokensButton() {
        composeTestRule.onNodeWithText("Swap")
            .performClick()

        composeTestRule.waitForIdle()

        // Find and click the swap direction button (usually arrows icon)
        composeTestRule.onNodeWithContentDescription("Swap tokens", substring = true)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Verify that token positions have been swapped
        // This would require checking the actual token positions
    }

    @Test
    fun testSlippageSettings() {
        composeTestRule.onNodeWithText("Swap")
            .performClick()

        composeTestRule.waitForIdle()

        // Look for slippage settings
        composeTestRule.onNodeWithText("Slippage", substring = true)
            .assertIsDisplayed()

        // Test preset slippage options
        composeTestRule.onNodeWithText("0.1%")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        // Test custom slippage
        composeTestRule.onNodeWithText("Custom")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Input custom slippage value
        composeTestRule.onNodeWithText("Custom slippage", substring = true)
            .performTextInput("2.5")

        // Verify custom value is accepted
        composeTestRule.onNodeWithText("2.5%", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testSwapButtonStates() {
        composeTestRule.onNodeWithText("Swap")
            .performClick()

        composeTestRule.waitForIdle()

        // Initially, swap button should show "Enter an amount"
        composeTestRule.onNodeWithText("Enter an amount", substring = true)
            .assertIsDisplayed()
            .assertIsNotEnabled()

        // Enter an amount
        composeTestRule.onAllNodesWithContentDescription("Amount", substring = true)
            .onFirst()
            .performTextInput("1")

        composeTestRule.waitForIdle()

        // Button text should change based on state
        // Could be "Select", "Insufficient balance", "No liquidity pool", or "Swap"
        composeTestRule.onAnyChild(
            hasText("Select", substring = true) or
            hasText("Insufficient", substring = true) or
            hasText("No liquidity", substring = true) or
            hasText("Swap", ignoreCase = true)
        ).assertExists()
    }

    @Test
    fun testSwapWithInsufficientBalance() {
        composeTestRule.onNodeWithText("Swap")
            .performClick()

        composeTestRule.waitForIdle()

        // Select tokens
        composeTestRule.onAllNodesWithText("Select")
            .filterToOne(hasAnyAncestor(hasText("From")))
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SOL", substring = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Enter a large amount (more than balance)
        composeTestRule.onAllNodesWithContentDescription("Amount", substring = true)
            .onFirst()
            .performTextInput("999999")

        composeTestRule.waitForIdle()

        // Verify insufficient balance message
        composeTestRule.onNodeWithText("Insufficient", substring = true)
            .assertIsDisplayed()

        // Swap button should be disabled
        composeTestRule.onAllNodesWithText("Swap", ignoreCase = true)
            .filterToOne(hasClickAction())
            .assertIsNotEnabled()
    }

    @Test
    fun testQuoteDisplay() {
        composeTestRule.onNodeWithText("Swap")
            .performClick()

        composeTestRule.waitForIdle()

        // Select tokens and enter amount
        composeTestRule.onAllNodesWithText("Select")
            .filterToOne(hasAnyAncestor(hasText("From")))
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SOL", substring = true)
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("Select")
            .filterToOne(hasAnyAncestor(hasText("To")))
            .performClick()

        composeTestRule.waitForIdle()

        // Select a different token if available
        composeTestRule.onAllNodesWithText("USDC", substring = true)
            .onFirstOrNull()
            ?.performClick()
            ?: composeTestRule.onAllNodes(hasTestTag("token_item"))
                .onFirst()
                .performClick()

        composeTestRule.waitForIdle()

        // Enter amount
        composeTestRule.onAllNodesWithContentDescription("Amount", substring = true)
            .onFirst()
            .performTextInput("1")

        composeTestRule.waitForIdle()

        // Look for quote details (if pool exists)
        // These might appear: Rate, Price Impact, Network Fee
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Rate", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("No liquidity pool", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}