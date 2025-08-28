package fi.darklake.wallet.ui.screens.lp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.darklake.wallet.data.api.HeliusApiService
import fi.darklake.wallet.data.lp.LpPositionService
import fi.darklake.wallet.data.lp.LpTransactionService
import fi.darklake.wallet.data.lp.PdaUtils
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.data.repository.BalanceRepository
import fi.darklake.wallet.data.repository.BalanceService
import fi.darklake.wallet.data.solana.SolanaTransactionService
import fi.darklake.wallet.data.swap.models.TokenInfo
import fi.darklake.wallet.data.swap.repository.TokenRepository
import fi.darklake.wallet.storage.WalletStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class LiquidityPosition(
    val id: String,
    val tokenA: TokenInfo,
    val tokenB: TokenInfo,
    val amountA: BigDecimal,
    val amountB: BigDecimal,
    val lpTokenBalance: BigDecimal,
    val poolShare: Double
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
    private val lpTransactionService = LpTransactionService(settingsManager, context)
    private val lpPositionService = LpPositionService(settingsManager)
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
        
        // Load saved slippage settings
        val savedSlippage = settingsManager.getSlippageTolerance()
        _uiState.value = _uiState.value.copy(
            slippagePercent = savedSlippage.toDouble(),
            useCustomSlippage = savedSlippage !in listOf(0.5f, 1.0f, 2.0f)
        )
        
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
                
                val signedTransactionBase64 = transactionService.signTransaction(unsignedTransactionBase64, wallet.privateKey)
                
                // Step 3: Submit transaction
                _uiState.value = _uiState.value.copy(
                    liquidityStep = LiquidityStep.PROCESSING
                )
                
                // Submit to Solana network
                val txSignature = transactionService.submitSignedTransaction(signedTransactionBase64)
                
                if (txSignature != null) {
                    // Wait for confirmation
                    val confirmed = transactionService.waitForConfirmation(txSignature, timeout = 30)
                    
                    if (confirmed) {
                        _uiState.value = _uiState.value.copy(
                            isAddingLiquidity = false,
                            isProcessing = false,
                            liquidityStep = LiquidityStep.COMPLETED,
                            successMessage = "Liquidity added successfully! TX: ${txSignature.take(8)}...",
                            tokenAAmount = "",
                            tokenBAmount = ""
                        )
                        
                        // Reload balances and positions after successful add liquidity
                        loadTokenBalances()
                        loadLiquidityPositions()
                    } else {
                        throw Exception("Transaction confirmation timeout")
                    }
                } else {
                    throw Exception("Failed to submit transaction")
                }
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
                
                val signedTransactionBase64 = transactionService.signTransaction(unsignedTransactionBase64, wallet.privateKey)
                
                // Step 3: Submit transaction
                _uiState.value = _uiState.value.copy(
                    liquidityStep = LiquidityStep.PROCESSING
                )
                
                // Submit to Solana network
                val txSignature = transactionService.submitSignedTransaction(signedTransactionBase64)
                
                if (txSignature != null) {
                    // Wait for confirmation
                    val confirmed = transactionService.waitForConfirmation(txSignature, timeout = 30)
                    
                    if (confirmed) {
                        _uiState.value = _uiState.value.copy(
                            isCreatingPool = false,
                            isProcessing = false,
                            liquidityStep = LiquidityStep.COMPLETED,
                            successMessage = "Pool created successfully! TX: ${txSignature.take(8)}...",
                            tokenAAmount = "",
                            tokenBAmount = "",
                            initialPrice = "1.0"
                        )
                        
                        // Reload data after successful pool creation
                        loadTokenBalances()
                        loadLiquidityPositions()
                        checkPoolExists()
                    } else {
                        throw Exception("Transaction confirmation timeout")
                    }
                } else {
                    throw Exception("Failed to submit transaction")
                }
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
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                liquidityStep = LiquidityStep.GENERATING_PROOF,
                errorMessage = null
            )
            
            val wallet = storageManager.getWallet().getOrNull() ?: throw Exception("Wallet not found")
            
            // Find the position to withdraw
            val position = _uiState.value.liquidityPositions.find { it.id == positionId }
                ?: throw Exception("Position not found")
            
            // Sort tokens to get tokenX and tokenY
            val (tokenXMint, tokenYMint) = sortTokenAddresses(
                position.tokenA.address, 
                position.tokenB.address
            )
            
            // Calculate minimum amounts based on slippage tolerance
            val slippageMultiplier = 1.0 - (_uiState.value.slippagePercent / 100.0)
            val minAmountX = if (position.tokenA.address == tokenXMint) {
                position.amountA.toDouble() * slippageMultiplier
            } else {
                position.amountB.toDouble() * slippageMultiplier
            }
            val minAmountY = if (position.tokenA.address == tokenXMint) {
                position.amountB.toDouble() * slippageMultiplier
            } else {
                position.amountA.toDouble() * slippageMultiplier
            }
            
            // Create withdraw liquidity transaction
            val transactionResult = lpTransactionService.createWithdrawLiquidityTransaction(
                userAddress = wallet.publicKey,
                tokenXMint = tokenXMint,
                tokenYMint = tokenYMint,
                lpTokenAmount = position.lpTokenBalance.toDouble(),
                minAmountX = minAmountX,
                minAmountY = minAmountY
            )
            
            transactionResult.onSuccess { unsignedTransactionBase64 ->
                // Step 2: Sign transaction
                _uiState.value = _uiState.value.copy(
                    liquidityStep = LiquidityStep.CONFIRM_TRANSACTION
                )
                
                val signedTransactionBase64 = transactionService.signTransaction(unsignedTransactionBase64, wallet.privateKey)
                
                // Step 3: Submit transaction
                _uiState.value = _uiState.value.copy(
                    liquidityStep = LiquidityStep.PROCESSING
                )
                
                // Submit to Solana network
                val txSignature = transactionService.submitSignedTransaction(signedTransactionBase64)
                
                if (txSignature != null) {
                    // Wait for confirmation
                    val confirmed = transactionService.waitForConfirmation(txSignature, timeout = 30)
                    
                    if (confirmed) {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            liquidityStep = LiquidityStep.COMPLETED,
                            successMessage = "Successfully withdrawn! TX: ${txSignature.take(8)}..."
                        )
                        
                        // Reload data after successful withdrawal
                        loadTokenBalances()
                        loadLiquidityPositions()
                    } else {
                        throw Exception("Transaction confirmation timeout")
                    }
                } else {
                    throw Exception("Failed to submit transaction")
                }
            }
            
            transactionResult.onFailure { error ->
                throw error
            }
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                liquidityStep = LiquidityStep.FAILED,
                errorMessage = "Withdraw liquidity failed: ${e.message}"
            )
        }
    }
    
    private fun calculateProportionalAmount(amount: BigDecimal, isTokenA: Boolean) {
        val poolDetails = _uiState.value.poolDetails
        
        if (poolDetails?.exists == true && poolDetails.reserveX > BigDecimal.ZERO && poolDetails.reserveY > BigDecimal.ZERO) {
            // For existing pools: Calculate proportional amount based on pool reserves
            if (isTokenA) {
                val ratio = poolDetails.reserveY.divide(poolDetails.reserveX, 6, java.math.RoundingMode.DOWN)
                val calculatedAmount = amount.multiply(ratio)
                _uiState.value = _uiState.value.copy(
                    tokenBAmount = formatAmount(calculatedAmount.toDouble(), 6),
                    insufficientBalanceB = calculatedAmount > _uiState.value.tokenBBalance
                )
            } else {
                val ratio = poolDetails.reserveX.divide(poolDetails.reserveY, 6, java.math.RoundingMode.DOWN)
                val calculatedAmount = amount.multiply(ratio)
                _uiState.value = _uiState.value.copy(
                    tokenAAmount = formatAmount(calculatedAmount.toDouble(), 9),
                    insufficientBalanceA = calculatedAmount > _uiState.value.tokenABalance
                )
            }
        } else if (poolDetails?.exists == false) {
            // For new pools: Use initial price to calculate
            val initialPrice = _uiState.value.initialPrice
            if (initialPrice.isNotEmpty()) {
                try {
                    val price = BigDecimal(initialPrice)
                    if (isTokenA && price > BigDecimal.ZERO) {
                        // TokenA changed, calculate TokenB = TokenA * price
                        val calculatedAmount = amount.multiply(price)
                        _uiState.value = _uiState.value.copy(
                            tokenBAmount = formatAmount(calculatedAmount.toDouble(), 6),
                            insufficientBalanceB = calculatedAmount > _uiState.value.tokenBBalance
                        )
                    } else if (!isTokenA && price > BigDecimal.ZERO) {
                        // TokenB changed, calculate TokenA = TokenB / price
                        val calculatedAmount = amount.divide(price, 9, java.math.RoundingMode.DOWN)
                        _uiState.value = _uiState.value.copy(
                            tokenAAmount = formatAmount(calculatedAmount.toDouble(), 9),
                            insufficientBalanceA = calculatedAmount > _uiState.value.tokenABalance
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.w("LpViewModel", "Error calculating with initial price", e)
                }
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
            
            try {
                // Calculate the pool PDA address using PdaUtils
                val poolPda = PdaUtils.getPoolPda(tokenA.address, tokenB.address)
                android.util.Log.d("LpViewModel", "Checking pool at address: $poolPda")
                
                // For now, use a simplified check - try to get balance of the pool address
                // If it has any SOL balance, we assume it exists (accounts with data need rent)
                val balanceResult = solanaApiService.getBalance(poolPda)
                
                if (balanceResult.isSuccess) {
                    val balance = balanceResult.getOrNull() ?: 0.0
                    
                    // If account has any balance (even rent), it exists
                    // Solana accounts need minimum rent to exist
                    val poolExists = balance > 0
                    
                    if (poolExists) {
                        android.util.Log.d("LpViewModel", "Pool exists at $poolPda with balance: $balance SOL")
                        
                        // Sort tokens canonically
                        val (tokenXMint, tokenYMint) = sortTokenAddresses(tokenA.address, tokenB.address)
                        
                        // Fetch actual reserve data from the pool
                        val reserveX = PdaUtils.getPoolReservePda(poolPda, tokenXMint)
                        val reserveY = PdaUtils.getPoolReservePda(poolPda, tokenYMint)
                        
                        // Fetch token balances from reserve accounts
                        val reserveXBalanceResult = solanaApiService.getTokenAccounts(reserveX)
                        val reserveYBalanceResult = solanaApiService.getTokenAccounts(reserveY)
                        
                        var reserveXAmount = BigDecimal("1000.0") // Default fallback
                        var reserveYAmount = BigDecimal("50000.0") // Default fallback
                        
                        reserveXBalanceResult.fold(
                            onSuccess = { tokens ->
                                if (tokens.isNotEmpty()) {
                                    reserveXAmount = BigDecimal(tokens.first().balance.uiAmount ?: 1000.0)
                                }
                            },
                            onFailure = { 
                                android.util.Log.w("LpViewModel", "Failed to fetch reserveX, using default")
                            }
                        )
                        
                        reserveYBalanceResult.fold(
                            onSuccess = { tokens ->
                                if (tokens.isNotEmpty()) {
                                    reserveYAmount = BigDecimal(tokens.first().balance.uiAmount ?: 50000.0)
                                }
                            },
                            onFailure = { 
                                android.util.Log.w("LpViewModel", "Failed to fetch reserveY, using default")
                            }
                        )
                        
                        _uiState.value = _uiState.value.copy(
                            poolDetails = PoolDetails(
                                exists = true,
                                tokenXMint = tokenXMint,
                                tokenYMint = tokenYMint,
                                poolAddress = poolPda,
                                reserveX = reserveXAmount,
                                reserveY = reserveYAmount,
                                totalLpSupply = BigDecimal("10000.0"), // Still need to fetch LP token supply
                                currentPrice = if (reserveXAmount > BigDecimal.ZERO) 
                                    reserveYAmount.divide(reserveXAmount, 6, java.math.RoundingMode.DOWN).toDouble() 
                                else 0.0
                            )
                        )
                    } else {
                        // Pool doesn't exist (no balance = no account)
                        android.util.Log.d("LpViewModel", "Pool does not exist at $poolPda")
                        _uiState.value = _uiState.value.copy(
                            poolDetails = PoolDetails(
                                exists = false,
                                tokenXMint = tokenA.address,
                                tokenYMint = tokenB.address,
                                poolAddress = poolPda
                            )
                        )
                    }
                } else {
                    // Error fetching balance usually means account doesn't exist
                    android.util.Log.d("LpViewModel", "Pool likely doesn't exist (balance fetch failed): ${balanceResult.exceptionOrNull()?.message}")
                    _uiState.value = _uiState.value.copy(
                        poolDetails = PoolDetails(
                            exists = false,
                            tokenXMint = tokenA.address,
                            tokenYMint = tokenB.address,
                            poolAddress = poolPda
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("LpViewModel", "Error checking pool existence", e)
                _uiState.value = _uiState.value.copy(
                    poolDetails = PoolDetails(
                        exists = false,
                        tokenXMint = tokenA.address,
                        tokenYMint = tokenB.address
                    ),
                    errorMessage = "Failed to check pool: ${e.message}"
                )
            }
        }
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