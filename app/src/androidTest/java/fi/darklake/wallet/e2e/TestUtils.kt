package fi.darklake.wallet.e2e

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import fi.darklake.wallet.crypto.SolanaWallet
import fi.darklake.wallet.storage.WalletStorageManager

/**
 * Utility functions for end-to-end tests
 */
object TestUtils {

    /**
     * Creates a test wallet with a known mnemonic
     */
    fun createTestWallet(): SolanaWallet {
        val testMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        return SolanaWallet.fromMnemonic(testMnemonic)
    }

    /**
     * Sets up a test wallet in storage
     */
    fun setupTestWallet(walletName: String = "Test Wallet") {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storageManager = WalletStorageManager(context)
        
        storageManager.clearAllData()
        
        val wallet = createTestWallet()
        storageManager.saveWallet(walletName, wallet.mnemonic, wallet.privateKey, wallet.publicKey)
    }

    /**
     * Clears all wallet data
     */
    fun clearWalletData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storageManager = WalletStorageManager(context)
        storageManager.clearAllData()
    }

    /**
     * Waits for a node with text to appear
     */
    fun ComposeTestRule.waitForText(
        text: String,
        substring: Boolean = true,
        timeoutMillis: Long = 5000
    ): SemanticsNodeInteraction {
        return try {
            this.waitUntil(timeoutMillis) {
                this.onAllNodesWithText(text, substring = substring)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            this.onNodeWithText(text, substring = substring)
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("Text '$text' not found within $timeoutMillis ms")
        }
    }

    /**
     * Waits for a node with tag to appear
     */
    fun ComposeTestRule.waitForTag(
        tag: String,
        timeoutMillis: Long = 5000
    ): SemanticsNodeInteraction {
        return try {
            this.waitUntil(timeoutMillis) {
                this.onAllNodesWithTag(tag)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            this.onNodeWithTag(tag)
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("Tag '$tag' not found within $timeoutMillis ms")
        }
    }

    /**
     * Performs a swipe up gesture to scroll
     */
    fun ComposeTestRule.scrollUp() {
        this.onNodeWithTag("scroll_container")
            .performTouchInput {
                swipeUp()
            }
    }

    /**
     * Performs a swipe down gesture to scroll
     */
    fun ComposeTestRule.scrollDown() {
        this.onNodeWithTag("scroll_container")
            .performTouchInput {
                swipeDown()
            }
    }

    /**
     * Test data for tokens
     */
    object TestTokens {
        const val SOL = "SOL"
        const val USDC = "USDC"
        const val USDT = "USDT"
        const val BONK = "BONK"
        
        val commonTokens = listOf(SOL, USDC, USDT, BONK)
    }

    /**
     * Test amounts for transactions
     */
    object TestAmounts {
        const val SMALL = "0.001"
        const val MEDIUM = "1"
        const val LARGE = "100"
        const val INVALID = "abc123"
        const val EXCESSIVE = "999999999"
    }

    /**
     * Common test assertions
     */
    fun ComposeTestRule.assertWalletScreenDisplayed() {
        this.onNodeWithText("WALLET_STATUS", substring = true)
            .assertIsDisplayed()
    }

    fun ComposeTestRule.assertSwapScreenDisplayed() {
        this.onNodeWithText("From")
            .assertIsDisplayed()
        this.onNodeWithText("To")
            .assertIsDisplayed()
    }

    fun ComposeTestRule.assertLPScreenDisplayed() {
        this.onNodeWithText("LIQUIDITY", substring = true)
            .assertIsDisplayed()
    }

    fun ComposeTestRule.assertSettingsScreenDisplayed() {
        this.onNodeWithText("Settings", substring = true)
            .assertIsDisplayed()
    }
}