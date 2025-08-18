package fi.darklake.wallet.data.swap

import fi.darklake.wallet.data.swap.models.*
import fi.darklake.wallet.data.swap.utils.SolanaUtils
import fi.darklake.wallet.data.swap.grpc.DexGatewayClient
import fi.darklake.wallet.grpc.TradeStatus as GrpcTradeStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ceil
import kotlin.random.Random


class SwapRepository(
    private val networkSettings: fi.darklake.wallet.data.model.NetworkSettings
) {
    private val grpcClient = DexGatewayClient(networkSettings)
    private val tokenMetadataCache = mutableMapOf<String, TokenMetadata>()
    
    private data class TokenMetadata(
        val address: String,
        val decimals: Int,
        val symbol: String,
        val name: String
    )
    
    private data class PoolData(
        val availableReserveX: Long,
        val availableReserveY: Long,
        val tradeFeeRate: Long,
        val protocolFeeRate: Long
    )
    
    private data class SwapResult(
        val destinationAmount: Long,
        val sourceAmountPostFees: Long,
        val rate: Double,
        val tradeFee: Long,
        val protocolFee: Long
    )
    
    // 100% = 1000000, 0.0001% = 1 (matching dex-web)
    private val MAX_PERCENTAGE = 1000000L
    
    // Calculate trade fee (matching dex-web gateFee function)
    private fun gateFee(sourceAmount: Long, tradeFeeRate: Long): Long {
        return ceil((sourceAmount * tradeFeeRate).toDouble() / MAX_PERCENTAGE).toLong()
    }
    
    // Constant product formula (like Uniswap AMM)
    private fun swapBaseInputWithoutFees(
        sourceAmount: Long,
        swapSourceAmount: Long,
        swapDestinationAmount: Long
    ): Long {
        // (x + delta_x) * (y - delta_y) = x * y
        // delta_y = (delta_x * y) / (x + delta_x)
        val numerator = sourceAmount * swapDestinationAmount
        val denominator = swapSourceAmount + sourceAmount
        return floor(numerator.toDouble() / denominator).toLong()
    }
    
    // Main swap calculation function (matching dex-web calculateSwap)
    private fun calculateSwap(
        sourceAmount: Long,
        poolSourceAmount: Long,
        poolDestinationAmount: Long,
        tradeFeeRate: Long,
        protocolFeeRate: Long
    ): SwapResult {
        // Calculate trade fee from input
        val tradeFee = gateFee(sourceAmount, tradeFeeRate)
        // Protocol fee is a percentage of the trade fee
        val protocolFee = gateFee(tradeFee, protocolFeeRate)
        
        // Subtract fee from input
        val sourceAmountPostFees = sourceAmount - tradeFee
        
        // Use post fee amount to calculate output
        val destinationAmountSwapped = swapBaseInputWithoutFees(
            sourceAmountPostFees,
            poolSourceAmount,
            poolDestinationAmount
        )
        
        // Calculate rate (output per input)
        val rate = destinationAmountSwapped.toDouble() / sourceAmount.toDouble()
        
        return SwapResult(
            destinationAmount = destinationAmountSwapped,
            sourceAmountPostFees = sourceAmountPostFees,
            rate = rate,
            tradeFee = tradeFee,
            protocolFee = protocolFee
        )
    }
    
    // For now, return mock pool data since we don't have pool fetching implemented
    // In production, this would fetch actual pool and AMM config data from chain
    private suspend fun getPoolData(tokenXMint: String, tokenYMint: String): PoolData {
        // Mock data - in production would fetch from chain like dex-web does
        return PoolData(
            availableReserveX = 1000000000L, // 1B units in token X reserve
            availableReserveY = 1000000000L, // 1B units in token Y reserve  
            tradeFeeRate = 2500L, // 0.25% fee (2500 / 1000000 = 0.0025)
            protocolFeeRate = 100000L // 10% of trade fee goes to protocol
        )
    }
    
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
            tokenMetadataCache[tokenAddress] = metadata
            metadata
        } catch (e: Exception) {
            android.util.Log.e("SwapRepository", "Failed to fetch token metadata for $tokenAddress", e)
            // Fallback to common defaults based on known tokens
            when (tokenAddress) {
                "So11111111111111111111111111111111111111112" -> TokenMetadata(tokenAddress, 9, "SOL", "Solana")
                "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v" -> TokenMetadata(tokenAddress, 6, "USDC", "USD Coin")
                "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB" -> TokenMetadata(tokenAddress, 6, "USDT", "Tether USD")
                else -> TokenMetadata(tokenAddress, 9, "UNKNOWN", "Unknown Token") // Default to 9 decimals
            }
        }
    }
    
    suspend fun getSwapQuote(
        amountIn: Double,
        isXtoY: Boolean,
        slippage: Double,
        tokenXMint: String,
        tokenYMint: String
    ): Result<SwapQuoteResponse> {
        return try {
            // Fetch token metadata for accurate decimal calculations
            val tokenXMetadata = getTokenMetadata(tokenXMint)
            val tokenYMetadata = getTokenMetadata(tokenYMint)
            
            // Calculate decimals based on swap direction
            val amountInDecimals = if (isXtoY) tokenXMetadata.decimals else tokenYMetadata.decimals
            val amountOutDecimals = if (isXtoY) tokenYMetadata.decimals else tokenXMetadata.decimals
            
            // Get pool and AMM config data to calculate real quote
            val poolData = getPoolData(tokenXMint, tokenYMint)
            
            // Convert input to raw amount (scaled by decimals)
            val scaledInput = (amountIn * 10.0.pow(amountInDecimals)).toLong()
            
            // Calculate swap using constant product formula
            val swapResult = calculateSwap(
                sourceAmount = scaledInput,
                poolSourceAmount = if (isXtoY) poolData.availableReserveX else poolData.availableReserveY,
                poolDestinationAmount = if (isXtoY) poolData.availableReserveY else poolData.availableReserveX,
                tradeFeeRate = poolData.tradeFeeRate,
                protocolFeeRate = poolData.protocolFeeRate
            )
            
            // Convert output back to human-readable amount
            val amountOut = swapResult.destinationAmount.toDouble() / 10.0.pow(amountOutDecimals)
            
            // Calculate price impact
            val originalRate = if (isXtoY) {
                poolData.availableReserveY.toDouble() / poolData.availableReserveX.toDouble()
            } else {
                poolData.availableReserveX.toDouble() / poolData.availableReserveY.toDouble()
            }
            
            val poolInputAmount = swapResult.sourceAmountPostFees + swapResult.tradeFee - swapResult.protocolFee
            val newAvailableReserveX = if (isXtoY) {
                poolData.availableReserveX + poolInputAmount
            } else {
                poolData.availableReserveX - swapResult.destinationAmount
            }
            val newAvailableReserveY = if (isXtoY) {
                poolData.availableReserveY - swapResult.destinationAmount
            } else {
                poolData.availableReserveY + poolInputAmount
            }
            
            val newRate = if (isXtoY) {
                newAvailableReserveY.toDouble() / newAvailableReserveX.toDouble()
            } else {
                newAvailableReserveX.toDouble() / newAvailableReserveY.toDouble()
            }
            
            val priceImpact = ((originalRate - newRate) / originalRate) * 100
            val priceImpactTruncated = floor(priceImpact * 100) / 100
            
            // Adjust rate for decimal differences
            var adjustedRate = swapResult.rate
            val decDiff = abs(tokenXMetadata.decimals - tokenYMetadata.decimals)
            if (isXtoY) {
                if (tokenXMetadata.decimals < tokenYMetadata.decimals) {
                    adjustedRate = swapResult.rate / 10.0.pow(decDiff)
                } else if (tokenXMetadata.decimals > tokenYMetadata.decimals) {
                    adjustedRate = swapResult.rate * 10.0.pow(decDiff)
                }
            } else {
                if (tokenYMetadata.decimals < tokenXMetadata.decimals) {
                    adjustedRate = swapResult.rate / 10.0.pow(decDiff)
                } else if (tokenYMetadata.decimals > tokenXMetadata.decimals) {
                    adjustedRate = swapResult.rate * 10.0.pow(decDiff)
                }
            }
            
            // Convert to raw amounts using actual decimals
            val amountInRaw = scaledInput.toString()
            val amountOutRaw = swapResult.destinationAmount.toString()
            val estimatedFee = swapResult.tradeFee.toDouble() / 10.0.pow(amountInDecimals)
            
            val quoteResponse = SwapQuoteResponse(
                amountIn = amountIn,
                amountInRaw = amountInRaw,
                amountOut = amountOut,
                amountOutRaw = amountOutRaw,
                estimatedFee = estimatedFee,
                estimatedFeesUsd = estimatedFee * 100, // Mock USD value
                isXtoY = isXtoY,
                priceImpactPercentage = priceImpactTruncated,
                rate = adjustedRate,
                routePlan = listOf(
                    RoutePlan(
                        amountIn = amountIn,
                        amountOut = amountOut,
                        feeAmount = estimatedFee,
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
            // Fetch token metadata to get actual decimals
            val tokenXMetadata = getTokenMetadata(tokenMintX)
            val tokenYMetadata = getTokenMetadata(tokenMintY)
            
            android.util.Log.d("SwapRepository", "Token X ($tokenMintX): decimals=${tokenXMetadata.decimals}")
            android.util.Log.d("SwapRepository", "Token Y ($tokenMintY): decimals=${tokenYMetadata.decimals}")
            
            // Calculate decimals based on swap direction (matching dex-web logic)
            val amountInDecimals = if (isSwapXtoY) tokenXMetadata.decimals else tokenYMetadata.decimals
            val minOutDecimals = if (isSwapXtoY) tokenYMetadata.decimals else tokenXMetadata.decimals
            
            // Convert to raw amounts using BigDecimal for precision
            val amountInRaw = BigDecimal(amountIn)
                .multiply(BigDecimal.TEN.pow(amountInDecimals))
                .toLong()
            val minOutRaw = BigDecimal(minOut)
                .multiply(BigDecimal.TEN.pow(minOutDecimals))
                .toLong()
            
            android.util.Log.d("SwapRepository", "Swap direction: isSwapXtoY=$isSwapXtoY")
            android.util.Log.d("SwapRepository", "Amount in: $amountIn (decimals=$amountInDecimals) -> raw=$amountInRaw")
            android.util.Log.d("SwapRepository", "Min out: $minOut (decimals=$minOutDecimals) -> raw=$minOutRaw")
            
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