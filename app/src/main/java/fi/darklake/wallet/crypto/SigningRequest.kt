package fi.darklake.wallet.crypto

import com.solana.core.Transaction

/**
 * Represents a signing request that can be processed either synchronously (LocalWallet)
 * or asynchronously with user interaction (SeedVaultWallet)
 */
data class SigningRequest(
    val transaction: Transaction,
    val signingMethod: SigningMethod,
    val messageBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SigningRequest

        if (transaction != other.transaction) return false
        if (signingMethod != other.signingMethod) return false
        if (!messageBytes.contentEquals(other.messageBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = transaction.hashCode()
        result = 31 * result + signingMethod.hashCode()
        result = 31 * result + messageBytes.contentHashCode()
        return result
    }
}

/**
 * Describes how a transaction should be signed
 */
sealed class SigningMethod {
    /**
     * Sign locally using private key
     */
    data class Local(val privateKey: ByteArray) : SigningMethod() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Local

            if (!privateKey.contentEquals(other.privateKey)) return false

            return true
        }

        override fun hashCode(): Int {
            return privateKey.contentHashCode()
        }
    }

    /**
     * Sign using Seed Vault with activity interaction
     */
    data class SeedVault(
        val authToken: Long,
        val signingIntent: android.content.Intent
    ) : SigningMethod()
}