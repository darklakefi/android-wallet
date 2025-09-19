package fi.darklake.wallet.crypto

import com.solana.core.HotAccount
import com.solana.core.Transaction
import com.solana.vendor.TweetNaclFast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * Local wallet implementation that stores private keys locally.
 * Signs transactions using the device's computational resources.
 */
class LocalWallet(
    override val publicKey: String,
    private val privateKey: ByteArray,
    val mnemonic: List<String>
) : SolanaWallet {

    companion object {
        private const val TAG = "LocalWallet"
    }

    private val account: HotAccount by lazy {
        // Create account from private key
        // If it's 32 bytes, it's a seed; if 64 bytes, it's a full keypair
        if (privateKey.size == 32) {
            // Create keypair from seed
            val keypair = TweetNaclFast.Signature.keyPair_fromSeed(privateKey)
            HotAccount(keypair.secretKey)
        } else {
            HotAccount(privateKey)
        }
    }

    override suspend fun prepareTransaction(transaction: Transaction): SigningRequest = withContext(Dispatchers.IO) {
        Log.d(TAG, "Preparing transaction for local signing")

        // Get the message to be signed
        val message = transaction.compileMessage()
        val messageBytes = message.serialize()

        // Return signing request with local signing method
        SigningRequest(
            transaction = transaction,
            signingMethod = SigningMethod.Local(privateKey),
            messageBytes = messageBytes
        )
    }

    override suspend fun completeSignature(transaction: Transaction, signature: ByteArray): Transaction = withContext(Dispatchers.IO) {
        Log.d(TAG, "Completing transaction with signature")

        // For local wallet, we actually do the signing here since it's immediate
        // This maintains consistency with the SeedVault flow
        transaction.sign(account)
        transaction
    }

    override suspend fun signMessage(message: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Signing message with local private key")
            account.sign(message)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sign message", e)
            throw Exception("Failed to sign message: ${e.message}", e)
        }
    }

    /**
     * Get the raw private key bytes
     */
    fun getPrivateKey(): ByteArray = privateKey

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalWallet

        if (publicKey != other.publicKey) return false
        if (!privateKey.contentEquals(other.privateKey)) return false
        if (mnemonic != other.mnemonic) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.hashCode()
        result = 31 * result + privateKey.contentHashCode()
        result = 31 * result + mnemonic.hashCode()
        return result
    }
}