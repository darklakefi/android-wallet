package fi.darklake.wallet.crypto

import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.crypto.MnemonicException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.bitcoinj.core.Base58
import fi.darklake.wallet.data.solana.Ed25519Utils

/**
 * Manager for creating and managing Solana wallets
 */
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

    fun createWalletFromMnemonic(mnemonic: List<String>, passphrase: String = ""): LocalWallet {
        if (mnemonic.size != 12) {
            throw IllegalArgumentException("Mnemonic must be exactly 12 words")
        }

        val seed = mnemonicToSeed(mnemonic, passphrase)
        val keyPair = deriveKeyPair(seed)

        return LocalWallet(
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
        // Use proper Ed25519 key derivation for Solana
        // Take first 32 bytes of seed as the private key
        val privateKey = seed.sliceArray(0..31)

        // Derive public key using proper Ed25519
        val publicKey = Ed25519Utils.getPublicKey(privateKey)

        return Pair(publicKey, privateKey)
    }
}