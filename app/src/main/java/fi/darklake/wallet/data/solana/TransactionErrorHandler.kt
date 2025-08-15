package fi.darklake.wallet.data.solana

import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Handles transaction errors and retry logic for Solana transactions
 */
object TransactionErrorHandler {
    
    /**
     * Maximum number of retry attempts
     */
    const val MAX_RETRIES = 3
    
    /**
     * Initial retry delay in milliseconds
     */
    const val INITIAL_RETRY_DELAY = 1000L
    
    /**
     * Maximum retry delay in milliseconds
     */
    const val MAX_RETRY_DELAY = 5000L
    
    /**
     * Executes a transaction with retry logic
     */
    suspend fun <T> withRetry(
        maxRetries: Int = MAX_RETRIES,
        initialDelay: Long = INITIAL_RETRY_DELAY,
        operation: suspend () -> Result<T>
    ): Result<T> {
        var lastException: Exception? = null
        var currentDelay = initialDelay
        
        repeat(maxRetries) { attempt ->
            try {
                val result = operation()
                if (result.isSuccess) {
                    return result
                }
                
                lastException = result.exceptionOrNull() as? Exception
                
                // Check if error is retryable
                if (!isRetryableError(lastException)) {
                    return result
                }
                
                // Don't delay on the last attempt
                if (attempt < maxRetries - 1) {
                    println("Retry attempt ${attempt + 1} after ${currentDelay}ms delay")
                    delay(currentDelay)
                    currentDelay = min(currentDelay * 2, MAX_RETRY_DELAY)
                }
                
            } catch (e: Exception) {
                lastException = e
                
                if (!isRetryableError(e)) {
                    return Result.failure(e)
                }
                
                // Don't delay on the last attempt
                if (attempt < maxRetries - 1) {
                    println("Retry attempt ${attempt + 1} after ${currentDelay}ms delay")
                    delay(currentDelay)
                    currentDelay = min(currentDelay * 2, MAX_RETRY_DELAY)
                }
            }
        }
        
        return Result.failure(
            lastException ?: Exception("Transaction failed after $maxRetries attempts")
        )
    }
    
    /**
     * Determines if an error is retryable
     */
    private fun isRetryableError(exception: Exception?): Boolean {
        if (exception == null) return false
        
        val message = exception.message?.lowercase() ?: return false
        
        return when {
            // Network errors
            message.contains("timeout") -> true
            message.contains("connection") -> true
            message.contains("network") -> true
            
            // RPC errors
            message.contains("blockhash not found") -> true
            message.contains("node is behind") -> true
            message.contains("too many requests") -> true
            message.contains("rate limit") -> true
            
            // Transaction errors that might be temporary
            message.contains("insufficient") && message.contains("fee") -> true
            message.contains("transaction simulation failed") -> false // Don't retry simulation failures
            
            // Don't retry these errors
            message.contains("invalid") -> false
            message.contains("signature verification failed") -> false
            message.contains("account does not exist") -> false
            message.contains("insufficient funds") -> false
            
            else -> false
        }
    }
    
    /**
     * Parses Solana RPC error codes
     */
    fun parseErrorCode(errorCode: Int): String {
        return when (errorCode) {
            -32700 -> "Parse error"
            -32600 -> "Invalid request"
            -32601 -> "Method not found"
            -32602 -> "Invalid params"
            -32603 -> "Internal error"
            -32000 -> "Server error"
            -32001 -> "Server error: Send transaction preflight failure"
            -32002 -> "Server error: Node is behind"
            -32003 -> "Server error: Transaction signature verification failure"
            -32004 -> "Server error: Block not available"
            -32005 -> "Server error: Node is unhealthy"
            -32006 -> "Server error: Transaction simulation failed"
            -32007 -> "Server error: Blockhash not found"
            -32008 -> "Server error: Account in use"
            -32009 -> "Server error: Minimum context slot not reached"
            -32010 -> "Server error: Unsupported transaction version"
            else -> "Unknown error code: $errorCode"
        }
    }
}