package fi.darklake.wallet.data.lp

import android.content.Context
import android.util.Base64
import com.solana.Solana
import com.solana.api.getLatestBlockhash
import com.solana.core.Transaction
import com.solana.networking.HttpNetworkingRouter
import com.solana.networking.RPCEndpoint
import fi.darklake.wallet.data.model.getHeliusRpcUrl
import fi.darklake.wallet.data.preferences.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Service for building and executing LP (Liquidity Provider) transactions
 * using Anchor IDL for proper instruction building
 */
class LpTransactionService(
    private val settingsManager: SettingsManager,
    private val context: Context? = null
) {
    private val lpTransactionHandler = LpTransactionHandler(settingsManager)
    private val idlBuilder = AnchorIDLTransactionBuilder(context)
    
    private fun createSolanaClient(): Solana {
        val rpcUrl = settingsManager.networkSettings.value.getHeliusRpcUrl()
        val endpoint = when {
            rpcUrl.contains("devnet") -> RPCEndpoint.devnetSolana
            rpcUrl.contains("mainnet") -> RPCEndpoint.mainnetBetaSolana
            else -> RPCEndpoint.devnetSolana // Default to devnet for custom URLs
        }
        return Solana(HttpNetworkingRouter(endpoint))
    }
    
    
    /**
     * Creates an add liquidity transaction using Anchor IDL
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
            android.util.Log.d("LpTransactionService", "Creating add liquidity transaction")
            android.util.Log.d("LpTransactionService", "User: $userAddress")
            android.util.Log.d("LpTransactionService", "TokenX: $tokenXMint, Amount: $maxAmountX")
            android.util.Log.d("LpTransactionService", "TokenY: $tokenYMint, Amount: $maxAmountY")
            android.util.Log.d("LpTransactionService", "Slippage: $slippage%")
            
            // Build the instruction using IDL
            val instruction = idlBuilder.addLiquidityInstruction(
                userAddress = userAddress,
                tokenXMint = tokenXMint,
                tokenYMint = tokenYMint,
                maxAmountX = maxAmountX,
                maxAmountY = maxAmountY
            )
            
            // Create transaction and add instruction
            val transaction = Transaction()
            transaction.addInstruction(instruction)
            
            // Get recent blockhash
            val solana = createSolanaClient()
            val recentBlockhashResult = solana.api.getLatestBlockhash()
            
            if (recentBlockhashResult.isFailure) {
                throw Exception("Failed to get recent blockhash: ${recentBlockhashResult.exceptionOrNull()?.message}")
            }
            
            transaction.recentBlockhash = recentBlockhashResult.getOrThrow().blockhash
            transaction.feePayer = com.solana.core.PublicKey(userAddress)
            
            // Serialize to base64
            val serializedTransaction = transaction.serialize()
            val base64Transaction = Base64.encodeToString(serializedTransaction, Base64.NO_WRAP)
            
            android.util.Log.d("LpTransactionService", "Add liquidity transaction created successfully")
            Result.success(base64Transaction)
            
        } catch (e: Exception) {
            android.util.Log.e("LpTransactionService", "Failed to create add liquidity transaction", e)
            Result.failure(e)
        }
    }
    
    /**
     * Creates a pool creation transaction using Anchor IDL
     */
    suspend fun createPoolTransaction(
        userAddress: String,
        tokenXMint: String,
        tokenYMint: String,
        depositAmountX: Double,
        depositAmountY: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("LpTransactionService", "Creating pool transaction")
            android.util.Log.d("LpTransactionService", "User: $userAddress")
            android.util.Log.d("LpTransactionService", "TokenX: $tokenXMint, Amount: $depositAmountX")
            android.util.Log.d("LpTransactionService", "TokenY: $tokenYMint, Amount: $depositAmountY")
            
            // Build the instruction using IDL
            val instruction = idlBuilder.createPoolInstruction(
                userAddress = userAddress,
                tokenXMint = tokenXMint,
                tokenYMint = tokenYMint,
                depositAmountX = depositAmountX,
                depositAmountY = depositAmountY
            )
            
            // Create transaction and add instruction
            val transaction = Transaction()
            transaction.addInstruction(instruction)
            
            // Get recent blockhash
            val solana = createSolanaClient()
            val recentBlockhashResult = solana.api.getLatestBlockhash()
            
            if (recentBlockhashResult.isFailure) {
                throw Exception("Failed to get recent blockhash: ${recentBlockhashResult.exceptionOrNull()?.message}")
            }
            
            transaction.recentBlockhash = recentBlockhashResult.getOrThrow().blockhash
            transaction.feePayer = com.solana.core.PublicKey(userAddress)
            
            // Serialize to base64
            val serializedTransaction = transaction.serialize()
            val base64Transaction = Base64.encodeToString(serializedTransaction, Base64.NO_WRAP)
            
            android.util.Log.d("LpTransactionService", "Pool creation transaction created successfully")
            Result.success(base64Transaction)
            
        } catch (e: Exception) {
            android.util.Log.e("LpTransactionService", "Failed to create pool transaction", e)
            Result.failure(e)
        }
    }
    
    /**
     * Creates a withdraw liquidity transaction using Anchor IDL
     */
    suspend fun createWithdrawLiquidityTransaction(
        userAddress: String,
        tokenXMint: String,
        tokenYMint: String,
        lpTokenAmount: Double,
        minAmountX: Double,
        minAmountY: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("LpTransactionService", "Creating withdraw liquidity transaction")
            android.util.Log.d("LpTransactionService", "User: $userAddress")
            android.util.Log.d("LpTransactionService", "LP Token Amount: $lpTokenAmount")
            android.util.Log.d("LpTransactionService", "Min amounts - X: $minAmountX, Y: $minAmountY")
            
            // Build the instruction using IDL
            val instruction = idlBuilder.withdrawLiquidityInstruction(
                userAddress = userAddress,
                tokenXMint = tokenXMint,
                tokenYMint = tokenYMint,
                lpTokenAmount = lpTokenAmount,
                minAmountX = minAmountX,
                minAmountY = minAmountY
            )
            
            // Create transaction and add instruction
            val transaction = Transaction()
            transaction.addInstruction(instruction)
            
            // Get recent blockhash
            val solana = createSolanaClient()
            val recentBlockhashResult = solana.api.getLatestBlockhash()
            
            if (recentBlockhashResult.isFailure) {
                throw Exception("Failed to get recent blockhash: ${recentBlockhashResult.exceptionOrNull()?.message}")
            }
            
            transaction.recentBlockhash = recentBlockhashResult.getOrThrow().blockhash
            transaction.feePayer = com.solana.core.PublicKey(userAddress)
            
            // Serialize to base64
            val serializedTransaction = transaction.serialize()
            val base64Transaction = Base64.encodeToString(serializedTransaction, Base64.NO_WRAP)
            
            android.util.Log.d("LpTransactionService", "Withdraw liquidity transaction created successfully")
            Result.success(base64Transaction)
            
        } catch (e: Exception) {
            android.util.Log.e("LpTransactionService", "Failed to create withdraw transaction", e)
            Result.failure(e)
        }
    }
    
}