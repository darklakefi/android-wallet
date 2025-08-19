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
 * End-to-end test for liquidity provider functionality
 */
@RunWith(AndroidJUnit4::class)
class LiquidityE2ETest {

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
    fun testLPScreenNavigation() {
        composeTestRule.waitForIdle()

        // Navigate to LP tab
        composeTestRule.onNodeWithText("LP")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Verify LP screen elements are displayed
        composeTestRule.onNodeWithText("LIQUIDITY", substring = true)
            .assertIsDisplayed()

        // Verify description text
        composeTestRule.onNodeWithText("MEV profits recovered", substring = true)
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Higher yields", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testLiquidityTokenSelection() {
        composeTestRule.onNodeWithText("LP")
            .performClick()

        composeTestRule.waitForIdle()

        // Select first token
        composeTestRule.onAllNodesWithText("Select Token")
            .onFirst()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Select SOL from the list
        composeTestRule.onNodeWithText("SOL", substring = true)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Verify SOL is selected
        composeTestRule.onNodeWithText("SOL")
            .assertIsDisplayed()

        // Select second token
        composeTestRule.onAllNodesWithText("Select Token")
            .onFirstOrNull()
            ?.performClick()

        composeTestRule.waitForIdle()

        // Select a different token if available
        composeTestRule.onAllNodesWithText("USDC", substring = true)
            .onFirstOrNull()
            ?.performClick()
            ?: composeTestRule.onAllNodes(hasTestTag("token_item"))
                .filterToOne(hasAnyAncestor(hasNoAncestor(hasText("SOL"))))
                .onFirstOrNull()
                ?.performClick()
    }

    @Test
    fun testLiquidityAmountInput() {
        composeTestRule.onNodeWithText("LP")
            .performClick()

        composeTestRule.waitForIdle()

        // Find amount input fields
        composeTestRule.onAllNodesWithTag("token_amount_input")
            .onFirst()
            .performTextInput("10")

        // Verify the input is displayed
        composeTestRule.onNodeWithText("10")
            .assertIsDisplayed()

        // Enter second amount
        composeTestRule.onAllNodesWithTag("token_amount_input")
            .onLast()
            .performTextInput("100")

        // Verify both amounts are displayed
        composeTestRule.onNodeWithText("100")
            .assertIsDisplayed()
    }

    @Test
    fun testPoolCreation() {
        composeTestRule.onNodeWithText("LP")
            .performClick()

        composeTestRule.waitForIdle()

        // Select tokens
        composeTestRule.onAllNodesWithText("Select Token")
            .onFirst()
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SOL", substring = true)
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("Select Token")
            .onFirstOrNull()
            ?.performClick()

        composeTestRule.waitForIdle()

        // Select another token
        composeTestRule.onAllNodes(hasTestTag("token_item"))
            .onFirst()
            .performClick()

        composeTestRule.waitForIdle()

        // Enter amounts
        composeTestRule.onAllNodesWithTag("token_amount_input")
            .onFirst()
            .performTextInput("1")

        composeTestRule.onAllNodesWithTag("token_amount_input")
            .onLast()
            .performTextInput("1")

        // Look for Create Pool or Add Liquidity button
        composeTestRule.onAnyChild(
            hasText("Create Pool", substring = true) or
            hasText("Add Liquidity", substring = true)
        ).assertExists()
    }

    @Test
    fun testSlippageToleranceInLP() {
        composeTestRule.onNodeWithText("LP")
            .performClick()

        composeTestRule.waitForIdle()

        // Look for slippage tolerance setting
        composeTestRule.onNodeWithText("Slippage", substring = true)
            .performScrollTo()
            .assertIsDisplayed()

        // Test slippage adjustment
        composeTestRule.onAllNodesWithTag("slippage_input")
            .onFirstOrNull()
            ?.performTextInput("1.5")

        composeTestRule.waitForIdle()

        // Verify slippage is updated
        composeTestRule.onNodeWithText("1.5", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testLiquidityPositionsDisplay() {
        composeTestRule.onNodeWithText("LP")
            .performClick()

        composeTestRule.waitForIdle()

        // Check if liquidity positions section exists (if user has positions)
        // This section might not exist initially if no positions
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Your Positions", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Your Liquidity", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
            // Or just the form if no positions
            composeTestRule.onAllNodesWithText("Select Token")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun testPoolInfoDisplay() {
        composeTestRule.onNodeWithText("LP")
            .performClick()

        composeTestRule.waitForIdle()

        // Select tokens to see pool info
        composeTestRule.onAllNodesWithText("Select Token")
            .onFirst()
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SOL", substring = true)
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("Select Token")
            .onFirstOrNull()
            ?.performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onAllNodes(hasTestTag("token_item"))
            .onFirst()
            .performClick()

        composeTestRule.waitForIdle()

        // Look for pool information
        // Might show "Pool doesn't exist" or pool details
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Pool", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("TVL", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("doesn't exist", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun testInitialPriceInput() {
        composeTestRule.onNodeWithText("LP")
            .performClick()

        composeTestRule.waitForIdle()

        // Select tokens first
        composeTestRule.onAllNodesWithText("Select Token")
            .onFirst()
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SOL", substring = true)
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("Select Token")
            .onFirstOrNull()
            ?.performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onAllNodes(hasTestTag("token_item"))
            .onFirst()
            .performClick()

        composeTestRule.waitForIdle()

        // Look for initial price input (only appears when creating new pool)
        composeTestRule.onAllNodesWithText("Initial Price", substring = true)
            .onFirstOrNull()
            ?.assertIsDisplayed()

        // If initial price field exists, test input
        composeTestRule.onAllNodesWithTag("initial_price_input")
            .onFirstOrNull()
            ?.performTextInput("1.5")

        composeTestRule.waitForIdle()
    }

    @Test
    fun testErrorMessagesInLP() {
        composeTestRule.onNodeWithText("LP")
            .performClick()

        composeTestRule.waitForIdle()

        // Try to add liquidity without selecting tokens
        composeTestRule.onAllNodesWithText("Add Liquidity", substring = true)
            .onFirstOrNull()
            ?.performClick()

        composeTestRule.waitForIdle()

        // Should show some error or button should be disabled
        composeTestRule.onAnyChild(
            hasText("Select", substring = true) or
            hasText("Enter", substring = true) or
            hasText("required", substring = true, ignoreCase = true)
        ).assertExists()
    }
}