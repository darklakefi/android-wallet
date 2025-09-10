package fi.darklake.wallet.data.api

import fi.darklake.wallet.data.model.NetworkSettings
import fi.darklake.wallet.data.model.SolanaNetwork
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class HeliusApiServiceTest {

    private lateinit var apiService: HeliusApiService

    @Before
    fun setup() {
        // Use a test RPC URL for testing
        apiService = HeliusApiService { "https://api.devnet.solana.com" }
    }

    @Test
    fun `service can be created`() {
        assertNotNull(apiService)
    }

    @Test
    fun `getTokenMetadata should handle empty mint list`() = runTest {
        val emptyMints = emptyList<String>()
        
        val result = apiService.getTokenMetadata(emptyMints)
        
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
    private lateinit var mockApiService: HeliusApiService

    @Before
    fun setup() {
        mockApiService = HeliusApiService { "https://api.devnet.solana.com" }
        repository = WalletAssetsRepository(mockApiService)
    }

    @Test
    fun `repository can be created`() {
        assertNotNull(repository)
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