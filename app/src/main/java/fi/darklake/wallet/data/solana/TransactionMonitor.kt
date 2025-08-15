package fi.darklake.wallet.data.solana

import fi.darklake.wallet.data.model.getHeliusRpcUrl
import fi.darklake.wallet.data.preferences.SettingsManager
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Monitors Solana transactions for confirmation status
 */
class TransactionMonitor(
    private val settingsManager: SettingsManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(this@TransactionMonitor.json)
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
    private data class SignatureStatusResponse(
        @SerialName("jsonrpc")
        val jsonrpc: String,
        @SerialName("id")
        val id: String,
        @SerialName("result")
        val result: SignatureStatusResult? = null,
        @SerialName("error")
        val error: SolanaError? = null
    )
    
    @Serializable
    private data class SignatureStatusResult(
        @SerialName("context")
        val context: Context,
        @SerialName("value")
        val value: List<SignatureStatus?>
    )
    
    @Serializable
    private data class Context(
        @SerialName("slot")
        val slot: Long
    )
    
    @Serializable
    private data class SignatureStatus(
        @SerialName("slot")
        val slot: Long,
        @SerialName("confirmations")
        val confirmations: Int? = null,
        @SerialName("confirmationStatus")
        val confirmationStatus: String? = null,
        @SerialName("err")
        val err: kotlinx.serialization.json.JsonElement? = null
    )
    
    @Serializable
    private data class SolanaError(
        @SerialName("code")
        val code: Int,
        @SerialName("message")
        val message: String
    )
    
    enum class ConfirmationStatus {
        PROCESSED,
        CONFIRMED,
        FINALIZED,
        UNKNOWN
    }
    
    data class TransactionStatus(
        val signature: String,
        val confirmationStatus: ConfirmationStatus,
        val confirmations: Int?,
        val slot: Long?,
        val error: String?
    )
    
    /**
     * Waits for a transaction to reach the specified confirmation status
     */
    suspend fun waitForConfirmation(
        signature: String,
        targetStatus: ConfirmationStatus = ConfirmationStatus.CONFIRMED,
        maxWaitTime: Long = 30_000L,
        checkInterval: Long = 1000L
    ): Result<TransactionStatus> {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            val statusResult = getSignatureStatus(signature)
            
            if (statusResult.isSuccess) {
                val status = statusResult.getOrNull()!!
                
                // Check if transaction failed
                if (status.error != null) {
                    return Result.failure(Exception("Transaction failed: ${status.error}"))
                }
                
                // Check if we've reached target confirmation
                when (targetStatus) {
                    ConfirmationStatus.PROCESSED -> {
                        if (status.confirmationStatus != ConfirmationStatus.UNKNOWN) {
                            return Result.success(status)
                        }
                    }
                    ConfirmationStatus.CONFIRMED -> {
                        if (status.confirmationStatus == ConfirmationStatus.CONFIRMED ||
                            status.confirmationStatus == ConfirmationStatus.FINALIZED) {
                            return Result.success(status)
                        }
                    }
                    ConfirmationStatus.FINALIZED -> {
                        if (status.confirmationStatus == ConfirmationStatus.FINALIZED) {
                            return Result.success(status)
                        }
                    }
                    else -> {}
                }
            }
            
            delay(checkInterval)
        }
        
        return Result.failure(Exception("Transaction confirmation timeout after ${maxWaitTime}ms"))
    }
    
    /**
     * Gets the current status of a transaction signature
     */
    suspend fun getSignatureStatus(signature: String): Result<TransactionStatus> {
        return try {
            val networkSettings = settingsManager.networkSettings.value
            val rpcUrl = networkSettings.getHeliusRpcUrl()
            
            val request = JsonRpcRequest(
                method = "getSignatureStatuses",
                params = listOf(
                    kotlinx.serialization.json.JsonArray(
                        listOf(kotlinx.serialization.json.JsonPrimitive(signature))
                    ),
                    kotlinx.serialization.json.buildJsonObject {
                        put("searchTransactionHistory", kotlinx.serialization.json.JsonPrimitive(true))
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
            val statusResponse: SignatureStatusResponse = json.decodeFromString(responseBody)
            
            if (statusResponse.error != null) {
                Result.failure(Exception("RPC Error: ${statusResponse.error.message}"))
            } else if (statusResponse.result == null || statusResponse.result.value.isEmpty()) {
                Result.failure(Exception("No status found for signature"))
            } else {
                val signatureStatus = statusResponse.result.value[0]
                if (signatureStatus == null) {
                    Result.failure(Exception("Transaction not found"))
                } else {
                    val status = TransactionStatus(
                        signature = signature,
                        confirmationStatus = parseConfirmationStatus(signatureStatus.confirmationStatus),
                        confirmations = signatureStatus.confirmations,
                        slot = signatureStatus.slot,
                        error = if (signatureStatus.err != null && !signatureStatus.err.toString().contains("null")) {
                            signatureStatus.err.toString()
                        } else null
                    )
                    Result.success(status)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseConfirmationStatus(status: String?): ConfirmationStatus {
        return when (status?.lowercase()) {
            "processed" -> ConfirmationStatus.PROCESSED
            "confirmed" -> ConfirmationStatus.CONFIRMED
            "finalized" -> ConfirmationStatus.FINALIZED
            else -> ConfirmationStatus.UNKNOWN
        }
    }
    
    fun close() {
        client.close()
    }
}