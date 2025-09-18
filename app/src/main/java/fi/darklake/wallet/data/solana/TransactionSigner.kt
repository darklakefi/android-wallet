package fi.darklake.wallet.data.solana

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.solana.core.Transaction
import fi.darklake.wallet.seedvault.SeedVaultManager
import fi.darklake.wallet.storage.SeedVaultStorageProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Interface for signing Solana transactions
 * Supports both local signing (with private key) and external signing (Seed Vault)
 */
interface TransactionSigner {
    suspend fun signTransaction(transaction: Transaction): Result<ByteArray>
    fun requiresActivity(): Boolean
}

/**
 * Local signer using a private key stored in memory
 */
class LocalTransactionSigner(
    private val privateKey: ByteArray
) : TransactionSigner {

    override suspend fun signTransaction(transaction: Transaction): Result<ByteArray> {
        return try {
            // Create HotAccount for signing
            val account = if (privateKey.size == 32) {
                // Create keypair from seed
                val keypair = com.solana.vendor.TweetNaclFast.Signature.keyPair_fromSeed(privateKey)
                com.solana.core.HotAccount(keypair.secretKey)
            } else {
                com.solana.core.HotAccount(privateKey)
            }

            // Sign the transaction
            transaction.sign(account)

            // Return serialized transaction
            Result.success(transaction.serialize())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun requiresActivity(): Boolean = false
}

/**
 * Seed Vault signer that uses hardware-secured signing
 */
class SeedVaultTransactionSigner(
    private val context: Context,
    private val authToken: Long
) : TransactionSigner {

    private val seedVaultManager = SeedVaultManager(context)

    // This will be set by the activity that handles signing
    companion object {
        @Volatile
        var pendingSignResult: Result<ByteArray>? = null

        @Volatile
        var signingLauncher: ActivityResultLauncher<Intent>? = null
    }

    override suspend fun signTransaction(transaction: Transaction): Result<ByteArray> = suspendCancellableCoroutine { cont ->
        try {
            // Check if we have an activity context and launcher
            val launcher = signingLauncher
            if (launcher == null) {
                cont.resumeWithException(IllegalStateException("Seed Vault signing requires activity context with launcher"))
                return@suspendCancellableCoroutine
            }

            // Serialize the transaction for signing
            val transactionBytes = transaction.serialize()

            // Create signing intent
            val signingIntent = seedVaultManager.createSignTransactionIntent(
                authToken = authToken,
                transaction = transactionBytes,
                derivationPath = "m/44'/501'/0'/0'" // Standard Solana derivation path
            )

            // Store continuation for result callback
            pendingSignResult = null

            // Launch the signing activity
            launcher.launch(signingIntent)

            // The result will be handled by the activity result launcher
            // which will set pendingSignResult and we'll need to wait for it
            // Note: In a real implementation, we'd use a more sophisticated callback mechanism

            // For now, return a placeholder failure
            // The actual implementation would need proper activity integration
            cont.resumeWithException(UnsupportedOperationException("Seed Vault signing requires full activity integration"))

        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }

    override fun requiresActivity(): Boolean = true
}

/**
 * Factory for creating appropriate transaction signers
 */
object TransactionSignerFactory {

    fun create(context: Context, wallet: fi.darklake.wallet.crypto.SolanaWallet): TransactionSigner {
        // Check if this is a Seed Vault wallet
        if (wallet.mnemonic == listOf("SEED_VAULT")) {
            // Get the auth token from SeedVaultStorageProvider
            val seedVaultProvider = SeedVaultStorageProvider(context)
            val authToken = seedVaultProvider.getAuthToken()

            if (authToken != -1L) {
                return SeedVaultTransactionSigner(context, authToken)
            }
        }

        // Default to local signer
        return LocalTransactionSigner(wallet.privateKey)
    }
}