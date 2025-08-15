package fi.darklake.wallet.data.solana

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Helper class for building Solana transactions
 */
object SolanaTransactionBuilder {
    
    data class Instruction(
        val programId: ByteArray,
        val accounts: List<AccountMeta>,
        val data: ByteArray
    )
    
    data class AccountMeta(
        val pubkey: ByteArray,
        val isSigner: Boolean,
        val isWritable: Boolean
    )
    
    data class Message(
        val header: MessageHeader,
        val accountKeys: List<ByteArray>,
        val recentBlockhash: ByteArray,
        val instructions: List<CompiledInstruction>
    )
    
    data class MessageHeader(
        val numRequiredSignatures: Byte,
        val numReadonlySignedAccounts: Byte,
        val numReadonlyUnsignedAccounts: Byte
    )
    
    data class CompiledInstruction(
        val programIdIndex: Byte,
        val accounts: ByteArray,
        val data: ByteArray
    )
    
    /**
     * Compiles instructions into a transaction message
     */
    fun compileMessage(
        instructions: List<Instruction>,
        payer: ByteArray,
        recentBlockhash: ByteArray
    ): Message {
        // Collect all account keys
        val accountKeys = mutableListOf<ByteArray>()
        val accountKeyMap = mutableMapOf<String, Int>()
        
        // Add payer as first account (fee payer must be first)
        accountKeys.add(payer)
        accountKeyMap[payer.toBase58()] = 0
        
        // Collect unique accounts from instructions
        for (instruction in instructions) {
            for (account in instruction.accounts) {
                val key = account.pubkey.toBase58()
                if (!accountKeyMap.containsKey(key)) {
                    accountKeyMap[key] = accountKeys.size
                    accountKeys.add(account.pubkey)
                }
            }
            
            // Add program ID
            val programKey = instruction.programId.toBase58()
            if (!accountKeyMap.containsKey(programKey)) {
                accountKeyMap[programKey] = accountKeys.size
                accountKeys.add(instruction.programId)
            }
        }
        
        // Calculate header
        var numRequiredSignatures: Byte = 1 // Payer always signs
        var numReadonlySignedAccounts: Byte = 0
        var numReadonlyUnsignedAccounts: Byte = 0
        
        // Compile instructions
        val compiledInstructions = instructions.map { instruction ->
            val programIdIndex = accountKeyMap[instruction.programId.toBase58()]!!.toByte()
            
            val accounts = ByteArray(instruction.accounts.size)
            instruction.accounts.forEachIndexed { index, account ->
                accounts[index] = accountKeyMap[account.pubkey.toBase58()]!!.toByte()
                
                // Update header counts
                if (account.isSigner && index > 0) {
                    numRequiredSignatures++
                }
            }
            
            CompiledInstruction(
                programIdIndex = programIdIndex,
                accounts = accounts,
                data = instruction.data
            )
        }
        
        val header = MessageHeader(
            numRequiredSignatures = numRequiredSignatures,
            numReadonlySignedAccounts = numReadonlySignedAccounts,
            numReadonlyUnsignedAccounts = numReadonlyUnsignedAccounts
        )
        
        return Message(
            header = header,
            accountKeys = accountKeys,
            recentBlockhash = recentBlockhash,
            instructions = compiledInstructions
        )
    }
    
    /**
     * Serializes a message to bytes
     */
    fun serializeMessage(message: Message): ByteArray {
        val buffer = ByteBuffer.allocate(1024 * 10) // 10KB should be enough
        
        // Header
        buffer.put(message.header.numRequiredSignatures)
        buffer.put(message.header.numReadonlySignedAccounts)
        buffer.put(message.header.numReadonlyUnsignedAccounts)
        
        // Account keys array
        buffer.putCompactArray(message.accountKeys.size)
        message.accountKeys.forEach { buffer.put(it) }
        
        // Recent blockhash
        buffer.put(message.recentBlockhash)
        
        // Instructions array
        buffer.putCompactArray(message.instructions.size)
        message.instructions.forEach { instruction ->
            buffer.put(instruction.programIdIndex)
            buffer.putCompactArray(instruction.accounts.size)
            buffer.put(instruction.accounts)
            buffer.putCompactArray(instruction.data.size)
            buffer.put(instruction.data)
        }
        
        return buffer.array().sliceArray(0 until buffer.position())
    }
    
    /**
     * Creates a signed transaction
     */
    fun createSignedTransaction(
        message: ByteArray,
        signatures: List<ByteArray>
    ): ByteArray {
        val buffer = ByteBuffer.allocate(1 + signatures.size * 64 + message.size)
        
        // Number of signatures
        buffer.putCompactArray(signatures.size)
        
        // Signatures
        signatures.forEach { buffer.put(it) }
        
        // Message
        buffer.put(message)
        
        return buffer.array().sliceArray(0 until buffer.position())
    }
    
    /**
     * Extension function to write compact array length
     */
    private fun ByteBuffer.putCompactArray(length: Int) {
        when {
            length < 0x80 -> put(length.toByte())
            length < 0x4000 -> {
                put((0x80 or (length and 0x7F)).toByte())
                put((length shr 7).toByte())
            }
            else -> {
                put((0x80 or (length and 0x7F)).toByte())
                put((0x80 or ((length shr 7) and 0x7F)).toByte())
                put((length shr 14).toByte())
            }
        }
    }
    
    /**
     * Extension function to convert ByteArray to Base58
     */
    private fun ByteArray.toBase58(): String {
        val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        var decimal = java.math.BigInteger(1, this)
        val base = java.math.BigInteger.valueOf(58)
        val result = StringBuilder()
        
        while (decimal > java.math.BigInteger.ZERO) {
            val divmod = decimal.divideAndRemainder(base)
            decimal = divmod[0]
            val remainder = divmod[1].toInt()
            result.insert(0, alphabet[remainder])
        }
        
        // Handle leading zeros
        for (byte in this) {
            if (byte == 0.toByte()) {
                result.insert(0, '1')
            } else {
                break
            }
        }
        
        return result.toString()
    }
}