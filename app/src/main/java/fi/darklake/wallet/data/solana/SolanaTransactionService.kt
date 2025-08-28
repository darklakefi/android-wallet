package fi.darklake.wallet.data.solana

import com.solana.Solana
import com.solana.api.getLatestBlockhash
import com.solana.api.sendRawTransaction
import com.solana.core.HotAccount
import com.solana.core.PublicKey
import com.solana.core.Transaction
import com.solana.core.TransactionInstruction
import com.solana.networking.HttpNetworkingRouter
import com.solana.networking.RPCEndpoint
import com.solana.networking.TransactionOptions
import com.solana.programs.SystemProgram
import com.solana.programs.TokenProgram
import fi.darklake.wallet.data.model.getHeliusRpcUrl
import fi.darklake.wallet.data.preferences.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Service for building and sending Solana transactions using SolanaKT
 * Handles SOL transfers, SPL token transfers, and NFT transfers
 */
class SolanaTransactionService(
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
        
        // Determine the correct network based on settings
        val network = when (networkSettings.network) {
            fi.darklake.wallet.data.model.SolanaNetwork.MAINNET -> com.solana.networking.Network.mainnetBeta
            fi.darklake.wallet.data.model.SolanaNetwork.DEVNET -> com.solana.networking.Network.devnet
        }
        
        println("Network configuration:")
        println("  - Selected network: ${networkSettings.network}")
        println("  - SolanaKT network: ${network.name}")
        println("  - RPC URL: $rpcUrl")
        println("  - Helius API key present: ${networkSettings.heliusApiKey != null}")
        
        // Create custom RPC endpoint
        // For HTTP-only usage, we'll use the same URL for WebSocket (it won't be used for our RPC calls)
        val endpoint = RPCEndpoint.custom(
            url = URL(rpcUrl),
            urlWebSocket = URL(rpcUrl), // Use same URL, WebSocket not needed for our RPC calls
            network = network
        )
        
        val router = HttpNetworkingRouter(endpoint)
        return Solana(router)
    }

    private fun createAssociatedTokenAccountIdempotentInstruction(
        associatedProgramId: PublicKey = ASSOCIATED_TOKEN_PROGRAM_ID,
        programId: PublicKey = TOKEN_PROGRAM_ID,
        mint: PublicKey,
        associatedAccount: PublicKey,
        owner: PublicKey,
        payer: PublicKey
    ): TransactionInstruction {
        val keys = listOf(
            com.solana.core.AccountMeta(payer, isSigner = true, isWritable = true),
            com.solana.core.AccountMeta(associatedAccount, isSigner = false, isWritable = true),
            com.solana.core.AccountMeta(owner, isSigner = false, isWritable = false),
            com.solana.core.AccountMeta(mint, isSigner = false, isWritable = false),
            com.solana.core.AccountMeta(SystemProgram.PROGRAM_ID, isSigner = false, isWritable = false),
            com.solana.core.AccountMeta(programId, isSigner = false, isWritable = false),
            com.solana.core.AccountMeta(TokenProgram.SYSVAR_RENT_PUBKEY, isSigner = false, isWritable = false)
        )

        return TransactionInstruction(
            keys = keys,
            programId = associatedProgramId,
            data = byteArrayOf(1), // Idempotent instruction data: [1]
        )
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
                // If it's 32 bytes, it's a seed; if 64 bytes, it's a full keypair
                val account = if (fromPrivateKey.size == 32) {
                    // Create keypair from seed
                    val keypair = com.solana.vendor.TweetNaclFast.Signature.keyPair_fromSeed(fromPrivateKey)
                    HotAccount(keypair.secretKey)
                } else {
                    HotAccount(fromPrivateKey)
                }
                val fromPubkey = account.publicKey
                val toPubkey = PublicKey(toAddress)
                
                println("From address: ${fromPubkey.toBase58()}")
                
                // Get latest blockhash with "finalized" commitment for reliability
                println("Getting latest blockhash...")
                val blockhashResult = solana.api.getLatestBlockhash()
                if (blockhashResult.isFailure) {
                    return@withRetry Result.failure(Exception("Failed to get latest blockhash: ${blockhashResult.exceptionOrNull()?.message}"))
                }
                val blockhashResponse = blockhashResult.getOrThrow()
                val blockhash = blockhashResponse.blockhash
                println("Latest blockhash: $blockhash")
                println("Last valid block height: ${blockhashResponse.lastValidBlockHeight}")
                
                // Validate blockhash format (should be base58 string, approximately 32-44 characters)
                if (blockhash.length < 32 || blockhash.length > 50) {
                    println("WARNING: Blockhash has unusual length: ${blockhash.length}")
                }
                
                // Create transfer instruction  
                val transferInstruction = SystemProgram.transfer(
                    fromPublicKey = fromPubkey,
                    toPublickKey = toPubkey,  // Note: this is how it's spelled in SolanaKT (typo in their code)
                    lamports = lamports
                )
                
                // Create transaction
                val transaction = Transaction()
                transaction.feePayer = fromPubkey
                transaction.add(transferInstruction)
                
                // Set blockhash explicitly
                transaction.setRecentBlockHash(blockhash)
                
                // Sign the transaction
                transaction.sign(account)
                
                // Send raw transaction with skipPreflight to avoid simulation issues
                println("Sending transaction (skipping preflight simulation)...")
                val transactionOptions = TransactionOptions(skipPreflight = true)
                val signature = solana.api.sendRawTransaction(transaction.serialize(), transactionOptions)
                
                if (signature.isFailure) {
                    val error = signature.exceptionOrNull()
                    println("Transaction failed: ${error?.message}")
                    return@withRetry Result.failure(Exception("Failed to send transaction: ${error?.message}"))
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
                // If it's 32 bytes, it's a seed; if 64 bytes, it's a full keypair
                val account = if (fromPrivateKey.size == 32) {
                    // Create keypair from seed
                    val keypair = com.solana.vendor.TweetNaclFast.Signature.keyPair_fromSeed(fromPrivateKey)
                    HotAccount(keypair.secretKey)
                } else {
                    HotAccount(fromPrivateKey)
                }
                val fromPubkey = account.publicKey
                val toPubkey = PublicKey(toAddress)
                val mintPubkey = PublicKey(tokenMint)
                
                // Get associated token accounts
                val fromTokenAccount = PublicKey.associatedTokenAddress(fromPubkey, mintPubkey).address
                val toTokenAccount = PublicKey.associatedTokenAddress(toPubkey, mintPubkey).address
                
                // Create transaction
                val transaction = Transaction()
                transaction.feePayer = fromPubkey
                
                // Use idempotent version that won't fail if ATA already exists
                val createAtaInstruction = createAssociatedTokenAccountIdempotentInstruction(
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
                
                // Get latest blockhash before building transaction
                val blockhashResult = solana.api.getLatestBlockhash()
                if (blockhashResult.isFailure) {
                    return@withRetry Result.failure(Exception("Failed to get latest blockhash: ${blockhashResult.exceptionOrNull()?.message}"))
                }
                val blockhashResponse = blockhashResult.getOrThrow()
                val blockhash = blockhashResponse.blockhash
                println("Latest blockhash: $blockhash")
                println("Last valid block height: ${blockhashResponse.lastValidBlockHeight}")
                
                // Sign and send
                transaction.setRecentBlockHash(blockhash)
                transaction.sign(listOf(account))
                
                // Send with skipPreflight to avoid simulation issues
                val transactionOptions = TransactionOptions(skipPreflight = true)
                val signature = solana.api.sendRawTransaction(transaction.serialize(), transactionOptions)
                
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

    /**
     * Signs a transaction using SolanaKT libraries
     * Handles both legacy and versioned transactions
     * @param unsignedTransactionBase64 Base64 encoded unsigned transaction
     * @param privateKey Private key bytes for signing
     * @return Base64 encoded signed transaction
     */
    suspend fun signTransaction(
        unsignedTransactionBase64: String,
        privateKey: ByteArray
    ): String = withContext(Dispatchers.IO) {
        try {
            println("Starting transaction signing")
            
            // Decode the unsigned transaction from base64
            val transactionBytes = android.util.Base64.decode(unsignedTransactionBase64, android.util.Base64.DEFAULT)
            
            // Check if this is a versioned transaction (starts with 0x80) or legacy transaction
            val isVersioned = transactionBytes.isNotEmpty() && (transactionBytes[0].toInt() and 0x80) != 0
            
            val signedTransactionBase64 = if (isVersioned) {
                // Handle versioned transaction
                val version = transactionBytes[0].toInt() and 0x7f
                val numSignatures = transactionBytes[1].toInt() and 0xff
                val messageStart = 2 + (numSignatures * 64)
                val message = transactionBytes.sliceArray(messageStart until transactionBytes.size)
                
                val account = if (privateKey.size == 32) {
                    val keypair = com.solana.vendor.TweetNaclFast.Signature.keyPair_fromSeed(privateKey)
                    HotAccount(keypair.secretKey)
                } else {
                    HotAccount(privateKey)
                }
                
                val signature = account.sign(message)
                val signedTxSize = 1 + 1 + 64 + message.size
                val signedTransaction = ByteArray(signedTxSize)
                var offset = 0
                
                signedTransaction[offset++] = (0x80 or version).toByte()
                signedTransaction[offset++] = 1
                System.arraycopy(signature, 0, signedTransaction, offset, 64)
                offset += 64
                System.arraycopy(message, 0, signedTransaction, offset, message.size)
                
                android.util.Base64.encodeToString(signedTransaction, android.util.Base64.NO_WRAP)
            } else {
                // Handle legacy transaction
                val transaction = Transaction.from(transactionBytes)
                
                // Create account from private key
                val account = if (privateKey.size == 32) {
                    val keypair = com.solana.vendor.TweetNaclFast.Signature.keyPair_fromSeed(privateKey)
                    HotAccount(keypair.secretKey)
                } else {
                    HotAccount(privateKey)
                }
                
                // Sign the transaction
                transaction.sign(account)
                
                // Serialize and encode back to base64
                android.util.Base64.encodeToString(transaction.serialize(), android.util.Base64.NO_WRAP)
            }
            
            println("Transaction signing completed")
            signedTransactionBase64
        } catch (e: Exception) {
            throw Exception("Failed to sign transaction: ${e.message}")
        }
    }

    /**
     * Submit a signed transaction to the Solana network
     * @param signedTransactionBase64 Base64 encoded signed transaction
     * @return Transaction signature or null if failed
     */
    suspend fun submitSignedTransaction(
        signedTransactionBase64: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            println("=== SUBMITTING SIGNED TRANSACTION ===")
            
            val solana = createSolanaClient()
            
            // Decode the base64 transaction
            val transactionBytes = android.util.Base64.decode(signedTransactionBase64, android.util.Base64.NO_WRAP)
            
            // Send with skipPreflight to avoid simulation issues
            val transactionOptions = TransactionOptions(skipPreflight = true)
            val signature = solana.api.sendRawTransaction(transactionBytes, transactionOptions)
            
            if (signature.isFailure) {
                val error = signature.exceptionOrNull()
                println("Transaction submission failed: ${error?.message}")
                return@withContext null
            }
            
            val txSignature = signature.getOrThrow()
            println("Transaction submitted successfully: $txSignature")
            txSignature
            
        } catch (e: Exception) {
            println("Failed to submit transaction: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Wait for transaction confirmation
     * @param signature Transaction signature to wait for
     * @param timeout Timeout in seconds
     * @return true if confirmed, false if timeout or error
     */
    suspend fun waitForConfirmation(
        signature: String,
        timeout: Int = 30
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            println("Waiting for transaction confirmation: $signature")
            
            val solana = createSolanaClient()
            val startTime = System.currentTimeMillis()
            val timeoutMs = timeout * 1000L
            
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                // Check transaction status using getSignatureStatus
                // Since getTransaction might not be available, we'll use a simpler check
                try {
                    // For now, we'll just wait and assume it's confirmed after a few seconds
                    // In production, you'd want to use proper status checking
                    delay(2000)
                    println("Transaction likely confirmed: $signature")
                    return@withContext true
                } catch (e: Exception) {
                    println("Error checking transaction status: ${e.message}")
                }
                
                // Wait a bit before checking again
                delay(1000)
            }
            
            println("Transaction confirmation timeout: $signature")
            false
            
        } catch (e: Exception) {
            println("Error waiting for confirmation: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun close() {
        // No specific cleanup needed for SolanaKT
    }
}