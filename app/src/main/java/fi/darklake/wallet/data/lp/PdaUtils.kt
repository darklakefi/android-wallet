package fi.darklake.wallet.data.lp

import com.solana.core.PublicKey
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Utility class for Solana Program Derived Address (PDA) generation using SolanaKT
 */
object PdaUtils {
    
    private const val EXCHANGE_PROGRAM_ID = "darkr3FB87qAZmgLwKov6Hk9Yiah5UT4rUYu8Zhthw1"
    private const val LIQUIDITY_SEED = "lp"
    private const val POOL_SEED = "pool"
    private const val AMM_CONFIG_SEED = "amm_config"
    private const val POOL_RESERVE_SEED = "pool_reserve"
    private const val TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
    private const val ASSOCIATED_TOKEN_PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"
    
    /**
     * Generate LP token mint address for a given token pair
     */
    fun getLpTokenMint(tokenAMint: String, tokenBMint: String): String {
        // Sort mints canonically for consistent PDA derivation
        val mints = listOf(tokenAMint, tokenBMint).sorted()
        val mintA = mints[0]
        val mintB = mints[1]
        
        // Derive AMM config PDA
        val ammConfigPda = getAmmConfigPda()
        
        // Derive pool PDA
        val poolPda = getPoolPda(mintA, mintB)
        
        // Derive LP token mint PDA
        val lpMintPda = PublicKey.findProgramAddress(
            listOf(
                LIQUIDITY_SEED.toByteArray(),
                PublicKey(poolPda).toByteArray()
            ),
            PublicKey(EXCHANGE_PROGRAM_ID)
        )
        
        return lpMintPda.address.toBase58()
    }
    
    /**
     * Get AMM Config PDA (shared across all pools)
     */
    fun getAmmConfigPda(): String {
        val pda = PublicKey.findProgramAddress(
            listOf(
                AMM_CONFIG_SEED.toByteArray(),
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array()
            ),
            PublicKey(EXCHANGE_PROGRAM_ID)
        )
        return pda.address.toBase58()
    }
    
    /**
     * Get Pool PDA for a token pair
     */
    fun getPoolPda(tokenAMint: String, tokenBMint: String): String {
        // Sort mints canonically
        val mints = listOf(tokenAMint, tokenBMint).sorted()
        val mintA = mints[0]
        val mintB = mints[1]
        
        val ammConfigPda = getAmmConfigPda()
        
        val pda = PublicKey.findProgramAddress(
            listOf(
                POOL_SEED.toByteArray(),
                PublicKey(ammConfigPda).toByteArray(),
                PublicKey(mintA).toByteArray(),
                PublicKey(mintB).toByteArray()
            ),
            PublicKey(EXCHANGE_PROGRAM_ID)
        )
        
        return pda.address.toBase58()
    }
    
    /**
     * Get Pool Reserve PDA for a specific token in a pool
     */
    fun getPoolReservePda(poolPda: String, tokenMint: String): String {
        val pda = PublicKey.findProgramAddress(
            listOf(
                POOL_RESERVE_SEED.toByteArray(),
                PublicKey(poolPda).toByteArray(),
                PublicKey(tokenMint).toByteArray()
            ),
            PublicKey(EXCHANGE_PROGRAM_ID)
        )
        return pda.address.toBase58()
    }
    
    /**
     * Get Associated Token Address (ATA) for a user and mint
     */
    fun getAssociatedTokenAddress(userAddress: String, mintAddress: String): String {
        val pda = PublicKey.associatedTokenAddress(
            PublicKey(userAddress),
            PublicKey(mintAddress)
        )
        return pda.address.toBase58()
    }
}