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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

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
        val params: kotlinx.serialization.json.JsonElement
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
                params = kotlinx.serialization.json.buildJsonArray {
                    add(kotlinx.serialization.json.JsonPrimitive(testPublicKey))
                }
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
            // Token Program ID (classic SPL Token)
            val tokenProgramId = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
            // Token-2022 Program ID (Token Extensions)
            val token2022ProgramId = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb"
            
            // Fetch both Token and Token-2022 accounts in parallel
            val results = awaitAll(
                async { fetchTokenAccountsByProgram(publicKey, tokenProgramId) },
                async { fetchTokenAccountsByProgram(publicKey, token2022ProgramId) }
            )
            
            // Combine results from both programs
            val allTokens = mutableListOf<TokenInfo>()
            
            results[0].fold(
                onSuccess = { tokens -> allTokens.addAll(tokens) },
                onFailure = { e -> println("Error fetching Token accounts: ${e.message}") }
            )
            
            results[1].fold(
                onSuccess = { tokens -> allTokens.addAll(tokens) },
                onFailure = { e -> println("Error fetching Token-2022 accounts: ${e.message}") }
            )
            
            Result.success(allTokens)
        } catch (e: Exception) {
            println("Token accounts error: ${e.message}")
            Result.failure(e)
        }
    }
    
    private suspend fun fetchTokenAccountsByProgram(publicKey: String, programId: String): Result<List<TokenInfo>> = withContext(Dispatchers.IO) {
        try {
            val tokenRequest = JsonRpcRequest(
                method = "getTokenAccountsByOwner",
                params = kotlinx.serialization.json.buildJsonArray {
                    add(kotlinx.serialization.json.JsonPrimitive(publicKey))
                    add(kotlinx.serialization.json.buildJsonObject {
                        put("programId", kotlinx.serialization.json.JsonPrimitive(programId))
                    })
                    add(kotlinx.serialization.json.buildJsonObject {
                        put("encoding", kotlinx.serialization.json.JsonPrimitive("jsonParsed"))
                    })
                }
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
            
            println("Helius Token API Response for $programId: $responseBody")
            
            val tokenResponse = json.decodeFromString<HeliusTokenResponse>(responseBody)
            
            // Check for API errors
            if (tokenResponse.error != null) {
                println("Token API Error for $programId: ${tokenResponse.error.message}")
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
            println("Token accounts error for $programId: ${e.message}")
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

    suspend fun getCompressedTokensByOwner(publicKey: String): Result<List<CompressedTokenBalance>> = withContext(Dispatchers.IO) {
        try {
            val rpcUrl = getRpcUrl()
            
            // Only use Helius if we have a Helius endpoint
            if (!rpcUrl.contains("helius")) {
                println("Compressed token fetch skipped - requires Helius API key")
                return@withContext Result.success(emptyList())
            }
            
            println("=== LIGHT PROTOCOL COMPRESSED TOKENS FETCH ===")
            println("Fetching compressed tokens for owner: $publicKey")
            
            // Use DAS API to get compressed tokens - params should be a direct object, not wrapped in array
            val compressedTokenRequest = JsonRpcRequest(
                method = "getAssetsByOwner",
                params = kotlinx.serialization.json.buildJsonObject {
                    put("ownerAddress", kotlinx.serialization.json.JsonPrimitive(publicKey))
                    put("page", kotlinx.serialization.json.JsonPrimitive(1))
                    put("limit", kotlinx.serialization.json.JsonPrimitive(1000))
                    put("displayOptions", kotlinx.serialization.json.buildJsonObject {
                        put("showFungible", kotlinx.serialization.json.JsonPrimitive(true))
                        put("showNativeBalance", kotlinx.serialization.json.JsonPrimitive(false))
                    })
                }
            )
            
            val jsonString = json.encodeToString(JsonRpcRequest.serializer(), compressedTokenRequest)
            println("Compressed Token Request: $jsonString")
            
            val response = client.post(rpcUrl) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(jsonString)
            }
            
            println("Compressed Token API Response status: ${response.status}")
            
            if (response.status != HttpStatusCode.OK) {
                println("Compressed Token API request failed with status: ${response.status}")
                return@withContext Result.success(emptyList())
            }
            
            val responseBody = response.bodyAsText()
            println("Compressed Token API Response received (${responseBody.length} chars)")
            
            if (responseBody.isBlank()) {
                println("Empty response from Compressed Token API")
                return@withContext Result.success(emptyList())
            }
            
            // Parse the DAS response for compressed tokens
            val dasResponse = json.decodeFromString<HeliusDasResponse>(responseBody)
            
            if (dasResponse.error != null) {
                println("Compressed Token API Error: ${dasResponse.error.message}")
                return@withContext Result.success(emptyList())
            }
            
            if (dasResponse.result == null) {
                println("No result in Compressed Token API response")
                return@withContext Result.success(emptyList())
            }
            
            val compressedTokens = dasResponse.result.items.mapNotNull { asset ->
                println("Processing asset: ${asset.id}, interface: ${asset.`interface`}, compressed: ${asset.compression?.compressed}")
                // Only process assets that are fungible tokens and compressed
                if (asset.`interface` == "FungibleToken" && asset.compression?.compressed == true) {
                    // Extract token information from the asset
                    CompressedTokenBalance(
                        mint = asset.id,
                        amount = "0", // Would need additional API call to get actual balance
                        decimals = 6, // Default, would need metadata lookup
                        compressed = true
                    )
                } else null
            }
            
            println("Successfully fetched ${compressedTokens.size} compressed tokens")
            Result.success(compressedTokens)
        } catch (e: Exception) {
            println("Compressed token fetch error: ${e.message}")
            e.printStackTrace()
            // Don't fail the whole operation if compressed token fetching fails
            Result.success(emptyList())
        }
    }

    suspend fun getCompressedNftsByOwner(publicKey: String): Result<List<CompressedNftMetadata>> = withContext(Dispatchers.IO) {
        try {
            val rpcUrl = getRpcUrl()
            
            // Only use Helius if we have a Helius endpoint
            if (!rpcUrl.contains("helius")) {
                println("Compressed NFT fetch skipped - requires Helius API key")
                return@withContext Result.success(emptyList())
            }
            
            println("=== LIGHT PROTOCOL COMPRESSED NFTS FETCH ===")
            println("Fetching compressed NFTs for owner: $publicKey")
            
            // Use DAS API to get all assets then filter for compressed NFTs - params should be a direct object
            val compressedNftRequest = JsonRpcRequest(
                method = "getAssetsByOwner",
                params = kotlinx.serialization.json.buildJsonObject {
                    put("ownerAddress", kotlinx.serialization.json.JsonPrimitive(publicKey))
                    put("page", kotlinx.serialization.json.JsonPrimitive(1))
                    put("limit", kotlinx.serialization.json.JsonPrimitive(1000))
                    put("displayOptions", kotlinx.serialization.json.buildJsonObject {
                        put("showFungible", kotlinx.serialization.json.JsonPrimitive(false))
                        put("showNativeBalance", kotlinx.serialization.json.JsonPrimitive(false))
                    })
                    // Don't filter in the request, we'll filter in the response processing
                }
            )
            
            val jsonString = json.encodeToString(JsonRpcRequest.serializer(), compressedNftRequest)
            println("Compressed NFT Request: $jsonString")
            
            val response = client.post(rpcUrl) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(jsonString)
            }
            
            println("Compressed NFT API Response status: ${response.status}")
            
            if (response.status != HttpStatusCode.OK) {
                println("Compressed NFT API request failed with status: ${response.status}")
                return@withContext Result.success(emptyList())
            }
            
            val responseBody = response.bodyAsText()
            println("Compressed NFT API Response received (${responseBody.length} chars)")
            
            if (responseBody.isBlank()) {
                println("Empty response from Compressed NFT API")
                return@withContext Result.success(emptyList())
            }
            
            // Parse the DAS response for compressed NFTs
            val dasResponse = json.decodeFromString<HeliusDasResponse>(responseBody)
            
            if (dasResponse.error != null) {
                println("Compressed NFT API Error: ${dasResponse.error.message}")
                return@withContext Result.success(emptyList())
            }
            
            if (dasResponse.result == null) {
                println("No result in Compressed NFT API response")
                return@withContext Result.success(emptyList())
            }
            
            val compressedNfts = dasResponse.result.items.mapNotNull { asset ->
                println("Processing NFT asset: ${asset.id}, interface: ${asset.`interface`}, compressed: ${asset.compression?.compressed}")
                // Only process assets that are NFTs and compressed
                if ((asset.`interface` == "V1_NFT" || asset.`interface` == "ProgrammableNFT") && 
                    asset.compression?.compressed == true) {
                    println("Found compressed NFT: ${asset.content?.metadata?.name}")
                    CompressedNftMetadata(
                        id = asset.id,
                        name = asset.content?.metadata?.name,
                        symbol = asset.content?.metadata?.symbol,
                        description = asset.content?.metadata?.description,
                        image = asset.content?.files?.firstOrNull()?.uri ?: asset.content?.jsonUri,
                        externalUrl = asset.content?.metadata?.externalUrl,
                        compressed = true,
                        compression = asset.compression?.let { compression ->
                            CompressedAssetCompression(
                                eligible = compression.eligible,
                                compressed = compression.compressed,
                                dataHash = compression.dataHash,
                                creatorHash = compression.creatorHash,
                                assetHash = compression.assetHash,
                                tree = compression.tree,
                                seq = compression.seq,
                                leafId = compression.leafId
                            )
                        }
                    )
                } else null
            }
            
            println("Successfully fetched ${compressedNfts.size} compressed NFTs")
            Result.success(compressedNfts)
        } catch (e: Exception) {
            println("Compressed NFT fetch error: ${e.message}")
            e.printStackTrace()
            // Don't fail the whole operation if compressed NFT fetching fails
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
            
            // Use Helius Digital Asset Standard (DAS) API for NFTs - params should be a direct object
            val dasRequest = JsonRpcRequest(
                method = "getAssetsByOwner",
                params = kotlinx.serialization.json.buildJsonObject {
                    put("ownerAddress", kotlinx.serialization.json.JsonPrimitive(publicKey))
                    put("page", kotlinx.serialization.json.JsonPrimitive(1))
                    put("limit", kotlinx.serialization.json.JsonPrimitive(1000))
                    put("displayOptions", kotlinx.serialization.json.buildJsonObject {
                        put("showFungible", kotlinx.serialization.json.JsonPrimitive(false))
                        put("showNativeBalance", kotlinx.serialization.json.JsonPrimitive(false))
                    })
                }
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
                if (asset.`interface` == "V1_NFT" || asset.`interface` == "ProgrammableNFT") {
                    NftMetadata(
                        mint = asset.id,
                        name = asset.content?.metadata?.name,
                        symbol = asset.content?.metadata?.symbol,
                        description = asset.content?.metadata?.description,
                        image = asset.content?.files?.firstOrNull()?.uri ?: asset.content?.jsonUri,
                        externalUrl = asset.content?.metadata?.externalUrl,
                        attributes = null,  // TODO: Parse from JsonElement if needed
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
            val compressedTokensResult = solanaApi.getCompressedTokensByOwner(publicKey)
            val compressedNftsResult = solanaApi.getCompressedNftsByOwner(publicKey)

            if (balanceResult.isFailure) {
                return Result.failure(balanceResult.exceptionOrNull() ?: Exception("Failed to get balance"))
            }

            val solBalance = balanceResult.getOrNull() ?: 0.0
            val tokens = tokensResult.getOrNull() ?: emptyList()
            val nfts = nftsResult.getOrNull() ?: emptyList()
            val compressedTokens = compressedTokensResult.getOrNull() ?: emptyList()
            val compressedNfts = compressedNftsResult.getOrNull() ?: emptyList()

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
                nfts = nfts,
                compressedTokens = compressedTokens,
                compressedNfts = compressedNfts
            ))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun close() {
        solanaApi.close()
    }
}