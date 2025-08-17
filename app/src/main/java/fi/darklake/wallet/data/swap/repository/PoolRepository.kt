package fi.darklake.wallet.data.swap.repository

import fi.darklake.wallet.data.model.SolanaNetwork
import fi.darklake.wallet.data.swap.utils.SolanaUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class PoolDetails(
    val poolAddress: String,
    val tokenXMint: String,
    val tokenYMint: String,
    val tokenXSymbol: String? = null,
    val tokenYSymbol: String? = null,
    val apr: Double = 0.0,
    val exists: Boolean = true
)

@Serializable
data class RpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String,
    val params: List<String>
)

@Serializable
data class RpcResponse(
    val jsonrpc: String,
    val id: Int,
    val result: RpcResult?,
    val error: RpcError?
)

@Serializable
data class RpcResult(
    val value: RpcAccountInfo?
)

@Serializable
data class RpcAccountInfo(
    val data: List<String>?,
    val executable: Boolean = false,
    val lamports: Long = 0,
    val owner: String = "",
    val rentEpoch: Long = 0
)

@Serializable
data class RpcError(
    val code: Int,
    val message: String
)

class PoolRepository {
    
    // Mock pool data matching dex-web implementation
    private val devnetPools = listOf(
        PoolDetails(
            poolAddress = "mock_dukY_duX_pool",
            tokenXMint = "DdLxrGFs2sKYbbqVk76eVx9268ASUdTMAhrsqphqDuX", // DuX
            tokenYMint = "HXsKnhXPtGr2mq4uTpxbxyy7ZydYWJwx4zMuYPEDukY", // DukY
            tokenXSymbol = "DuX",
            tokenYSymbol = "DukY"
        )
    )
    
    private val mainnetPools = listOf(
        PoolDetails(
            poolAddress = "mock_fartcoin_usdc_pool",
            tokenXMint = "9BB6NFEcjBCtnNLFko2FqVQBq8HHM13kCyYcdQbgpump", // Fartcoin
            tokenYMint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v", // USDC
            tokenXSymbol = "Fartcoin",
            tokenYSymbol = "USDC"
        )
    )
    
    /**
     * Check if a pool exists for the given token pair
     * This follows the dex-web logic: check local data first, then on-chain
     */
    fun getPoolDetails(
        tokenA: String,
        tokenB: String,
        network: SolanaNetwork,
        rpcUrl: String? = null
    ): Flow<Result<PoolDetails?>> = flow {
        try {
            // Sort tokens deterministically like dex-web
            val (tokenX, tokenY) = SolanaUtils.sortSolanaAddresses(tokenA, tokenB)
            
            // First check local mock data (matching dex-web pattern)
            val localPool = getPoolFromLocalData(tokenX, tokenY, network)
            if (localPool != null) {
                emit(Result.success(localPool))
                return@flow
            }
            
            // If not found locally, check on-chain (simplified for now)
            val poolPda = SolanaUtils.generatePoolPda(tokenX, tokenY)
            val onChainExists = checkPoolExistsOnChain(poolPda, rpcUrl, network)
            
            if (onChainExists) {
                val poolDetails = PoolDetails(
                    poolAddress = poolPda,
                    tokenXMint = tokenX,
                    tokenYMint = tokenY,
                    exists = true
                )
                emit(Result.success(poolDetails))
            } else {
                emit(Result.success(null)) // Pool doesn't exist
            }
            
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * Get pool from local mock data, matching dex-web getPoolOnLocalData
     */
    private fun getPoolFromLocalData(tokenX: String, tokenY: String, network: SolanaNetwork): PoolDetails? {
        val pools = when (network) {
            SolanaNetwork.DEVNET -> devnetPools
            SolanaNetwork.MAINNET -> mainnetPools
        }
        
        return pools.find { pool ->
            (pool.tokenXMint == tokenX && pool.tokenYMint == tokenY) ||
            (pool.tokenXMint == tokenY && pool.tokenYMint == tokenX)
        }
    }
    
    /**
     * Check if pool exists on-chain by querying the PDA
     * This is a simplified version - in production would use proper Solana RPC
     */
    private suspend fun checkPoolExistsOnChain(
        poolPda: String,
        rpcUrl: String?,
        network: SolanaNetwork
    ): Boolean {
        if (rpcUrl == null) return false
        
        try {
            // For now, return false as we don't have full RPC implementation
            // In production, this would make an actual HTTP request to check if account exists
            // by calling getAccountInfo on the poolPda
            return false
            
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Simple pool existence check that just looks at local data
     * This is what we'll use for now until full RPC integration
     */
    fun poolExistsLocally(tokenA: String, tokenB: String, network: SolanaNetwork): Boolean {
        val (tokenX, tokenY) = SolanaUtils.sortSolanaAddresses(tokenA, tokenB)
        return getPoolFromLocalData(tokenX, tokenY, network) != null
    }
}