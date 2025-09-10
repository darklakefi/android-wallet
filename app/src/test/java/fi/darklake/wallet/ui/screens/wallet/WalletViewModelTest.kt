package fi.darklake.wallet.ui.screens.wallet

import fi.darklake.wallet.crypto.SolanaWallet
import fi.darklake.wallet.data.api.WalletAssetsRepository
import fi.darklake.wallet.data.model.*
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.screens.wallet.WalletViewModel
import fi.darklake.wallet.ui.screens.wallet.WalletUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class WalletViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Mock
    private lateinit var storageManager: WalletStorageManager

    @Mock
    private lateinit var settingsManager: SettingsManager

    @Mock
    private lateinit var assetsRepository: WalletAssetsRepository

    private lateinit var viewModel: WalletViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(settingsManager.networkSettings)
            .thenReturn(MutableStateFlow(NetworkSettings()))

        // Create a custom ViewModel that accepts the mocked repository
        viewModel = object : WalletViewModel(storageManager, settingsManager, context = null) {
            override fun createAssetsRepository(): WalletAssetsRepository = assetsRepository
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have expected defaults`() = runTest {
        val state = viewModel.uiState.value
        
        // Check initial state defaults
        assertEquals(0.0, state.solBalance, 0.001)
        assertTrue(state.tokens.isEmpty())
        assertTrue(state.nfts.isEmpty())
    }

    @Test
    fun `clearError should remove error from state`() = runTest {
        viewModel.clearError()
        
        val state = viewModel.uiState.value
        assertNull(state.error)
    }
    
    @Test
    fun `loadWalletData should handle no wallet case`() = runTest {
        whenever(storageManager.getWallet()).thenReturn(Result.success(null))
        
        viewModel.loadWalletData()
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("No wallet found"))
    }
}