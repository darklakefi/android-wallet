package fi.darklake.wallet.data.lp

import android.content.Context
import com.solana.core.PublicKey
import com.solana.core.Transaction
import com.solana.core.TransactionInstruction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Builder for creating Anchor program transactions using IDL definitions
 * Based on the Anchor IDL format and compatible with dex-web's darklake program
 */
class AnchorIDLTransactionBuilder(
    private val context: Context? = null,
    private val idlPath: String = "idl/darklake.json"
) {
    companion object {
        const val PROGRAM_ID = "darkr3FB87qAZmgLwKov6Hk9Yiah5UT4rUYu8Zhthw1"
        const val TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
        const val ASSOCIATED_TOKEN_PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"
        const val SYSTEM_PROGRAM_ID = "11111111111111111111111111111111"
        const val RENT_SYSVAR = "SysvarRent111111111111111111111111111111111"
        
        // Instruction discriminators (8-byte signatures)
        private val INSTRUCTION_DISCRIMINATORS = mapOf(
            "initialize" to byteArrayOf(-24, 110, 28, -69, -88, -79, 106, 90),
            "createPool" to byteArrayOf(-34, -67, 94, 47, -19, -112, 6, 106),
            "depositLiquidity" to byteArrayOf(-22, 23, 19, -73, 87, -63, 12, 2),
            "withdrawLiquidity" to byteArrayOf(-32, -127, -12, 17, 76, 86, -99, -109),
            "swap" to byteArrayOf(-40, 22, -31, 67, -38, 47, -74, 97),
            "settle" to byteArrayOf(-13, 90, 30, 120, -64, -15, -73, 119),
            "cancel" to byteArrayOf(35, -18, -47, -39, -83, -93, 53, 82),
            "slash" to byteArrayOf(11, 49, 117, -102, -25, -87, 91, 30)
        )
    }
    
    private var idl: JsonObject? = null
    
    init {
        loadIDL()
    }
    
    /**
     * Load and parse the IDL JSON file
     */
    private fun loadIDL() {
        try {
            context?.let { ctx ->
                val inputStream = ctx.assets.open(idlPath)
                val reader = InputStreamReader(inputStream)
                val jsonString = reader.readText()
                reader.close()
                
                idl = Json.parseToJsonElement(jsonString).jsonObject
                android.util.Log.d("AnchorIDLTransactionBuilder", "IDL loaded successfully")
            }
        } catch (e: Exception) {
            android.util.Log.e("AnchorIDLTransactionBuilder", "Failed to load IDL", e)
        }
    }
    
    /**
     * Create a pool creation instruction
     * Based on dex-web's createPoolTransaction.handler.ts
     */
    fun createPoolInstruction(
        userAddress: String,
        tokenXMint: String,
        tokenYMint: String,
        depositAmountX: Double,
        depositAmountY: Double
    ): TransactionInstruction {
        val discriminator = INSTRUCTION_DISCRIMINATORS["createPool"] 
            ?: throw Exception("CreatePool discriminator not found")
        
        // Get all required PDAs
        val ammConfigPda = PdaUtils.getAmmConfigPda()
        val poolPda = PdaUtils.getPoolPda(tokenXMint, tokenYMint)
        val lpMintPda = PdaUtils.getLpTokenMint(tokenXMint, tokenYMint)
        
        // Get pool reserve PDAs
        val poolReserveX = PdaUtils.getPoolReservePda(poolPda, tokenXMint)
        val poolReserveY = PdaUtils.getPoolReservePda(poolPda, tokenYMint)
        
        // Get user ATAs
        val userTokenX = PdaUtils.getAssociatedTokenAddress(userAddress, tokenXMint)
        val userTokenY = PdaUtils.getAssociatedTokenAddress(userAddress, tokenYMint)
        val userLpToken = PdaUtils.getAssociatedTokenAddress(userAddress, lpMintPda)
        
        // Build instruction data
        val data = ByteBuffer.allocate(24).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put(discriminator)
            putLong((depositAmountX * 1e9).toLong()) // Convert to lamports/smallest unit
            putLong((depositAmountY * 1e9).toLong())
        }.array()
        
        // Build accounts array based on IDL
        val keys = listOf(
            AccountMeta(PublicKey(userAddress), isSigner = true, isWritable = true),
            AccountMeta(PublicKey(ammConfigPda), isSigner = false, isWritable = false),
            AccountMeta(PublicKey(poolPda), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(lpMintPda), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(tokenXMint), isSigner = false, isWritable = false),
            AccountMeta(PublicKey(tokenYMint), isSigner = false, isWritable = false),
            AccountMeta(PublicKey(poolReserveX), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(poolReserveY), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(userTokenX), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(userTokenY), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(userLpToken), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(TOKEN_PROGRAM_ID), isSigner = false, isWritable = false),
            AccountMeta(PublicKey(ASSOCIATED_TOKEN_PROGRAM_ID), isSigner = false, isWritable = false),
            AccountMeta(PublicKey(SYSTEM_PROGRAM_ID), isSigner = false, isWritable = false),
            AccountMeta(PublicKey(RENT_SYSVAR), isSigner = false, isWritable = false)
        ).map { it.toSolanaAccountMeta() }
        
        return TransactionInstruction(
            PublicKey(PROGRAM_ID),
            keys,
            data
        )
    }
    
    /**
     * Create an add liquidity (deposit) instruction
     */
    fun addLiquidityInstruction(
        userAddress: String,
        tokenXMint: String,
        tokenYMint: String,
        maxAmountX: Double,
        maxAmountY: Double
    ): TransactionInstruction {
        val discriminator = INSTRUCTION_DISCRIMINATORS["depositLiquidity"]
            ?: throw Exception("DepositLiquidity discriminator not found")
        
        // Get all required PDAs
        val ammConfigPda = PdaUtils.getAmmConfigPda()
        val poolPda = PdaUtils.getPoolPda(tokenXMint, tokenYMint)
        val lpMintPda = PdaUtils.getLpTokenMint(tokenXMint, tokenYMint)
        
        // Get pool reserve PDAs
        val poolReserveX = PdaUtils.getPoolReservePda(poolPda, tokenXMint)
        val poolReserveY = PdaUtils.getPoolReservePda(poolPda, tokenYMint)
        
        // Get user ATAs
        val userTokenX = PdaUtils.getAssociatedTokenAddress(userAddress, tokenXMint)
        val userTokenY = PdaUtils.getAssociatedTokenAddress(userAddress, tokenYMint)
        val userLpToken = PdaUtils.getAssociatedTokenAddress(userAddress, lpMintPda)
        
        // Build instruction data
        val data = ByteBuffer.allocate(24).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put(discriminator)
            putLong((maxAmountX * 1e9).toLong()) // Convert to lamports/smallest unit
            putLong((maxAmountY * 1e9).toLong())
        }.array()
        
        // Build accounts array
        val keys = listOf(
            AccountMeta(PublicKey(userAddress), isSigner = true, isWritable = true),
            AccountMeta(PublicKey(poolPda), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(lpMintPda), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(poolReserveX), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(poolReserveY), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(userTokenX), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(userTokenY), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(userLpToken), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(TOKEN_PROGRAM_ID), isSigner = false, isWritable = false)
        ).map { it.toSolanaAccountMeta() }
        
        return TransactionInstruction(
            PublicKey(PROGRAM_ID),
            keys,
            data
        )
    }
    
    /**
     * Create a withdraw liquidity instruction
     */
    fun withdrawLiquidityInstruction(
        userAddress: String,
        tokenXMint: String,
        tokenYMint: String,
        lpTokenAmount: Double,
        minAmountX: Double,
        minAmountY: Double
    ): TransactionInstruction {
        val discriminator = INSTRUCTION_DISCRIMINATORS["withdrawLiquidity"]
            ?: throw Exception("WithdrawLiquidity discriminator not found")
        
        // Get all required PDAs
        val poolPda = PdaUtils.getPoolPda(tokenXMint, tokenYMint)
        val lpMintPda = PdaUtils.getLpTokenMint(tokenXMint, tokenYMint)
        
        // Get pool reserve PDAs
        val poolReserveX = PdaUtils.getPoolReservePda(poolPda, tokenXMint)
        val poolReserveY = PdaUtils.getPoolReservePda(poolPda, tokenYMint)
        
        // Get user ATAs
        val userTokenX = PdaUtils.getAssociatedTokenAddress(userAddress, tokenXMint)
        val userTokenY = PdaUtils.getAssociatedTokenAddress(userAddress, tokenYMint)
        val userLpToken = PdaUtils.getAssociatedTokenAddress(userAddress, lpMintPda)
        
        // Build instruction data
        val data = ByteBuffer.allocate(32).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put(discriminator)
            putLong((lpTokenAmount * 1e9).toLong()) // LP token amount
            putLong((minAmountX * 1e9).toLong()) // Min amount X
            putLong((minAmountY * 1e9).toLong()) // Min amount Y
        }.array()
        
        // Build accounts array
        val keys = listOf(
            AccountMeta(PublicKey(userAddress), isSigner = true, isWritable = true),
            AccountMeta(PublicKey(poolPda), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(lpMintPda), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(poolReserveX), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(poolReserveY), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(userTokenX), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(userTokenY), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(userLpToken), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(TOKEN_PROGRAM_ID), isSigner = false, isWritable = false)
        ).map { it.toSolanaAccountMeta() }
        
        return TransactionInstruction(
            PublicKey(PROGRAM_ID),
            keys,
            data
        )
    }
    
    /**
     * Account metadata for transaction instructions
     */
    data class AccountMeta(
        val pubkey: PublicKey,
        val isSigner: Boolean,
        val isWritable: Boolean
    )
}

/**
 * Extension to convert our AccountMeta to SolanaKT's format
 */
fun AnchorIDLTransactionBuilder.AccountMeta.toSolanaAccountMeta(): com.solana.core.AccountMeta {
    return com.solana.core.AccountMeta(
        publicKey = pubkey,
        isSigner = isSigner,
        isWritable = isWritable
    )
}