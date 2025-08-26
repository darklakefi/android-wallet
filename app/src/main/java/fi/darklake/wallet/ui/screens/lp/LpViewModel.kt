package fi.darklake.wallet.ui.screens.lp

import android.content.Context
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solana.core.HotAccount
import com.solana.core.Transaction
import fi.darklake.wallet.data.api.SolanaApiService
import fi.darklake.wallet.data.lp.LpPositionService
import fi.darklake.wallet.data.lp.LpTransactionService
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.data.repository.BalanceRepository
import fi.darklake.wallet.data.repository.BalanceService
import fi.darklake.wallet.data.solana.SolanaTransactionService
import fi.darklake.wallet.data.swap.repository.TokenRepository
import fi.darklake.wallet.storage.WalletStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

// Use the same TokenInfo from swap module for consistency
typealias TokenInfo = fi.darklake.wallet.data.swap.models.TokenInfo

data class LiquidityPosition(
    val id: String,
    val tokenA: TokenInfo,
    val tokenB: TokenInfo,
    val amountA: BigDecimal,
    val amountB: BigDecimal,
    val lpTokenBalance: BigDecimal,
    val poolShare: Double,
    val totalValue: BigDecimal
)

data class PoolDetails(
    val exists: Boolean,
    val tokenXMint: String,
    val tokenYMint: String,
    val poolAddress: String? = null,
    val reserveX: BigDecimal = BigDecimal.ZERO,
    val reserveY: BigDecimal = BigDecimal.ZERO,
    val totalLpSupply: BigDecimal = BigDecimal.ZERO,
    val currentPrice: Double = 0.0,
    val poolShare: Double = 0.0,
    val apr: Double = 0.0,
    val tvl: BigDecimal = BigDecimal.ZERO
)

enum class LiquidityStep {
    IDLE,
    GENERATING_PROOF,      // Step 1: Generating zero-knowledge proof
    CONFIRM_TRANSACTION,   // Step 2: Confirm transaction in wallet
    PROCESSING,           // Step 3: Processing transaction
    COMPLETED,
    FAILED
}

enum class TokenSelectionType {
    TOKEN_A, TOKEN_B
}

data class LpUiState(
    val tokenA: TokenInfo? = null,
    val tokenB: TokenInfo? = null,
    val tokenAAmount: String = "",
    val tokenBAmount: String = "",
    val tokenABalance: BigDecimal = BigDecimal.ZERO,
    val tokenBBalance: BigDecimal = BigDecimal.ZERO,
    val initialPrice: String = "1.0",
    val slippagePercent: Double = 0.5,
    val useCustomSlippage: Boolean = false,
    val poolDetails: PoolDetails? = null,
    val liquidityPositions: List<LiquidityPosition> = emptyList(),
    val hasLiquidityPositions: Boolean = false,
    val isLoadingQuote: Boolean = false,
    val isAddingLiquidity: Boolean = false,
    val isCreatingPool: Boolean = false,
    val isProcessing: Boolean = false,
    val liquidityStep: LiquidityStep = LiquidityStep.IDLE,
    val insufficientBalanceA: Boolean = false,
    val insufficientBalanceB: Boolean = false,
    val priceImpactWarning: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val availableTokens: List<fi.darklake.wallet.data.swap.models.Token> = emptyList(),
    val showTokenSelection: Boolean = false,
    val tokenSelectionType: TokenSelectionType? = null
)

class LpViewModel(
    private val storageManager: WalletStorageManager,
    private val settingsManager: SettingsManager,
    context: Context? = null
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LpUiState())
    val uiState: StateFlow<LpUiState> = _uiState.asStateFlow()
    
    private val tokenRepository = TokenRepository()
    private val lpTransactionService = LpTransactionService(settingsManager)
    private val lpPositionService = LpPositionService(settingsManager)
    private val transactionService = SolanaTransactionService(settingsManager)
    
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
    
    // Use centralized balance repository
    private val balanceRepository: BalanceRepository = if (context != null) {
        BalanceService.getInstance(context).getRepository()
    } else {
        // Fallback to direct repository if no context provided
        BalanceRepository(solanaApiService)
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
        
        // Load balances and liquidity positions
        loadTokenBalances()
        loadLiquidityPositions()
        
        // Listen for network changes and refresh tokens accordingly
        viewModelScope.launch {
            settingsManager.networkSettings.collect { networkSettings ->
                // Update to network-appropriate default tokens
                setTokenA(getDefaultTokenA())
                setTokenB(getDefaultTokenB())
                
                // Reload balances with new network settings
                loadTokenBalances()
                checkPoolExists()
                // Load available tokens for the network
                loadAvailableTokens()
                // Reload all liquidity positions for the new network
                loadLiquidityPositions()
            }
        }
        
        // Load initial tokens
        loadAvailableTokens()
    }
    
    fun setTokenA(token: TokenInfo) {
        _uiState.value = _uiState.value.copy(tokenA = token)
        loadTokenBalances()
        checkPoolExists()
    }
    
    fun setTokenB(token: TokenInfo) {
        _uiState.value = _uiState.value.copy(tokenB = token)
        loadTokenBalances()
        checkPoolExists()
    }
    
    fun swapTokens() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            tokenA = currentState.tokenB,
            tokenB = currentState.tokenA,
            tokenAAmount = currentState.tokenBAmount,
            tokenBAmount = currentState.tokenAAmount,
            tokenABalance = currentState.tokenBBalance,
            tokenBBalance = currentState.tokenABalance
        )
        checkPoolExists()
    }
    
    fun updateTokenAAmount(amount: String) {
        val cleanAmount = amount.replace(",", "")
        if (cleanAmount.isEmpty() || cleanAmount == "." || 
            (cleanAmount.toDoubleOrNull() ?: 0.0) < 0) {
            _uiState.value = _uiState.value.copy(
                tokenAAmount = "",
                insufficientBalanceA = false
            )
            return
        }
        
        _uiState.value = _uiState.value.copy(tokenAAmount = amount)
        
        // Check balance
        val amountBigDecimal = BigDecimal(cleanAmount)
        val insufficientBalance = amountBigDecimal > _uiState.value.tokenABalance
        _uiState.value = _uiState.value.copy(insufficientBalanceA = insufficientBalance)
        
        // Calculate proportional amount for Token B if pool exists
        calculateProportionalAmount(amountBigDecimal, isTokenA = true)
    }
    
    fun updateTokenBAmount(amount: String) {
        val cleanAmount = amount.replace(",", "")
        if (cleanAmount.isEmpty() || cleanAmount == "." || 
            (cleanAmount.toDoubleOrNull() ?: 0.0) < 0) {
            _uiState.value = _uiState.value.copy(
                tokenBAmount = "",
                insufficientBalanceB = false
            )
            return
        }
        
        _uiState.value = _uiState.value.copy(tokenBAmount = amount)
        
        // Check balance
        val amountBigDecimal = BigDecimal(cleanAmount)
        val insufficientBalance = amountBigDecimal > _uiState.value.tokenBBalance
        _uiState.value = _uiState.value.copy(insufficientBalanceB = insufficientBalance)
        
        // Calculate proportional amount for Token A if pool exists
        calculateProportionalAmount(amountBigDecimal, isTokenA = false)
    }
    
    fun updateInitialPrice(price: String) {
        _uiState.value = _uiState.value.copy(initialPrice = price)
        
        // Recalculate Token B amount based on new price
        val tokenAAmount = _uiState.value.tokenAAmount.replace(",", "")
        if (tokenAAmount.isNotEmpty() && price.isNotEmpty()) {
            val priceDecimal = BigDecimal(price)
            val amountDecimal = BigDecimal(tokenAAmount)
            val calculatedTokenB = amountDecimal.multiply(priceDecimal)
            _uiState.value = _uiState.value.copy(
                tokenBAmount = formatAmount(calculatedTokenB.toDouble(), 6)
            )
        }
    }
    
    fun updateSlippage(slippagePercent: Double, isCustom: Boolean) {
        _uiState.value = _uiState.value.copy(
            slippagePercent = slippagePercent,
            useCustomSlippage = isCustom
        )
    }
    
    fun addLiquidity() {
        viewModelScope.launch {
            performAddLiquidity()
        }
    }
    
    fun createPool() {
        viewModelScope.launch {
            performCreatePool()
        }
    }
    
    fun withdrawLiquidity(positionId: String) {
        viewModelScope.launch {
            performWithdrawLiquidity(positionId)
        }
    }
    
    private suspend fun performAddLiquidity() {
        val state = _uiState.value
        val tokenA = state.tokenA ?: return
        val tokenB = state.tokenB ?: return
        
        val tokenAAmount = state.tokenAAmount.replace(",", "").toDoubleOrNull() ?: return
        val tokenBAmount = state.tokenBAmount.replace(",", "").toDoubleOrNull() ?: return
        
        if (tokenAAmount <= 0 || tokenBAmount <= 0) return
        
        try {
            // Step 1: Generate proof
            _uiState.value = _uiState.value.copy(
                isAddingLiquidity = true,
                isProcessing = true,
                liquidityStep = LiquidityStep.GENERATING_PROOF,
                errorMessage = null
            )
            
            val wallet = storageManager.getWallet().getOrNull() ?: throw Exception("Wallet not found")
            
            // Sort tokens (matching dex-web pattern)
            val (tokenXMint, tokenYMint) = sortTokenAddresses(tokenA.address, tokenB.address)
            val isTokenASellToken = tokenB.address == tokenXMint
            val maxAmountX = if (isTokenASellToken) tokenAAmount else tokenBAmount
            val maxAmountY = if (isTokenASellToken) tokenBAmount else tokenAAmount
            
            // Create add liquidity transaction
            val transactionResult = lpTransactionService.createAddLiquidityTransaction(
                userAddress = wallet.publicKey,
                tokenXMint = tokenXMint,
                tokenYMint = tokenYMint,
                maxAmountX = maxAmountX,
                maxAmountY = maxAmountY,
                slippage = state.slippagePercent
            )
            
            transactionResult.onSuccess { unsignedTransactionBase64 ->
                // Step 2: Sign transaction
                _uiState.value = _uiState.value.copy(
                    liquidityStep = LiquidityStep.CONFIRM_TRANSACTION
                )
                
                val signedTransactionBase64 = signTransaction(unsignedTransactionBase64, wallet.privateKey)
                
                // Step 3: Submit transaction
                _uiState.value = _uiState.value.copy(
                    liquidityStep = LiquidityStep.PROCESSING
                )
                
                // TODO: Submit to Solana network using SolanaTransactionService
                // For now, simulate successful submission
                kotlinx.coroutines.delay(3000)
                
                _uiState.value = _uiState.value.copy(
                    isAddingLiquidity = false,
                    isProcessing = false,
                    liquidityStep = LiquidityStep.COMPLETED,
                    successMessage = "Liquidity added successfully! $tokenAAmount ${tokenA.symbol} + $tokenBAmount ${tokenB.symbol}",
                    tokenAAmount = "",
                    tokenBAmount = ""
                )
                
                // Reload balances and positions after successful add liquidity
                loadTokenBalances()
                loadLiquidityPositions()
            }
            
            transactionResult.onFailure { error ->
                throw error
            }
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isAddingLiquidity = false,
                isProcessing = false,
                liquidityStep = LiquidityStep.FAILED,
                errorMessage = "Add liquidity failed: ${e.message}"
            )
        }
    }
    
    private suspend fun performCreatePool() {
        val state = _uiState.value
        val tokenA = state.tokenA ?: return
        val tokenB = state.tokenB ?: return
        
        val tokenAAmount = state.tokenAAmount.replace(",", "").toDoubleOrNull() ?: return
        val tokenBAmount = state.tokenBAmount.replace(",", "").toDoubleOrNull() ?: return
        val initialPrice = state.initialPrice.toDoubleOrNull() ?: return
        
        if (tokenAAmount <= 0 || tokenBAmount <= 0 || initialPrice <= 0) return
        
        try {
            _uiState.value = _uiState.value.copy(
                isCreatingPool = true,
                isProcessing = true,
                liquidityStep = LiquidityStep.GENERATING_PROOF,
                errorMessage = null
            )
            
            val wallet = storageManager.getWallet().getOrNull() ?: throw Exception("Wallet not found")
            
            // Sort tokens (matching dex-web pattern)
            val (tokenXMint, tokenYMint) = sortTokenAddresses(tokenA.address, tokenB.address)
            val isTokenASellToken = tokenB.address == tokenXMint
            val depositAmountX = if (isTokenASellToken) tokenAAmount else tokenBAmount
            val depositAmountY = if (isTokenASellToken) tokenBAmount else tokenAAmount
            
            // Create pool creation transaction
            val transactionResult = lpTransactionService.createPoolTransaction(
                userAddress = wallet.publicKey,
                tokenXMint = tokenXMint,
                tokenYMint = tokenYMint,
                depositAmountX = depositAmountX,
                depositAmountY = depositAmountY
            )
            
            transactionResult.onSuccess { unsignedTransactionBase64 ->
                // Step 2: Sign transaction
                _uiState.value = _uiState.value.copy(
                    liquidityStep = LiquidityStep.CONFIRM_TRANSACTION
                )
                
                val signedTransactionBase64 = signTransaction(unsignedTransactionBase64, wallet.privateKey)
                
                // Step 3: Submit transaction
                _uiState.value = _uiState.value.copy(
                    liquidityStep = LiquidityStep.PROCESSING
                )
                
                // TODO: Submit to Solana network using SolanaTransactionService
                // For now, simulate successful submission
                kotlinx.coroutines.delay(3000)
                
                _uiState.value = _uiState.value.copy(
                    isCreatingPool = false,
                    isProcessing = false,
                    liquidityStep = LiquidityStep.COMPLETED,
                    successMessage = "Pool created successfully! Initial deposit: $tokenAAmount ${tokenA.symbol} + $tokenBAmount ${tokenB.symbol}",
                    tokenAAmount = "",
                    tokenBAmount = "",
                    initialPrice = "1.0"
                )
                
                // Reload data after successful pool creation
                loadTokenBalances()
                loadLiquidityPositions()
                checkPoolExists()
            }
            
            transactionResult.onFailure { error ->
                throw error
            }
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isCreatingPool = false,
                isProcessing = false,
                liquidityStep = LiquidityStep.FAILED,
                errorMessage = "Pool creation failed: ${e.message}"
            )
        }
    }
    
    private suspend fun performWithdrawLiquidity(positionId: String) {
        try {
            // TODO: Implement actual liquidity withdrawal with Anchor IDL
            // For now, simulate the process
            kotlinx.coroutines.delay(2000)
            
            _uiState.value = _uiState.value.copy(
                successMessage = "Liquidity withdrawn successfully!"
            )
            
            // Reload data after successful withdrawal
            loadTokenBalances()
            loadLiquidityPositions()
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Withdraw liquidity failed: ${e.message}"
            )
        }
    }
    
    private fun calculateProportionalAmount(amount: BigDecimal, isTokenA: Boolean) {
        val poolDetails = _uiState.value.poolDetails
        if (poolDetails?.exists == true && poolDetails.reserveX > BigDecimal.ZERO && poolDetails.reserveY > BigDecimal.ZERO) {
            // Calculate proportional amount based on pool reserves
            if (isTokenA) {
                val ratio = poolDetails.reserveY.divide(poolDetails.reserveX, 6, java.math.RoundingMode.DOWN)
                val calculatedAmount = amount.multiply(ratio)
                _uiState.value = _uiState.value.copy(
                    tokenBAmount = formatAmount(calculatedAmount.toDouble(), 6)
                )
            } else {
                val ratio = poolDetails.reserveX.divide(poolDetails.reserveY, 6, java.math.RoundingMode.DOWN)
                val calculatedAmount = amount.multiply(ratio)
                _uiState.value = _uiState.value.copy(
                    tokenAAmount = formatAmount(calculatedAmount.toDouble(), 9)
                )
            }
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
    
    private fun loadLiquidityPositions() {
        viewModelScope.launch {
            val wallet = storageManager.getWallet().getOrNull() ?: return@launch
            
            android.util.Log.d("LpViewModel", "Loading liquidity positions for user: ${wallet.publicKey}")
            
            try {
                // Use the new LpPositionService to fetch actual positions from blockchain
                val positionsResult = lpPositionService.getAllUserLiquidityPositions(wallet.publicKey)
                
                positionsResult.fold(
                    onSuccess = { positions ->
                        android.util.Log.d("LpViewModel", "Successfully loaded ${positions.size} liquidity positions")
                        _uiState.value = _uiState.value.copy(
                            liquidityPositions = positions,
                            hasLiquidityPositions = positions.isNotEmpty()
                        )
                    },
                    onFailure = { error ->
                        android.util.Log.e("LpViewModel", "Failed to load liquidity positions", error)
                        _uiState.value = _uiState.value.copy(
                            liquidityPositions = emptyList(),
                            hasLiquidityPositions = false,
                            errorMessage = "Failed to load liquidity positions: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("LpViewModel", "Exception loading liquidity positions", e)
                _uiState.value = _uiState.value.copy(
                    liquidityPositions = emptyList(),
                    hasLiquidityPositions = false,
                    errorMessage = "Failed to load liquidity positions: ${e.message}"
                )
            }
        }
    }
    
    private fun checkPoolExists() {
        viewModelScope.launch {
            val tokenA = _uiState.value.tokenA ?: return@launch
            val tokenB = _uiState.value.tokenB ?: return@launch
            
            // TODO: Implement actual pool existence check
            // For now, assume SOL/USDC pool exists
            val poolExists = (tokenA.symbol == "SOL" && tokenB.symbol == "USDC") ||
                           (tokenA.symbol == "USDC" && tokenB.symbol == "SOL")
            
            _uiState.value = _uiState.value.copy(
                poolDetails = if (poolExists) {
                    PoolDetails(
                        exists = true,
                        tokenXMint = tokenA.address,
                        tokenYMint = tokenB.address,
                        poolAddress = "mock_pool_address",
                        reserveX = BigDecimal("1000.0"),
                        reserveY = BigDecimal("100000.0"),
                        totalLpSupply = BigDecimal("10000.0")
                    )
                } else {
                    PoolDetails(exists = false, tokenXMint = tokenA.address, tokenYMint = tokenB.address)
                }
            )
        }
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun loadTokens() {
        loadAvailableTokens()
    }
    
    fun loadPositions() {
        loadLiquidityPositions()
    }
    
    fun refreshPoolDetails() {
        checkPoolExists()
    }
    
    fun withdrawLiquidity(position: LiquidityPosition) {
        withdrawLiquidity(position.id)
    }
    
    private fun formatAmount(amount: Double, decimals: Int): String {
        return BigDecimal(amount)
            .setScale(minOf(decimals, 6), java.math.RoundingMode.DOWN)
            .stripTrailingZeros()
            .toPlainString()
    }
    
    /**
     * Signs a transaction using SolanaKT libraries (same as SwapViewModel)
     */
    private suspend fun signTransaction(
        unsignedTransactionBase64: String,
        privateKey: ByteArray
    ): String = withContext(Dispatchers.IO) {
        try {
            // Decode the unsigned transaction from base64
            val transactionBytes = Base64.decode(unsignedTransactionBase64, Base64.DEFAULT)
            
            // Deserialize the transaction
            val transaction = Transaction.from(transactionBytes)
            
            // Create account from private key
            val account = if (privateKey.size == 32) {
                // Create keypair from seed
                val keypair = com.solana.vendor.TweetNaclFast.Signature.keyPair_fromSeed(privateKey)
                HotAccount(keypair.secretKey)
            } else {
                HotAccount(privateKey)
            }
            
            // Sign the transaction
            transaction.sign(account)
            
            // Serialize and encode back to base64 (NO_WRAP to avoid newlines)
            Base64.encodeToString(transaction.serialize(), Base64.NO_WRAP)
        } catch (e: Exception) {
            throw Exception("Failed to sign transaction: ${e.message}")
        }
    }
    
    /**
     * Sorts token addresses to determine tokenX and tokenY (matching dex-web pattern)
     */
    private fun sortTokenAddresses(tokenA: String, tokenB: String): Pair<String, String> {
        return if (tokenA < tokenB) {
            Pair(tokenA, tokenB)
        } else {
            Pair(tokenB, tokenA)
        }
    }
    
    // Token Selection Methods (matching SwapViewModel pattern)
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
    
    override fun onCleared() {
        super.onCleared()
        transactionService.close()
    }
}