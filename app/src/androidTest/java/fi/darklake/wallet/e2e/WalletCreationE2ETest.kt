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

/**
 * End-to-end test for wallet creation flow
 */
@RunWith(AndroidJUnit4::class)
class WalletCreationE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        // Clear any existing wallet data before each test
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storageManager = WalletStorageManager(context)
        storageManager.clearAllData()
    }

    @Test
    fun testCompleteWalletCreationFlow() {
        // Wait for the welcome screen to load
        composeTestRule.waitForIdle()

        // Step 1: Verify we're on the welcome screen
        composeTestRule.onNodeWithText("DARKLAKE", substring = true)
            .assertIsDisplayed()

        // Step 2: Click on "Create New Wallet"
        composeTestRule.onNodeWithText("CREATE NEW WALLET")
            .assertIsDisplayed()
            .performClick()

        // Wait for navigation
        composeTestRule.waitForIdle()

        // Step 3: Verify we're on the create wallet screen
        composeTestRule.onNodeWithText("Create Your Wallet", substring = true)
            .assertIsDisplayed()

        // Step 4: Enter wallet name
        composeTestRule.onNodeWithTag("wallet_name_input", useUnmergedTree = true)
            .performTextInput("Test Wallet")

        // Step 5: Click generate wallet button
        composeTestRule.onNodeWithText("GENERATE WALLET")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        // Wait for wallet generation
        composeTestRule.waitForIdle()

        // Step 6: Verify we're on the mnemonic display screen
        composeTestRule.onNodeWithText("Your Recovery Phrase", substring = true)
            .assertIsDisplayed()

        // Step 7: Verify that 12 mnemonic words are displayed
        composeTestRule.onAllNodesWithTag("mnemonic_word")
            .assertCountEquals(12)

        // Step 8: Click continue to go to verification
        composeTestRule.onNodeWithText("I've saved my recovery phrase")
            .assertIsDisplayed()
            .performClick()

        // Wait for navigation
        composeTestRule.waitForIdle()

        // Step 9: Verify we're on the verification screen
        composeTestRule.onNodeWithText("Verify Your Recovery Phrase", substring = true)
            .assertIsDisplayed()

        // Step 10: Select the correct words (this would need the actual words from the previous screen)
        // For testing purposes, we'll verify that word selection buttons exist
        composeTestRule.onAllNodesWithTag("word_option")
            .assertAny(hasText("", substring = false))

        // Note: In a real test, we would need to:
        // 1. Store the mnemonic words from the display screen
        // 2. Select the correct words in the correct order
        // 3. Verify successful wallet creation
    }

    @Test
    fun testWalletImportFlow() {
        // Wait for the welcome screen
        composeTestRule.waitForIdle()

        // Step 1: Click on "Import Existing Wallet"
        composeTestRule.onNodeWithText("IMPORT EXISTING WALLET")
            .assertIsDisplayed()
            .performClick()

        // Wait for navigation
        composeTestRule.waitForIdle()

        // Step 2: Verify we're on the import wallet screen
        composeTestRule.onNodeWithText("Import Your Wallet", substring = true)
            .assertIsDisplayed()

        // Step 3: Enter wallet name
        composeTestRule.onNodeWithTag("wallet_name_input", useUnmergedTree = true)
            .performTextInput("Imported Wallet")

        // Step 4: Enter a test mnemonic phrase
        val testMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        composeTestRule.onNodeWithTag("mnemonic_input", useUnmergedTree = true)
            .performTextInput(testMnemonic)

        // Step 5: Click import wallet button
        composeTestRule.onNodeWithText("IMPORT WALLET")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        // Wait for import to complete
        composeTestRule.waitForIdle()

        // Step 6: Verify successful import (should navigate to main wallet screen)
        // The main wallet screen should show balance and wallet address
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("SOL", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun testInvalidMnemonicImport() {
        // Navigate to import screen
        composeTestRule.onNodeWithText("IMPORT EXISTING WALLET")
            .performClick()

        composeTestRule.waitForIdle()

        // Enter wallet name
        composeTestRule.onNodeWithTag("wallet_name_input", useUnmergedTree = true)
            .performTextInput("Invalid Wallet")

        // Enter invalid mnemonic
        composeTestRule.onNodeWithTag("mnemonic_input", useUnmergedTree = true)
            .performTextInput("invalid mnemonic phrase that should not work")

        // Try to import
        composeTestRule.onNodeWithText("IMPORT WALLET")
            .performClick()

        // Verify error message is displayed
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Invalid", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun testNavigationBetweenOnboardingScreens() {
        // Test navigation from welcome to create
        composeTestRule.onNodeWithText("CREATE NEW WALLET")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify we're on create screen
        composeTestRule.onNodeWithText("Create Your Wallet", substring = true)
            .assertIsDisplayed()

        // Go back using system back button
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressed()
        }

        composeTestRule.waitForIdle()

        // Verify we're back on welcome screen
        composeTestRule.onNodeWithText("DARKLAKE", substring = true)
            .assertIsDisplayed()

        // Test navigation to import
        composeTestRule.onNodeWithText("IMPORT EXISTING WALLET")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify we're on import screen
        composeTestRule.onNodeWithText("Import Your Wallet", substring = true)
            .assertIsDisplayed()
    }
}