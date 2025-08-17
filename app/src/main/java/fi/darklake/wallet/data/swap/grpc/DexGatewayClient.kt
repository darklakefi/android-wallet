package fi.darklake.wallet.data.swap.grpc

import fi.darklake.wallet.data.model.NetworkSettings
import fi.darklake.wallet.data.model.SolanaNetwork
import fi.darklake.wallet.grpc.CreateUnsignedTransactionRequest
import fi.darklake.wallet.grpc.CreateUnsignedTransactionResponse
import fi.darklake.wallet.grpc.SendSignedTransactionRequest
import fi.darklake.wallet.grpc.SendSignedTransactionResponse
import fi.darklake.wallet.grpc.CheckTradeStatusRequest
import fi.darklake.wallet.grpc.CheckTradeStatusResponse
import fi.darklake.wallet.grpc.GetTokenMetadataRequest
import fi.darklake.wallet.grpc.GetTokenMetadataResponse
import fi.darklake.wallet.grpc.GetTokenMetadataListRequest
import fi.darklake.wallet.grpc.GetTokenMetadataListResponse
import fi.darklake.wallet.grpc.GetTradesListByUserRequest
import fi.darklake.wallet.grpc.GetTradesListByUserResponse
import fi.darklake.wallet.grpc.TokenAddressesList
import fi.darklake.wallet.grpc.TokenSymbolsList
import fi.darklake.wallet.grpc.TokenNamesList
import fi.darklake.wallet.grpc.SolanaGatewayServiceGrpcKt
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DexGatewayClient(
    private val networkSettings: NetworkSettings
) {
    companion object {
        private const val GATEWAY_PORT = 50051
        private const val DEX_GATEWAY_HOST_STAGING = "dex-gateway-staging.dex.darklake.fi"
        private const val DEX_GATEWAY_HOST_PRODUCTION = "dex-gateway-prod.dex.darklake.fi"
    }
    
    private val gatewayHost: String
        get() = when (networkSettings.network) {
            SolanaNetwork.MAINNET -> DEX_GATEWAY_HOST_PRODUCTION
            SolanaNetwork.DEVNET -> DEX_GATEWAY_HOST_STAGING
        }
    
    private var channel: ManagedChannel? = null
    private var stub: SolanaGatewayServiceGrpcKt.SolanaGatewayServiceCoroutineStub? = null
    
    private fun ensureInitialized() {
        if (channel == null) {
            channel = ManagedChannelBuilder
                .forAddress(gatewayHost, GATEWAY_PORT)
                .usePlaintext() // Use TLS in production
                .build()
            
            stub = SolanaGatewayServiceGrpcKt.SolanaGatewayServiceCoroutineStub(channel!!)
        }
    }
    
    suspend fun createUnsignedTransaction(
        userAddress: String,
        tokenMintX: String,
        tokenMintY: String,
        amountIn: Long,
        minOut: Long,
        trackingId: String,
        isSwapXtoY: Boolean
    ): CreateUnsignedTransactionResponse = withContext(Dispatchers.IO) {
        ensureInitialized()
        
        val request = CreateUnsignedTransactionRequest.newBuilder()
            .setUserAddress(userAddress)
            .setTokenMintX(tokenMintX)
            .setTokenMintY(tokenMintY)
            .setAmountIn(amountIn)
            .setMinOut(minOut)
            .setTrackingId(trackingId)
            .setIsSwapXToY(isSwapXtoY)
            .build()
        
        stub!!.createUnsignedTransaction(request)
    }
    
    suspend fun sendSignedTransaction(
        signedTransaction: String,
        trackingId: String,
        tradeId: String
    ): SendSignedTransactionResponse = withContext(Dispatchers.IO) {
        ensureInitialized()
        
        val request = SendSignedTransactionRequest.newBuilder()
            .setSignedTransaction(signedTransaction)
            .setTrackingId(trackingId)
            .setTradeId(tradeId)
            .build()
        
        stub!!.sendSignedTransaction(request)
    }
    
    suspend fun checkTradeStatus(
        trackingId: String,
        tradeId: String
    ): CheckTradeStatusResponse = withContext(Dispatchers.IO) {
        ensureInitialized()
        
        val request = CheckTradeStatusRequest.newBuilder()
            .setTrackingId(trackingId)
            .setTradeId(tradeId)
            .build()
        
        stub!!.checkTradeStatus(request)
    }
    
    suspend fun getTokenMetadata(
        tokenAddress: String? = null,
        tokenSymbol: String? = null,
        tokenName: String? = null
    ): GetTokenMetadataResponse = withContext(Dispatchers.IO) {
        ensureInitialized()
        
        val requestBuilder = GetTokenMetadataRequest.newBuilder()
        
        when {
            tokenAddress != null -> requestBuilder.setTokenAddress(tokenAddress)
            tokenSymbol != null -> requestBuilder.setTokenSymbol(tokenSymbol)
            tokenName != null -> requestBuilder.setTokenName(tokenName)
            else -> throw IllegalArgumentException("Must provide either tokenAddress, tokenSymbol, or tokenName")
        }
        
        stub!!.getTokenMetadata(requestBuilder.build())
    }
    
    suspend fun getTokenMetadataList(
        addressesList: List<String>? = null,
        symbolsList: List<String>? = null,
        namesList: List<String>? = null,
        pageSize: Int = 50,
        pageNumber: Int = 1
    ): GetTokenMetadataListResponse = withContext(Dispatchers.IO) {
        ensureInitialized()
        
        val requestBuilder = GetTokenMetadataListRequest.newBuilder()
            .setPageSize(pageSize)
            .setPageNumber(pageNumber)
        
        when {
            addressesList != null -> {
                requestBuilder.setAddressesList(
                    TokenAddressesList.newBuilder()
                        .addAllTokenAddresses(addressesList)
                        .build()
                )
            }
            symbolsList != null -> {
                requestBuilder.setSymbolsList(
                    TokenSymbolsList.newBuilder()
                        .addAllTokenSymbols(symbolsList)
                        .build()
                )
            }
            namesList != null -> {
                requestBuilder.setNamesList(
                    TokenNamesList.newBuilder()
                        .addAllTokenNames(namesList)
                        .build()
                )
            }
        }
        
        stub!!.getTokenMetadataList(requestBuilder.build())
    }
    
    suspend fun getTradesListByUser(
        userAddress: String,
        pageSize: Int = 50,
        pageNumber: Int = 1
    ): GetTradesListByUserResponse = withContext(Dispatchers.IO) {
        ensureInitialized()
        
        val request = GetTradesListByUserRequest.newBuilder()
            .setUserAddress(userAddress)
            .setPageSize(pageSize)
            .setPageNumber(pageNumber)
            .build()
        
        stub!!.getTradesListByUser(request)
    }
    
    fun shutdown() {
        channel?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
        channel = null
        stub = null
    }
}