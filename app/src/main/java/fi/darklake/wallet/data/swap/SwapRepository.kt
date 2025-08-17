package fi.darklake.wallet.data.swap

import fi.darklake.wallet.data.swap.models.*
import fi.darklake.wallet.data.swap.utils.SolanaUtils
import io.ktor.client.*
import kotlinx.serialization.Serializable
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode

@Serializable
data class RpcRequest<T>(
    val method: String,
    val params: T
)

@Serializable
data class SwapRateParams(
    val amountIn: Double,
    val isXtoY: Boolean,
    val tokenXMint: String,
    val tokenYMint: String
)

class SwapRepository(
    private val networkSettings: fi.darklake.wallet.data.model.NetworkSettings
) {
    
    companion object {
        // Staging/Development endpoints
        private const val RPC_BASE_URL_STAGING = "https://dex-web-staging.dex.darklake.fi"
        private const val DEX_GATEWAY_URL_STAGING = "https://dex-gateway-staging.dex.darklake.fi:50051"
        
        // Production endpoints  
        private const val RPC_BASE_URL_PRODUCTION = "https://dex-web.dex.darklake.fi"
        private const val DEX_GATEWAY_URL_PRODUCTION = "https://dex-gateway.dex.darklake.fi:50051"
    }
    
    private val rpcBaseUrl: String
        get() = when (networkSettings.network) {
            fi.darklake.wallet.data.model.SolanaNetwork.MAINNET -> RPC_BASE_URL_PRODUCTION
            fi.darklake.wallet.data.model.SolanaNetwork.DEVNET -> RPC_BASE_URL_STAGING
        }
    
    private val dexGatewayUrl: String
        get() = when (networkSettings.network) {
            fi.darklake.wallet.data.model.SolanaNetwork.MAINNET -> DEX_GATEWAY_URL_PRODUCTION
            fi.darklake.wallet.data.model.SolanaNetwork.DEVNET -> DEX_GATEWAY_URL_STAGING
        }
    
    private val networkId: Int
        get() = when (networkSettings.network) {
            fi.darklake.wallet.data.model.SolanaNetwork.MAINNET -> 1 // Mainnet network ID
            fi.darklake.wallet.data.model.SolanaNetwork.DEVNET -> 2  // Devnet network ID  
        }
    
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
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
            val request = SwapQuoteRequest(
                amountIn = amountIn,
                isXtoY = isXtoY,
                slippage = slippage,
                tokenXMint = tokenXMint,
                tokenYMint = tokenYMint
            )
            
            val rpcRequest = RpcRequest(
                method = "swap.getSwapQuote",
                params = request
            )
            
            val response: HttpResponse = httpClient.post("$rpcBaseUrl/rpc") {
                contentType(ContentType.Application.Json)
                setBody(rpcRequest)
            }
            
            if (response.status.isSuccess()) {
                val quote = response.body<SwapQuoteResponse>()
                Result.success(quote)
            } else {
                Result.failure(Exception("Failed to get quote: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSwapRate(
        amountIn: Double,
        isXtoY: Boolean,
        tokenXMint: String,
        tokenYMint: String
    ): Result<SwapRateResponse> {
        return try {
            val params = SwapRateParams(
                amountIn = amountIn,
                isXtoY = isXtoY,
                tokenXMint = tokenXMint,
                tokenYMint = tokenYMint
            )
            
            val rpcRequest = RpcRequest(
                method = "swap.getSwapRate",
                params = params
            )
            
            val response: HttpResponse = httpClient.post("$rpcBaseUrl/rpc") {
                contentType(ContentType.Application.Json)
                setBody(rpcRequest)
            }
            
            if (response.status.isSuccess()) {
                val rate = response.body<SwapRateResponse>()
                Result.success(rate)
            } else {
                Result.failure(Exception("Failed to get rate: ${response.status}"))
            }
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
        trackingId: String = "id${System.currentTimeMillis()}"
    ): Result<SwapResponse> {
        return try {
            val request = SwapRequest(
                amountIn = amountIn,
                isSwapXtoY = isSwapXtoY,
                minOut = minOut,
                network = networkId,
                tokenMintX = tokenMintX,
                tokenMintY = tokenMintY,
                trackingId = trackingId,
                userAddress = userAddress
            )
            
            val rpcRequest = RpcRequest(
                method = "dexGateway.getSwap",
                params = request
            )
            
            val response: HttpResponse = httpClient.post("$rpcBaseUrl/rpc") {
                contentType(ContentType.Application.Json)
                setBody(rpcRequest)
            }
            
            if (response.status.isSuccess()) {
                val swapResponse = response.body<SwapResponse>()
                Result.success(swapResponse)
            } else {
                Result.failure(Exception("Failed to create swap: ${response.status}"))
            }
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
            val request = SignedTransactionRequest(
                signedTransaction = signedTransaction,
                trackingId = trackingId,
                tradeId = tradeId
            )
            
            val rpcRequest = RpcRequest(
                method = "dexGateway.submitSignedTransaction",
                params = request
            )
            
            val response: HttpResponse = httpClient.post("$rpcBaseUrl/rpc") {
                contentType(ContentType.Application.Json)
                setBody(rpcRequest)
            }
            
            if (response.status.isSuccess()) {
                val submitResponse = response.body<SignedTransactionResponse>()
                Result.success(submitResponse)
            } else {
                Result.failure(Exception("Failed to submit transaction: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun checkTradeStatus(
        trackingId: String,
        tradeId: String
    ): Result<TradeStatusResponse> {
        return try {
            val request = TradeStatusRequest(
                trackingId = trackingId,
                tradeId = tradeId
            )
            
            val rpcRequest = RpcRequest(
                method = "dexGateway.checkTradeStatus",
                params = request
            )
            
            val response: HttpResponse = httpClient.post("$rpcBaseUrl/rpc") {
                contentType(ContentType.Application.Json)
                setBody(rpcRequest)
            }
            
            if (response.status.isSuccess()) {
                val statusResponse = response.body<TradeStatusResponse>()
                Result.success(statusResponse)
            } else {
                Result.failure(Exception("Failed to check status: ${response.status}"))
            }
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
    
    suspend fun getPoolDetails(
        tokenXMint: String,
        tokenYMint: String
    ): Result<PoolDetails> {
        return try {
            val request = PoolDetailsRequest(
                tokenXMint = tokenXMint,
                tokenYMint = tokenYMint
            )
            
            val rpcRequest = RpcRequest(
                method = "pools.getPoolDetails",
                params = request
            )
            
            val response: HttpResponse = httpClient.post("$rpcBaseUrl/rpc") {
                contentType(ContentType.Application.Json)
                setBody(rpcRequest)
            }
            
            if (response.status.isSuccess()) {
                val poolDetails = response.body<PoolDetails>()
                Result.success(poolDetails)
            } else {
                Result.failure(Exception("Failed to get pool details: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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

data class SwapRateResponse(
    val amountIn: Double,
    val amountInRaw: String,
    val amountOut: Double,
    val amountOutRaw: String,
    val rate: Double,
    val priceImpact: Double,
    val estimatedFee: Double,
    val tokenX: TokenInfo? = null,
    val tokenY: TokenInfo? = null
)