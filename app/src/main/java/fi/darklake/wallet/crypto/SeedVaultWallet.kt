package fi.darklake.wallet.crypto

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.solana.core.PublicKey
import com.solana.core.Transaction
import com.solanamobile.seedvault.SigningRequest
import com.solanamobile.seedvault.WalletContractV1
import fi.darklake.wallet.seedvault.SeedVaultManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seed Vault wallet implementation for hardware-secured key management.
 * Signs transactions using Solana Mobile's Seed Vault through activity intents.
 */
class SeedVaultWallet(
    override val publicKey: String,
    private val authToken: Long,
    private val context: Context
) : SolanaWallet {

    companion object {
        private const val TAG = "SeedVaultWallet"
    }

    private val seedVaultManager = SeedVaultManager(context)

    override suspend fun prepareTransaction(transaction: Transaction): fi.darklake.wallet.crypto.SigningRequest = withContext(Dispatchers.IO) {
        Log.d(TAG, "Preparing transaction for Seed Vault signing (auth token: $authToken)")

        // Log transaction state before compiling
        Log.d(TAG, "Transaction fee payer: ${transaction.feePayer?.toBase58()}")
        Log.d(TAG, "Transaction signatures count: ${transaction.signatures.size}")

        // Build the transaction message for signing
        val message = transaction.compileMessage()
        val messageBytes = message.serialize()
        Log.d(TAG, "Transaction message serialized: ${messageBytes.size} bytes")

        // Log the actual message bytes we're sending to Seed Vault
        val messageHex = messageBytes.take(32).joinToString("") { "%02x".format(it) }
        Log.d(TAG, "Message bytes being sent to Seed Vault: $messageHex...")

        // Log the account keys in the message
        Log.d(TAG, "Message has ${message.accountKeys.size} account keys:")
        message.accountKeys.forEachIndexed { index, key ->
            Log.d(TAG, "  Account[$index]: ${key.toBase58()} (isSigner: ${index < message.header.numRequiredSignatures})")
        }
        Log.d(TAG, "Our public key: ${publicKey}")

        // Create BIP44 derivation path for Solana (m/44'/501'/0'/0')
        // This is the standard Solana derivation path
        // The account we're using should be at this path
        val derivationPath = Uri.parse("bip44:/0'/0'")
        val requestedSignatures = arrayListOf(derivationPath)

        // Create SigningRequest object as expected by Seed Vault
        val signingRequest = SigningRequest(messageBytes, requestedSignatures)
        val signingRequests = arrayListOf(signingRequest)

        // Create signing intent for Seed Vault
        val signingIntent = Intent(WalletContractV1.ACTION_SIGN_TRANSACTION).apply {
            setPackage("com.solanamobile.seedvaultimpl")
            // Pass the auth token
            putExtra(WalletContractV1.EXTRA_AUTH_TOKEN, authToken)
            // Pass the signing requests (must be ArrayList<SigningRequest>)
            putParcelableArrayListExtra(WalletContractV1.EXTRA_SIGNING_REQUEST, signingRequests)
        }

        // Return signing request with Seed Vault signing method
        fi.darklake.wallet.crypto.SigningRequest(
            transaction = transaction,
            signingMethod = SigningMethod.SeedVault(authToken, signingIntent),
            messageBytes = messageBytes
        )
    }

    override suspend fun completeSignature(transaction: Transaction, signature: ByteArray): Transaction = withContext(Dispatchers.IO) {
        Log.d(TAG, "Completing transaction with Seed Vault signature (${signature.size} bytes)")

        // Log the public key we're trying to use
        val pubkey = PublicKey(publicKey)
        Log.d(TAG, "Adding signature for public key: ${pubkey.toBase58()}")

        // Log transaction state before adding signature
        Log.d(TAG, "Transaction signatures before: ${transaction.signatures.size}")
        transaction.signatures.forEachIndexed { index, sig ->
            Log.d(TAG, "  Signature[$index]: pubkey=${sig.publicKey.toBase58()}, sig=${sig.signature?.size ?: "null"} bytes")
        }

        // DO NOT compile message again - it was already compiled in prepareTransaction
        // Just add the signature directly
        Log.d(TAG, "Adding signature without recompiling message")

        // Use the proper addSignature method which handles the signature placement correctly
        // This will find the right index in the signatures array and update it
        transaction.addSignature(pubkey, signature)

        // Log transaction state after adding signature
        Log.d(TAG, "Transaction signatures after: ${transaction.signatures.size}")
        transaction.signatures.forEachIndexed { index, sig ->
            Log.d(TAG, "  Signature[$index]: pubkey=${sig.publicKey.toBase58()}, sig=${sig.signature?.size ?: "null"} bytes")
        }

        // Try to serialize to see if it works
        try {
            Log.d(TAG, "Attempting to serialize transaction...")
            val serialized = transaction.serialize()
            Log.d(TAG, "Transaction serialized successfully: ${serialized.size} bytes")
        } catch (e: Throwable) {  // Catch Error too, not just Exception
            Log.e(TAG, "Failed to serialize transaction: ${e.message}")
            Log.e(TAG, "This suggests the signature doesn't match the transaction message")

            // Log hex of first few bytes of signature for debugging
            val sigHex = signature.take(16).joinToString("") { "%02x".format(it) }
            Log.e(TAG, "Signature starts with: $sigHex...")
            Log.e(TAG, "Signature length: ${signature.size} bytes")

            // Let's also log what message we're expecting the signature to be for
            val message = transaction.compileMessage()
            val messageBytes = message.serialize()
            val messageHex = messageBytes.take(16).joinToString("") { "%02x".format(it) }
            Log.e(TAG, "Expected message starts with: $messageHex...")
            Log.e(TAG, "Message size: ${messageBytes.size} bytes")

            // Let's manually verify the signature using TweetNaCl to see what's wrong
            try {
                val pubkeyBytes = PublicKey(publicKey).pubkey
                Log.e(TAG, "Public key bytes: ${pubkeyBytes.size} bytes")
                val pubkeyHex = pubkeyBytes.take(16).joinToString("") { "%02x".format(it) }
                Log.e(TAG, "Public key starts with: $pubkeyHex...")

                // Try to verify manually
                val verifier = com.solana.vendor.TweetNaclFast.Signature(pubkeyBytes, ByteArray(0))
                val isValid = verifier.detached_verify(messageBytes, signature)
                Log.e(TAG, "Manual signature verification result: $isValid")

                if (!isValid) {
                    // The signature doesn't verify. This could mean:
                    // 1. Seed Vault signed a different message
                    // 2. The public key doesn't match
                    // 3. The signature format is wrong
                    Log.e(TAG, "Signature verification failed! Possible causes:")
                    Log.e(TAG, "1. Seed Vault might be signing with a different derivation path")
                    Log.e(TAG, "2. The message might have been modified")
                    Log.e(TAG, "3. The signature format might be incorrect")
                }
            } catch (verifyError: Throwable) {
                Log.e(TAG, "Error during manual verification: ${verifyError.message}")
            }
        }

        // Return the signed transaction
        transaction
    }

    override suspend fun signMessage(message: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Signing message with Seed Vault (auth token: $authToken)")

            // For message signing, we need to create a simple transaction with the message
            // or use a different Seed Vault API if available
            // For now, this is a placeholder implementation
            throw UnsupportedOperationException("Message signing with Seed Vault not yet implemented")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sign message with Seed Vault", e)
            throw Exception("Failed to sign message with Seed Vault: ${e.message}", e)
        }
    }

    /**
     * Get the auth token for this Seed Vault wallet
     */
    fun getAuthToken(): Long = authToken

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SeedVaultWallet

        if (publicKey != other.publicKey) return false
        if (authToken != other.authToken) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.hashCode()
        result = 31 * result + authToken.hashCode()
        return result
    }
}