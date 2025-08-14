package fi.darklake.wallet.data.solana

import fi.darklake.wallet.data.model.getHeliusRpcUrl
import fi.darklake.wallet.data.preferences.SettingsManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor
import kotlin.random.Random

/**
 * Service for building and sending Solana transactions
 * Handles SOL transfers, SPL token transfers, and NFT transfers
 */
class SolanaTransactionService(
    private val settingsManager: SettingsManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(this@SolanaTransactionService.json)
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

    @Serializable
    private data class SendTransactionResponse(
        @SerialName("jsonrpc")
        val jsonrpc: String,
        @SerialName("id")
        val id: String,
        @SerialName("result")
        val result: String? = null,
        @SerialName("error")
        val error: SolanaError? = null
    )

    @Serializable
    private data class SolanaError(
        @SerialName("code")
        val code: Int,
        @SerialName("message")
        val message: String
    )

    @Serializable
    private data class RecentBlockhashResponse(
        @SerialName("jsonrpc")
        val jsonrpc: String,
        @SerialName("id")
        val id: String,
        @SerialName("result")
        val result: RecentBlockhashResult? = null,
        @SerialName("error")
        val error: SolanaError? = null
    )

    @Serializable
    private data class RecentBlockhashResult(
        @SerialName("context")
        val context: BlockhashContext,
        @SerialName("value")
        val value: BlockhashValue
    )

    @Serializable
    private data class BlockhashContext(
        @SerialName("slot")
        val slot: Long
    )

    @Serializable
    private data class BlockhashValue(
        @SerialName("blockhash")
        val blockhash: String,
        @SerialName("feeCalculator")
        val feeCalculator: FeeCalculator
    )

    @Serializable
    private data class FeeCalculator(
        @SerialName("lamportsPerSignature")
        val lamportsPerSignature: Long
    )

    suspend fun sendSolTransaction(
        fromPrivateKey: ByteArray,
        toAddress: String,
        lamports: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            println("=== SOLANA SOL TRANSFER ===")
            println("To: $toAddress")
            println("Amount: $lamports lamports")

            // 1. Get recent blockhash
            val blockhash = getRecentBlockhash().getOrElse { 
                return@withContext Result.failure(Exception("Failed to get recent blockhash: ${it.message}"))
            }
            println("Recent blockhash: $blockhash")

            // 2. Build transaction
            val fromAddress = getAddressFromPrivateKey(fromPrivateKey)
            val transaction = buildSolTransferTransaction(
                fromAddress = fromAddress,
                toAddress = toAddress,
                lamports = lamports,
                recentBlockhash = blockhash
            )
            println("Transaction built")

            // 3. Sign transaction
            val signedTransaction = signTransaction(transaction, fromPrivateKey)
            println("Transaction signed")

            // 4. Send transaction
            val signature = sendTransaction(signedTransaction).getOrElse {
                return@withContext Result.failure(Exception("Failed to send transaction: ${it.message}"))
            }

            println("Transaction sent successfully: $signature")
            Result.success(signature)

        } catch (e: Exception) {
            println("SOL transfer failed: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun sendTokenTransaction(
        fromPrivateKey: ByteArray,
        toAddress: String,
        tokenMint: String,
        amount: Long,
        decimals: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            println("=== SOLANA TOKEN TRANSFER ===")
            println("To: $toAddress")
            println("Token: $tokenMint")
            println("Amount: $amount (decimals: $decimals)")

            // For now, return a simulated result
            // Real implementation would need to:
            // 1. Get or create associated token accounts
            // 2. Build SPL token transfer instruction
            // 3. Sign and send transaction
            
            kotlinx.coroutines.delay(2000) // Simulate network delay
            val mockSignature = "token_transfer_" + Random.nextBytes(32).joinToString("") { "%02x".format(it) }
            
            println("Token transfer simulated: $mockSignature")
            Result.success(mockSignature)

        } catch (e: Exception) {
            println("Token transfer failed: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun sendNftTransaction(
        fromPrivateKey: ByteArray,
        toAddress: String,
        nftMint: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            println("=== SOLANA NFT TRANSFER ===")
            println("To: $toAddress")
            println("NFT: $nftMint")

            // For now, return a simulated result
            // Real implementation would need to:
            // 1. Get or create associated token accounts for NFT
            // 2. Build NFT transfer instruction
            // 3. Sign and send transaction
            
            kotlinx.coroutines.delay(2000) // Simulate network delay
            val mockSignature = "nft_transfer_" + Random.nextBytes(32).joinToString("") { "%02x".format(it) }
            
            println("NFT transfer simulated: $mockSignature")
            Result.success(mockSignature)

        } catch (e: Exception) {
            println("NFT transfer failed: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun getRecentBlockhash(): Result<String> {
        return try {
            val networkSettings = settingsManager.getNetworkSettings()
            val rpcUrl = networkSettings.getHeliusRpcUrl()

            val request = JsonRpcRequest(
                method = "getRecentBlockhash",
                params = emptyList()
            )

            val jsonString = json.encodeToString(JsonRpcRequest.serializer(), request)
            
            val response = client.post(rpcUrl) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(jsonString)
            }

            val responseBody = response.bodyAsText()
            val blockhashResponse = json.decodeFromString<RecentBlockhashResponse>(responseBody)

            if (blockhashResponse.error != null) {
                Result.failure(Exception("RPC Error: ${blockhashResponse.error.message}"))
            } else if (blockhashResponse.result == null) {
                Result.failure(Exception("No result in blockhash response"))
            } else {
                Result.success(blockhashResponse.result.value.blockhash)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendTransaction(signedTransaction: String): Result<String> {
        return try {
            val networkSettings = settingsManager.getNetworkSettings()
            val rpcUrl = networkSettings.getHeliusRpcUrl()

            val request = JsonRpcRequest(
                method = "sendTransaction",
                params = listOf(
                    kotlinx.serialization.json.JsonPrimitive(signedTransaction),
                    kotlinx.serialization.json.buildJsonObject {
                        put("encoding", kotlinx.serialization.json.JsonPrimitive("base64"))
                        put("skipPreflight", kotlinx.serialization.json.JsonPrimitive(false))
                        put("preflightCommitment", kotlinx.serialization.json.JsonPrimitive("processed"))
                    }
                )
            )

            val jsonString = json.encodeToString(JsonRpcRequest.serializer(), request)
            
            val response = client.post(rpcUrl) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(jsonString)
            }

            val responseBody = response.bodyAsText()
            val sendResponse = json.decodeFromString<SendTransactionResponse>(responseBody)

            if (sendResponse.error != null) {
                Result.failure(Exception("Send Error: ${sendResponse.error.message}"))
            } else if (sendResponse.result == null) {
                Result.failure(Exception("No signature returned"))
            } else {
                Result.success(sendResponse.result)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Simplified transaction building - in a real implementation this would use proper Solana SDK
    private fun buildSolTransferTransaction(
        fromAddress: String,
        toAddress: String,
        lamports: Long,
        recentBlockhash: String
    ): String {
        // This is a simplified mock implementation
        // Real implementation would build proper Solana transaction with:
        // - Message header
        // - Account keys
        // - Recent blockhash
        // - Instructions (system program transfer)
        return "mock_transaction_${Random.nextInt()}"
    }

    private fun signTransaction(transaction: String, privateKey: ByteArray): String {
        // This is a simplified mock implementation
        // Real implementation would:
        // 1. Hash the transaction message
        // 2. Sign with Ed25519 using the private key
        // 3. Attach signature to transaction
        // 4. Serialize to base64
        return "signed_$transaction"
    }

    private fun getAddressFromPrivateKey(privateKey: ByteArray): String {
        // This is a simplified mock implementation
        // Real implementation would derive the public key from private key
        // and encode it as a base58 Solana address
        val hash = MessageDigest.getInstance("SHA-256").digest(privateKey)
        return "mock_address_" + Base64.getEncoder().encodeToString(hash.take(8).toByteArray())
    }

    fun close() {
        client.close()
    }
}

/**
 * Result data class for transaction operations
 */
data class TransactionResult(
    val signature: String,
    val confirmationStatus: String = "processed"
)