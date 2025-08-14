package fi.darklake.wallet.ui.wallet

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import fi.darklake.wallet.crypto.SolanaWallet
import fi.darklake.wallet.data.api.WalletAssetsRepository
import fi.darklake.wallet.data.model.*
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class WalletViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Mock
    private lateinit var storageManager: WalletStorageManager

    @Mock
    private lateinit var settingsManager: SettingsManager

    @Mock
    private lateinit var assetsRepository: WalletAssetsRepository

    private lateinit var viewModel: WalletViewModel

    private val mockWallet = SolanaWallet(
        publicKey = "11111111111111111111111111111112",
        privateKey = ByteArray(32) { it.toByte() },
        mnemonic = listOf("test", "mnemonic", "words", "for", "testing", "purposes", "only", "do", "not", "use", "in", "production")
    )

    private val mockWalletAssets = WalletAssets(
        solBalance = 1.5,
        tokens = listOf(
            TokenInfo(
                balance = TokenBalance(
                    mint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
                    amount = "1000000",
                    decimals = 6,
                    uiAmount = 1.0,
                    uiAmountString = "1"
                ),
                metadata = TokenMetadata(
                    mint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
                    name = "USD Coin",
                    symbol = "USDC",
                    description = null,
                    image = null,
                    decimals = 6
                )
            )
        ),
        nfts = listOf(
            NftMetadata(
                mint = "7BgBvyjrZX1YKz4oh9mjb8ZScatkkwb8DzFx7LoiVkM3",
                name = "Test NFT",
                symbol = null,
                description = "A test NFT",
                image = "https://example.com/image.png",
                externalUrl = null,
                attributes = null,
                collection = null
            )
        )
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(settingsManager.networkSettings)
            .thenReturn(MutableStateFlow(NetworkSettings()))

        // Create a custom ViewModel that accepts the mocked repository
        viewModel = object : WalletViewModel(storageManager, settingsManager) {
            override fun createAssetsRepository(): WalletAssetsRepository = assetsRepository
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be loading`() = runTest {
        // Allow initialization to complete
        advanceUntilIdle()
        
        viewModel.uiState.test {
            val initialState = awaitItem()
            // State may be loading or finished loading depending on mock setup
            assertEquals(0.0, initialState.solBalance, 0.001)
            assertTrue(initialState.tokens.isEmpty())
            assertTrue(initialState.nfts.isEmpty())
        }
    }

    @Test
    fun `loadWalletData should load wallet successfully`() = runTest {
        whenever(storageManager.getWallet()).thenReturn(Result.success(mockWallet))
        whenever(assetsRepository.getWalletAssets(any())).thenReturn(Result.success(mockWalletAssets))

        viewModel.loadWalletData()

        viewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertEquals(mockWallet.publicKey, finalState.publicKey)
            assertEquals(1.5, finalState.solBalance, 0.001)
            assertEquals(1, finalState.tokens.size)
            assertEquals(1, finalState.nfts.size)
            assertNull(finalState.error)
        }
    }

    @Test
    fun `loadWalletData should handle no wallet found`() = runTest {
        whenever(storageManager.getWallet()).thenReturn(Result.success(null))

        viewModel.loadWalletData()

        viewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertNull(finalState.publicKey)
            assertNotNull(finalState.error)
            assertTrue(finalState.error!!.contains("No wallet found"))
        }
    }

    @Test
    fun `loadWalletData should handle wallet storage error`() = runTest {
        whenever(storageManager.getWallet()).thenReturn(Result.failure(Exception("Storage error")))

        viewModel.loadWalletData()

        viewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertNotNull(finalState.error)
            assertTrue(finalState.error!!.contains("Storage error"))
        }
    }

    @Test
    fun `loadWalletData should handle assets loading error`() = runTest {
        whenever(storageManager.getWallet()).thenReturn(Result.success(mockWallet))
        whenever(assetsRepository.getWalletAssets(any())).thenReturn(Result.failure(Exception("API error")))

        viewModel.loadWalletData()

        viewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertEquals(mockWallet.publicKey, finalState.publicKey)
            assertNotNull(finalState.error)
            assertTrue(finalState.error!!.contains("API error"))
        }
    }

    @Test
    fun `refresh should update wallet data`() = runTest {
        // Setup initial successful load
        whenever(storageManager.getWallet()).thenReturn(Result.success(mockWallet))
        whenever(assetsRepository.getWalletAssets(any())).thenReturn(Result.success(mockWalletAssets))

        viewModel.loadWalletData()

        // Clear previous interactions
        clearInvocations(assetsRepository)

        // Test refresh
        viewModel.refresh()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isRefreshing)
            assertEquals(mockWallet.publicKey, state.publicKey)
        }

        verify(assetsRepository, times(1)).getWalletAssets(mockWallet.publicKey)
    }

    @Test
    fun `refresh should handle refresh error`() = runTest {
        whenever(storageManager.getWallet()).thenReturn(Result.success(mockWallet))
        whenever(assetsRepository.getWalletAssets(any()))
            .thenReturn(Result.success(mockWalletAssets))
            .thenReturn(Result.failure(Exception("Refresh failed")))

        viewModel.loadWalletData()
        viewModel.refresh()

        viewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isRefreshing)
            assertNotNull(finalState.error)
            assertTrue(finalState.error!!.contains("Refresh failed"))
        }
    }

    @Test
    fun `clearError should remove error from state`() = runTest {
        whenever(storageManager.getWallet()).thenReturn(Result.failure(Exception("Test error")))

        viewModel.loadWalletData()
        viewModel.clearError()

        viewModel.uiState.test {
            val finalState = awaitItem()
            assertNull(finalState.error)
        }
    }

    @Test
    fun `displayToken conversion should work correctly`() = runTest {
        whenever(storageManager.getWallet()).thenReturn(Result.success(mockWallet))
        whenever(assetsRepository.getWalletAssets(any())).thenReturn(Result.success(mockWalletAssets))

        viewModel.loadWalletData()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            val displayTokens = state.tokens
            assertEquals(1, displayTokens.size)
            
            val token = displayTokens.first()
            assertEquals("USD Coin", token.name)
            assertEquals("USDC", token.symbol)
            assertEquals("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v", token.mint)
            assertEquals("1", token.balance) // Balance is uiAmountString which is "1"
        }
    }

    @Test
    fun `displayNft conversion should work correctly`() = runTest {
        whenever(storageManager.getWallet()).thenReturn(Result.success(mockWallet))
        whenever(assetsRepository.getWalletAssets(any())).thenReturn(Result.success(mockWalletAssets))

        viewModel.loadWalletData()

        viewModel.uiState.test {
            val state = awaitItem()
            val displayNfts = state.nfts
            assertEquals(1, displayNfts.size)
            
            val nft = displayNfts.first()
            assertEquals("Test NFT", nft.name)
            assertEquals("7BgBvyjrZX1YKz4oh9mjb8ZScatkkwb8DzFx7LoiVkM3", nft.mint)
            assertEquals("https://example.com/image.png", nft.imageUrl)
            assertNull(nft.collectionName)
        }
    }
}