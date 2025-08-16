package fi.darklake.wallet.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.darklake.wallet.data.api.SolanaApiService
import fi.darklake.wallet.data.api.WalletAssetsRepository
import fi.darklake.wallet.data.model.DisplayNft
import fi.darklake.wallet.data.model.DisplayToken
import fi.darklake.wallet.data.model.WalletAssets
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WalletUiState(
    val isLoading: Boolean = false,
    val solBalance: Double = 0.0,
    val tokens: List<DisplayToken> = emptyList(),
    val nfts: List<DisplayNft> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val publicKey: String? = null
)

open class WalletViewModel(
    private val storageManager: WalletStorageManager,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val assetsRepository = createAssetsRepository()

    protected open fun createAssetsRepository(): WalletAssetsRepository {
        val solanaApi = SolanaApiService { settingsManager.getCurrentRpcUrl() }
        return WalletAssetsRepository(solanaApi)
    }

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    init {
        loadWalletData()
    }

    fun loadWalletData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get wallet from storage to extract public key
                val walletResult = storageManager.getWallet()
                if (walletResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load wallet: ${walletResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                val wallet = walletResult.getOrNull()
                if (wallet == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No wallet found"
                    )
                    return@launch
                }

                val publicKey = wallet.publicKey
                _uiState.value = _uiState.value.copy(publicKey = publicKey)

                // Fetch assets from Helius
                val assetsResult = assetsRepository.getWalletAssets(publicKey)
                if (assetsResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load assets: ${assetsResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                val assets = assetsResult.getOrNull()!!
                updateUiWithAssets(assets)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    fun refresh() {
        val currentPublicKey = _uiState.value.publicKey
        if (currentPublicKey == null) {
            loadWalletData()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            try {
                val assetsResult = assetsRepository.getWalletAssets(currentPublicKey)
                if (assetsResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = "Failed to refresh: ${assetsResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                val assets = assetsResult.getOrNull()!!
                updateUiWithAssets(assets)
                _uiState.value = _uiState.value.copy(isRefreshing = false)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "Refresh failed: ${e.message}"
                )
            }
        }
    }

    private fun updateUiWithAssets(assets: WalletAssets) {
        val displayTokens = mutableListOf<DisplayToken>()
        
        // Add regular tokens
        displayTokens.addAll(assets.tokens.map { tokenInfo ->
            DisplayToken(
                mint = tokenInfo.balance.mint,
                name = tokenInfo.metadata?.name ?: "Unknown Token",
                symbol = tokenInfo.metadata?.symbol ?: tokenInfo.balance.mint.take(6),
                balance = tokenInfo.balance.uiAmountString ?: "0",
                imageUrl = tokenInfo.metadata?.image,
                compressed = false
            )
        })
        
        // Add compressed tokens
        displayTokens.addAll(assets.compressedTokens.map { compressedToken ->
            DisplayToken(
                mint = compressedToken.mint,
                name = "Compressed Token",
                symbol = "cTOKEN",
                balance = compressedToken.amount,
                imageUrl = null,
                compressed = true
            )
        })

        val displayNfts = mutableListOf<DisplayNft>()
        
        // Add regular NFTs
        displayNfts.addAll(assets.nfts.map { nft ->
            DisplayNft(
                mint = nft.mint,
                name = nft.name ?: "Unknown NFT",
                imageUrl = nft.image,
                collectionName = nft.collection?.name,
                compressed = false
            )
        })
        
        // Add compressed NFTs
        displayNfts.addAll(assets.compressedNfts.map { compressedNft ->
            DisplayNft(
                mint = compressedNft.id,
                name = compressedNft.name ?: "Compressed NFT",
                imageUrl = compressedNft.image,
                collectionName = null,
                compressed = true
            )
        })

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            solBalance = assets.solBalance,
            tokens = displayTokens,
            nfts = displayNfts,
            error = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        assetsRepository.close()
    }
}