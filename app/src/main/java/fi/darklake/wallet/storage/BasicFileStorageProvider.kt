package fi.darklake.wallet.storage

import android.content.Context
import android.util.Log
import fi.darklake.wallet.crypto.SolanaWallet
import fi.darklake.wallet.crypto.SolanaWalletManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class BasicFileStorageProvider(
    private val context: Context
) : WalletStorageProvider {
    
    companion object {
        private const val TAG = "BasicFileStorageProvider"
        private const val FILE_NAME = "wallet_data.enc"
        private const val KEY_FILE_NAME = "wallet_key.enc"
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }
    
    override val providerName: String = "Basic File Encryption"
    
    override val isAvailable: Boolean = true // Always available as fallback
    
    private val walletFile: File by lazy {
        File(context.filesDir, FILE_NAME)
    }
    
    private val keyFile: File by lazy {
        File(context.filesDir, KEY_FILE_NAME)
    }
    
    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }
    
    private fun getOrCreateSecretKey(): SecretKey {
        return if (keyFile.exists()) {
            // Load existing key
            val keyBytes = keyFile.readBytes()
            SecretKeySpec(keyBytes, ALGORITHM)
        } else {
            // Generate new key and save it
            val key = generateSecretKey()
            keyFile.writeBytes(key.encoded)
            Log.d(TAG, "Generated and saved new encryption key")
            key
        }
    }
    
    private fun encryptData(data: String, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray())
        
        // Combine IV + encrypted data
        return iv + encryptedData
    }
    
    private fun decryptData(encryptedData: ByteArray, key: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        
        // Extract IV and encrypted data
        val iv = encryptedData.sliceArray(0 until GCM_IV_LENGTH)
        val encrypted = encryptedData.sliceArray(GCM_IV_LENGTH until encryptedData.size)
        
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        val decryptedBytes = cipher.doFinal(encrypted)
        return String(decryptedBytes)
    }
    
    override suspend fun storeWallet(wallet: SolanaWallet): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Storing wallet using basic file encryption")
            
            val key = getOrCreateSecretKey()
            val walletData = Json.encodeToString(wallet.mnemonic)
            
            val encryptedData = encryptData(walletData, key)
            walletFile.writeBytes(encryptedData)
            
            Log.d(TAG, "Wallet stored successfully in encrypted file")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store wallet in file", e)
            Result.failure(StorageError.StorageFailed("Failed to store wallet: ${e.message}"))
        }
    }
    
    override suspend fun getWallet(): Result<SolanaWallet?> = withContext(Dispatchers.IO) {
        try {
            if (!walletFile.exists()) {
                Log.d(TAG, "No wallet file found")
                return@withContext Result.success(null)
            }
            
            val key = getOrCreateSecretKey()
            val encryptedData = walletFile.readBytes()
            
            val decryptedData = decryptData(encryptedData, key)
            val mnemonic = Json.decodeFromString<List<String>>(decryptedData)
            
            val wallet = SolanaWalletManager.createWalletFromMnemonic(mnemonic)
            Log.d(TAG, "Wallet retrieved successfully from encrypted file")
            Result.success(wallet)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve wallet from file", e)
            Result.failure(StorageError.RetrievalFailed("Failed to retrieve wallet: ${e.message}"))
        }
    }
    
    override suspend fun deleteWallet(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var success = true
            
            if (walletFile.exists()) {
                success = walletFile.delete() && success
                Log.d(TAG, "Deleted wallet file: $success")
            }
            
            if (keyFile.exists()) {
                success = keyFile.delete() && success
                Log.d(TAG, "Deleted key file: $success")
            }
            
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(StorageError.DeletionFailed("Failed to delete wallet files"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete wallet files", e)
            Result.failure(StorageError.DeletionFailed("Failed to delete wallet: ${e.message}"))
        }
    }
    
    override suspend fun hasWallet(): Boolean = withContext(Dispatchers.IO) {
        try {
            walletFile.exists() && keyFile.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check wallet existence", e)
            false
        }
    }
}
