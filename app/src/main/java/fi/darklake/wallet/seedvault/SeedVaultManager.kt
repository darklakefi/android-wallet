package fi.darklake.wallet.seedvault

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
// Seed Vault SDK imports will be added when available
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager class for interacting with Solana Mobile's Seed Vault.
 * Provides methods to check availability, authorize seeds, and sign transactions.
 */
class SeedVaultManager(private val context: Context) {

    companion object {
        private const val TAG = "SeedVaultManager"

        // Seed Vault content provider URIs
        private const val SEED_VAULT_AUTHORITY = "com.solanamobile.seedvault.wallet.v1.walletprovider"
        private val UNAUTHORIZED_SEEDS_URI = Uri.parse("content://$SEED_VAULT_AUTHORITY/unauthorizedseeds")
        private val AUTHORIZED_SEEDS_URI = Uri.parse("content://$SEED_VAULT_AUTHORITY/authorizedseeds")
        private val SEEDS_URI = Uri.parse("content://$SEED_VAULT_AUTHORITY/seeds")

        // Intent actions for Seed Vault
        const val ACTION_AUTHORIZE_SEED_ACCESS = "com.solanamobile.seedvault.wallet.v1.ACTION_AUTHORIZE_SEED_ACCESS"
        const val ACTION_SIGN_TRANSACTION = "com.solanamobile.seedvault.wallet.v1.ACTION_SIGN_TRANSACTION"
        const val ACTION_CREATE_SEED = "com.solanamobile.seedvault.wallet.v1.ACTION_CREATE_SEED"
        const val ACTION_IMPORT_SEED = "com.solanamobile.seedvault.wallet.v1.ACTION_IMPORT_SEED"

        // Request codes for activities
        const val REQUEST_AUTHORIZE_SEED = 1001
        const val REQUEST_SIGN_TRANSACTION = 1002
        const val REQUEST_CREATE_SEED = 1003
        const val REQUEST_IMPORT_SEED = 1004

        // Result extras
        const val EXTRA_SEED_AUTH_TOKEN = "auth_token"
        const val EXTRA_SIGNED_TRANSACTION = "signed_transaction"
        const val EXTRA_SEED_NAME = "seed_name"
        const val EXTRA_DERIVATION_PATH = "derivation_path"
        const val EXTRA_TRANSACTION = "transaction"
        const val EXTRA_PURPOSE = "purpose"

        // Purpose constants (from WalletContractV1)
        const val PURPOSE_SIGN_SOLANA_TRANSACTION = 0
    }

    /**
     * Data class representing a Seed from the Seed Vault
     */
    data class Seed(
        val authToken: Long,
        val name: String,
        val publicKey: ByteArray,
        val purpose: Int = PURPOSE_SIGN_SOLANA_TRANSACTION // Store the purpose for unauthorized seeds
    )

    /**
     * Check if Seed Vault is available on this device
     */
    suspend fun isSeedVaultAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val cursor = context.contentResolver.query(
                UNAUTHORIZED_SEEDS_URI,
                null,
                null,
                null,
                null
            )
            cursor?.use {
                Log.d(TAG, "Seed Vault is available")
                return@withContext true
            }
            Log.d(TAG, "Seed Vault query returned null - not available")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check Seed Vault availability: ${e.message}")
            false
        }
    }

    /**
     * Get list of purposes for which unauthorized seeds are available
     */
    suspend fun getUnauthorizedSeeds(): List<Seed> = withContext(Dispatchers.IO) {
        val seeds = mutableListOf<Seed>()

        try {
            val cursor = context.contentResolver.query(
                UNAUTHORIZED_SEEDS_URI,
                null, // Query all columns to see what's available
                null,
                null,
                null
            )

            cursor?.use {
                // The _id column contains the purpose, and UnauthorizedSeeds_HasUnauthorizedSeeds indicates if seeds are available
                val idIndex = it.getColumnIndex("_id") // This is the PURPOSE, not auth token
                val hasUnauthorizedIndex = it.getColumnIndex("UnauthorizedSeeds_HasUnauthorizedSeeds")

                if (it.moveToFirst()) {
                    do {
                        if (hasUnauthorizedIndex >= 0 && it.getInt(hasUnauthorizedIndex) > 0) {
                            // There are unauthorized seeds for this purpose
                            val purpose = if (idIndex >= 0) it.getInt(idIndex) else PURPOSE_SIGN_SOLANA_TRANSACTION
                            Log.d(TAG, "Found unauthorized seeds for purpose: $purpose")

                            // Create a placeholder entry for this purpose
                            // The authToken is -1 to indicate this is not yet authorized
                            seeds.add(
                                Seed(
                                    authToken = -1L, // Placeholder - will get real token after authorization
                                    name = "Available Seed",
                                    publicKey = ByteArray(0), // Will be populated after authorization
                                    purpose = purpose // Store the purpose for later use
                                )
                            )
                        }
                    } while (it.moveToNext())
                }
            }

            Log.d(TAG, "Found ${seeds.size} unauthorized seed purposes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get unauthorized seeds: ${e.message}", e)
        }

        return@withContext seeds
    }

    /**
     * Check if there are unauthorized seeds available
     */
    suspend fun hasUnauthorizedSeeds(): Boolean = withContext(Dispatchers.IO) {
        try {
            val cursor = context.contentResolver.query(
                UNAUTHORIZED_SEEDS_URI,
                null,
                null,
                null,
                null
            )

            cursor?.use {
                val hasUnauthorizedIndex = it.getColumnIndex("UnauthorizedSeeds_HasUnauthorizedSeeds")
                if (hasUnauthorizedIndex >= 0 && it.moveToFirst()) {
                    val hasUnauthorized = it.getInt(hasUnauthorizedIndex) > 0
                    Log.d(TAG, "Has unauthorized seeds available: $hasUnauthorized")
                    return@withContext hasUnauthorized
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for unauthorized seeds: ${e.message}")
        }
        return@withContext false
    }

    /**
     * Get list of already authorized seeds
     */
    suspend fun getAuthorizedSeeds(): List<Seed> = withContext(Dispatchers.IO) {
        val seeds = mutableListOf<Seed>()

        try {
            val cursor = context.contentResolver.query(
                AUTHORIZED_SEEDS_URI,
                arrayOf("auth_token", "name", "public_key"),
                null,
                null,
                null
            )

            cursor?.use {
                val authTokenIndex = it.getColumnIndex("auth_token")
                val nameIndex = it.getColumnIndex("name")
                val publicKeyIndex = it.getColumnIndex("public_key")

                while (it.moveToNext()) {
                    if (authTokenIndex >= 0 && nameIndex >= 0 && publicKeyIndex >= 0) {
                        seeds.add(
                            Seed(
                                authToken = it.getLong(authTokenIndex),
                                name = it.getString(nameIndex),
                                publicKey = it.getBlob(publicKeyIndex)
                            )
                        )
                    }
                }
            }

            Log.d(TAG, "Found ${seeds.size} authorized seeds")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get authorized seeds: ${e.message}")
        }

        return@withContext seeds
    }

    /**
     * Create an intent to authorize a seed for a specific purpose
     * @param purpose The purpose for which to authorize a seed (e.g., PURPOSE_SIGN_SOLANA_TRANSACTION)
     * @param seedName Optional name for the seed
     */
    fun createAuthorizeSeedIntent(purpose: Int, seedName: String? = null): Intent {
        Log.d(TAG, "Creating authorize intent for purpose: $purpose")
        return Intent(ACTION_AUTHORIZE_SEED_ACCESS).apply {
            setPackage("com.solanamobile.seedvaultimpl") // Explicitly target the simulator
            // Always use EXTRA_PURPOSE for authorization
            putExtra(EXTRA_PURPOSE, purpose)
            seedName?.let { putExtra(EXTRA_SEED_NAME, it) }
        }
    }

    /**
     * Create an intent to sign a transaction
     * @param authToken The auth token of the authorized seed
     * @param transaction The transaction bytes to sign
     * @param derivationPath Optional derivation path for the signing key
     */
    fun createSignTransactionIntent(
        authToken: Long,
        transaction: ByteArray,
        derivationPath: String? = null
    ): Intent {
        return Intent(ACTION_SIGN_TRANSACTION).apply {
            putExtra(EXTRA_SEED_AUTH_TOKEN, authToken)
            putExtra(EXTRA_TRANSACTION, transaction)
            derivationPath?.let { putExtra(EXTRA_DERIVATION_PATH, it) }
        }
    }

    /**
     * Create an intent to create a new seed in Seed Vault
     * @param seedName Optional name for the new seed
     */
    fun createNewSeedIntent(seedName: String? = null): Intent {
        return Intent(ACTION_CREATE_SEED).apply {
            setPackage("com.solanamobile.seedvaultimpl") // Explicitly target the simulator
            putExtra(EXTRA_PURPOSE, PURPOSE_SIGN_SOLANA_TRANSACTION) // Default to Solana signing
            seedName?.let { putExtra(EXTRA_SEED_NAME, it) }
        }
    }

    /**
     * Create an intent to import a seed into Seed Vault
     * @param seedName Optional name for the imported seed
     */
    fun createImportSeedIntent(seedName: String? = null): Intent {
        return Intent(ACTION_IMPORT_SEED).apply {
            setPackage("com.solanamobile.seedvaultimpl") // Explicitly target the simulator
            putExtra(EXTRA_PURPOSE, PURPOSE_SIGN_SOLANA_TRANSACTION) // Default to Solana signing
            seedName?.let { putExtra(EXTRA_SEED_NAME, it) }
        }
    }

    /**
     * Process the result from a Seed Vault authorization
     * @param data The result intent data
     * @return The auth token if successful, null otherwise
     */
    fun processAuthorizationResult(data: Intent?): Long? {
        return data?.getLongExtra(EXTRA_SEED_AUTH_TOKEN, -1)?.let {
            if (it != -1L) {
                Log.d(TAG, "Seed authorized with token: $it")
                it
            } else {
                Log.e(TAG, "Authorization failed - no auth token in result")
                null
            }
        }
    }

    /**
     * Process the result from a Seed Vault transaction signing
     * @param data The result intent data
     * @return The signed transaction bytes if successful, null otherwise
     */
    fun processSignTransactionResult(data: Intent?): ByteArray? {
        return data?.getByteArrayExtra(EXTRA_SIGNED_TRANSACTION)?.also {
            Log.d(TAG, "Transaction signed successfully, size: ${it.size} bytes")
        } ?: run {
            Log.e(TAG, "Signing failed - no signed transaction in result")
            null
        }
    }

    /**
     * Store the authorized seed auth token for later use
     * This should be stored securely using EncryptedSharedPreferences
     */
    suspend fun storeAuthorizedSeed(authToken: Long, publicKey: ByteArray) {
        // This will be integrated with WalletStorageManager
        // For now, just log
        Log.d(TAG, "Storing authorized seed with token: $authToken")
    }
}