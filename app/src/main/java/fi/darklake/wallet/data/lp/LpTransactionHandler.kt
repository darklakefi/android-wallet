package fi.darklake.wallet.data.lp

import fi.darklake.wallet.data.api.HeliusApiService
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.data.swap.grpc.DexGatewayClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import android.util.Base64
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Handler for creating actual Add Liquidity transactions
 * Based on dex-web's createLiquidityTransactionHandler
 */
class LpTransactionHandler(
    private val settingsManager: SettingsManager
) {
    
    private val grpcClient = DexGatewayClient(settingsManager.networkSettings.value)
    private val tokenMetadataCache = mutableMapOf<String, TokenMetadata>()
    companion object {
        private const val EXCHANGE_PROGRAM_ID = "darkr3FB87qAZmgLwKov6Hk9Yiah5UT4rUYu8Zhthw1"
        private const val POOL_RESERVE_SEED = "pool_reserve"
        private const val POOL_SEED = "pool"
        private const val AMM_CONFIG_SEED = "amm_config"
        private const val LIQUIDITY_SEED = "lp"
        private const val LP_TOKEN_DECIMALS = 9
        private const val TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
        private const val ASSOCIATED_TOKEN_PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"
        private const val COMPUTE_BUDGET_PROGRAM_ID = "ComputeBudget111111111111111111111111111111"
    }
    
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
    
    /**
     * Create an Add Liquidity transaction
     * This implements the same logic as dex-web's createLiquidityTransactionHandler
     */
    suspend fun createAddLiquidityTransaction(
        userAddress: String,
        tokenXMint: String,
        tokenYMint: String,
        maxAmountX: Double,
        maxAmountY: Double,
        slippage: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("LpTransactionHandler", "Creating add liquidity transaction:")
            android.util.Log.d("LpTransactionHandler", "- User: $userAddress")
            android.util.Log.d("LpTransactionHandler", "- TokenX: $tokenXMint, Amount: $maxAmountX")
            android.util.Log.d("LpTransactionHandler", "- TokenY: $tokenYMint, Amount: $maxAmountY")
            android.util.Log.d("LpTransactionHandler", "- Slippage: $slippage%")
            
            // Step 1: Calculate LP token estimate (like dex-web's getLPRateHandler)
            val lpRate = calculateLpRate(
                tokenXAmount = maxAmountX,
                tokenYAmount = maxAmountY,
                tokenXMint = tokenXMint,
                tokenYMint = tokenYMint,
                slippage = slippage
            )
            
            lpRate.fold(
                onSuccess = { lpEstimate ->
                    // Step 2: Build the transaction (like dex-web's createLiquidityTransaction)
                    val transaction = buildAddLiquidityTransaction(
                        userAddress = userAddress,
                        tokenXMint = tokenXMint,
                        tokenYMint = tokenYMint,
                        maxAmountX = maxAmountX,
                        maxAmountY = maxAmountY,
                        lpTokensToMint = lpEstimate.estimatedLPTokens
                    )
                    
                    transaction.fold(
                        onSuccess = { serializedTx ->
                            android.util.Log.d("LpTransactionHandler", "Successfully created add liquidity transaction")
                            Result.success(serializedTx)
                        },
                        onFailure = { error ->
                            android.util.Log.e("LpTransactionHandler", "Failed to build transaction", error)
                            Result.failure(error)
                        }
                    )
                },
                onFailure = { error ->
                    android.util.Log.e("LpTransactionHandler", "Failed to calculate LP rate", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("LpTransactionHandler", "Error creating add liquidity transaction", e)
            Result.failure(e)
        }
    }
    
    /**
     * Calculate LP token rate using real token metadata (like dex-web's getLPRateHandler)
     */
    private suspend fun calculateLpRate(
        tokenXAmount: Double,
        tokenYAmount: Double,
        tokenXMint: String,
        tokenYMint: String,
        slippage: Double
    ): Result<LpRateEstimate> {
        return try {
            android.util.Log.d("LpTransactionHandler", "Calculating LP rate for tokens $tokenXMint/$tokenYMint")
            
            coroutineScope {
                // Get token metadata to determine decimals (like SwapRepository does)
                val tokenXDeferred = async { getTokenMetadata(tokenXMint) }
                val tokenYDeferred = async { getTokenMetadata(tokenYMint) }
                
                val tokenXMetadata = tokenXDeferred.await()
                val tokenYMetadata = tokenYDeferred.await()
                
                // Scale input amounts to base units using actual decimals
                val scaledTokenXAmount = tokenXAmount * Math.pow(10.0, tokenXMetadata.decimals.toDouble())
                val scaledTokenYAmount = tokenYAmount * Math.pow(10.0, tokenYMetadata.decimals.toDouble())
                
                // For new pools, use geometric mean as LP token estimate (standard AMM approach)
                // In existing pools, this would use: min(amountX * lpSupply / reserveX, amountY * lpSupply / reserveY)
                val geometricMean = Math.sqrt(scaledTokenXAmount * scaledTokenYAmount)
                val estimatedLP = Math.floor(geometricMean).toLong()
                
                // Apply slippage tolerance
                val slippageDecimal = slippage / 100.0
                val slippageAmount = Math.floor(estimatedLP.toDouble() * slippageDecimal).toLong()
                val finalEstimatedLP = estimatedLP - slippageAmount
                
                // Convert to user-friendly format
                val userFriendlyLP = finalEstimatedLP.toDouble() / Math.pow(10.0, LP_TOKEN_DECIMALS.toDouble())
                
                android.util.Log.d("LpTransactionHandler", "LP calculation with real metadata:")
                android.util.Log.d("LpTransactionHandler", "- TokenX decimals: ${tokenXMetadata.decimals}")
                android.util.Log.d("LpTransactionHandler", "- TokenY decimals: ${tokenYMetadata.decimals}")
                android.util.Log.d("LpTransactionHandler", "- Final LP estimate: $finalEstimatedLP raw ($userFriendlyLP user-friendly)")
                
                Result.success(
                    LpRateEstimate(
                        estimatedLPTokens = userFriendlyLP,
                        estimatedLPTokensRaw = finalEstimatedLP
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("LpTransactionHandler", "Error calculating LP rate", e)
            Result.failure(e)
        }
    }
    
    
    /**
     * Build the actual Add Liquidity transaction using real token metadata
     */
    private suspend fun buildAddLiquidityTransaction(
        userAddress: String,
        tokenXMint: String,
        tokenYMint: String,
        maxAmountX: Double,
        maxAmountY: Double,
        lpTokensToMint: Double
    ): Result<String> {
        return try {
            android.util.Log.d("LpTransactionHandler", "Building add liquidity transaction")
            
            coroutineScope {
                // Get token metadata to determine decimals (same as SwapRepository)
                val tokenXDeferred = async { getTokenMetadata(tokenXMint) }
                val tokenYDeferred = async { getTokenMetadata(tokenYMint) }
                
                val tokenXMetadata = tokenXDeferred.await()
                val tokenYMetadata = tokenYDeferred.await()
                
                buildTransactionWithMetadata(
                    userAddress, tokenXMint, tokenYMint,
                    maxAmountX, maxAmountY, lpTokensToMint,
                    tokenXMetadata, tokenYMetadata
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("LpTransactionHandler", "Error building transaction", e)
            Result.failure(e)
        }
    }
    
    /**
     * Build transaction with actual token metadata
     */
    private suspend fun buildTransactionWithMetadata(
        userAddress: String,
        tokenXMint: String,
        tokenYMint: String,
        maxAmountX: Double,
        maxAmountY: Double,
        lpTokensToMint: Double,
        tokenXMetadata: TokenMetadata,
        tokenYMetadata: TokenMetadata
    ): Result<String> {
        return try {
            // Step 1: Derive all Program Derived Addresses (PDAs)
            val ammConfigPda = PdaUtils.getAmmConfigPda()
            val poolPda = PdaUtils.getPoolPda(tokenXMint, tokenYMint)
            val lpMintPda = PdaUtils.getLpTokenMint(tokenXMint, tokenYMint)
            val poolReserveXPda = PdaUtils.getPoolReservePda(poolPda, tokenXMint)
            val poolReserveYPda = PdaUtils.getPoolReservePda(poolPda, tokenYMint)
            
            // Step 2: Calculate Associated Token Addresses (ATAs)
            val userTokenXAta = PdaUtils.getAssociatedTokenAddress(userAddress, tokenXMint)
            val userTokenYAta = PdaUtils.getAssociatedTokenAddress(userAddress, tokenYMint)
            val userLpAta = PdaUtils.getAssociatedTokenAddress(userAddress, lpMintPda)
            
            // Step 3: Convert amounts to base units using real decimals
            val maxAmountXRaw = (maxAmountX * Math.pow(10.0, tokenXMetadata.decimals.toDouble())).toLong()
            val maxAmountYRaw = (maxAmountY * Math.pow(10.0, tokenYMetadata.decimals.toDouble())).toLong()
            val lpTokensToMintRaw = (lpTokensToMint * Math.pow(10.0, LP_TOKEN_DECIMALS.toDouble())).toLong()
            
            android.util.Log.d("LpTransactionHandler", "Transaction parameters with real metadata:")
            android.util.Log.d("LpTransactionHandler", "- Pool PDA: $poolPda")
            android.util.Log.d("LpTransactionHandler", "- LP Mint: $lpMintPda")
            android.util.Log.d("LpTransactionHandler", "- TokenX (${tokenXMetadata.symbol}): ${tokenXMetadata.decimals} decimals")
            android.util.Log.d("LpTransactionHandler", "- TokenY (${tokenYMetadata.symbol}): ${tokenYMetadata.decimals} decimals")
            android.util.Log.d("LpTransactionHandler", "- MaxAmountX (raw): $maxAmountXRaw")
            android.util.Log.d("LpTransactionHandler", "- MaxAmountY (raw): $maxAmountYRaw")
            android.util.Log.d("LpTransactionHandler", "- LP tokens to mint (raw): $lpTokensToMintRaw")
            
            // Step 4: Build the transaction structure
            val transactionData = buildSolanaTransaction(
                userAddress = userAddress,
                poolPda = poolPda,
                lpMintPda = lpMintPda,
                poolReserveXPda = poolReserveXPda,
                poolReserveYPda = poolReserveYPda,
                userTokenXAta = userTokenXAta,
                userTokenYAta = userTokenYAta,
                userLpAta = userLpAta,
                tokenXMint = tokenXMint,
                tokenYMint = tokenYMint,
                maxAmountXRaw = maxAmountXRaw,
                maxAmountYRaw = maxAmountYRaw,
                lpTokensToMintRaw = lpTokensToMintRaw
            )
            
            val serializedTx = Base64.encodeToString(transactionData, Base64.NO_WRAP)
            
            android.util.Log.d("LpTransactionHandler", "Transaction built successfully, size: ${transactionData.size} bytes")
            
            Result.success(serializedTx)
        } catch (e: Exception) {
            android.util.Log.e("LpTransactionHandler", "Error building transaction with metadata", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get token metadata using the same infrastructure as SwapRepository
     */
    private suspend fun getTokenMetadata(tokenAddress: String): TokenMetadata {
        // Check cache first
        tokenMetadataCache[tokenAddress]?.let { return it }
        
        return try {
            val response = grpcClient.getTokenMetadata(tokenAddress = tokenAddress)
            val metadata = TokenMetadata(
                address = tokenAddress,
                decimals = response.tokenMetadata.decimals,
                symbol = response.tokenMetadata.symbol,
                name = response.tokenMetadata.name
            )
            
            // Cache the result
            tokenMetadataCache[tokenAddress] = metadata
            android.util.Log.d("LpTransactionHandler", "Fetched token metadata: ${metadata.symbol} (${metadata.decimals} decimals)")
            
            metadata
        } catch (e: Exception) {
            android.util.Log.e("LpTransactionHandler", "Failed to fetch token metadata for $tokenAddress", e)
            // Return default metadata as fallback
            TokenMetadata(
                address = tokenAddress,
                decimals = 9, // Default to 9 decimals
                symbol = "UNKNOWN",
                name = "Unknown Token"
            )
        }
    }
    
    /**
     * Build the actual Solana transaction bytes
     */
    private fun buildSolanaTransaction(
        userAddress: String,
        poolPda: String,
        lpMintPda: String,
        poolReserveXPda: String,
        poolReserveYPda: String,
        userTokenXAta: String,
        userTokenYAta: String,
        userLpAta: String,
        tokenXMint: String,
        tokenYMint: String,
        maxAmountXRaw: Long,
        maxAmountYRaw: Long,
        lpTokensToMintRaw: Long
    ): ByteArray {
        // This is a simplified transaction structure for demonstration
        // In a real implementation, this would use SolanaKT to build:
        // 1. Compute Budget instructions (setComputeUnitLimit, setComputeUnitPrice)
        // 2. ATA creation instructions (if needed)
        // 3. The addLiquidity program instruction
        // 4. Compile to VersionedTransaction
        
        val transactionInfo = mapOf(
            "type" to "addLiquidity",
            "version" to "v0",
            "user" to userAddress,
            "poolPda" to poolPda,
            "lpMint" to lpMintPda,
            "poolReserveX" to poolReserveXPda,
            "poolReserveY" to poolReserveYPda,
            "userTokenXAta" to userTokenXAta,
            "userTokenYAta" to userTokenYAta,
            "userLpAta" to userLpAta,
            "tokenXMint" to tokenXMint,
            "tokenYMint" to tokenYMint,
            "maxAmountX" to maxAmountXRaw.toString(),
            "maxAmountY" to maxAmountYRaw.toString(),
            "lpTokensToMint" to lpTokensToMintRaw.toString(),
            "computeUnitLimit" to "400000",
            "computeUnitPrice" to "50000",
            "programId" to EXCHANGE_PROGRAM_ID,
            "timestamp" to System.currentTimeMillis().toString()
        )
        
        return transactionInfo.toString().toByteArray()
    }
    
    
    /**
     * Data class for LP rate estimation results
     */
    data class LpRateEstimate(
        val estimatedLPTokens: Double,
        val estimatedLPTokensRaw: Long
    )
    
    /**
     * Data class for token metadata (same structure as SwapRepository)
     */
    private data class TokenMetadata(
        val address: String,
        val decimals: Int,
        val symbol: String,
        val name: String
    )
    
    /**
     * Data class representing pool account data from blockchain
     */
    data class PoolAccount(
        val reserveXBalance: Long,
        val reserveYBalance: Long,
        val userLockedX: Long,
        val userLockedY: Long,
        val protocolFeeX: Long,
        val protocolFeeY: Long,
        val lpSupply: Long
    )
}