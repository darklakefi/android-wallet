package fi.darklake.wallet.crypto

import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.crypto.MnemonicException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.bitcoinj.core.Base58
import java.security.MessageDigest
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.security.KeyPair

data class SolanaWallet(
    val publicKey: String,
    val privateKey: ByteArray,
    val mnemonic: List<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SolanaWallet

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

object SolanaWalletManager {
    private const val SEED_LENGTH = 64
    private const val DERIVATION_PATH = "m/44'/501'/0'/0'"
    
    fun generateMnemonic(): List<String> {
        val entropy = ByteArray(16) // 128 bits for 12 words
        SecureRandom().nextBytes(entropy)
        
        return try {
            MnemonicCode.INSTANCE.toMnemonic(entropy)
        } catch (e: MnemonicException.MnemonicLengthException) {
            throw IllegalStateException("Failed to generate mnemonic", e)
        }
    }
    
    fun createWalletFromMnemonic(mnemonic: List<String>, passphrase: String = ""): SolanaWallet {
        if (mnemonic.size != 12) {
            throw IllegalArgumentException("Mnemonic must be exactly 12 words")
        }
        
        val seed = mnemonicToSeed(mnemonic, passphrase)
        val keyPair = deriveKeyPair(seed)
        
        return SolanaWallet(
            publicKey = Base58.encode(keyPair.first),
            privateKey = keyPair.second,
            mnemonic = mnemonic
        )
    }
    
    fun validateMnemonic(mnemonic: List<String>): Boolean {
        return try {
            MnemonicCode.INSTANCE.check(mnemonic)
            true
        } catch (e: MnemonicException) {
            false
        }
    }
    
    private fun mnemonicToSeed(mnemonic: List<String>, passphrase: String): ByteArray {
        val mnemonicString = mnemonic.joinToString(" ")
        val salt = "mnemonic$passphrase".toByteArray()
        
        val spec = PBEKeySpec(mnemonicString.toCharArray(), salt, 2048, SEED_LENGTH * 8)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        
        return factory.generateSecret(spec).encoded
    }
    
    private fun deriveKeyPair(seed: ByteArray): Pair<ByteArray, ByteArray> {
        // Ed25519 key derivation for Solana
        // For a production app, you'd want to use proper Ed25519 implementation
        // This is a simplified version for demonstration
        
        val privateKey = seed.sliceArray(0..31)
        val publicKey = derivePublicKey(privateKey)
        
        return Pair(publicKey, privateKey)
    }
    
    private fun derivePublicKey(privateKey: ByteArray): ByteArray {
        // Simplified Ed25519 public key derivation
        // In production, use a proper Ed25519 library
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(privateKey).sliceArray(0..31)
    }
}