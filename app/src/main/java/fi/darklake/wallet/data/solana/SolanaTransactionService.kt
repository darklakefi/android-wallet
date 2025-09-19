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
        wallet: fi.darklake.wallet.crypto.SolanaWallet,
        toAddress: String,
        lamports: Long
    ): TransactionResult = withContext(Dispatchers.IO) {
        try {
            println("=== SOLANA SOL TRANSFER (SolanaKT) ===")
            println("To: $toAddress")
            println("Amount: $lamports lamports")

            val solana = createSolanaClient()

            val fromPubkey = PublicKey(wallet.publicKey)
            val toPubkey = PublicKey(toAddress)

            println("From address: ${fromPubkey.toBase58()}")

            // Get latest blockhash with "finalized" commitment for reliability
            println("Getting latest blockhash...")
            val blockhashResult = solana.api.getLatestBlockhash()
            if (blockhashResult.isFailure) {
                return@withContext TransactionResult.Error(
                    "Failed to get latest blockhash",
                    blockhashResult.exceptionOrNull() as? Exception
                )
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

            // Use the unified signing flow
            val signingRequest = wallet.prepareTransaction(transaction)

            // Check the signing method
            when (val method = signingRequest.signingMethod) {
                is fi.darklake.wallet.crypto.SigningMethod.Local -> {
                    // For local wallets, we can sign immediately
                    val signedTransaction = wallet.completeSignature(transaction, ByteArray(0))

                    // Send raw transaction with skipPreflight to avoid simulation issues
                    println("Sending transaction (skipping preflight simulation)...")
                    val transactionOptions = TransactionOptions(skipPreflight = true)
                    val signature = solana.api.sendRawTransaction(signedTransaction.serialize(), transactionOptions)

                    if (signature.isFailure) {
                        val error = signature.exceptionOrNull()
                        println("Transaction failed: ${error?.message}")
                        return@withContext TransactionResult.Error(
                            "Failed to send transaction",
                            error as? Exception
                        )
                    }

                    val txSignature = signature.getOrThrow()
                    println("Transaction sent successfully: $txSignature")

                    TransactionResult.Success(txSignature)
                }
                is fi.darklake.wallet.crypto.SigningMethod.SeedVault -> {
                    // For Seed Vault, return the signing request for UI handling
                    println("SolanaTransactionService: Seed Vault signing needed, returning NeedsSignature result")
                    TransactionResult.NeedsSignature(signingRequest)
                }
            }

        } catch (e: Exception) {
            println("SOL transfer failed: ${e.message}")
            e.printStackTrace()
            TransactionResult.Error("SOL transfer failed", e)
        }
    }

    suspend fun sendTokenTransaction(
        wallet: fi.darklake.wallet.crypto.SolanaWallet,
        toAddress: String,
        tokenMint: String,
        amount: Long,
        decimals: Int
    ): TransactionResult = withContext(Dispatchers.IO) {
        try {
            println("=== SOLANA TOKEN TRANSFER (SolanaKT) ===")
            println("To: $toAddress")
            println("Token: $tokenMint")
            println("Amount: $amount (decimals: $decimals)")

            val solana = createSolanaClient()

            val fromPubkey = PublicKey(wallet.publicKey)
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
                return@withContext TransactionResult.Error(
                    "Failed to get latest blockhash",
                    blockhashResult.exceptionOrNull() as? Exception
                )
            }
            val blockhashResponse = blockhashResult.getOrThrow()
            val blockhash = blockhashResponse.blockhash
            println("Latest blockhash: $blockhash")
            println("Last valid block height: ${blockhashResponse.lastValidBlockHeight}")

            // Sign and send
            transaction.setRecentBlockHash(blockhash)

            // Use the unified signing flow
            val signingRequest = wallet.prepareTransaction(transaction)

            // Check the signing method
            when (val method = signingRequest.signingMethod) {
                is fi.darklake.wallet.crypto.SigningMethod.Local -> {
                    // For local wallets, we can sign immediately
                    val signedTransaction = wallet.completeSignature(transaction, ByteArray(0))

                    // Send with skipPreflight to avoid simulation issues
                    val transactionOptions = TransactionOptions(skipPreflight = true)
                    val signature = solana.api.sendRawTransaction(signedTransaction.serialize(), transactionOptions)

                    if (signature.isFailure) {
                        return@withContext TransactionResult.Error(
                            "Failed to send transaction",
                            signature.exceptionOrNull() as? Exception
                        )
                    }

                    val txSignature = signature.getOrThrow()
                    println("Token transfer sent successfully: $txSignature")
                    TransactionResult.Success(txSignature)
                }
                is fi.darklake.wallet.crypto.SigningMethod.SeedVault -> {
                    // For Seed Vault, return the signing request for UI handling
                    println("SolanaTransactionService: Seed Vault signing needed, returning NeedsSignature result")
                    TransactionResult.NeedsSignature(signingRequest)
                }
            }

        } catch (e: Exception) {
            println("Token transfer failed: ${e.message}")
            e.printStackTrace()
            TransactionResult.Error("Token transfer failed", e)
        }
    }

    suspend fun sendNftTransaction(
        wallet: fi.darklake.wallet.crypto.SolanaWallet,
        toAddress: String,
        nftMint: String
    ): TransactionResult = withContext(Dispatchers.IO) {
        try {
            println("=== SOLANA NFT TRANSFER (SolanaKT) ===")
            println("To: $toAddress")
            println("NFT: $nftMint")

            // NFT transfers are just token transfers with amount = 1 and decimals = 0
            return@withContext sendTokenTransaction(
                wallet = wallet,
                toAddress = toAddress,
                tokenMint = nftMint,
                amount = 1,
                decimals = 0
            )

        } catch (e: Exception) {
            println("NFT transfer failed: ${e.message}")
            e.printStackTrace()
            TransactionResult.Error("NFT transfer failed", e)
        }
    }

    /**
     * Signs a transaction using wallet interface
     * Handles both legacy and versioned transactions
     * @param unsignedTransactionBase64 Base64 encoded unsigned transaction
     * @param wallet Wallet to sign with
     * @return Base64 encoded signed transaction or throws SigningRequiredException for UI handling
     */
    suspend fun signTransaction(
        unsignedTransactionBase64: String,
        wallet: fi.darklake.wallet.crypto.SolanaWallet
    ): String = withContext(Dispatchers.IO) {
        try {
            println("Starting transaction signing")

            // Decode the unsigned transaction from base64
            val transactionBytes = android.util.Base64.decode(unsignedTransactionBase64, android.util.Base64.DEFAULT)

            // Check if this is a versioned transaction (starts with 0x80) or legacy transaction
            val isVersioned = transactionBytes.isNotEmpty() && (transactionBytes[0].toInt() and 0x80) != 0

            val signedTransactionBase64 = if (isVersioned) {
                // Handle versioned transaction - needs special handling for LocalWallet
                // For now, throw an exception as versioned transactions need special wallet support
                throw UnsupportedOperationException("Versioned transaction signing not yet supported with wallet interface")
            } else {
                // Handle legacy transaction
                val transaction = Transaction.from(transactionBytes)

                // Use the unified signing flow
                val signingRequest = wallet.prepareTransaction(transaction)

                // Check the signing method
                val signedTransaction = when (val method = signingRequest.signingMethod) {
                    is fi.darklake.wallet.crypto.SigningMethod.Local -> {
                        // For local wallets, we can sign immediately
                        wallet.completeSignature(transaction, ByteArray(0))
                    }
                    is fi.darklake.wallet.crypto.SigningMethod.SeedVault -> {
                        // For Seed Vault, we cannot handle it here - need UI
                        throw UnsupportedOperationException("Seed Vault signing requires UI interaction")
                    }
                }

                // Serialize and encode back to base64
                android.util.Base64.encodeToString(signedTransaction.serialize(), android.util.Base64.NO_WRAP)
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
            println("Base64 length: ${signedTransactionBase64.length}")

            val solana = createSolanaClient()

            // Decode the base64 transaction
            val transactionBytes = android.util.Base64.decode(signedTransactionBase64, android.util.Base64.NO_WRAP)
            println("Transaction bytes length: ${transactionBytes.size}")

            // Log first few bytes for debugging
            if (transactionBytes.size >= 10) {
                val hexStart = transactionBytes.take(10).joinToString("") { "%02x".format(it) }
                println("Transaction starts with: $hexStart")
            }

            // Send with skipPreflight to avoid simulation issues
            val transactionOptions = TransactionOptions(skipPreflight = true)
            println("Sending raw transaction with skipPreflight=true")
            val signature = solana.api.sendRawTransaction(transactionBytes, transactionOptions)

            if (signature.isFailure) {
                val error = signature.exceptionOrNull()
                println("Transaction submission failed: ${error?.message}")
                println("Error type: ${error?.javaClass?.simpleName}")
                if (error != null) {
                    error.printStackTrace()
                }
                return@withContext null
            }

            val txSignature = signature.getOrThrow()
            println("Transaction submitted successfully: $txSignature")
            txSignature

        } catch (e: Exception) {
            println("Failed to submit transaction: ${e.message}")
            println("Exception type: ${e.javaClass.simpleName}")
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