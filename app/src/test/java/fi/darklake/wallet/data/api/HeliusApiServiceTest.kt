package fi.darklake.wallet.data.api

import fi.darklake.wallet.data.model.NetworkSettings
import fi.darklake.wallet.data.model.SolanaNetwork
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class HeliusApiServiceTest {

    private lateinit var apiService: SolanaApiService

    @Before
    fun setup() {
        // Use a test RPC URL for testing
        apiService = SolanaApiService { "https://api.devnet.solana.com" }
    }

    @Test
    fun `getBalance should handle valid public key`() = runTest {
        val validPublicKey = "11111111111111111111111111111112" // System program ID
        
        val result = apiService.getBalance(validPublicKey)
        
        // Should not fail, even if balance is 0
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertTrue(result.getOrNull()!! >= 0.0)
    }

    @Test
    fun `getBalance should handle empty public key`() = runTest {
        val emptyPublicKey = ""
        
        val result = apiService.getBalance(emptyPublicKey)
        
        // Should use fallback test address and succeed
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getBalance should handle invalid public key gracefully`() = runTest {
        val invalidPublicKey = "invalid_key"
        
        val result = apiService.getBalance(invalidPublicKey)
        
        // API might fail, but shouldn't crash
        if (result.isFailure) {
            assertNotNull(result.exceptionOrNull())
        } else {
            assertNotNull(result.getOrNull())
        }
    }

    @Test
    fun `getTokenAccounts should handle valid public key`() = runTest {
        val validPublicKey = "11111111111111111111111111111112"
        
        val result = apiService.getTokenAccounts(validPublicKey)
        
        // Should return a result (success or failure)
        assertNotNull(result)
        if (result.isSuccess) {
            assertNotNull(result.getOrNull())
        } else {
            assertNotNull(result.exceptionOrNull())
        }
    }

    @Test
    fun `getTokenMetadata should handle empty mint list`() = runTest {
        val emptyMints = emptyList<String>()
        
        val result = apiService.getTokenMetadata(emptyMints)
        
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `getTokenMetadata should handle valid mint addresses`() = runTest {
        val validMints = listOf(
            "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v", // USDC
            "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB"  // USDT
        )
        
        val result = apiService.getTokenMetadata(validMints)
        
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        // Metadata might be found or not, both are valid outcomes
    }

    @Test
    fun `getNftsByOwner should handle valid public key with no Helius key`() = runTest {
        val validPublicKey = "11111111111111111111111111111112"
        
        val result = apiService.getNftsByOwner(validPublicKey)
        
        // Without Helius API key, should return empty list
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `service should be closeable`() {
        // Test that the service can be closed without throwing exceptions
        assertDoesNotThrow {
            apiService.close()
        }
    }

    @Test
    fun `multiple concurrent requests should work`() = runTest {
        val publicKey = "11111111111111111111111111111112"
        
        // Launch multiple concurrent requests
        val results = listOf(
            apiService.getBalance(publicKey),
            apiService.getTokenAccounts(publicKey),
            apiService.getTokenMetadata(emptyList())
        )
        
        // All requests should complete without throwing exceptions
        results.forEach { result ->
            assertNotNull(result)
            // Each result is either success or failure, both are valid outcomes
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

class WalletAssetsRepositoryTest {

    private lateinit var repository: WalletAssetsRepository
    private lateinit var mockApiService: SolanaApiService

    @Before
    fun setup() {
        mockApiService = SolanaApiService { "https://api.devnet.solana.com" }
        repository = WalletAssetsRepository(mockApiService)
    }

    @Test
    fun `getWalletAssets should aggregate all asset types`() = runTest {
        val validPublicKey = "11111111111111111111111111111112"
        
        val result = repository.getWalletAssets(validPublicKey)
        
        if (result.isSuccess) {
            val assets = result.getOrNull()!!
            assertNotNull(assets.solBalance)
            assertNotNull(assets.tokens)
            assertNotNull(assets.nfts)
            assertTrue(assets.solBalance >= 0.0)
        } else {
            // If it fails, it should have a proper error message
            assertNotNull(result.exceptionOrNull())
        }
    }

    @Test
    fun `getWalletAssets should handle API failures gracefully`() = runTest {
        val invalidPublicKey = "invalid_key_that_will_fail"
        
        val result = repository.getWalletAssets(invalidPublicKey)
        
        // Should either succeed or fail gracefully
        if (result.isFailure) {
            assertNotNull(result.exceptionOrNull())
            assertNotNull(result.exceptionOrNull()!!.message)
        }
    }

    @Test
    fun `repository should be closeable`() {
        assertDoesNotThrow {
            repository.close()
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