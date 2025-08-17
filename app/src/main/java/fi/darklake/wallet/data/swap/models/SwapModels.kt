package fi.darklake.wallet.data.swap.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Quote models
@Serializable
data class SwapQuoteRequest(
    val amountIn: Double,
    val isXtoY: Boolean,
    val slippage: Double,
    val tokenXMint: String,
    val tokenYMint: String
)

@Serializable
data class SwapQuoteResponse(
    val amountIn: Double,
    val amountInRaw: String,
    val amountOut: Double,
    val amountOutRaw: String,
    val estimatedFee: Double,
    val estimatedFeesUsd: Double,
    val isXtoY: Boolean,
    val priceImpactPercentage: Double,
    val rate: Double,
    val routePlan: List<RoutePlan>,
    val slippage: Double,
    val tokenX: TokenInfo? = null,
    val tokenXMint: String,
    val tokenY: TokenInfo? = null,
    val tokenYMint: String
)

@Serializable
data class RoutePlan(
    val amountIn: Double,
    val amountOut: Double,
    val feeAmount: Double,
    val tokenXMint: String,
    val tokenYMint: String
)

@Serializable
data class TokenInfo(
    val address: String,
    val symbol: String,
    val name: String,
    val decimals: Int,
    val logoURI: String? = null
)

// Swap transaction models
@Serializable
data class SwapRequest(
    @SerialName("amount_in") val amountIn: Double,
    @SerialName("is_swap_x_to_y") val isSwapXtoY: Boolean,
    @SerialName("min_out") val minOut: Double,
    @SerialName("network") val network: Int,
    @SerialName("token_mint_x") val tokenMintX: String,
    @SerialName("token_mint_y") val tokenMintY: String,
    @SerialName("tracking_id") val trackingId: String,
    @SerialName("user_address") val userAddress: String
)

@Serializable
data class SwapResponse(
    val success: Boolean,
    val trackingId: String,
    val tradeId: String,
    val unsignedTransaction: String,
    val error: String? = null
)

// Signed transaction models
@Serializable
data class SignedTransactionRequest(
    @SerialName("signed_transaction") val signedTransaction: String,
    @SerialName("tracking_id") val trackingId: String,
    @SerialName("trade_id") val tradeId: String
)

@Serializable
data class SignedTransactionResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)

// Trade status models
@Serializable
data class TradeStatusRequest(
    @SerialName("tracking_id") val trackingId: String,
    @SerialName("trade_id") val tradeId: String
)

@Serializable
data class TradeStatusResponse(
    val status: TradeStatus,
    val message: String? = null
)

enum class TradeStatus {
    PENDING,
    SETTLED,
    SLASHED,
    CANCELLED,
    FAILED
}

// Pool details
@Serializable
data class PoolDetailsRequest(
    val tokenXMint: String,
    val tokenYMint: String
)

@Serializable
data class PoolDetails(
    val poolAddress: String? = null,
    val tokenXMint: String,
    val tokenYMint: String,
    val tokenXSymbol: String? = null,
    val tokenYSymbol: String? = null,
    val liquidityX: String? = null,
    val liquidityY: String? = null,
    val feeRate: Double? = null,
    val volume24h: Double? = null,
    val tvl: Double? = null
)