package fi.darklake.wallet.data.solana

import fi.darklake.wallet.crypto.SigningRequest

/**
 * Result of a transaction operation.
 * Can be either a completed signature or a signing request that needs UI handling.
 */
sealed class TransactionResult {
    /**
     * Transaction was signed and submitted successfully
     */
    data class Success(val signature: String) : TransactionResult()

    /**
     * Transaction needs user interaction for signing
     */
    data class NeedsSignature(val signingRequest: SigningRequest) : TransactionResult()

    /**
     * Transaction failed with an error
     */
    data class Error(val message: String, val exception: Exception? = null) : TransactionResult()
}