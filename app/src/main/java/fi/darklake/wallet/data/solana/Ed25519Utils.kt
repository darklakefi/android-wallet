package fi.darklake.wallet.data.solana

import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.SecureRandom
import java.security.MessageDigest

/**
 * Ed25519 utility functions for Solana using Bouncy Castle
 * Provides proper Ed25519 cryptographic operations
 */
object Ed25519Utils {
    
    /**
     * Derives a public key from a private key using proper Ed25519
     */
    fun getPublicKey(privateKey: ByteArray): ByteArray {
        require(privateKey.size == 64 || privateKey.size == 32) {
            "Private key must be 32 or 64 bytes"
        }
        
        // If 64 bytes, the public key is in the second half
        if (privateKey.size == 64) {
            return privateKey.sliceArray(32..63)
        }
        
        // For 32-byte private key, derive public key using proper Ed25519
        return try {
            val privateKeyParams = Ed25519PrivateKeyParameters(privateKey, 0)
            val publicKeyParams = privateKeyParams.generatePublicKey()
            publicKeyParams.encoded
        } catch (e: Exception) {
            // Fallback to seed-based derivation if direct key derivation fails
            derivePublicKeyFromSeed(privateKey)
        }
    }
    
    /**
     * Derives public key from seed using Ed25519 standard derivation
     */
    private fun derivePublicKeyFromSeed(seed: ByteArray): ByteArray {
        // Hash the seed to get the private scalar
        val hash = MessageDigest.getInstance("SHA-512").digest(seed)
        
        // Clamp the private scalar according to Ed25519 specification
        hash[0] = (hash[0].toInt() and 0xF8).toByte()
        hash[31] = (hash[31].toInt() and 0x7F).toByte()
        hash[31] = (hash[31].toInt() or 0x40).toByte()
        
        // Use first 32 bytes as the private key
        val privateKey = hash.sliceArray(0..31)
        
        return try {
            val privateKeyParams = Ed25519PrivateKeyParameters(privateKey, 0)
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