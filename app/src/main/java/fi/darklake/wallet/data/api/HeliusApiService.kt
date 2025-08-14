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
            // Helius provides token metadata through their enhanced APIs
            // For now, return empty list - would need to implement metadata fetching
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNftsByOwner(publicKey: String): Result<List<NftMetadata>> = withContext(Dispatchers.IO) {
        try {
            // For now, return empty list to keep the app working
            // NFT functionality can be implemented later when the basic RPC is working
            Result.success(emptyList())
        } catch (e: Exception) {
            println("NFT fetch error: ${e.message}")
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

            return Result.success(WalletAssets(
                solBalance = solBalance,
                tokens = tokens,
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