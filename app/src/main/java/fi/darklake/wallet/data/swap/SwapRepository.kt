package fi.darklake.wallet.data.swap

import fi.darklake.wallet.data.swap.models.*
import fi.darklake.wallet.data.swap.utils.SolanaUtils
import fi.darklake.wallet.data.swap.grpc.DexGatewayClient
import fi.darklake.wallet.grpc.TradeStatus as GrpcTradeStatus
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random


class SwapRepository(
    private val networkSettings: fi.darklake.wallet.data.model.NetworkSettings
) {
    private val grpcClient = DexGatewayClient(networkSettings)
    
    suspend fun getSwapQuote(
        amountIn: Double,
        isXtoY: Boolean,
        slippage: Double,
        tokenXMint: String,
        tokenYMint: String
    ): Result<SwapQuoteResponse> {
        return try {
            // Note: The gateway doesn't have a direct quote endpoint
            // We simulate it based on expected behavior
            // In production, this might need to be calculated client-side
            // or a new endpoint added to the gateway
            
            // For now, return a simulated quote
            val estimatedRate = 0.95 // Simulated exchange rate
            val amountOut = amountIn * estimatedRate
            val priceImpact = 0.02 // 2% price impact
            val fee = amountIn * 0.003 // 0.3% fee
            
            val quoteResponse = SwapQuoteResponse(
                amountIn = amountIn,
                amountInRaw = (amountIn * 1e9).toLong().toString(), // Assuming 9 decimals
                amountOut = amountOut,
                amountOutRaw = (amountOut * 1e6).toLong().toString(), // Assuming 6 decimals
                estimatedFee = fee,
                estimatedFeesUsd = fee * 100, // Mock USD value
                isXtoY = isXtoY,
                priceImpactPercentage = priceImpact * 100,
                rate = estimatedRate,
                routePlan = listOf(
                    RoutePlan(
                        amountIn = amountIn,
                        amountOut = amountOut,
                        feeAmount = fee,
                        tokenXMint = tokenXMint,
                        tokenYMint = tokenYMint
                    )
                ),
                slippage = slippage,
                tokenXMint = tokenXMint,
                tokenYMint = tokenYMint
            )
            
            Result.success(quoteResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createSwapTransaction(
        amountIn: Double,
        isSwapXtoY: Boolean,
        minOut: Double,
        tokenMintX: String,
        tokenMintY: String,
        userAddress: String,
        trackingId: String = "id${Random.nextLong().toString(16)}"
    ): Result<SwapResponse> {
        return try {
            // Convert amounts to raw values with proper decimals
            // TODO: Get actual token decimals from token metadata
            val tokenXDecimals = if (isSwapXtoY) 9 else 6
            val tokenYDecimals = if (isSwapXtoY) 6 else 9
            
            val amountInRaw = (amountIn * Math.pow(10.0, tokenXDecimals.toDouble())).toLong()
            val minOutRaw = (minOut * Math.pow(10.0, tokenYDecimals.toDouble())).toLong()
            
            val response = grpcClient.createUnsignedTransaction(
                userAddress = userAddress,
                tokenMintX = tokenMintX,
                tokenMintY = tokenMintY,
                amountIn = amountInRaw,
                minOut = minOutRaw,
                trackingId = trackingId,
                isSwapXtoY = isSwapXtoY
            )
            
            Result.success(
                SwapResponse(
                    success = true,
                    trackingId = trackingId,
                    tradeId = response.tradeId,
                    unsignedTransaction = response.unsignedTransaction,
                    error = null
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun submitSignedTransaction(
        signedTransaction: String,
        trackingId: String,
        tradeId: String
    ): Result<SignedTransactionResponse> {
        return try {
            android.util.Log.d("SwapRepository", "Submitting signed transaction:")
            android.util.Log.d("SwapRepository", "- TrackingId: $trackingId")
            android.util.Log.d("SwapRepository", "- TradeId: $tradeId")
            android.util.Log.d("SwapRepository", "- Transaction length: ${signedTransaction.length}")
            android.util.Log.d("SwapRepository", "- Transaction preview: ${signedTransaction.take(100)}...")
            
            val response = grpcClient.sendSignedTransaction(
                signedTransaction = signedTransaction,
                trackingId = trackingId,
                tradeId = tradeId
            )
            
            android.util.Log.d("SwapRepository", "Server response received:")
            android.util.Log.d("SwapRepository", "- Success: ${response.success}")
            android.util.Log.d("SwapRepository", "- Error logs: ${response.errorLogsList}")
            
            val signedTransactionResponse = SignedTransactionResponse(
                success = response.success,
                message = if (response.errorLogsList.isNotEmpty()) {
                    response.errorLogsList.joinToString("; ")
                } else null,
                error = if (!response.success && response.errorLogsList.isNotEmpty()) {
                    response.errorLogsList.firstOrNull()
                } else null
            )
            
            if (!response.success) {
                android.util.Log.w("SwapRepository", "Transaction submission failed:")
                android.util.Log.w("SwapRepository", "- Message: ${signedTransactionResponse.message}")
                android.util.Log.w("SwapRepository", "- Error: ${signedTransactionResponse.error}")
            }
            
            Result.success(signedTransactionResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun checkTradeStatus(
        trackingId: String,
        tradeId: String
    ): Result<TradeStatusResponse> {
        return try {
            val response = grpcClient.checkTradeStatus(
                trackingId = trackingId,
                tradeId = tradeId
            )
            
            // Map gRPC TradeStatus to our TradeStatus enum
            val status = when (response.status) {
                GrpcTradeStatus.UNSIGNED -> TradeStatus.PENDING
                GrpcTradeStatus.SIGNED -> TradeStatus.PENDING
                GrpcTradeStatus.CONFIRMED -> TradeStatus.PENDING
                GrpcTradeStatus.SETTLED -> TradeStatus.SETTLED
                GrpcTradeStatus.SLASHED -> TradeStatus.SLASHED
                GrpcTradeStatus.CANCELLED -> TradeStatus.CANCELLED
                GrpcTradeStatus.FAILED -> TradeStatus.FAILED
                else -> TradeStatus.PENDING
            }
            
            Result.success(
                TradeStatusResponse(
                    status = status,
                    message = "Trade ${response.tradeId}: ${response.status.name}"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun pollTradeStatus(
        trackingId: String,
        tradeId: String,
        maxAttempts: Int = 10,
        delayMs: Long = 2000
    ): Result<TradeStatusResponse> {
        repeat(maxAttempts) { attempt ->
            val result = checkTradeStatus(trackingId, tradeId)
            
            result.onSuccess { response ->
                when (response.status) {
                    TradeStatus.SETTLED, TradeStatus.SLASHED -> {
                        return Result.success(response)
                    }
                    TradeStatus.CANCELLED, TradeStatus.FAILED -> {
                        return Result.failure(Exception("Trade ${response.status}: ${response.message}"))
                    }
                    TradeStatus.PENDING -> {
                        if (attempt < maxAttempts - 1) {
                            delay(delayMs)
                        }
                    }
                }
            }
            
            result.onFailure { error ->
                if (attempt == maxAttempts - 1) {
                    return Result.failure(error)
                }
                delay(delayMs)
            }
        }
        
        return Result.failure(Exception("Trade status check timed out"))
    }
    
    fun shutdown() {
        grpcClient.shutdown()
    }
    
    // Helper function to sort token addresses (matching dex-web sortSolanaAddresses)
    fun sortTokenAddresses(tokenA: String, tokenB: String): Pair<String, String> {
        return SolanaUtils.sortSolanaAddresses(tokenA, tokenB)
    }
    
    // Helper to calculate minimum output with slippage
    fun calculateMinOutput(amountOut: Double, slippagePercent: Double): Double {
        val slippageFactor = 1 - (slippagePercent / 100)
        return BigDecimal(amountOut)
            .multiply(BigDecimal(slippageFactor))
            .setScale(6, RoundingMode.DOWN)
            .toDouble()
    }
}