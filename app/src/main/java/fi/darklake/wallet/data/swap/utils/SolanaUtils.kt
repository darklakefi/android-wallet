package fi.darklake.wallet.data.swap.utils

import java.security.MessageDigest
import java.util.*

/**
 * Utility functions for Solana address operations, matching dex-web implementation
 */
object SolanaUtils {
    
    /**
     * Sort two Solana addresses deterministically using buffer comparison
     * This matches the dex-web sortSolanaAddresses function
     */
    fun sortSolanaAddresses(addrA: String, addrB: String): Pair<String, String> {
        // Convert addresses to bytes for comparison (mimicking PublicKey.toBuffer().compare())
        val bufferA = decodeBase58(addrA)
        val bufferB = decodeBase58(addrB)
        
        // Compare bytes lexicographically
        val comparison = bufferA.compareWith(bufferB)
        
        return if (comparison > 0) {
            Pair(addrB, addrA) // tokenX=addrB, tokenY=addrA
        } else {
            Pair(addrA, addrB) // tokenX=addrA, tokenY=addrB
        }
    }
    
    /**
     * Generate deterministic Pool PDA address matching dex-web implementation
     * This follows the same seed structure: ["pool", ammConfig, tokenX, tokenY]
     */
    fun generatePoolPda(tokenX: String, tokenY: String, programId: String = DEFAULT_PROGRAM_ID): String {
        // First get the AMM Config PDA
        val ammConfigPda = generateAmmConfigPda(programId)
        
        // Generate pool PDA with seeds: ["pool", ammConfig, tokenX, tokenY]
        val seeds = listOf(
            "pool".toByteArray(),
            decodeBase58(ammConfigPda),
            decodeBase58(tokenX),
            decodeBase58(tokenY)
        )
        
        return findProgramAddress(seeds, programId)
    }
    
    /**
     * Generate AMM Config PDA with seeds: ["amm_config", 0]
     */
    private fun generateAmmConfigPda(programId: String): String {
        val seeds = listOf(
            "amm_config".toByteArray(),
            byteArrayOf(0, 0, 0, 0) // 4-byte little-endian representation of 0
        )
        
        return findProgramAddress(seeds, programId)
    }
    
    /**
     * Simplified PDA derivation (this would normally use proper Solana libraries)
     * For production, this should use actual Solana PDA derivation
     */
    private fun findProgramAddress(seeds: List<ByteArray>, programId: String): String {
        // This is a simplified mock implementation
        // In a real implementation, you would use proper Solana PDA derivation
        val combined = seeds.fold(ByteArray(0)) { acc, seed -> acc + seed } + decodeBase58(programId)
        val hash = MessageDigest.getInstance("SHA-256").digest(combined)
        return encodeBase58(hash.take(32).toByteArray())
    }
    
    /**
     * Simplified Base58 decoding - in production, use proper Solana libraries
     */
    private fun decodeBase58(input: String): ByteArray {
        // Simplified implementation - in production use actual Base58 decoder
        return input.toByteArray()
    }
    
    /**
     * Simplified Base58 encoding - in production, use proper Solana libraries
     */
    private fun encodeBase58(input: ByteArray): String {
        // Simplified implementation - in production use actual Base58 encoder
        return android.util.Base64.encodeToString(input, android.util.Base64.NO_WRAP)
    }
    
    /**
     * Compare two byte arrays lexicographically
     */
    private fun ByteArray.compareWith(other: ByteArray): Int {
        val minLength = minOf(this.size, other.size)
        
        for (i in 0 until minLength) {
            val comparison = this[i].toInt().and(0xFF).compareTo(other[i].toInt().and(0xFF))
            if (comparison != 0) return comparison
        }
        
        return this.size.compareTo(other.size)
    }
    
    /**
     * Validate if a string is a valid Solana address
     */
    fun isSolanaAddress(address: String): Boolean {
        return address.matches(Regex("^[1-9A-HJ-NP-Za-km-z]{32,44}$"))
    }
    
    // Default Darklake exchange program ID from dex-web
    const val DEFAULT_PROGRAM_ID = "darkr3FB87qAZmgLwKov6Hk9Yiah5UT4rUYu8Zhthw1"
}