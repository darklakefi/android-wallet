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
 * End-to-end test for main wallet functionality
 */
@RunWith(AndroidJUnit4::class)
class WalletMainE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        // Set up a test wallet for these tests
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storageManager = WalletStorageManager(context)
        
        // Clear existing data
        storageManager.clearAllData()
        
        // Create a test wallet with known mnemonic
        val testMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val wallet = SolanaWallet.fromMnemonic(testMnemonic)
        storageManager.saveWallet("Test Wallet", wallet.mnemonic, wallet.privateKey, wallet.publicKey)
    }

    @Test
    fun testWalletBalanceDisplay() {
        // Wait for wallet screen to load
        composeTestRule.waitForIdle()

        // Verify wallet balance section is displayed
        composeTestRule.onNodeWithText("WALLET_STATUS", substring = true)
            .assertIsDisplayed()

        // Verify SOL balance is displayed (initially 0)
        composeTestRule.onNodeWithText("SOL", substring = true)
            .assertIsDisplayed()

        // Verify wallet address is displayed
        composeTestRule.onNodeWithText("WALLET_ADDRESS", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testRefreshWalletBalance() {
        composeTestRule.waitForIdle()

        // Find and click the refresh button
        composeTestRule.onNodeWithContentDescription("Refresh")
            .assertIsDisplayed()
            .performClick()

        // Wait for refresh to start
        composeTestRule.waitForIdle()

        // Verify that some loading indicator appears or balance updates
        // Note: In a real test with network, this would verify actual balance update
    }

    @Test
    fun testNavigationToSettings() {
        composeTestRule.waitForIdle()

        // Click on settings button
        composeTestRule.onNodeWithContentDescription("Settings")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Verify we're on settings screen
        composeTestRule.onNodeWithText("Settings", substring = true)
            .assertIsDisplayed()

        // Verify settings options are displayed
        composeTestRule.onNodeWithText("Network", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testBottomNavigationTabs() {
        composeTestRule.waitForIdle()

        // Test Wallet tab (should be selected by default)
        composeTestRule.onNodeWithText("Wallet")
            .assertIsDisplayed()
            .assertIsSelected()

        // Navigate to Swap tab
        composeTestRule.onNodeWithText("Swap")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Verify Swap screen is displayed
        composeTestRule.onNodeWithText("From", substring = true)
            .assertIsDisplayed()

        // Navigate to LP tab
        composeTestRule.onNodeWithText("LP")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Verify LP screen is displayed
        composeTestRule.onNodeWithText("LIQUIDITY", substring = true)
            .assertIsDisplayed()

        // Navigate back to Wallet tab
        composeTestRule.onNodeWithText("Wallet")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify we're back on wallet screen
        composeTestRule.onNodeWithText("WALLET_STATUS", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testTokenListDisplay() {
        composeTestRule.waitForIdle()

        // Check if token inventory section exists
        composeTestRule.onNodeWithText("TOKEN_INVENTORY", substring = true)
            .assertIsDisplayed()

        // Initially might show "NO_TOKENS_DETECTED" or actual tokens if connected
        // This depends on network connectivity
        composeTestRule.onAnyChild(
            hasText("NO_TOKENS_DETECTED", substring = true) or
            hasTestTag("token_item")
        ).assertExists()
    }

    @Test
    fun testNFTCollectionDisplay() {
        composeTestRule.waitForIdle()

        // Check if NFT collection section exists
        composeTestRule.onNodeWithText("NFT_COLLECTION", substring = true)
            .assertIsDisplayed()

        // Initially might show "NO_NFT_ASSETS_FOUND" or actual NFTs if connected
        composeTestRule.onAnyChild(
            hasText("NO_NFT_ASSETS_FOUND", substring = true) or
            hasTestTag("nft_item")
        ).assertExists()
    }

    @Test
    fun testCopyWalletAddress() {
        composeTestRule.waitForIdle()

        // Find and click on the wallet address (which should copy it)
        composeTestRule.onNodeWithText("WALLET_ADDRESS", substring = true)
            .assertIsDisplayed()

        // Find the address display area and click it
        composeTestRule.onAllNodesWithTag("address_display")
            .filterToOne(hasClickAction())
            .performClick()

        // Verify that "Copied!" message appears
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Copied", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun testNetworkStatusDisplay() {
        composeTestRule.waitForIdle()

        // Verify network status is displayed
        composeTestRule.onNodeWithText("DEVNET", substring = true)
            .assertIsDisplayed()

        // Check for connection status indicator
        composeTestRule.onAnyChild(
            hasText("CONNECTED", substring = true) or
            hasText("OFFLINE", substring = true) or
            hasText("SYSTEM_READY", substring = true)
        ).assertExists()
    }
}