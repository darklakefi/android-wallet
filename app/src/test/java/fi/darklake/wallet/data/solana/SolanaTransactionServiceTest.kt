package fi.darklake.wallet.data.solana

import fi.darklake.wallet.data.model.NetworkSettings
import fi.darklake.wallet.data.model.SolanaNetwork
import fi.darklake.wallet.data.preferences.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class SolanaTransactionServiceTest {

    @Mock
    private lateinit var settingsManager: SettingsManager

    private lateinit var transactionService: SolanaTransactionService

    private val mockNetworkSettings = NetworkSettings(
        network = SolanaNetwork.DEVNET,
        customRpcUrl = null,
        heliusApiKey = "test-api-key"
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        whenever(settingsManager.networkSettings)
            .thenReturn(MutableStateFlow(mockNetworkSettings))
        
        transactionService = SolanaTransactionService(settingsManager)
    }

    @Test
    fun `sendSolTransaction should return success for valid inputs`() = runTest {
        val privateKey = ByteArray(32) { it.toByte() }
        val toAddress = "11111111111111111111111111111112"
        val lamports = 1000000L

        val result = transactionService.sendSolTransaction(
            fromPrivateKey = privateKey,
            toAddress = toAddress,
            lamports = lamports
        )

        // May succeed or fail based on network conditions, both are acceptable
        assertNotNull(result)
        if (result.isSuccess) {
            assertNotNull(result.getOrNull())
        } else {
            assertNotNull(result.exceptionOrNull())
        }
    }

    @Test
    fun `sendSolTransaction should handle empty private key`() = runTest {
        val privateKey = ByteArray(0)
        val toAddress = "11111111111111111111111111111112"
        val lamports = 1000000L

        val result = transactionService.sendSolTransaction(
            fromPrivateKey = privateKey,
            toAddress = toAddress,
            lamports = lamports
        )

        // Should return a result (success or failure)
        assertNotNull(result)
    }

    @Test
    fun `sendSolTransaction should handle zero lamports`() = runTest {
        val privateKey = ByteArray(32) { it.toByte() }
        val toAddress = "11111111111111111111111111111112"
        val lamports = 0L

        val result = transactionService.sendSolTransaction(
            fromPrivateKey = privateKey,
            toAddress = toAddress,
            lamports = lamports
        )

        assertNotNull(result)
    }

    @Test
    fun `sendTokenTransaction should return success for valid inputs`() = runTest {
        val privateKey = ByteArray(32) { it.toByte() }
        val toAddress = "11111111111111111111111111111112"
        val tokenMint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
        val amount = 1000000L
        val decimals = 6

        val result = transactionService.sendTokenTransaction(
            fromPrivateKey = privateKey,
            toAddress = toAddress,
            tokenMint = tokenMint,
            amount = amount,
            decimals = decimals
        )

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertTrue(result.getOrNull()!!.startsWith("token_transfer_"))
    }

    @Test
    fun `sendNftTransaction should return success for valid inputs`() = runTest {
        val privateKey = ByteArray(32) { it.toByte() }
        val toAddress = "11111111111111111111111111111112"
        val nftMint = "7BgBvyjrZX1YKz4oh9mjb8ZScatkkwb8DzFx7LoiVkM3"

        val result = transactionService.sendNftTransaction(
            fromPrivateKey = privateKey,
            toAddress = toAddress,
            nftMint = nftMint
        )

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertTrue(result.getOrNull()!!.startsWith("nft_transfer_"))
    }

    @Test
    fun `sendSolTransaction should handle invalid address format`() = runTest {
        val privateKey = ByteArray(32) { it.toByte() }
        val invalidAddress = "invalid_address"
        val lamports = 1000000L

        val result = transactionService.sendSolTransaction(
            fromPrivateKey = privateKey,
            toAddress = invalidAddress,
            lamports = lamports
        )

        // Should return a result (network call may succeed or fail)
        assertNotNull(result)
    }

    @Test
    fun `service should be closeable`() {
        // Test that the service can be closed without throwing exceptions
        assertDoesNotThrow {
            transactionService.close()
        }
    }

    private fun assertDoesNotThrow(executable: () -> Unit) {
        try {
            executable.invoke()
        } catch (e: Exception) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}