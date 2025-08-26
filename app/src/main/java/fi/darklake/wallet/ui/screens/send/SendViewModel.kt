package fi.darklake.wallet.ui.screens.send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.darklake.wallet.data.api.SolanaApiService
import fi.darklake.wallet.data.api.WalletAssetsRepository
import fi.darklake.wallet.data.model.getHeliusRpcUrl
import fi.darklake.wallet.data.model.TokenInfo
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.data.solana.SolanaTransactionService
import fi.darklake.wallet.storage.WalletStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow
import java.util.Locale

data class SendUiState(
    val recipientAddress: String = "",
    val amountInput: String = "",
    val amount: Double = 0.0,
    val solBalance: Double = 0.0,
    val estimatedFee: Double = 0.005, // Standard Solana transaction fee
    val isLoading: Boolean = false,
    val error: String? = null,
    val recipientAddressError: String? = null,
    val amountError: String? = null,
    val canSend: Boolean = false,
    val transactionSuccess: Boolean = false,
    val transactionSignature: String? = null,
    // For token sends
    val tokenMint: String? = null,
    val tokenSymbol: String? = null,
    val tokenName: String? = null,
    val tokenImageUrl: String? = null,
    val tokenBalance: String? = null,
    val tokenDecimals: Int = 0,
    val selectedToken: TokenInfo? = null,
    // For NFT sends
    val nftMint: String? = null,
    val nftName: String? = null,
    val nftImageUrl: String? = null
)

class SendViewModel(
    private val storageManager: WalletStorageManager,
    private val settingsManager: SettingsManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SendUiState())
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()
    
    private val transactionService = SolanaTransactionService(settingsManager)
    
    private lateinit var solanaApiService: SolanaApiService
    private lateinit var assetsRepository: WalletAssetsRepository
    
    init {
        initializeServices()
        loadWalletBalance()
    }
    
    private fun initializeServices() {
        val networkSettings = settingsManager.networkSettings.value
        solanaApiService = SolanaApiService { networkSettings.getHeliusRpcUrl() }
        assetsRepository = WalletAssetsRepository(solanaApiService)
    }
    
    private fun loadWalletBalance() {
        viewModelScope.launch {
            try {
                val wallet = storageManager.getWallet().getOrNull()
                if (wallet != null) {
                    val balanceResult = solanaApiService.getBalance(wallet.publicKey)
                    if (balanceResult.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            solBalance = balanceResult.getOrNull() ?: 0.0
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load balance: ${balanceResult.exceptionOrNull()?.message}"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "No wallet found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load balance: ${e.message}"
                )
            }
        }
    }
    
    fun updateRecipientAddress(address: String) {
        _uiState.value = _uiState.value.copy(
            recipientAddress = address,
            recipientAddressError = null,
            error = null
        )
        validateForm()
    }
    
    fun updateAmount(amountStr: String) {
        // Only allow numeric input with decimals
        val filteredAmount = amountStr.filter { it.isDigit() || it == '.' }
        val amount = filteredAmount.toDoubleOrNull() ?: 0.0
        
        _uiState.value = _uiState.value.copy(
            amountInput = filteredAmount,
            amount = amount,
            amountError = null,
            error = null
        )
        validateForm()
    }
    
    fun setMaxAmount() {
        val state = _uiState.value
        val maxAmount = (state.solBalance - state.estimatedFee).coerceAtLeast(0.0)
        val maxAmountStr = String.format(Locale.US, "%.6f", maxAmount)
        
        _uiState.value = state.copy(
            amountInput = maxAmountStr,
            amount = maxAmount,
            amountError = null,
            error = null
        )
        validateForm()
    }
    
    private fun validateForm() {
        val state = _uiState.value
        var isValid = true
        var recipientError: String? = null
        var amountError: String? = null
        
        // Validate recipient address
        if (state.recipientAddress.isBlank()) {
            isValid = false
        } else if (state.recipientAddress.length < 32 || state.recipientAddress.length > 44) {
            recipientError = "Invalid Solana address length"
            isValid = false
        } else if (!isValidSolanaAddress(state.recipientAddress)) {
            recipientError = "Invalid Solana address format"
            isValid = false
        }
        
        // Validate amount
        if (state.amount <= 0.0) {
            isValid = false
        } else if (state.amount + state.estimatedFee > state.solBalance) {
            amountError = "Insufficient balance (including fees)"
            isValid = false
        }
        
        _uiState.value = state.copy(
            recipientAddressError = recipientError,
            amountError = amountError,
            canSend = isValid
        )
    }
    
    private fun isValidSolanaAddress(address: String): Boolean {
        // Basic Solana address validation
        // Should be 32-44 characters, base58 encoded
        val base58Chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        return address.all { it in base58Chars }
    }
    
    fun sendSol() {
        if (!_uiState.value.canSend) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val wallet = storageManager.getWallet().getOrNull()
                if (wallet == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No wallet found"
                    )
                    return@launch
                }
                
                val state = _uiState.value
                val lamports = (state.amount * 1_000_000_000L).toLong() // Convert SOL to lamports
                
                val result = transactionService.sendSolTransaction(
                    fromPrivateKey = wallet.privateKey,
                    toAddress = state.recipientAddress,
                    lamports = lamports
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        transactionSuccess = true,
                        transactionSignature = result.getOrNull()
                    )
                    
                    // Reset form after successful send
                    resetForm()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Transaction failed: ${result.exceptionOrNull()?.message}"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to send SOL: ${e.message}"
                )
            }
        }
    }
    
    private fun resetForm() {
        _uiState.value = _uiState.value.copy(
            recipientAddress = "",
            amountInput = "",
            amount = 0.0,
            recipientAddressError = null,
            amountError = null,
            canSend = false,
            error = null
        )
        loadWalletBalance() // Refresh balance
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(
            transactionSuccess = false,
            transactionSignature = null
        )
    }
    
    // For token sends
    fun setTokenMint(mint: String) {
        viewModelScope.launch {
            try {
                val wallet = storageManager.getWallet().getOrNull() ?: return@launch
                
                // Get token accounts for the wallet
                val tokensResult = solanaApiService.getTokenAccounts(wallet.publicKey)
                if (tokensResult.isSuccess) {
                    val token = tokensResult.getOrNull()?.find { it.balance.mint == mint }
                    if (token != null) {
                        _uiState.value = _uiState.value.copy(
                            tokenMint = mint,
                            tokenSymbol = token.metadata?.symbol ?: "TOKEN",
                            tokenName = token.metadata?.name ?: "Unknown Token",
                            tokenImageUrl = token.metadata?.image,
                            tokenBalance = token.balance.uiAmountString,
                            tokenDecimals = token.balance.decimals,
                            selectedToken = token
                        )
                        
                        // Load metadata if not already present
                        if (token.metadata == null) {
                            loadTokenMetadata(mint)
                        }
                    }
                }
            } catch (e: Exception) {
                println("Failed to load token info: ${e.message}")
            }
        }
    }
    
    fun initializeTokenSend(tokenMint: String, tokenSymbol: String, tokenBalance: String, decimals: Int) {
        _uiState.value = _uiState.value.copy(
            tokenMint = tokenMint,
            tokenSymbol = tokenSymbol,
            tokenBalance = tokenBalance,
            tokenDecimals = decimals
        )
        
        // Load actual token metadata
        loadTokenMetadata(tokenMint)
    }
    
    private fun loadTokenMetadata(mint: String) {
        viewModelScope.launch {
            try {
                val metadataResult = solanaApiService.getTokenMetadata(listOf(mint))
                if (metadataResult.isSuccess) {
                    val metadata = metadataResult.getOrNull()?.firstOrNull()
                    if (metadata != null) {
                        _uiState.value = _uiState.value.copy(
                            tokenSymbol = metadata.symbol,
                            tokenName = metadata.name,
                            tokenImageUrl = metadata.image,
                            tokenDecimals = metadata.decimals
                        )
                    }
                }
            } catch (e: Exception) {
                // Metadata loading failed, but continue with existing data
                println("Failed to load token metadata: ${e.message}")
            }
        }
    }
    
    fun sendToken() {
        if (!_uiState.value.canSend) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val wallet = storageManager.getWallet().getOrNull()
                if (wallet == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No wallet found"
                    )
                    return@launch
                }
                
                val state = _uiState.value
                val tokenMint = state.tokenMint ?: return@launch
                val amount = (state.amount * (10.0).pow(state.tokenDecimals)).toLong()
                
                val result = transactionService.sendTokenTransaction(
                    fromPrivateKey = wallet.privateKey,
                    toAddress = state.recipientAddress,
                    tokenMint = tokenMint,
                    amount = amount,
                    decimals = state.tokenDecimals
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        transactionSuccess = true,
                        transactionSignature = result.getOrNull()
                    )
                    
                    resetForm()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Token transfer failed: ${result.exceptionOrNull()?.message}"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to send token: ${e.message}"
                )
            }
        }
    }
    
    // For NFT sends
    fun initializeNftSend(nftMint: String, nftName: String, nftImageUrl: String?) {
        _uiState.value = _uiState.value.copy(
            nftMint = nftMint,
            nftName = nftName,
            nftImageUrl = nftImageUrl
        )
        
        // Load actual NFT metadata
        loadNftMetadata(nftMint)
    }
    
    private fun loadNftMetadata(mint: String) {
        viewModelScope.launch {
            try {
                val wallet = storageManager.getWallet().getOrNull()
                if (wallet != null) {
                    val nftsResult = solanaApiService.getNftsByOwner(wallet.publicKey)
                    if (nftsResult.isSuccess) {
                        val nft = nftsResult.getOrNull()?.find { it.mint == mint }
                        if (nft != null) {
                            _uiState.value = _uiState.value.copy(
                                nftName = nft.name ?: _uiState.value.nftName,
                                nftImageUrl = nft.image ?: _uiState.value.nftImageUrl
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // NFT metadata loading failed, but continue with existing data
                println("Failed to load NFT metadata: ${e.message}")
            }
        }
    }
    
    fun sendNft() {
        val state = _uiState.value
        if (state.recipientAddress.isBlank() || state.recipientAddressError != null) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val wallet = storageManager.getWallet().getOrNull()
                if (wallet == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No wallet found"
                    )
                    return@launch
                }
                
                val nftMint = state.nftMint ?: return@launch
                
                val result = transactionService.sendNftTransaction(
                    fromPrivateKey = wallet.privateKey,
                    toAddress = state.recipientAddress,
                    nftMint = nftMint
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        transactionSuccess = true,
                        transactionSignature = result.getOrNull()
                    )
                    
                    resetForm()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "NFT transfer failed: ${result.exceptionOrNull()?.message}"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to send NFT: ${e.message}"
                )
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        transactionService.close()
        if (::solanaApiService.isInitialized) {
            solanaApiService.close()
        }
        if (::assetsRepository.isInitialized) {
            assetsRepository.close()
        }
    }
}