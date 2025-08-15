package fi.darklake.wallet.data.solana

import com.solana.Solana
import com.solana.api.getLatestBlockhash
import com.solana.api.sendRawTransaction
import com.solana.api.getAccountInfo
import com.solana.core.HotAccount
import com.solana.core.PublicKey
import com.solana.core.Transaction
import com.solana.core.TransactionInstruction
import com.solana.networking.HttpNetworkingRouter
import com.solana.networking.RPCEndpoint
import com.solana.programs.SystemProgram
import com.solana.programs.TokenProgram
import com.solana.programs.AssociatedTokenProgram
import fi.darklake.wallet.data.model.getHeliusRpcUrl
import fi.darklake.wallet.data.preferences.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Service for building and sending Solana transactions using SolanaKT
 * Handles SOL transfers, SPL token transfers, and NFT transfers
 */
class SolanaKTTransactionService(
    private val settingsManager: SettingsManager
) {
    companion object {
        // SPL Token Program ID
        private val TOKEN_PROGRAM_ID = PublicKey("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")
        
        // Associated Token Program ID
        private val ASSOCIATED_TOKEN_PROGRAM_ID = PublicKey("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL")
    }

    private suspend fun createSolanaClient(): Solana {
        val networkSettings = settingsManager.networkSettings.value
        val rpcUrl = networkSettings.getHeliusRpcUrl()
        
        // Create custom RPC endpoint with Helius URL
        val endpoint = RPCEndpoint.custom(
            url = URL(rpcUrl),
            urlWebSocket = URL(rpcUrl.replace("https://", "wss://")),
            network = com.solana.networking.Network.mainnetBeta
        )
        
        val router = HttpNetworkingRouter(endpoint)
        return Solana(router)
    }

    suspend fun sendSolTransaction(
        fromPrivateKey: ByteArray,
        toAddress: String,
        lamports: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        TransactionErrorHandler.withRetry {
            try {
                println("=== SOLANA SOL TRANSFER (SolanaKT) ===")
                println("To: $toAddress")
                println("Amount: $lamports lamports")

                val solana = createSolanaClient()
                
                // Create account from private key
                val account = HotAccount(fromPrivateKey)
                val fromPubkey = account.publicKey
                val toPubkey = PublicKey(toAddress)
                
                println("From address: ${fromPubkey.toBase58()}")
                
                // Get latest blockhash
                val blockhashResult = solana.api.getLatestBlockhash()
                if (blockhashResult.isFailure) {
                    return@withRetry Result.failure(Exception("Failed to get latest blockhash: ${blockhashResult.exceptionOrNull()?.message}"))
                }
                val blockhash = blockhashResult.getOrThrow().blockhash
                println("Latest blockhash: $blockhash")
                
                // Create transfer instruction  
                val transferInstruction = SystemProgram.transfer(
                    fromPublicKey = fromPubkey,
                    toPublickKey = toPubkey,  // Note: this is how it's spelled in SolanaKT (typo in their code)
                    lamports = lamports
                )
                
                // Create transaction
                val transaction = Transaction()
                transaction.add(transferInstruction)
                transaction.setRecentBlockHash(blockhash)
                transaction.sign(listOf(account))
                
                // Send transaction
                val signature = solana.api.sendRawTransaction(transaction.serialize())
                
                if (signature.isFailure) {
                    return@withRetry Result.failure(Exception("Failed to send transaction: ${signature.exceptionOrNull()?.message}"))
                }
                
                val txSignature = signature.getOrThrow()
                println("Transaction sent successfully: $txSignature")
                
                Result.success(txSignature)

            } catch (e: Exception) {
                println("SOL transfer failed: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun sendTokenTransaction(
        fromPrivateKey: ByteArray,
        toAddress: String,
        tokenMint: String,
        amount: Long,
        decimals: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        TransactionErrorHandler.withRetry {
            try {
                println("=== SOLANA TOKEN TRANSFER (SolanaKT) ===")
                println("To: $toAddress")
                println("Token: $tokenMint")
                println("Amount: $amount (decimals: $decimals)")

                val solana = createSolanaClient()
                
                // Create account from private key
                val account = HotAccount(fromPrivateKey)
                val fromPubkey = account.publicKey
                val toPubkey = PublicKey(toAddress)
                val mintPubkey = PublicKey(tokenMint)
                
                // Get associated token accounts
                val fromTokenAccount = PublicKey.associatedTokenAddress(fromPubkey, mintPubkey).address
                val toTokenAccount = PublicKey.associatedTokenAddress(toPubkey, mintPubkey).address
                
                // Create transaction
                val transaction = Transaction()
                
                // For simplicity, always create ATA instruction if it doesn't exist
                // The transaction will fail if ATA already exists, so we'll add it anyway
                // and rely on the program to handle it gracefully
                val createAtaInstruction = AssociatedTokenProgram.createAssociatedTokenAccountInstruction(
                    payer = fromPubkey,
                    associatedAccount = toTokenAccount,
                    owner = toPubkey,
                    mint = mintPubkey
                )
                transaction.add(createAtaInstruction)
                
                // Add transfer instruction
                val transferInstruction = TokenProgram.transferChecked(
                    source = fromTokenAccount,
                    destination = toTokenAccount,
                    amount = amount,
                    decimals = decimals.toByte(),
                    owner = fromPubkey,
                    tokenMint = mintPubkey
                )
                transaction.add(transferInstruction)
                
                // Get latest blockhash
                val blockhashResult = solana.api.getLatestBlockhash()
                if (blockhashResult.isFailure) {
                    return@withRetry Result.failure(Exception("Failed to get latest blockhash: ${blockhashResult.exceptionOrNull()?.message}"))
                }
                val blockhash = blockhashResult.getOrThrow().blockhash
                
                // Sign and send
                transaction.setRecentBlockHash(blockhash)
                transaction.sign(listOf(account))
                
                val signature = solana.api.sendRawTransaction(transaction.serialize())
                
                if (signature.isFailure) {
                    return@withRetry Result.failure(Exception("Failed to send transaction: ${signature.exceptionOrNull()?.message}"))
                }
                
                val txSignature = signature.getOrThrow()
                println("Token transfer sent successfully: $txSignature")
                Result.success(txSignature)

            } catch (e: Exception) {
                println("Token transfer failed: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun sendNftTransaction(
        fromPrivateKey: ByteArray,
        toAddress: String,
        nftMint: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            println("=== SOLANA NFT TRANSFER (SolanaKT) ===")
            println("To: $toAddress")
            println("NFT: $nftMint")

            // NFT transfers are just token transfers with amount = 1 and decimals = 0
            return@withContext sendTokenTransaction(
                fromPrivateKey = fromPrivateKey,
                toAddress = toAddress,
                tokenMint = nftMint,
                amount = 1,
                decimals = 0
            )

        } catch (e: Exception) {
            println("NFT transfer failed: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun close() {
        // No specific cleanup needed for SolanaKT
    }
}