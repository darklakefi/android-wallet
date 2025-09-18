package fi.darklake.wallet.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import fi.darklake.wallet.crypto.SolanaWallet
import fi.darklake.wallet.seedvault.SeedVaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bitcoinj.core.Base58

/**
 * Storage provider that uses Solana Mobile's Seed Vault for secure key management.
 * This provider stores only a reference to the authorized seed in Seed Vault,
 * not the actual private keys which remain in secure hardware.
 */
class SeedVaultStorageProvider(private val context: Context) : WalletStorageProvider {

    companion object {
        private const val TAG = "SeedVaultStorageProvider"
        private const val PREFS_NAME = "seed_vault_wallet_prefs"
        private const val KEY_AUTH_TOKEN = "seed_vault_auth_token"
        private const val KEY_PUBLIC_KEY = "seed_vault_public_key"
        private const val KEY_WALLET_TYPE = "wallet_type"
        private const val WALLET_TYPE_SEED_VAULT = "seed_vault"
    }

    private val seedVaultManager = SeedVaultManager(context)
    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create encrypted preferences, falling back to regular", e)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    override val providerName: String = "Seed Vault"

    override val isAvailable: Boolean
        get() = runCatching {
            // Check if we have stored Seed Vault auth
            sharedPreferences.getString(KEY_WALLET_TYPE, null) == WALLET_TYPE_SEED_VAULT
        }.getOrDefault(false)

    override suspend fun storeWallet(wallet: SolanaWallet): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // For Seed Vault, we don't store the actual wallet
            // This method is here for interface compatibility
            // The actual auth token should be stored via storeSeedVaultAuth
            Log.d(TAG, "Seed Vault wallet storage not needed - keys remain in secure hardware")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store wallet reference", e)
            Result.failure(StorageError.StorageFailed("Failed to store Seed Vault reference: ${e.message}"))
        }
    }

    /**
     * Store the Seed Vault authorization details
     * This should be called after successful Seed Vault authorization
     */
    suspend fun storeSeedVaultAuth(authToken: Long, publicKey: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sharedPreferences.edit().apply {
                putLong(KEY_AUTH_TOKEN, authToken)
                putString(KEY_PUBLIC_KEY, Base64.encodeToString(publicKey, Base64.NO_WRAP))
                putString(KEY_WALLET_TYPE, WALLET_TYPE_SEED_VAULT)
                apply()
            }
            Log.d(TAG, "Stored Seed Vault authorization")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store Seed Vault auth", e)
            Result.failure(StorageError.StorageFailed("Failed to store Seed Vault auth: ${e.message}"))
        }
    }

    override suspend fun getWallet(): Result<SolanaWallet?> = withContext(Dispatchers.IO) {
        try {
            val walletType = sharedPreferences.getString(KEY_WALLET_TYPE, null)
            if (walletType != WALLET_TYPE_SEED_VAULT) {
                return@withContext Result.success(null)
            }

            val authToken = sharedPreferences.getLong(KEY_AUTH_TOKEN, -1L)
            val publicKeyStr = sharedPreferences.getString(KEY_PUBLIC_KEY, null)

            if (authToken == -1L || publicKeyStr == null) {
                Log.d(TAG, "No Seed Vault wallet found")
                return@withContext Result.success(null)
            }

            val publicKey = Base64.decode(publicKeyStr, Base64.NO_WRAP)

            // Create a SolanaWallet with special marker for Seed Vault
            // We use empty private key and mnemonic as they're not accessible
            val wallet = SolanaWallet(
                publicKey = Base58.encode(publicKey),
                privateKey = ByteArray(0), // Empty as private key stays in Seed Vault
                mnemonic = listOf("SEED_VAULT") // Special marker
            )

            Log.d(TAG, "Retrieved Seed Vault wallet")
            Result.success(wallet)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve wallet", e)
            Result.failure(StorageError.RetrievalFailed("Failed to retrieve Seed Vault wallet: ${e.message}"))
        }
    }

    override suspend fun deleteWallet(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sharedPreferences.edit().clear().apply()
            Log.d(TAG, "Deleted Seed Vault wallet reference")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete wallet reference", e)
            Result.failure(StorageError.DeletionFailed("Failed to delete Seed Vault reference: ${e.message}"))
        }
    }

    override suspend fun hasWallet(): Boolean = withContext(Dispatchers.IO) {
        val walletType = sharedPreferences.getString(KEY_WALLET_TYPE, null)
        walletType == WALLET_TYPE_SEED_VAULT && sharedPreferences.contains(KEY_AUTH_TOKEN)
    }

    /**
     * Check if Seed Vault is actually available on this device
     */
    suspend fun checkAvailability(): Boolean = withContext(Dispatchers.IO) {
        seedVaultManager.isSeedVaultAvailable()
    }

    /**
     * Get the stored auth token for Seed Vault signing operations
     */
    fun getAuthToken(): Long {
        return sharedPreferences.getLong(KEY_AUTH_TOKEN, -1L)
    }

    /**
     * Check if the current wallet is a Seed Vault wallet
     */
    fun isSeedVaultWallet(): Boolean {
        return sharedPreferences.getString(KEY_WALLET_TYPE, null) == WALLET_TYPE_SEED_VAULT
    }
}