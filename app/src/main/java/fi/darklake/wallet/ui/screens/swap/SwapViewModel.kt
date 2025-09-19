package fi.darklake.wallet.ui.screens.swap

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.data.swap.SwapRepository
import fi.darklake.wallet.data.swap.models.*
import fi.darklake.wallet.data.swap.repository.TokenRepository
import fi.darklake.wallet.data.swap.repository.PoolRepository
import fi.darklake.wallet.data.api.HeliusApiService
import fi.darklake.wallet.data.repository.BalanceRepository
import fi.darklake.wallet.data.repository.BalanceService
import fi.darklake.wallet.data.solana.SolanaTransactionService
import fi.darklake.wallet.storage.WalletStorageManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

data class SwapUiState(
    val tokenA: TokenInfo? = null,
    val tokenB: TokenInfo? = null,
    val tokenAAmount: String = "",
    val tokenBAmount: String = "",
    val tokenABalance: BigDecimal = BigDecimal.ZERO,
    val tokenBBalance: BigDecimal = BigDecimal.ZERO,
    val quote: SwapQuoteResponse? = null,
    val slippagePercent: Double = 0.5,
    val useCustomSlippage: Boolean = false,
    val isLoadingQuote: Boolean = false,
    val isSwapping: Boolean = false,
    val swapStep: SwapStep = SwapStep.IDLE,
    val insufficientBalance: Boolean = false,
    val poolExists: Boolean = true,
    val priceImpactWarning: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val trackingDetails: TrackingDetails? = null,
    val availableTokens: List<fi.darklake.wallet.data.swap.models.Token> = emptyList(),
    val showTokenSelection: Boolean = false,
    val tokenSelectionType: TokenSelectionType? = null
)

enum class TokenSelectionType {
    TOKEN_A, TOKEN_B
}

data class TrackingDetails(
    val trackingId: String,
    val tradeId: String
)

enum class SwapStep {
    IDLE,
    GENERATING_PROOF,      // Step 1: Generating zero-knowledge proof
    CONFIRM_TRANSACTION,   // Step 2: Confirm transaction in wallet
    PROCESSING,           // Step 3: Processing transaction
    COMPLETED,
    FAILED
}

class SwapViewModel(
    private val storageManager: WalletStorageManager,
    private val settingsManager: SettingsManager,
    context: Context? = null
) : ViewModel() {
    
    private val swapRepository: SwapRepository
        get() = SwapRepository(settingsManager.networkSettings.value)
        
    private val tokenRepository = TokenRepository()
    private val poolRepository = PoolRepository(settingsManager)
    private val transactionService = SolanaTransactionService(settingsManager)
        
    private val solanaApiService = HeliusApiService {
        settingsManager.networkSettings.value.let { settings ->
            settings.heliusApiKey?.let { key ->
                when (settings.network) {
                    fi.darklake.wallet.data.model.SolanaNetwork.MAINNET -> 
                        "https://mainnet.helius-rpc.com/?api-key=$key"
                    fi.darklake.wallet.data.model.SolanaNetwork.DEVNET -> 
                        "https://devnet.helius-rpc.com/?api-key=$key"
                }
            } ?: settings.network.rpcUrl
        }
    }
    
    // Use centralized balance repository
    private val balanceRepository: BalanceRepository = if (context != null) {
        BalanceService.getInstance(context).getRepository()
    } else {
        // Fallback to direct repository if no context provided
        BalanceRepository(solanaApiService)
    }
    
    private val _uiState = MutableStateFlow(SwapUiState())
    val uiState: StateFlow<SwapUiState> = _uiState.asStateFlow()
    
    private var quoteJob: Job? = null
    private val QUOTE_DEBOUNCE_MS = 500L
    
    // Generate a unique tracking ID for this swap session
    private var currentTrackingId: String = generateTrackingId()
    
    private fun generateTrackingId(): String {
        return "id${kotlin.random.Random.nextLong().toString(16)}"
    }
    
    // Default token addresses based on dex-web pattern
    // Mainnet: Fartcoin-USDC pair
    // Devnet: DukY-DuX pair (as per dex-web logic)
    
    private fun getDefaultTokenA(): TokenInfo = when (settingsManager.networkSettings.value.network) {
        fi.darklake.wallet.data.model.SolanaNetwork.MAINNET -> TokenInfo(
            address = "9BB6NFEcjBCtnNLFko2FqVQBq8HHM13kCyYcdQbgpump", // Fartcoin
            symbol = "Fartcoin",
            name = "Fartcoin",
            decimals = 6,
            logoURI = null
        )
        fi.darklake.wallet.data.model.SolanaNetwork.DEVNET -> TokenInfo(
            address = "HXsKnhXPtGr2mq4uTpxbxyy7ZydYWJwx4zMuYPEDukY", // DukY
            symbol = "DukY",
            name = "DukY",
            decimals = 9,
            logoURI = null
        )
    }
    
    private fun getDefaultTokenB(): TokenInfo = when (settingsManager.networkSettings.value.network) {
        fi.darklake.wallet.data.model.SolanaNetwork.MAINNET -> TokenInfo(
            address = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v", // USDC
            symbol = "USDC",
            name = "USD Coin",
            decimals = 6,
            logoURI = null
        )
        fi.darklake.wallet.data.model.SolanaNetwork.DEVNET -> TokenInfo(
            address = "DdLxrGFs2sKYbbqVk76eVx9268ASUdTMAhrsqphqDuX", // DuX
            symbol = "DuX",
            name = "DuX",
            decimals = 6,
            logoURI = null
        )
    }
    
    init {
        // Initialize with network-appropriate default tokens
        setTokenA(getDefaultTokenA())
        setTokenB(getDefaultTokenB())
        
        // Load balances
        loadTokenBalances()
        
        // Listen for network changes and refresh tokens accordingly
        viewModelScope.launch {
            settingsManager.networkSettings.collect { networkSettings ->
                // Update to network-appropriate default tokens
                setTokenA(getDefaultTokenA())
                setTokenB(getDefaultTokenB())
                
                // Reload balances with new network settings
                loadTokenBalances()
                // Load available tokens for the network
                loadAvailableTokens()
            }
        }
        
        // Load initial tokens
        loadAvailableTokens()
    }
    
    fun setTokenA(token: TokenInfo) {
        _uiState.value = _uiState.value.copy(tokenA = token)
        loadTokenBalances()
        checkPoolExists()
        if (_uiState.value.tokenAAmount.isNotEmpty()) {
            fetchQuoteDebounced()
        }
    }
    
    fun setTokenB(token: TokenInfo) {
        _uiState.value = _uiState.value.copy(tokenB = token)
        loadTokenBalances()
        checkPoolExists()
        if (_uiState.value.tokenAAmount.isNotEmpty()) {
            fetchQuoteDebounced()
        }
    }
    
    fun swapTokens() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            tokenA = currentState.tokenB,
            tokenB = currentState.tokenA,
            tokenAAmount = currentState.tokenBAmount,
            tokenBAmount = currentState.tokenAAmount,
            tokenABalance = currentState.tokenBBalance,
            tokenBBalance = currentState.tokenABalance,
            quote = null
        )
        
        if (_uiState.value.tokenAAmount.isNotEmpty()) {
            fetchQuoteDebounced()
        }
    }
    
    fun updateTokenAAmount(amount: String) {
        // Filter out invalid characters - only allow digits, decimal point, and commas
        val filteredAmount = amount.filter { it.isDigit() || it == '.' || it == ',' }
        
        // Prevent multiple decimal points
        val dotCount = filteredAmount.count { it == '.' }
        val validAmount = if (dotCount > 1) {
            val firstDotIndex = filteredAmount.indexOf('.')
            filteredAmount.substring(0, firstDotIndex + 1) + 
            filteredAmount.substring(firstDotIndex + 1).replace(".", "")
        } else {
            filteredAmount
        }
        
        // Validate input
        val cleanAmount = validAmount.replace(",", "")
        if (cleanAmount.isEmpty() || cleanAmount == "." || cleanAmount == "0.") {
            _uiState.value = _uiState.value.copy(
                tokenAAmount = validAmount,
                tokenBAmount = "",
                quote = null,
                insufficientBalance = false
            )
            return
        }
        
        // Try to parse as number - if it fails, don't update
        val parsedDouble = cleanAmount.toDoubleOrNull()
        if (parsedDouble == null || parsedDouble < 0) {
            // Don't update if invalid
            return
        }
        
        _uiState.value = _uiState.value.copy(tokenAAmount = validAmount)
        
        // Check balance safely
        try {
            val amountBigDecimal = BigDecimal(cleanAmount)
            val insufficientBalance = amountBigDecimal > _uiState.value.tokenABalance
            _uiState.value = _uiState.value.copy(insufficientBalance = insufficientBalance)
            
            // Fetch quote
            if (!insufficientBalance && amountBigDecimal > BigDecimal.ZERO) {
                fetchQuoteDebounced()
            }
        } catch (e: NumberFormatException) {
            // If parsing fails, just don't update balance check or fetch quote
            return
        }
    }
    
    fun updateSlippage(slippagePercent: Double, isCustom: Boolean) {
        _uiState.value = _uiState.value.copy(
            slippagePercent = slippagePercent,
            useCustomSlippage = isCustom
        )
        
        // Refetch quote with new slippage
        if (_uiState.value.tokenAAmount.isNotEmpty()) {
            fetchQuoteDebounced()
        }
    }
    
    private fun fetchQuoteDebounced() {
        quoteJob?.cancel()
        quoteJob = viewModelScope.launch {
            delay(QUOTE_DEBOUNCE_MS)
            fetchQuote()
        }
    }
    
    private suspend fun fetchQuote() {
        val state = _uiState.value
        val tokenA = state.tokenA ?: return
        val tokenB = state.tokenB ?: return
        val amountIn = state.tokenAAmount.replace(",", "").toDoubleOrNull() ?: return
        
        if (amountIn <= 0) return
        
        _uiState.value = _uiState.value.copy(isLoadingQuote = true, errorMessage = null)
        
        try {
            // Sort tokens to get X and Y
            val (tokenX, tokenY) = swapRepository.sortTokenAddresses(tokenA.address, tokenB.address)
            val isXtoY = tokenB.address == tokenX
            
            val result = swapRepository.getSwapQuote(
                amountIn = amountIn,
                isXtoY = isXtoY,
                slippage = state.slippagePercent,
                tokenXMint = tokenX,
                tokenYMint = tokenY
            )
            
            result.onSuccess { quote ->
                _uiState.value = _uiState.value.copy(
                    quote = quote,
                    tokenBAmount = formatAmount(quote.amountOut, tokenB.decimals),
                    isLoadingQuote = false,
                    priceImpactWarning = quote.priceImpactPercentage > 5.0
                )
            }
            
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingQuote = false,
                    errorMessage = "Failed to get quote: ${error.message}"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoadingQuote = false,
                errorMessage = "Failed to get quote: ${e.message}"
            )
        }
    }
    
    fun executeSwap() {
        viewModelScope.launch {
            performSwap()
        }
    }
    
    private suspend fun performSwap() {
        val state = _uiState.value
        val tokenA = state.tokenA ?: return
        val tokenB = state.tokenB ?: return
        val quote = state.quote ?: return
        
        val amountIn = state.tokenAAmount.replace(",", "").toDoubleOrNull() ?: return
        if (amountIn <= 0) return
        
        // Generate new tracking ID for this swap
        currentTrackingId = generateTrackingId()
        
        try {
            // Step 1: Generate proof
            _uiState.value = _uiState.value.copy(
                isSwapping = true,
                swapStep = SwapStep.GENERATING_PROOF,
                errorMessage = null,
                trackingDetails = TrackingDetails(currentTrackingId, "")
            )
            
            val wallet = storageManager.getWallet().getOrNull() ?: throw Exception("Wallet not found")
            val (tokenX, tokenY) = swapRepository.sortTokenAddresses(tokenA.address, tokenB.address)
            val isXtoY = tokenB.address == tokenX
            
            // Calculate min output with slippage
            val minOut = if (state.useCustomSlippage) {
                swapRepository.calculateMinOutput(quote.amountOut, state.slippagePercent)
            } else {
                quote.amountOut
            }
            
            // Pass human-readable amounts - SwapRepository will handle decimal conversion
            val swapResult = swapRepository.createSwapTransaction(
                amountIn = amountIn, // Keep as human-readable amount
                isSwapXtoY = isXtoY,
                minOut = minOut, // Keep as human-readable amount  
                tokenMintX = tokenX,
                tokenMintY = tokenY,
                userAddress = wallet.publicKey,
                trackingId = currentTrackingId
            )
            
            swapResult.onSuccess { swapResponse ->
                if (!swapResponse.success) {
                    throw Exception(swapResponse.error ?: "Failed to create swap transaction")
                }
                
                // Step 2: Sign transaction
                _uiState.value = _uiState.value.copy(
                    swapStep = SwapStep.CONFIRM_TRANSACTION,
                    trackingDetails = TrackingDetails(
                        trackingId = currentTrackingId,
                        tradeId = swapResponse.tradeId
                    )
                )
                
                // Sign the transaction using SolanaKT
                val signedTransactionBase64 = try {
                    transactionService.signTransaction(swapResponse.unsignedTransaction, wallet)
                } catch (e: Exception) {
                    throw Exception("Failed to sign transaction: ${e.message}")
                }
                
                // Step 3: Submit signed transaction
                _uiState.value = _uiState.value.copy(swapStep = SwapStep.PROCESSING)
                
                val submitResult = swapRepository.submitSignedTransaction(
                    signedTransaction = signedTransactionBase64,
                    trackingId = currentTrackingId,
                    tradeId = swapResponse.tradeId
                )
                
                submitResult.onSuccess { submitResponse ->
                    if (submitResponse.success) {
                        // Poll for trade status
                        pollTradeStatus(currentTrackingId, swapResponse.tradeId)
                    } else {
                        throw Exception("${submitResponse.error ?: "Failed to submit transaction"}\nTracking ID: $currentTrackingId")
                    }
                }
                
                submitResult.onFailure { error ->
                    throw error
                }
            }
            
            swapResult.onFailure { error ->
                throw error
            }
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isSwapping = false,
                swapStep = SwapStep.FAILED,
                errorMessage = "Swap failed: ${e.message}\n\nTracking ID: $currentTrackingId\n(Long press to copy)",
                trackingDetails = TrackingDetails(currentTrackingId, _uiState.value.trackingDetails?.tradeId ?: "")
            )
        }
    }
    
    private suspend fun pollTradeStatus(trackingId: String, tradeId: String) {
        val result = swapRepository.pollTradeStatus(trackingId, tradeId)
        
        result.onSuccess { statusResponse ->
            when (statusResponse.status) {
                TradeStatus.SETTLED, TradeStatus.SLASHED -> {
                    _uiState.value = _uiState.value.copy(
                        isSwapping = false,
                        swapStep = SwapStep.COMPLETED,
                        successMessage = "Swap completed successfully!",
                        tokenAAmount = "",
                        tokenBAmount = "",
                        quote = null
                    )
                    // Reload balances
                    loadTokenBalances()
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isSwapping = false,
                        swapStep = SwapStep.FAILED,
                        errorMessage = "Trade ${statusResponse.status}: ${statusResponse.message}"
                    )
                }
            }
        }
        
        result.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                isSwapping = false,
                swapStep = SwapStep.FAILED,
                errorMessage = error.message
            )
        }
    }
    
    private fun loadTokenBalances() {
        viewModelScope.launch {
            val wallet = storageManager.getWallet().getOrNull() ?: return@launch
            val tokenA = _uiState.value.tokenA
            val tokenB = _uiState.value.tokenB
            
            // SOL mint address (same on all networks)
            val SOL_MINT = "So11111111111111111111111111111111111111112"
            
            // Fetch balances from centralized repository
            // This will use cached data when available
            balanceRepository.fetchSolBalance(wallet.publicKey)
            balanceRepository.fetchTokens(wallet.publicKey)
            
            // Load token A balance
            if (tokenA != null) {
                if (tokenA.address == SOL_MINT) {
                    // Get SOL balance from repository (already in SOL units)
                    val solBalance = balanceRepository.solBalance.value
                    _uiState.value = _uiState.value.copy(
                        tokenABalance = BigDecimal(solBalance)
                    )
                } else {
                    // Get SPL token balance from repository
                    val tokenInfo = balanceRepository.getTokenByMint(tokenA.address)
                    _uiState.value = _uiState.value.copy(
                        tokenABalance = BigDecimal(tokenInfo?.balance?.uiAmount ?: 0.0)
                    )
                }
            }
            
            // Load token B balance
            if (tokenB != null) {
                if (tokenB.address == SOL_MINT) {
                    // Get SOL balance from repository (already in SOL units)
                    val solBalance = balanceRepository.solBalance.value
                    _uiState.value = _uiState.value.copy(
                        tokenBBalance = BigDecimal(solBalance)
                    )
                } else {
                    // Get SPL token balance from repository
                    val tokenInfo = balanceRepository.getTokenByMint(tokenB.address)
                    _uiState.value = _uiState.value.copy(
                        tokenBBalance = BigDecimal(tokenInfo?.balance?.uiAmount ?: 0.0)
                    )
                }
            }
        }
    }
    
    private fun checkPoolExists() {
        viewModelScope.launch {
            val tokenA = _uiState.value.tokenA ?: return@launch
            val tokenB = _uiState.value.tokenB ?: return@launch
            
            // Use the new PoolRepository for deterministic pool checking
            val poolExists = poolRepository.poolExistsLocally(
                tokenA.address, 
                tokenB.address, 
                settingsManager.networkSettings.value.network
            )
            
            _uiState.value = _uiState.value.copy(poolExists = poolExists)
        }
    }
    
    fun resetSwap() {
        _uiState.value = _uiState.value.copy(
            swapStep = SwapStep.IDLE,
            isSwapping = false,
            trackingDetails = null
        )
    }
    
    private fun formatAmount(amount: Double, decimals: Int): String {
        return BigDecimal(amount)
            .setScale(minOf(decimals, 6), RoundingMode.DOWN)
            .stripTrailingZeros()
            .toPlainString()
    }
    
    
    // Token Selection Methods
    fun showTokenSelection(type: TokenSelectionType) {
        _uiState.value = _uiState.value.copy(
            showTokenSelection = true,
            tokenSelectionType = type
        )
    }
    
    fun hideTokenSelection() {
        _uiState.value = _uiState.value.copy(
            showTokenSelection = false,
            tokenSelectionType = null
        )
    }
    
    fun selectToken(token: fi.darklake.wallet.data.swap.models.Token) {
        val selectionType = _uiState.value.tokenSelectionType ?: return
        val tokenInfo = tokenRepository.convertToTokenInfo(token)
        
        when (selectionType) {
            TokenSelectionType.TOKEN_A -> {
                // Prevent selecting the same token as Token B
                if (token.address != _uiState.value.tokenB?.address) {
                    setTokenA(tokenInfo)
                }
            }
            TokenSelectionType.TOKEN_B -> {
                // Prevent selecting the same token as Token A
                if (token.address != _uiState.value.tokenA?.address) {
                    setTokenB(tokenInfo)
                }
            }
        }
        
        hideTokenSelection()
    }
    
    private fun loadAvailableTokens() {
        viewModelScope.launch {
            tokenRepository.getTokens(
                network = settingsManager.networkSettings.value.network,
                query = "",
                limit = 100
            ).collect { tokens ->
                _uiState.value = _uiState.value.copy(availableTokens = tokens)
            }
        }
    }
    
    fun loadTokens() {
        loadAvailableTokens()
    }
    
    fun refreshQuote() {
        viewModelScope.launch {
            fetchQuote()
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        transactionService.close()
    }
}