package fi.darklake.wallet.data.solana

import com.solana.vendor.TweetNaclFast

/**
 * Ed25519 utility functions for Solana using SolanaKT's TweetNaclFast
 * Provides proper Ed25519 cryptographic operations
 */
object Ed25519Utils {
    
    /**
     * Derives a public key from a private key using proper Ed25519
     * Uses TweetNaclFast for all Ed25519 operations
     */
    fun getPublicKey(privateKey: ByteArray): ByteArray {
        require(privateKey.size == 64 || privateKey.size == 32) {
            "Private key must be 32 or 64 bytes"
        }
        
        return try {
            when (privateKey.size) {
                64 -> {
                    // It's already a full keypair, extract public key
                    privateKey.sliceArray(32..63)
                }
                32 -> {
                    // It's a seed, generate keypair and extract public key
                    val keypair = TweetNaclFast.Signature.keyPair_fromSeed(privateKey)
                    keypair.publicKey
                }
                else -> throw IllegalArgumentException("Invalid private key size")
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to derive public key: ${e.message}")
        }
    }
    
    /**
     * Signs a message using proper Ed25519 signing
     */
    fun sign(message: ByteArray, privateKey: ByteArray): ByteArray {
        require(privateKey.size == 64 || privateKey.size == 32) {
            "Private key must be 32 or 64 bytes"
        }
        
        return try {
            val secretKey = when (privateKey.size) {
                64 -> privateKey // Already a full keypair
                32 -> {
                    // It's a seed, generate full keypair
                    val keypair = TweetNaclFast.Signature.keyPair_fromSeed(privateKey)
                    keypair.secretKey
                }
                else -> throw IllegalArgumentException("Invalid private key size")
            }
            
            val signer = TweetNaclFast.Signature(ByteArray(0), secretKey)
            signer.detached(message)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to sign message: ${e.message}")
        }
    }
}