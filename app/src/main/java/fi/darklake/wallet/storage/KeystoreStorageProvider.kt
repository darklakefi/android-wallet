package fi.darklake.wallet.storage

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import fi.darklake.wallet.crypto.SolanaWallet
import fi.darklake.wallet.crypto.SolanaWalletManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore

class KeystoreStorageProvider(
    private val context: Context,
    private val useStrongBox: Boolean = false
) : WalletStorageProvider {
    
    companion object {
        private const val PREFS_FILE_NAME = "darklake_secure_prefs"
        private const val KEY_MNEMONIC = "wallet_mnemonic"
        private const val KEY_PUBLIC_KEY = "wallet_public_key"
        private const val MASTER_KEY_ALIAS = "darklake_master_key"
    }
    
    override val providerName: String = if (useStrongBox) "StrongBox Keystore" else "Android Keystore"
    
    override val isAvailable: Boolean
        get() = if (useStrongBox) {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && hasStrongBoxSupport()
        } else {
            true // Regular Keystore is always available
        }
    
    private fun hasStrongBoxSupport(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.hasSystemFeature("android.hardware.strongbox_keystore")
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getMasterKey(): MasterKey {
        val spec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setKeySize(256)
            
            if (useStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setIsStrongBoxBacked(true)
            }
        }.build()
        
        return MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyGenParameterSpec(spec)
            .build()
    }
    
    private fun getEncryptedPrefs() = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        getMasterKey(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    override suspend fun storeWallet(wallet: SolanaWallet): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isAvailable) {
                return@withContext Result.failure(
                    StorageError.NotAvailable("$providerName is not available")
                )
            }
            
            val prefs = getEncryptedPrefs()
            val mnemonicJson = Json.encodeToString(wallet.mnemonic)
            
            prefs.edit().apply {
                putString(KEY_MNEMONIC, mnemonicJson)
                putString(KEY_PUBLIC_KEY, wallet.publicKey)
                apply()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(StorageError.StorageFailed("Failed to store wallet: ${e.message}"))
        }
    }
    
    override suspend fun getWallet(): Result<SolanaWallet?> = withContext(Dispatchers.IO) {
        try {
            if (!isAvailable) {
                return@withContext Result.failure(
                    StorageError.NotAvailable("$providerName is not available")
                )
            }
            
            val prefs = getEncryptedPrefs()
            val mnemonicJson = prefs.getString(KEY_MNEMONIC, null)
            
            if (mnemonicJson != null) {
                val mnemonic = Json.decodeFromString<List<String>>(mnemonicJson)
                val wallet = SolanaWalletManager.createWalletFromMnemonic(mnemonic)
                Result.success(wallet)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(StorageError.RetrievalFailed("Failed to retrieve wallet: ${e.message}"))
        }
    }
    
    override suspend fun deleteWallet(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isAvailable) {
                return@withContext Result.failure(
                    StorageError.NotAvailable("$providerName is not available")
                )
            }
            
            val prefs = getEncryptedPrefs()
            prefs.edit().clear().apply()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(StorageError.DeletionFailed("Failed to delete wallet: ${e.message}"))
        }
    }
    
    override suspend fun hasWallet(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isAvailable) return@withContext false
            
            val prefs = getEncryptedPrefs()
            prefs.contains(KEY_MNEMONIC)
        } catch (e: Exception) {
            false
        }
    }
}