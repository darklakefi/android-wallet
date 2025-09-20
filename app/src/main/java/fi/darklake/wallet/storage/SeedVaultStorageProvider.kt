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
        private const val KEY_DERIVATION_PATH = "seed_vault_derivation_path"
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
            // Only SeedVaultWallet can be stored with this provider
            if (wallet !is fi.darklake.wallet.crypto.SeedVaultWallet) {
                return@withContext Result.failure(StorageError.StorageFailed("SeedVaultStorageProvider can only store SeedVaultWallet"))
            }

            // For Seed Vault, we don't store the actual wallet keys
            // The auth token should be stored via storeSeedVaultAuth
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
    suspend fun storeSeedVaultAuth(authToken: Long, publicKey: ByteArray, derivationPath: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // If no derivation path provided, try to query it from Seed Vault
            val actualDerivationPath = if (derivationPath != null) {
                derivationPath
            } else {
                val accountInfo = seedVaultManager.getAccountInfoForAuthToken(authToken)
                accountInfo?.derivationPath ?: run {
                    Log.w(TAG, "Could not get derivation path from Seed Vault, using default")
                    "bip32:/m/44'/501'/0'"  // Default Solana path
                }
            }

            sharedPreferences.edit().apply {
                putLong(KEY_AUTH_TOKEN, authToken)
                putString(KEY_PUBLIC_KEY, Base64.encodeToString(publicKey, Base64.NO_WRAP))
                putString(KEY_DERIVATION_PATH, actualDerivationPath)
                putString(KEY_WALLET_TYPE, WALLET_TYPE_SEED_VAULT)
                apply()
            }
            Log.d(TAG, "Stored Seed Vault authorization with derivation path: $actualDerivationPath")
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
            val derivationPath = sharedPreferences.getString(KEY_DERIVATION_PATH, null)

            if (authToken == -1L || publicKeyStr == null) {
                Log.d(TAG, "No Seed Vault wallet found")
                return@withContext Result.success(null)
            }

            val publicKey = Base64.decode(publicKeyStr, Base64.NO_WRAP)
            Log.d(TAG, "Decoded public key: ${publicKey.size} bytes")

            val publicKeyBase58 = Base58.encode(publicKey)
            Log.d(TAG, "Base58 encoded public key: $publicKeyBase58")

            // Use stored derivation path or default
            val actualDerivationPath = derivationPath ?: "bip32:/m/44'/501'/0'"
            Log.d(TAG, "Using derivation path: $actualDerivationPath")

            // Create a SeedVaultWallet implementation with derivation path
            val wallet = fi.darklake.wallet.crypto.SeedVaultWallet(
                publicKey = publicKeyBase58,
                authToken = authToken,
                derivationPath = actualDerivationPath,
                context = context
            )

            Log.d(TAG, "Retrieved Seed Vault wallet with address: $publicKeyBase58 and path: $actualDerivationPath")
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
}