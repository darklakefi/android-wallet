package fi.darklake.wallet.data.solana

import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.SecureRandom

/**
 * Ed25519 utility functions for Solana using Bouncy Castle
 * Provides proper Ed25519 cryptographic operations
 */
object Ed25519Utils {
    
    /**
     * Derives a public key from a private key using proper Ed25519
     * Uses Bouncy Castle for all Ed25519 operations
     */
    fun getPublicKey(privateKey: ByteArray): ByteArray {
        require(privateKey.size == 64 || privateKey.size == 32) {
            "Private key must be 32 or 64 bytes"
        }
        
        return try {
            // For Ed25519, if we have 64 bytes, it's typically [32-byte private key][32-byte public key]
            // For 32 bytes, we need to derive the public key
            val actualPrivateKey = if (privateKey.size == 64) {
                // Extract the private key portion (first 32 bytes) and derive public key
                // Don't trust the second half, always derive for security
                privateKey.sliceArray(0..31)
            } else {
                privateKey
            }
            
            // Use Bouncy Castle to derive the public key from the private key
            val privateKeyParams = Ed25519PrivateKeyParameters(actualPrivateKey, 0)
            val publicKeyParams = privateKeyParams.generatePublicKey()
            publicKeyParams.encoded
        } catch (e: Exception) {
            // If the above fails, treat it as a seed and derive from it
            try {
                derivePublicKeyFromSeed(privateKey)
            } catch (seedException: Exception) {
                throw IllegalArgumentException("Failed to derive public key: ${e.message}. Seed derivation also failed: ${seedException.message}")
            }
        }
    }
    
    /**
     * Derives public key from seed using Ed25519 standard derivation
     * Uses Bouncy Castle's proper Ed25519 key derivation from seed
     */
    private fun derivePublicKeyFromSeed(seed: ByteArray): ByteArray {
        return try {
            // Use Bouncy Castle's Ed25519PrivateKeyParameters constructor that accepts a seed
            // This automatically handles the proper SHA-512 hashing and scalar clamping
            val privateKeyParams = Ed25519PrivateKeyParameters(seed, 0)
            val publicKeyParams = privateKeyParams.generatePublicKey()
            publicKeyParams.encoded
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to derive public key from seed: ${e.message}")
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
            // Extract the actual private key (first 32 bytes if 64-byte keypair)
            val secretKey = if (privateKey.size == 64) {
                privateKey.sliceArray(0..31)
            } else {
                privateKey
            }
            
            val privateKeyParams = Ed25519PrivateKeyParameters(secretKey, 0)
            val signer = Ed25519Signer()
            signer.init(true, privateKeyParams)
            signer.update(message, 0, message.size)
            signer.generateSignature()
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to sign message: ${e.message}")
        }
    }
    
    /**
     * Verifies an Ed25519 signature
     */
    fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        require(signature.size == 64) { "Signature must be 64 bytes" }
        require(publicKey.size == 32) { "Public key must be 32 bytes" }
        
        return try {
            val publicKeyParams = Ed25519PublicKeyParameters(publicKey, 0)
            val verifier = Ed25519Signer()
            verifier.init(false, publicKeyParams)
            verifier.update(message, 0, message.size)
            verifier.verifySignature(signature)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Generates a new Ed25519 key pair
     */
    fun generateKeyPair(): AsymmetricCipherKeyPair {
        val keyPairGenerator = Ed25519KeyPairGenerator()
        keyPairGenerator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        return keyPairGenerator.generateKeyPair()
    }
    
    /**
     * Converts private key parameters to byte array
     */
    fun privateKeyToBytes(privateKey: Ed25519PrivateKeyParameters): ByteArray {
        return privateKey.encoded
    }
    
    /**
     * Converts public key parameters to byte array
     */
    fun publicKeyToBytes(publicKey: Ed25519PublicKeyParameters): ByteArray {
        return publicKey.encoded
    }
}