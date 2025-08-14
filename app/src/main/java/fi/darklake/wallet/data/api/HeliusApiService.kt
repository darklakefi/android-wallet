package fi.darklake.wallet.data.api

import fi.darklake.wallet.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SolanaApiService(
    private val getRpcUrl: () -> String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(this@SolanaApiService.json)
        }
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    println("HTTP Client: $message")
                }
            }
        }
        engine {
            connectTimeout = 30_000
            socketTimeout = 30_000
        }
    }

    @Serializable
    private data class JsonRpcRequest(
        @SerialName("jsonrpc")
        val jsonrpc: String = "2.0",
        @SerialName("id")
        val id: String = "1",
        @SerialName("method")
        val method: String,
        @SerialName("params")
        val params: List<kotlinx.serialization.json.JsonElement>
    )

    suspend fun getBalance(publicKey: String): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val rpcUrl = getRpcUrl()
            println("=== HELIUS API REQUEST ===")
            println("Making request to: $rpcUrl")
            println("Public key: $publicKey")
            
            // Log if this is using Helius or fallback
            if (rpcUrl.contains("helius")) {
                println("Using Helius RPC endpoint")
            } else {
                println("Using standard Solana RPC endpoint (no Helius API key)")
            }
            
            // First, test basic connectivity
            try {
                val testResponse = client.get(rpcUrl)
                println("Basic connectivity test - Status: ${testResponse.status}")
            } catch (e: Exception) {
                println("Connectivity test failed: ${e.message}")
                return@withContext Result.failure(Exception("Network connectivity issue: ${e.message}"))
            }
            
            // For demo purposes, if no valid public key, use a test address
            val testPublicKey = if (publicKey.isBlank() || publicKey.length < 32) {
                "11111111111111111111111111111112" // System program ID - always exists
            } else {
                publicKey
            }
            
            // Create the JSON-RPC request
            val requestBody = JsonRpcRequest(
                method = "getBalance",
                params = listOf(
                    kotlinx.serialization.json.JsonPrimitive(testPublicKey)
                )
            )
            
            val jsonString = json.encodeToString(JsonRpcRequest.serializer(), requestBody)
            println("Request body to be sent: $jsonString")
            
            val response = client.post(rpcUrl) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(jsonString)
            }

            println("Response status: ${response.status}")
            println("Response content-type: ${response.headers["Content-Type"]}")
            
            // Get raw response as text first
            val rawResponse = response.bodyAsText()
            println("Raw response first 500 chars: ${rawResponse.take(500)}")
            
            // Check if response is HTML (common error response)
            if (rawResponse.contains("<html", ignoreCase = true)) {
                println("ERROR: Received HTML response instead of JSON")
                return@withContext Result.failure(Exception("API returned HTML instead of JSON. Check if the API key is valid."))
            }
            
            val responseBody = rawResponse
            println("Raw response body: '$responseBody'")
            println("Response body length: ${responseBody.length}")
            
            if (responseBody.isBlank()) {
                println("ERROR: Received empty response body")
                println("Response status code: ${response.status.value}")
                println("Response status description: ${response.status.description}")
                return@withContext Result.failure(Exception("Empty response from API. Status: ${response.status}. Make sure you have configured a valid Helius API key in Settings."))
            }
            
            println("Helius API Response: $responseBody")
            
            // Try to parse the response
            val balanceResponse = json.decodeFromString<HeliusBalanceResponse>(responseBody)
            
            // Check for API errors
            if (balanceResponse.error != null) {
                return@withContext Result.failure(Exception("Helius API Error: ${balanceResponse.error.message}"))
            }
            
            if (balanceResponse.result == null) {
                return@withContext Result.failure(Exception("No result in Helius API response"))
            }
            
            val lamports = balanceResponse.result.value
            val sol = lamports / 1_000_000_000.0 // Convert lamports to SOL
            Result.success(sol)
        } catch (e: Exception) {
            println("Solana API Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getTokenAccounts(publicKey: String): Result<List<TokenInfo>> = withContext(Dispatchers.IO) {
        try {
            val tokenRequest = JsonRpcRequest(
                method = "getTokenAccountsByOwner",
                params = listOf(
                    kotlinx.serialization.json.JsonPrimitive(publicKey),
                    kotlinx.serialization.json.buildJsonObject {
                        put("programId", kotlinx.serialization.json.JsonPrimitive("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"))
                    },
                    kotlinx.serialization.json.buildJsonObject {
                        put("encoding", kotlinx.serialization.json.JsonPrimitive("jsonParsed"))
                    }
                )
            )
            
            val tokenJsonString = json.encodeToString(JsonRpcRequest.serializer(), tokenRequest)
            
            val response = client.post(getRpcUrl()) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(tokenJsonString)
            }

            val responseBody = response.body<String>()
            if (responseBody.isBlank()) {
                return@withContext Result.success(emptyList())
            }
            
            println("Helius Token API Response: $responseBody")
            
            val tokenResponse = json.decodeFromString<HeliusTokenResponse>(responseBody)
            
            // Check for API errors
            if (tokenResponse.error != null) {
                println("Token API Error: ${tokenResponse.error.message}")
                return@withContext Result.success(emptyList())
            }
            
            if (tokenResponse.result == null) {
                return@withContext Result.success(emptyList())
            }
            
            val tokens = tokenResponse.result.value.mapNotNull { tokenAccount ->
                val tokenInfo = tokenAccount.account.data.parsed.info
                if (tokenInfo.tokenAmount.uiAmount != null && tokenInfo.tokenAmount.uiAmount!! > 0.0) {
                    TokenInfo(
                        balance = TokenBalance(
                            mint = tokenInfo.mint,
                            amount = tokenInfo.tokenAmount.amount,
                            decimals = tokenInfo.tokenAmount.decimals,
                            uiAmount = tokenInfo.tokenAmount.uiAmount,
                            uiAmountString = tokenInfo.tokenAmount.uiAmountString
                        ),
                        metadata = null // Will be fetched separately
                    )
                } else null
            }
            Result.success(tokens)
        } catch (e: Exception) {
            println("Token accounts error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getTokenMetadata(mints: List<String>): Result<List<TokenMetadata>> = withContext(Dispatchers.IO) {
        try {
            if (mints.isEmpty()) {
                return@withContext Result.success(emptyList())
            }
            
            println("=== JUPITER TOKEN METADATA FETCH ===")
            println("Fetching metadata for ${mints.size} tokens")
            
            // Use Jupiter's token list API for metadata
            // This is a public API that doesn't require authentication
            val jupiterTokenListUrl = "https://token.jup.ag/strict"
            
            val response = client.get(jupiterTokenListUrl) {
                accept(ContentType.Application.Json)
            }
            
            println("Jupiter API Response status: ${response.status}")
            
            if (response.status != HttpStatusCode.OK) {
                println("Jupiter API request failed with status: ${response.status}")
                return@withContext Result.success(emptyList())
            }
            
            val responseBody = response.bodyAsText()
            println("Jupiter API Response received (${responseBody.length} chars)")
            
            // Parse the Jupiter token list
            val jupiterTokens = json.decodeFromString<List<JupiterToken>>(responseBody)
            println("Parsed ${jupiterTokens.size} tokens from Jupiter")
            
            // Filter tokens that match our mint addresses
            val matchingTokens = jupiterTokens.filter { token ->
                mints.contains(token.address)
            }.map { token ->
                TokenMetadata(
                    mint = token.address,
                    name = token.name,
                    symbol = token.symbol,
                    description = null, // Jupiter doesn't provide description
                    image = token.logoURI,
                    decimals = token.decimals
                )
            }
            
            println("Found metadata for ${matchingTokens.size} out of ${mints.size} requested tokens")
            Result.success(matchingTokens)
        } catch (e: Exception) {
            println("Token metadata fetch error: ${e.message}")
            e.printStackTrace()
            // Don't fail the whole operation if metadata fetching fails
            Result.success(emptyList())
        }
    }

    suspend fun getNftsByOwner(publicKey: String): Result<List<NftMetadata>> = withContext(Dispatchers.IO) {
        try {
            val rpcUrl = getRpcUrl()
            
            // Only use Helius DAS API if we have a Helius endpoint
            if (!rpcUrl.contains("helius")) {
                println("NFT fetch skipped - requires Helius API key")
                return@withContext Result.success(emptyList())
            }
            
            println("=== HELIUS DAS API NFT FETCH ===")
            println("Fetching NFTs for owner: $publicKey")
            
            // Use Helius Digital Asset Standard (DAS) API for NFTs
            val dasRequest = JsonRpcRequest(
                method = "getAssetsByOwner",
                params = listOf(
                    kotlinx.serialization.json.buildJsonObject {
                        put("ownerAddress", kotlinx.serialization.json.JsonPrimitive(publicKey))
                        put("page", kotlinx.serialization.json.JsonPrimitive(1))
                        put("limit", kotlinx.serialization.json.JsonPrimitive(1000))
                        put("displayOptions", kotlinx.serialization.json.buildJsonObject {
                            put("showFungible", kotlinx.serialization.json.JsonPrimitive(false))
                            put("showNativeBalance", kotlinx.serialization.json.JsonPrimitive(false))
                        })
                    }
                )
            )
            
            val jsonString = json.encodeToString(JsonRpcRequest.serializer(), dasRequest)
            println("DAS Request: $jsonString")
            
            val response = client.post(rpcUrl) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(jsonString)
            }
            
            println("DAS API Response status: ${response.status}")
            
            if (response.status != HttpStatusCode.OK) {
                println("DAS API request failed with status: ${response.status}")
                return@withContext Result.success(emptyList())
            }
            
            val responseBody = response.bodyAsText()
            println("DAS API Response received (${responseBody.length} chars)")
            
            if (responseBody.isBlank()) {
                println("Empty response from DAS API")
                return@withContext Result.success(emptyList())
            }
            
            // Parse the DAS response
            val dasResponse = json.decodeFromString<HeliusDasResponse>(responseBody)
            
            if (dasResponse.error != null) {
                println("DAS API Error: ${dasResponse.error.message}")
                return@withContext Result.success(emptyList())
            }
            
            if (dasResponse.result == null) {
                println("No result in DAS API response")
                return@withContext Result.success(emptyList())
            }
            
            val nfts = dasResponse.result.items.mapNotNull { asset ->
                // Only process assets that are NFTs (non-fungible)
                if (asset.interface == "V1_NFT" || asset.interface == "ProgrammableNFT") {
                    NftMetadata(
                        mint = asset.id,
                        name = asset.content?.metadata?.name,
                        symbol = asset.content?.metadata?.symbol,
                        description = asset.content?.metadata?.description,
                        image = asset.content?.files?.firstOrNull()?.uri ?: asset.content?.jsonUri,
                        externalUrl = asset.content?.metadata?.externalUrl,
                        attributes = asset.content?.metadata?.attributes?.map { attr ->
                            NftAttribute(
                                traitType = attr.traitType ?: "",
                                value = attr.value ?: ""
                            )
                        },
                        collection = asset.grouping?.firstOrNull { it.groupKey == "collection" }?.let { group ->
                            NftCollection(
                                name = group.groupValue,
                                family = null
                            )
                        }
                    )
                } else null
            }
            
            println("Successfully fetched ${nfts.size} NFTs")
            Result.success(nfts)
        } catch (e: Exception) {
            println("NFT fetch error: ${e.message}")
            e.printStackTrace()
            // Don't fail the whole operation if NFT fetching fails
            Result.success(emptyList())
        }
    }

    fun close() {
        client.close()
    }
}

// Repository to coordinate API calls
class WalletAssetsRepository(
    private val solanaApi: SolanaApiService
) {
    suspend fun getWalletAssets(publicKey: String): Result<WalletAssets> {
        try {
            val balanceResult = solanaApi.getBalance(publicKey)
            val tokensResult = solanaApi.getTokenAccounts(publicKey)
            val nftsResult = solanaApi.getNftsByOwner(publicKey)

            if (balanceResult.isFailure) {
                return Result.failure(balanceResult.exceptionOrNull() ?: Exception("Failed to get balance"))
            }

            val solBalance = balanceResult.getOrNull() ?: 0.0
            val tokens = tokensResult.getOrNull() ?: emptyList()
            val nfts = nftsResult.getOrNull() ?: emptyList()

            // Fetch token metadata for all tokens
            val tokenMints = tokens.map { it.balance.mint }
            val metadataResult = solanaApi.getTokenMetadata(tokenMints)
            val tokenMetadata = metadataResult.getOrNull() ?: emptyList()
            
            // Associate metadata with tokens
            val tokensWithMetadata = tokens.map { token ->
                val metadata = tokenMetadata.find { it.mint == token.balance.mint }
                token.copy(metadata = metadata)
            }

            return Result.success(WalletAssets(
                solBalance = solBalance,
                tokens = tokensWithMetadata,
                nfts = nfts
            ))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun close() {
        solanaApi.close()
    }
}