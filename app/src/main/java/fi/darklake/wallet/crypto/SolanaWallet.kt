package fi.darklake.wallet.crypto

import com.solana.core.Transaction

/**
 * Interface for Solana wallet implementations.
 * Provides abstraction over different signing mechanisms (local, hardware, etc.)
 *
 * Uses a unified two-phase signing flow:
 * 1. prepareTransaction() - returns a SigningRequest with method details
 * 2. completeSignature() - completes signing with the provided signature
 */
interface SolanaWallet {
    /**
     * The public key (address) of this wallet in Base58 format
     */
    val publicKey: String

    /**
     * Prepare a transaction for signing.
     * Returns a SigningRequest that describes how the transaction should be signed.
     * For LocalWallet, this includes the private key for immediate signing.
     * For SeedVaultWallet, this includes the signing intent for activity-based signing.
     *
     * @param transaction The unsigned transaction to prepare
     * @return SigningRequest containing the transaction and signing method
     */
    suspend fun prepareTransaction(transaction: Transaction): SigningRequest

    /**
     * Complete the signature process with the provided signature bytes.
     * This applies the signature to the transaction and returns the signed version.
     *
     * @param transaction The original unsigned transaction
     * @param signature The signature bytes obtained from the signing process
     * @return The signed transaction
     */
    suspend fun completeSignature(transaction: Transaction, signature: ByteArray): Transaction

    /**
     * Sign an arbitrary message
     * @param message The message bytes to sign
     * @return The signature bytes
     */
    suspend fun signMessage(message: ByteArray): ByteArray
}