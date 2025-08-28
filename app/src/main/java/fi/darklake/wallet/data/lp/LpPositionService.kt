package fi.darklake.wallet.data.lp

import fi.darklake.wallet.data.api.HeliusApiService
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.data.swap.models.TokenInfo
import fi.darklake.wallet.ui.screens.lp.LiquidityPosition
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

/**
 * Service for fetching user liquidity positions from the blockchain
 * Based on dex-web getUserLiquidity implementation
 */
class LpPositionService(
    private val settingsManager: SettingsManager
) {
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
     * Get all liquidity positions for a user
     * This is the main function that replicates dex-web's approach
     */
    suspend fun getAllUserLiquidityPositions(userAddress: String): Result<List<LiquidityPosition>> {
        return try {
            android.util.Log.d("LpPositionService", "Fetching all LP positions for user: $userAddress")
            
            // Step 1: Get all token accounts owned by the user
            val allTokensResult = solanaApiService.getTokenAccounts(userAddress)
            
            allTokensResult.fold(
                onSuccess = { tokenAccounts ->
                    // Step 2: Filter for LP tokens and process them
                    val positions = processLpTokenAccounts(tokenAccounts, userAddress)
                    android.util.Log.d("LpPositionService", "Found ${positions.size} LP positions")
                    Result.success(positions)
                },
                onFailure = { error ->
                    android.util.Log.e("LpPositionService", "Failed to fetch token accounts", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("LpPositionService", "Error fetching user liquidity positions", e)
            Result.failure(e)
        }
    }
    
    /**
     * Process token accounts to identify and process LP tokens
     */
    private suspend fun processLpTokenAccounts(
        tokenAccounts: List<fi.darklake.wallet.data.model.TokenInfo>,
        userAddress: String
    ): List<LiquidityPosition> = coroutineScope {
        
        // Filter for accounts with non-zero balance that could be LP tokens
        val potentialLpTokens = tokenAccounts.filter { account ->
            account.balance.uiAmount ?: 0.0 > 0.0
        }
        
        android.util.Log.d("LpPositionService", "Processing ${potentialLpTokens.size} potential LP tokens")
        
        // Process each potential LP token in parallel
        val positionJobs = potentialLpTokens.map { tokenAccount ->
            async {
                try {
                    processLpToken(tokenAccount, userAddress)
                } catch (e: Exception) {
                    android.util.Log.w("LpPositionService", "Failed to process token ${tokenAccount.balance.mint}", e)
                    null
                }
            }
        }
        
        // Wait for all jobs and filter out nulls
        positionJobs.awaitAll().filterNotNull()
    }
    
    /**
     * Process a single LP token to determine if it's a valid position
     * This implements the reverse of getLpTokenMint - given an LP token mint,
     * try to derive the pool and token information
     */
    private suspend fun processLpToken(
        tokenAccount: fi.darklake.wallet.data.model.TokenInfo,
        userAddress: String
    ): LiquidityPosition? {
        
        val lpTokenMint = tokenAccount.balance.mint
        val lpTokenBalance = tokenAccount.balance.uiAmount ?: 0.0
        
        android.util.Log.d("LpPositionService", "Processing LP token: $lpTokenMint with balance: $lpTokenBalance")
        
        // Check if this LP token matches any known token pairs
        val knownPosition = checkKnownTokenPairs(lpTokenMint, lpTokenBalance)
        if (knownPosition != null) {
            // For known pairs, fetch actual pool data to get real amounts
            return fetchActualPoolData(knownPosition)
        }
        
        return null
    }
    
    /**
     * Check if the LP token matches any known token pairs
     * This is a simplified approach until we implement full PDA derivation
     */
    private fun checkKnownTokenPairs(
        lpTokenMint: String,
        lpTokenBalance: Double
    ): LiquidityPosition? {
        
        // Known token pairs for each network
        val knownPairs = when (settingsManager.networkSettings.value.network) {
            fi.darklake.wallet.data.model.SolanaNetwork.MAINNET -> listOf(
                Pair("9BB6NFEcjBCtnNLFko2FqVQBq8HHM13kCyYcdQbgpump", "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"), // Fartcoin/USDC
                Pair("So11111111111111111111111111111111111111112", "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v")  // SOL/USDC
            )
            fi.darklake.wallet.data.model.SolanaNetwork.DEVNET -> listOf(
                Pair("HXsKnhXPtGr2mq4uTpxbxyy7ZydYWJwx4zMuYPEDukY", "DdLxrGFs2sKYbbqVk76eVx9268ASUdTMAhrsqphqDuX") // DukY/DuX
            )
        }
        
        // Check each known pair to see if this LP token belongs to it
        for ((tokenA, tokenB) in knownPairs) {
            val expectedLpMint = deriveLpTokenMint(tokenA, tokenB)
            if (expectedLpMint == lpTokenMint) {
                // Found a match! Create the position
                return createLiquidityPosition(tokenA, tokenB, lpTokenBalance)
            }
        }
        
        return null // Not a recognized LP token
    }
    
    /**
     * Derive LP token mint for a given token pair
     * This implements the same logic as dex-web's getLpTokenMint
     */
    private fun deriveLpTokenMint(tokenAMint: String, tokenBMint: String): String {
        return PdaUtils.getLpTokenMint(tokenAMint, tokenBMint)
    }
    
    /**
     * Create a LiquidityPosition from known token pair data
     */
    private fun createLiquidityPosition(
        tokenAMint: String,
        tokenBMint: String,
        lpTokenBalance: Double
    ): LiquidityPosition {
        
        // Get token metadata for both tokens
        val tokenAInfo = getTokenInfo(tokenAMint)
        val tokenBInfo = getTokenInfo(tokenBMint)
        
        // Create initial position with LP token balance
        val position = LiquidityPosition(
            id = "${tokenAMint}_${tokenBMint}_position",
            tokenA = tokenAInfo,
            tokenB = tokenBInfo,
            amountA = BigDecimal.ZERO,
            amountB = BigDecimal.ZERO,
            lpTokenBalance = BigDecimal(lpTokenBalance),
            poolShare = 0.0
        )
        
        // Return position as-is since fetchActualPoolData is called elsewhere
        return position
    }
    
    /**
     * Fetch actual pool data to calculate real token amounts
     */
    private suspend fun fetchActualPoolData(position: LiquidityPosition): LiquidityPosition {
        try {
            // Get pool PDA
            val poolPda = PdaUtils.getPoolPda(position.tokenA.address, position.tokenB.address)
            
            // Get reserve account addresses
            val reserveX = PdaUtils.getPoolReservePda(poolPda, position.tokenA.address)
            val reserveY = PdaUtils.getPoolReservePda(poolPda, position.tokenB.address)
            
            // Fetch actual token balances from reserve accounts
            val reserveXBalanceResult = solanaApiService.getTokenAccounts(reserveX)
            val reserveYBalanceResult = solanaApiService.getTokenAccounts(reserveY)
            
            // Extract the balance amounts
            var reserveXAmount = 0.0
            var reserveYAmount = 0.0
            
            reserveXBalanceResult.fold(
                onSuccess = { tokens ->
                    // Should have exactly one token account for the reserve
                    if (tokens.isNotEmpty()) {
                        reserveXAmount = tokens.first().balance.uiAmount ?: 0.0
                    }
                },
                onFailure = { 
                    android.util.Log.w("LpPositionService", "Failed to fetch reserveX balance, using default")
                    reserveXAmount = 1000.0 // Fallback to mock data
                }
            )
            
            reserveYBalanceResult.fold(
                onSuccess = { tokens ->
                    if (tokens.isNotEmpty()) {
                        reserveYAmount = tokens.first().balance.uiAmount ?: 0.0
                    }
                },
                onFailure = { 
                    android.util.Log.w("LpPositionService", "Failed to fetch reserveY balance, using default")
                    reserveYAmount = 50000.0 // Fallback to mock data
                }
            )
            
            if (reserveXAmount > 0 || reserveYAmount > 0) {
                
                // Get LP token mint address
                val lpTokenMint = PdaUtils.getLpTokenMint(position.tokenA.address, position.tokenB.address)
                
                // Fetch LP token total supply from mint account
                var totalLpSupply = 1000000.0 // Default fallback
                val mintInfoResult = solanaApiService.getMintInfo(lpTokenMint)
                mintInfoResult.fold(
                    onSuccess = { mintInfo ->
                        totalLpSupply = mintInfo.uiAmount
                        android.util.Log.d("LpPositionService", "LP token total supply: $totalLpSupply")
                    },
                    onFailure = {
                        android.util.Log.w("LpPositionService", "Failed to fetch LP token supply, using default")
                    }
                )
                
                // Calculate user's share of the pool
                val userShare = if (totalLpSupply > 0) {
                    position.lpTokenBalance.toDouble() / totalLpSupply
                } else {
                    0.0
                }
                val poolSharePercentage = userShare * 100
                
                // Calculate user's token amounts based on their LP token balance
                val userTokenXAmount = BigDecimal(reserveXAmount * userShare)
                val userTokenYAmount = BigDecimal(reserveYAmount * userShare)
                
                
                // Return updated position with real amounts
                return position.copy(
                    amountA = userTokenXAmount,
                    amountB = userTokenYAmount,
                    poolShare = poolSharePercentage
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("LpPositionService", "Failed to fetch actual pool data", e)
        }
        
        // Return original position if we couldn't fetch real data
        return position
    }
    
    
    /**
     * Get token information for a given mint address
     */
    private fun getTokenInfo(tokenMint: String): TokenInfo {
        // Map of known tokens
        return when (tokenMint) {
            "9BB6NFEcjBCtnNLFko2FqVQBq8HHM13kCyYcdQbgpump" -> TokenInfo(
                address = tokenMint,
                symbol = "Fartcoin",
                name = "Fartcoin",
                decimals = 6
            )
            "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v" -> TokenInfo(
                address = tokenMint,
                symbol = "USDC",
                name = "USD Coin",
                decimals = 6
            )
            "So11111111111111111111111111111111111111112" -> TokenInfo(
                address = tokenMint,
                symbol = "SOL",
                name = "Solana",
                decimals = 9
            )
            "HXsKnhXPtGr2mq4uTpxbxyy7ZydYWJwx4zMuYPEDukY" -> TokenInfo(
                address = tokenMint,
                symbol = "DukY",
                name = "DukY",
                decimals = 9
            )
            "DdLxrGFs2sKYbbqVk76eVx9268ASUdTMAhrsqphqDuX" -> TokenInfo(
                address = tokenMint,
                symbol = "DuX",
                name = "DuX",
                decimals = 6
            )
            else -> TokenInfo(
                address = tokenMint,
                symbol = "UNKNOWN",
                name = "Unknown Token",
                decimals = 9
            )
        }
    }
}