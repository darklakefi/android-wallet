package fi.darklake.wallet.ui.screens.swap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.data.swap.SwapRepository
import fi.darklake.wallet.data.swap.models.*
import fi.darklake.wallet.data.swap.repository.TokenRepository
import fi.darklake.wallet.data.swap.repository.PoolRepository
import fi.darklake.wallet.data.api.SolanaApiService
import fi.darklake.wallet.data.solana.SolanaKTTransactionService
import fi.darklake.wallet.storage.WalletStorageManager
import com.solana.core.HotAccount
import com.solana.core.Transaction
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.util.Base64
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
    private val settingsManager: SettingsManager
) : ViewModel() {
    
    private val swapRepository: SwapRepository
        get() = SwapRepository(settingsManager.networkSettings.value)
        
    private val tokenRepository = TokenRepository()
    private val poolRepository = PoolRepository()
    private val transactionService = SolanaKTTransactionService(settingsManager)
        
    private val solanaApiService = SolanaApiService {
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
            
            // Convert amounts to raw values with decimals
            val amountInRaw = convertToRawAmount(amountIn, tokenA.decimals)
            val minOutRaw = convertToRawAmount(minOut, tokenB.decimals)
            
            val swapResult = swapRepository.createSwapTransaction(
                amountIn = amountInRaw,
                isSwapXtoY = isXtoY,
                minOut = minOutRaw,
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
                    signTransaction(swapResponse.unsignedTransaction, wallet.privateKey)
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
            
            // Load token A balance
            if (tokenA != null) {
                if (tokenA.address == SOL_MINT) {
                    // Get SOL balance
                    val balanceResult = solanaApiService.getBalance(wallet.publicKey)
                    balanceResult.onSuccess { balance ->
                        _uiState.value = _uiState.value.copy(
                            tokenABalance = BigDecimal(balance).divide(BigDecimal(1_000_000_000), 9, RoundingMode.DOWN)
                        )
                    }
                } else {
                    // Get SPL token balance
                    val tokensResult = solanaApiService.getTokenAccounts(wallet.publicKey)
                    tokensResult.onSuccess { tokens ->
                        val tokenInfo = tokens.find { it.balance.mint == tokenA.address }
                        _uiState.value = _uiState.value.copy(
                            tokenABalance = BigDecimal(tokenInfo?.balance?.uiAmount ?: 0.0)
                        )
                    }
                }
            }
            
            // Load token B balance
            if (tokenB != null) {
                if (tokenB.address == SOL_MINT) {
                    // Get SOL balance
                    val balanceResult = solanaApiService.getBalance(wallet.publicKey)
                    balanceResult.onSuccess { balance ->
                        _uiState.value = _uiState.value.copy(
                            tokenBBalance = BigDecimal(balance).divide(BigDecimal(1_000_000_000), 9, RoundingMode.DOWN)
                        )
                    }
                } else {
                    // Get SPL token balance
                    val tokensResult = solanaApiService.getTokenAccounts(wallet.publicKey)
                    tokensResult.onSuccess { tokens ->
                        val tokenInfo = tokens.find { it.balance.mint == tokenB.address }
                        _uiState.value = _uiState.value.copy(
                            tokenBBalance = BigDecimal(tokenInfo?.balance?.uiAmount ?: 0.0)
                        )
                    }
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
    
    private fun convertToRawAmount(amount: Double, decimals: Int): Double {
        return BigDecimal(amount)
            .multiply(BigDecimal(10).pow(decimals))
            .toDouble()
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
    
    /**
     * Signs a transaction using SolanaKT libraries
     * @param unsignedTransactionBase64 The unsigned transaction as base64 string
     * @param privateKey The wallet's private key
     * @return The signed transaction as base64 string
     */
    private suspend fun signTransaction(
        unsignedTransactionBase64: String,
        privateKey: ByteArray
    ): String = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("SwapViewModel", "Starting transaction signing")
            android.util.Log.d("SwapViewModel", "- Unsigned transaction length: ${unsignedTransactionBase64.length}")
            android.util.Log.d("SwapViewModel", "- Unsigned transaction preview: ${unsignedTransactionBase64.take(100)}...")
            
            // Decode the unsigned transaction from base64
            val transactionBytes = Base64.decode(unsignedTransactionBase64, Base64.DEFAULT)
            
            android.util.Log.d("SwapViewModel", "- Raw transaction bytes length: ${transactionBytes.size}")
            android.util.Log.d("SwapViewModel", "- First few bytes: ${transactionBytes.take(10).joinToString { "%02x".format(it) }}")
            
            // Check if this is a versioned transaction (starts with 0x80) or legacy transaction
            val isVersioned = transactionBytes.isNotEmpty() && (transactionBytes[0].toInt() and 0x80) != 0
            
            android.util.Log.d("SwapViewModel", "- Is versioned transaction: $isVersioned")
            
            val signedTransactionBase64 = if (isVersioned) {
                // Handle versioned transaction (keep existing logic)
                val version = transactionBytes[0].toInt() and 0x7f
                val numSignatures = transactionBytes[1].toInt() and 0xff
                val messageStart = 2 + (numSignatures * 64)
                val message = transactionBytes.sliceArray(messageStart until transactionBytes.size)
                
                val account = if (privateKey.size == 32) {
                    val keypair = com.solana.vendor.TweetNaclFast.Signature.keyPair_fromSeed(privateKey)
                    HotAccount(keypair.secretKey)
                } else {
                    HotAccount(privateKey)
                }
                
                val signature = account.sign(message)
                val signedTxSize = 1 + 1 + 64 + message.size
                val signedTransaction = ByteArray(signedTxSize)
                var offset = 0
                
                signedTransaction[offset++] = (0x80 or version).toByte()
                signedTransaction[offset++] = 1
                System.arraycopy(signature, 0, signedTransaction, offset, 64)
                offset += 64
                System.arraycopy(message, 0, signedTransaction, offset, message.size)
                
                Base64.encodeToString(signedTransaction, Base64.NO_WRAP)
            } else {
                // Handle legacy transaction (like dex-web)
                android.util.Log.d("SwapViewModel", "Processing as legacy transaction")
                
                val transaction = Transaction.from(transactionBytes)
                
                // Create account from private key
                val account = if (privateKey.size == 32) {
                    val keypair = com.solana.vendor.TweetNaclFast.Signature.keyPair_fromSeed(privateKey)
                    HotAccount(keypair.secretKey)
                } else {
                    HotAccount(privateKey)
                }
                
                // Sign the transaction
                transaction.sign(account)
                
                // Serialize and encode back to base64
                Base64.encodeToString(transaction.serialize(), Base64.NO_WRAP)
            }
            
            android.util.Log.d("SwapViewModel", "Transaction signing completed")
            android.util.Log.d("SwapViewModel", "- Signed transaction length: ${signedTransactionBase64.length}")
            android.util.Log.d("SwapViewModel", "- Signed transaction preview: ${signedTransactionBase64.take(100)}...")
            
            signedTransactionBase64
        } catch (e: Exception) {
            android.util.Log.e("SwapViewModel", "Transaction signing failed", e)
            throw Exception("Failed to sign versioned transaction: ${e.message}")
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        transactionService.close()
    }
}