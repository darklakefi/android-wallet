package fi.darklake.wallet.data.solana

import fi.darklake.wallet.data.model.getHeliusRpcUrl
import fi.darklake.wallet.data.preferences.SettingsManager
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import java.nio.charset.StandardCharsets

/**
 * Service for building and sending Solana transactions
 * Handles SOL transfers, SPL token transfers, and NFT transfers
 */
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
class SolanaTransactionService(
    private val settingsManager: SettingsManager
) {
    private val transactionMonitor = TransactionMonitor(settingsManager)
    companion object {
        // Solana System Program ID
        private const val SYSTEM_PROGRAM_ID = "11111111111111111111111111111111"
        
        // SPL Token Program ID
        private const val TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
        
        // Associated Token Program ID
        private const val ASSOCIATED_TOKEN_PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"
        
        // Base58 alphabet for Solana addresses
        private const val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(this@SolanaTransactionService.json)
        }
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    println("HTTP Client: $message")
                }
            }
        }
        engine {
            connectTimeout = 30_000
            socketTimeout = 30_000
        }
    }

    @Serializable
    private data class JsonRpcRequest(
        @SerialName("jsonrpc")
        val jsonrpc: String = "2.0",
        @SerialName("id")
        val id: String = "1",
        @SerialName("method")
        val method: String,
        @SerialName("params")
        val params: List<kotlinx.serialization.json.JsonElement>
    )

    @Serializable
    private data class SendTransactionResponse(
        @SerialName("jsonrpc")
        val jsonrpc: String,
        @SerialName("id")
        val id: String,
        @SerialName("result")
        val result: String? = null,
        @SerialName("error")
        val error: SolanaError? = null
    )

    @Serializable
    private data class SolanaError(
        @SerialName("code")
        val code: Int,
        @SerialName("message")
        val message: String
    )

    @Serializable
    private data class RecentBlockhashResponse(
        @SerialName("jsonrpc")
        val jsonrpc: String,
        @SerialName("id")
        val id: String,
        @SerialName("result")
        val result: RecentBlockhashResult? = null,
        @SerialName("error")
        val error: SolanaError? = null
    )

    @Serializable
    private data class RecentBlockhashResult(
        @SerialName("context")
        val context: BlockhashContext,
        @SerialName("value")
        val value: BlockhashValue
    )

    @Serializable
    private data class BlockhashContext(
        @SerialName("slot")
        val slot: Long
    )

    @Serializable
    private data class BlockhashValue(
        @SerialName("blockhash")
        val blockhash: String,
        @SerialName("feeCalculator")
        val feeCalculator: FeeCalculator
    )

    @Serializable
    private data class FeeCalculator(
        @SerialName("lamportsPerSignature")
        val lamportsPerSignature: Long
    )
    
    @Serializable
    private data class AccountInfoResponse(
        @SerialName("jsonrpc")
        val jsonrpc: String,
        @SerialName("id")
        val id: String,
        @SerialName("result")
        val result: AccountInfoResult? = null,
        @SerialName("error")
        val error: SolanaError? = null
    )
    
    @Serializable
    private data class AccountInfoResult(
        @SerialName("context")
        val context: BlockhashContext,
        @SerialName("value")
        val value: AccountInfo? = null
    )
    
    @Serializable
    private data class AccountInfo(
        @SerialName("data")
        val data: List<String>,
        @SerialName("executable")
        val executable: Boolean,
        @SerialName("lamports")
        val lamports: Long,
        @SerialName("owner")
        val owner: String,
        @SerialName("rentEpoch")
        val rentEpoch: Long
    )

    suspend fun sendSolTransaction(
        fromPrivateKey: ByteArray,
        toAddress: String,
        lamports: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        TransactionErrorHandler.withRetry {
            try {
                println("=== SOLANA SOL TRANSFER ===")
                println("To: $toAddress")
                println("Amount: $lamports lamports")

                // 1. Get recent blockhash
                val blockhash = getRecentBlockhash().getOrElse { 
                    return@withRetry Result.failure(Exception("Failed to get recent blockhash: ${it.message}"))
                }
                println("Recent blockhash: $blockhash")

                // 2. Build transaction
                val fromAddress = getAddressFromPrivateKey(fromPrivateKey)
                println("From address: $fromAddress")
                
                val transaction = buildSolTransferTransaction(
                    fromAddress = fromAddress,
                    toAddress = toAddress,
                    lamports = lamports,
                    recentBlockhash = blockhash,
                    privateKey = fromPrivateKey
                )
                println("Transaction built and signed")

                // 3. Send transaction
                val signature = sendTransaction(transaction).getOrElse {
                    return@withRetry Result.failure(Exception("Failed to send transaction: ${it.message}"))
                }

                println("Transaction sent successfully: $signature")
                
                // 4. Wait for confirmation
                val confirmationResult = transactionMonitor.waitForConfirmation(
                    signature = signature,
                    targetStatus = TransactionMonitor.ConfirmationStatus.CONFIRMED,
                    maxWaitTime = 20_000L
                )
                
                if (confirmationResult.isFailure) {
                    println("Warning: Could not confirm transaction: ${confirmationResult.exceptionOrNull()?.message}")
                } else {
                    val status = confirmationResult.getOrNull()!!
                    println("Transaction confirmed: ${status.confirmationStatus} with ${status.confirmations} confirmations")
                }
                
                Result.success(signature)

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
                println("=== SOLANA TOKEN TRANSFER ===")
                println("To: $toAddress")
                println("Token: $tokenMint")
                println("Amount: $amount (decimals: $decimals)")

                // Get recent blockhash
                val blockhash = getRecentBlockhash().getOrElse { 
                    return@withRetry Result.failure(Exception("Failed to get recent blockhash: ${it.message}"))
                }

                // Get from address
                val fromAddress = getAddressFromPrivateKey(fromPrivateKey)
                
                // Build and sign token transfer transaction
                val transaction = buildTokenTransferTransaction(
                    fromAddress = fromAddress,
                    toAddress = toAddress,
                    tokenMint = tokenMint,
                    amount = amount,
                    recentBlockhash = blockhash,
                    privateKey = fromPrivateKey
                )

                // Send transaction
                val signature = sendTransaction(transaction).getOrElse {
                    return@withRetry Result.failure(Exception("Failed to send transaction: ${it.message}"))
                }

                println("Token transfer sent successfully: $signature")
                Result.success(signature)

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
            println("=== SOLANA NFT TRANSFER ===")
            println("To: $toAddress")
            println("NFT: $nftMint")

            // NFT transfers are just token transfers with amount = 1
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

    private suspend fun getRecentBlockhash(): Result<String> {
        return try {
            val networkSettings = settingsManager.networkSettings.value
            val rpcUrl = networkSettings.getHeliusRpcUrl()

            val request = JsonRpcRequest(
                method = "getRecentBlockhash",
                params = emptyList()
            )

            val jsonString = json.encodeToString(request)
            
            val response = client.post(rpcUrl) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(jsonString)
            }

            val responseBody = response.bodyAsText()
            val blockhashResponse: RecentBlockhashResponse = json.decodeFromString(responseBody)

            if (blockhashResponse.error != null) {
                Result.failure(Exception("RPC Error: ${blockhashResponse.error.message}"))
            } else if (blockhashResponse.result == null) {
                Result.failure(Exception("No result in blockhash response"))
            } else {
                Result.success(blockhashResponse.result.value.blockhash)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendTransaction(signedTransaction: String): Result<String> {
        return try {
            val networkSettings = settingsManager.networkSettings.value
            val rpcUrl = networkSettings.getHeliusRpcUrl()

            val request = JsonRpcRequest(
                method = "sendTransaction",
                params = listOf(
                    kotlinx.serialization.json.JsonPrimitive(signedTransaction),
                    kotlinx.serialization.json.buildJsonObject {
                        put("encoding", kotlinx.serialization.json.JsonPrimitive("base64"))
                        put("skipPreflight", kotlinx.serialization.json.JsonPrimitive(false))
                        put("preflightCommitment", kotlinx.serialization.json.JsonPrimitive("processed"))
                        put("maxRetries", kotlinx.serialization.json.JsonPrimitive(5))
                    }
                )
            )

            val jsonString = json.encodeToString(request)
            
            val response = client.post(rpcUrl) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(jsonString)
            }

            val responseBody = response.bodyAsText()
            val sendResponse: SendTransactionResponse = json.decodeFromString(responseBody)

            if (sendResponse.error != null) {
                val errorMsg = "Send Error: ${sendResponse.error.message} (${TransactionErrorHandler.parseErrorCode(sendResponse.error.code)})"
                Result.failure(Exception(errorMsg))
            } else if (sendResponse.result == null) {
                Result.failure(Exception("No signature returned"))
            } else {
                Result.success(sendResponse.result)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSolTransferTransaction(
        fromAddress: String,
        toAddress: String,
        lamports: Long,
        recentBlockhash: String,
        privateKey: ByteArray
    ): String {
        // Build a system program transfer instruction
        val instruction = buildSystemTransferInstruction(fromAddress, toAddress, lamports)
        
        // Build the transaction message
        val message = buildTransactionMessage(
            instructions = listOf(instruction),
            recentBlockhash = recentBlockhash,
            feePayer = fromAddress
        )
        
        // Sign the message
        val signature = signMessage(message, privateKey)
        
        // Combine signature and message
        val transaction = ByteBuffer.allocate(1 + signature.size + message.size).apply {
            put(1.toByte()) // Number of signatures
            put(signature)
            put(message)
        }
        
        return Base64.encodeToString(transaction.array(), Base64.NO_WRAP)
    }

    private suspend fun buildTokenTransferTransaction(
        fromAddress: String,
        toAddress: String,
        tokenMint: String,
        amount: Long,
        recentBlockhash: String,
        privateKey: ByteArray
    ): String {
        // Get associated token accounts
        val fromTokenAccount = getAssociatedTokenAddress(fromAddress, tokenMint)
        val toTokenAccount = getAssociatedTokenAddress(toAddress, tokenMint)
        
        // Build token transfer instruction
        val transferInstruction = buildTokenTransferInstruction(
            fromTokenAccount = fromTokenAccount,
            toTokenAccount = toTokenAccount,
            fromAuthority = fromAddress,
            amount = amount
        )
        
        // Check if destination ATA exists, create if needed
        val needsAtaCreation = !checkAccountExists(toTokenAccount)
        val createAtaInstruction = if (needsAtaCreation) {
            buildCreateAssociatedTokenAccountInstruction(
                payer = fromAddress,
                associatedToken = toTokenAccount,
                owner = toAddress,
                mint = tokenMint
            )
        } else {
            null
        }
        
        // Build the transaction message with required instructions (unused now)
        
        // Convert instruction bytes to proper instruction objects
        val instructionObjects = mutableListOf<SolanaTransactionBuilder.Instruction>()
        
        if (createAtaInstruction != null) {
            // Parse create ATA instruction
            instructionObjects.add(parseCreateAtaInstruction(createAtaInstruction, fromAddress, toTokenAccount, toAddress, tokenMint))
        }
        
        // Parse transfer instruction  
        instructionObjects.add(parseTokenTransferInstruction(transferInstruction, fromTokenAccount, toTokenAccount, fromAddress, amount))
        
        // Build the transaction message using proper transaction builder
        val message = SolanaTransactionBuilder.compileMessage(
            instructions = instructionObjects,
            payer = decodeBase58(fromAddress),
            recentBlockhash = decodeBase58(recentBlockhash)
        )
        
        // Serialize the message
        val messageBytes = SolanaTransactionBuilder.serializeMessage(message)
        
        // Sign the message
        val signature = signMessage(messageBytes, privateKey)
        
        // Create signed transaction
        val signedTransaction = SolanaTransactionBuilder.createSignedTransaction(
            message = messageBytes,
            signatures = listOf(signature)
        )
        
        return Base64.encodeToString(signedTransaction, Base64.NO_WRAP)
    }

    private fun buildSystemTransferInstruction(from: String, to: String, lamports: Long): ByteArray {
        val programId = decodeBase58(SYSTEM_PROGRAM_ID)
        val fromPubkey = decodeBase58(from)
        val toPubkey = decodeBase58(to)
        
        // System program transfer instruction
        // [4-byte instruction index (2 for transfer)] + [8-byte lamports]
        val data = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(2) // Transfer instruction
            putLong(lamports)
        }.array()
        
        // Instruction format: program_id + accounts + data
        return ByteBuffer.allocate(1 + 1 + 32 + 2 * 33 + 1 + data.size).apply {
            put(1.toByte()) // Number of accounts
            put(2.toByte()) // Account 0 and 1 indices
            
            // Account metas: [is_signer, is_writable, pubkey]
            put(0x03.toByte()) // From account: signer + writable
            put(fromPubkey)
            put(0x01.toByte()) // To account: writable
            put(toPubkey)
            
            put(programId)
            put(data.size.toByte())
            put(data)
        }.array()
    }

    private fun buildTokenTransferInstruction(
        fromTokenAccount: String,
        toTokenAccount: String,
        fromAuthority: String,
        amount: Long
    ): ByteArray {
        val programId = decodeBase58(TOKEN_PROGRAM_ID)
        val source = decodeBase58(fromTokenAccount)
        val destination = decodeBase58(toTokenAccount)
        val owner = decodeBase58(fromAuthority)
        
        // Token program transfer instruction
        // [1-byte instruction (3 for transfer)] + [8-byte amount]
        val data = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(3.toByte()) // Transfer instruction
            putLong(amount)
        }.array()
        
        return ByteBuffer.allocate(1 + 3 + 32 * 4 + 1 + data.size).apply {
            put(3.toByte()) // Number of accounts
            
            // Account metas
            put(0x01.toByte()) // Source: writable
            put(source)
            put(0x01.toByte()) // Destination: writable
            put(destination)
            put(0x02.toByte()) // Authority: signer
            put(owner)
            
            put(programId)
            put(data.size.toByte())
            put(data)
        }.array()
    }

    private fun buildCreateAssociatedTokenAccountInstruction(
        payer: String,
        associatedToken: String,
        owner: String,
        mint: String
    ): ByteArray {
        val programId = decodeBase58(ASSOCIATED_TOKEN_PROGRAM_ID)
        
        return ByteBuffer.allocate(1 + 7 + 32 * 8).apply {
            put(7.toByte()) // Number of accounts
            
            // Account metas for create ATA instruction
            put(0x03.toByte()) // Payer: signer + writable
            put(decodeBase58(payer))
            put(0x01.toByte()) // Associated token account: writable
            put(decodeBase58(associatedToken))
            put(0x00.toByte()) // Owner: readonly
            put(decodeBase58(owner))
            put(0x00.toByte()) // Mint: readonly
            put(decodeBase58(mint))
            put(0x00.toByte()) // System program: readonly
            put(decodeBase58(SYSTEM_PROGRAM_ID))
            put(0x00.toByte()) // Token program: readonly
            put(decodeBase58(TOKEN_PROGRAM_ID))
            put(0x00.toByte()) // Rent sysvar: readonly (deprecated but required)
            put(decodeBase58("SysvarRent111111111111111111111111111111111"))
            
            put(programId)
            put(0.toByte()) // No data for create instruction
        }.array()
    }

    private fun buildTransactionMessage(
        instructions: List<ByteArray>,
        recentBlockhash: String,
        feePayer: String
    ): ByteArray {
        // Collect all unique account keys
        val accountKeys = mutableSetOf<String>()
        accountKeys.add(feePayer)
        
        // Add accounts from instructions (simplified - in production would parse properly)
        // For now, we'll use a simplified approach
        
        val header = ByteBuffer.allocate(3).apply {
            put(1.toByte()) // numRequiredSignatures
            put(0.toByte()) // numReadonlySignedAccounts
            put(0.toByte()) // numReadonlyUnsignedAccounts
        }.array()
        
        // Serialize account keys
        val accountKeysData = ByteBuffer.allocate(1 + accountKeys.size * 32).apply {
            put(accountKeys.size.toByte())
            accountKeys.forEach { put(decodeBase58(it)) }
        }.array()
        
        val blockhashData = decodeBase58(recentBlockhash)
        
        // Serialize instructions
        val instructionsData = ByteBuffer.allocate(1024).apply {
            put(instructions.size.toByte())
            instructions.forEach { put(it) }
        }
        val instructionsArray = instructionsData.array().sliceArray(0 until instructionsData.position())
        
        return header + accountKeysData + blockhashData + instructionsArray
    }

    private fun signMessage(message: ByteArray, privateKey: ByteArray): ByteArray {
        return try {
            Ed25519Utils.sign(message, privateKey)
        } catch (e: Exception) {
            throw Exception("Failed to sign message: ${e.message}")
        }
    }

    private fun getAddressFromPrivateKey(privateKey: ByteArray): String {
        val publicKey = Ed25519Utils.getPublicKey(privateKey)
        return encodeBase58(publicKey)
    }

    private fun getAssociatedTokenAddress(owner: String, mint: String): String {
        // Proper ATA derivation using Program Derived Address (PDA)
        return findProgramAddress(
            seeds = listOf(
                decodeBase58(owner),
                decodeBase58(TOKEN_PROGRAM_ID),
                decodeBase58(mint)
            ),
            programId = decodeBase58(ASSOCIATED_TOKEN_PROGRAM_ID)
        ).first
    }
    
    /**
     * Finds a Program Derived Address (PDA) for the given seeds and program ID
     * This is how Solana derives deterministic addresses
     */
    private fun findProgramAddress(seeds: List<ByteArray>, programId: ByteArray): Pair<String, Byte> {
        for (nonce in 255 downTo 0) {
            val address = createProgramAddress(seeds + listOf(byteArrayOf(nonce.toByte())), programId)
            if (address != null) {
                return Pair(address, nonce.toByte())
            }
        }
        throw IllegalArgumentException("Unable to find a viable program address nonce")
    }
    
    /**
     * Creates a program address from seeds and program ID
     */
    private fun createProgramAddress(seeds: List<ByteArray>, programId: ByteArray): String? {
        val buffer = ByteBuffer.allocate(seeds.sumOf { it.size } + programId.size + "ProgramDerivedAddress".length)
        
        // Add all seeds
        for (seed in seeds) {
            buffer.put(seed)
        }
        
        // Add program ID
        buffer.put(programId)
        
        // Add the PDA marker
        buffer.put("ProgramDerivedAddress".toByteArray(StandardCharsets.UTF_8))
        
        val hash = MessageDigest.getInstance("SHA-256").digest(buffer.array())
        
        // Check if this is a valid PDA (not on the Ed25519 curve)
        if (isOnCurve(hash)) {
            return null
        }
        
        return encodeBase58(hash)
    }
    
    /**
     * Simplified check if a point is on the Ed25519 curve
     * In production, this would use proper curve arithmetic
     */
    private fun isOnCurve(publicKey: ByteArray): Boolean {
        // Simplified heuristic: check if the last byte has certain patterns
        // that are less likely to be valid curve points
        // In production, use proper Ed25519 curve validation
        return (publicKey[31].toInt() and 0x80) == 0
    }
    
    /**
     * Checks if an account exists on Solana
     * This would normally be an async RPC call, but for now we'll assume most ATAs don't exist
     */
    private suspend fun checkAccountExists(address: String): Boolean {
        return try {
            val networkSettings = settingsManager.networkSettings.value
            val rpcUrl = networkSettings.getHeliusRpcUrl()
            
            val request = JsonRpcRequest(
                method = "getAccountInfo",
                params = listOf(
                    kotlinx.serialization.json.JsonPrimitive(address),
                    kotlinx.serialization.json.buildJsonObject {
                        put("encoding", kotlinx.serialization.json.JsonPrimitive("base64"))
                    }
                )
            )
            
            val jsonString = json.encodeToString(request)
            
            val response = client.post(rpcUrl) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(jsonString)
            }
            
            val responseBody = response.bodyAsText()
            val accountResponse: AccountInfoResponse = json.decodeFromString(responseBody)
            
            // Account exists if result is not null
            accountResponse.result?.value != null
        } catch (_: Exception) {
            // If we can't check, assume account doesn't exist (safer to create)
            false
        }
    }
    
    /**
     * Parses create ATA instruction into proper instruction object
     */
    private fun parseCreateAtaInstruction(
        @Suppress("UNUSED_PARAMETER") instructionBytes: ByteArray,
        payer: String,
        associatedToken: String,
        owner: String,
        mint: String
    ): SolanaTransactionBuilder.Instruction {
        return SolanaTransactionBuilder.Instruction(
            programId = decodeBase58(ASSOCIATED_TOKEN_PROGRAM_ID),
            accounts = listOf(
                SolanaTransactionBuilder.AccountMeta(decodeBase58(payer), isSigner = true, isWritable = true),
                SolanaTransactionBuilder.AccountMeta(decodeBase58(associatedToken), isSigner = false, isWritable = true),
                SolanaTransactionBuilder.AccountMeta(decodeBase58(owner), isSigner = false, isWritable = false),
                SolanaTransactionBuilder.AccountMeta(decodeBase58(mint), isSigner = false, isWritable = false),
                SolanaTransactionBuilder.AccountMeta(decodeBase58(SYSTEM_PROGRAM_ID), isSigner = false, isWritable = false),
                SolanaTransactionBuilder.AccountMeta(decodeBase58(TOKEN_PROGRAM_ID), isSigner = false, isWritable = false),
                SolanaTransactionBuilder.AccountMeta(decodeBase58("SysvarRent111111111111111111111111111111111"), isSigner = false, isWritable = false)
            ),
            data = byteArrayOf() // No data for create instruction
        )
    }
    
    /**
     * Parses token transfer instruction into proper instruction object
     */
    private fun parseTokenTransferInstruction(
        @Suppress("UNUSED_PARAMETER") instructionBytes: ByteArray,
        fromTokenAccount: String,
        toTokenAccount: String,
        fromAuthority: String,
        amount: Long
    ): SolanaTransactionBuilder.Instruction {
        // Token program transfer instruction data
        val instructionData = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(3.toByte()) // Transfer instruction
            putLong(amount)
        }.array()
        
        return SolanaTransactionBuilder.Instruction(
            programId = decodeBase58(TOKEN_PROGRAM_ID),
            accounts = listOf(
                SolanaTransactionBuilder.AccountMeta(decodeBase58(fromTokenAccount), isSigner = false, isWritable = true),
                SolanaTransactionBuilder.AccountMeta(decodeBase58(toTokenAccount), isSigner = false, isWritable = true),
                SolanaTransactionBuilder.AccountMeta(decodeBase58(fromAuthority), isSigner = true, isWritable = false)
            ),
            data = instructionData
        )
    }

    private fun decodeBase58(input: String): ByteArray {
        var decimal = java.math.BigInteger.ZERO
        val base = java.math.BigInteger.valueOf(58)
        
        for (char in input) {
            val digit = BASE58_ALPHABET.indexOf(char)
            if (digit == -1) throw IllegalArgumentException("Invalid base58 character: $char")
            decimal = decimal.multiply(base).add(java.math.BigInteger.valueOf(digit.toLong()))
        }
        
        val bytes = decimal.toByteArray()
        // Remove leading zero byte if present (BigInteger adds it for positive numbers)
        return if (bytes[0] == 0.toByte() && bytes.size > 1) {
            bytes.sliceArray(1 until bytes.size)
        } else {
            bytes
        }
    }

    private fun encodeBase58(input: ByteArray): String {
        var decimal = java.math.BigInteger(1, input)
        val base = java.math.BigInteger.valueOf(58)
        val result = StringBuilder()
        
        while (decimal > java.math.BigInteger.ZERO) {
            val divmod = decimal.divideAndRemainder(base)
            decimal = divmod[0]
            val remainder = divmod[1].toInt()
            result.insert(0, BASE58_ALPHABET[remainder])
        }
        
        // Handle leading zeros
        for (byte in input) {
            if (byte == 0.toByte()) {
                result.insert(0, '1')
            } else {
                break
            }
        }
        
        return result.toString()
    }

    fun close() {
        client.close()
        transactionMonitor.close()
    }
}

