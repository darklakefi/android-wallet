package fi.darklake.wallet.ui.screens.wallet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.darklake.wallet.data.api.SolanaApiService
import fi.darklake.wallet.data.api.WalletAssetsRepository
import fi.darklake.wallet.data.repository.BalanceRepository
import fi.darklake.wallet.data.repository.BalanceService
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
    private val settingsManager: SettingsManager,
    context: Context? = null
) : ViewModel() {

    private val assetsRepository = createAssetsRepository()
    
    // Use centralized balance repository
    private val balanceRepository: BalanceRepository = if (context != null) {
        BalanceService.getInstance(context).getRepository()
    } else {
        // Fallback to direct repository if no context provided
        val solanaApi = SolanaApiService { settingsManager.getCurrentRpcUrl() }
        BalanceRepository(solanaApi)
    }

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

                // Fetch balances from centralized repository (with caching)
                val solBalanceResult = balanceRepository.fetchSolBalance(publicKey)
                val tokensResult = balanceRepository.fetchTokens(publicKey)
                
                // Also fetch NFTs and compressed tokens from assets repository
                val assetsResult = assetsRepository.getWalletAssets(publicKey)
                if (assetsResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load assets: ${assetsResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                val assets = assetsResult.getOrNull()!!
                
                // Use SOL balance from centralized repository
                val solBalance = balanceRepository.solBalance.value
                
                // Use token balances from centralized repository
                val tokens = balanceRepository.tokens.value
                
                // Merge with NFT data from assets
                updateUiWithAssets(assets, solBalance, tokens)
                
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
                // Force refresh balances in centralized repository
                val solBalanceResult = balanceRepository.fetchSolBalance(currentPublicKey)
                val tokensResult = balanceRepository.fetchTokens(currentPublicKey, forceRefresh = true)
                
                // Also refresh NFTs from assets repository
                val assetsResult = assetsRepository.getWalletAssets(currentPublicKey)
                if (assetsResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = "Failed to refresh: ${assetsResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                val assets = assetsResult.getOrNull()!!
                val solBalance = balanceRepository.solBalance.value
                val tokens = balanceRepository.tokens.value
                
                updateUiWithAssets(assets, solBalance, tokens)
                _uiState.value = _uiState.value.copy(isRefreshing = false)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "Refresh failed: ${e.message}"
                )
            }
        }
    }

    private fun updateUiWithAssets(
        assets: WalletAssets,
        solBalance: Double = assets.solBalance,
        tokens: List<fi.darklake.wallet.data.model.TokenInfo> = assets.tokens
    ) {
        val displayTokens = mutableListOf<DisplayToken>()
        
        // Add regular tokens from centralized repository
        displayTokens.addAll(tokens.map { tokenInfo ->
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
            solBalance = solBalance,
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