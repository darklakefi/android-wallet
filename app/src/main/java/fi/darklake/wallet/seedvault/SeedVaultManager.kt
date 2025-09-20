package fi.darklake.wallet.seedvault

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
// Seed Vault SDK imports will be added when available
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Account information retrieved from Seed Vault
 */
data class AccountInfo(
    val publicKey: ByteArray,
    val derivationPath: String  // e.g., "bip32:/m/44'/501'/0'"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AccountInfo

        if (!publicKey.contentEquals(other.publicKey)) return false
        if (derivationPath != other.derivationPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + derivationPath.hashCode()
        return result
    }
}

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
        private val ACCOUNTS_URI = Uri.parse("content://$SEED_VAULT_AUTHORITY/accounts")

        // Intent actions for Seed Vault
        const val ACTION_AUTHORIZE_SEED_ACCESS = "com.solanamobile.seedvault.wallet.v1.ACTION_AUTHORIZE_SEED_ACCESS"
        const val ACTION_SIGN_TRANSACTION = "com.solanamobile.seedvault.wallet.v1.ACTION_SIGN_TRANSACTION"
        const val ACTION_CREATE_SEED = "com.solanamobile.seedvault.wallet.v1.ACTION_CREATE_SEED"
        const val ACTION_IMPORT_SEED = "com.solanamobile.seedvault.wallet.v1.ACTION_IMPORT_SEED"

        // Result extras (must match WalletContractV1 constants)
        const val EXTRA_SEED_AUTH_TOKEN = "AuthToken"  // WalletContractV1.EXTRA_AUTH_TOKEN
        const val EXTRA_SIGNED_TRANSACTION = "SignedPayloads"  // For signed transaction result
        const val EXTRA_SEED_NAME = "seed_name"
        const val EXTRA_DERIVATION_PATH = "derivation_path"
        const val EXTRA_TRANSACTION = "transaction"
        const val EXTRA_PURPOSE = "Purpose"  // Must match WalletContractV1.EXTRA_PURPOSE

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

                            Log.d(TAG, "Seed columns: $it.columnNames")
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
                null,  // Get all columns to see what's available
                null,
                null,
                null
            )

            cursor?.use {
                // Log column names for debugging
                Log.d(TAG, "Authorized seeds columns: ${it.columnNames.joinToString()}")

                val authTokenIndex = it.getColumnIndex("_id")  // AUTHORIZED_SEEDS_AUTH_TOKEN
                val nameIndex = it.getColumnIndex("AuthorizedSeeds_SeedName")  // AUTHORIZED_SEEDS_SEED_NAME

                while (it.moveToNext()) {
                    if (authTokenIndex >= 0) {
                        val authToken = it.getLong(authTokenIndex)
                        val name = if (nameIndex >= 0) it.getString(nameIndex) ?: "Seed" else "Seed"

                        // For now, we'll get the public key from accounts later
                        // Just create the seed with empty public key
                        seeds.add(
                            Seed(
                                authToken = authToken,
                                name = name,
                                publicKey = ByteArray(0)  // Will need to query accounts table for this
                            )
                        )
                        Log.d(TAG, "Found authorized seed: token=$authToken, name=$name")
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
     * Get the public key for an authorized seed
     * Queries the accounts table to get the default account's public key
     */
    suspend fun getPublicKeyForAuthToken(authToken: Long): ByteArray? = withContext(Dispatchers.IO) {
        val accountInfo = getAccountInfoForAuthToken(authToken)
        return@withContext accountInfo?.publicKey
    }

    /**
     * Get the account information for an authorized seed
     * Queries the accounts table to get the default account's public key and derivation path
     */
    suspend fun getAccountInfoForAuthToken(authToken: Long): AccountInfo? = withContext(Dispatchers.IO) {
        try {
            // The SDK expects AuthToken in the extras bundle
            val bundle = android.os.Bundle().apply {
                putLong("AuthToken", authToken)  // This is what WalletContentProvider looks for
            }

            val cursor = context.contentResolver.query(
                ACCOUNTS_URI,
                null,  // projection
                bundle,  // queryArgs
                null  // cancellationSignal
            )

            cursor?.use {
                Log.d(TAG, "Accounts columns: ${it.columnNames.joinToString()}")

                val publicKeyRawIndex = it.getColumnIndex("Accounts_PublicKeyRaw")
                val publicKeyEncodedIndex = it.getColumnIndex("Accounts_PublicKeyEncoded")
                val derivationPathIndex = it.getColumnIndex("Accounts_Bip32DerivationPath")

                if (it.moveToFirst()) {
                    val publicKey = when {
                        publicKeyRawIndex >= 0 && !it.isNull(publicKeyRawIndex) -> {
                            it.getBlob(publicKeyRawIndex).also { key ->
                                Log.d(TAG, "Got raw public key for token $authToken: ${key.size} bytes")
                            }
                        }
                        publicKeyEncodedIndex >= 0 && !it.isNull(publicKeyEncodedIndex) -> {
                            // If we only have encoded, we'd need to decode it
                            val encoded = it.getString(publicKeyEncodedIndex)
                            Log.e(TAG, "Got encoded public key but can't decode Base58 yet: $encoded")
                            // FAIL - don't return empty data
                            null
                        }
                        else -> null
                    }

                    val derivationPath = if (derivationPathIndex >= 0 && !it.isNull(derivationPathIndex)) {
                        it.getString(derivationPathIndex).also { path ->
                            Log.d(TAG, "Got derivation path for token $authToken: $path")
                        }
                    } else {
                        Log.e(TAG, "No derivation path found for token $authToken")
                        null
                    }

                    if (publicKey != null && derivationPath != null) {
                        return@withContext AccountInfo(publicKey, derivationPath)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get account info for auth token $authToken: ${e.message}")
        }
        return@withContext null
    }

    /**
     * Create an intent to authorize an existing seed for a specific purpose
     * @param purpose The purpose for which to authorize a seed (e.g., PURPOSE_SIGN_SOLANA_TRANSACTION)
     * Note: Do NOT pass a seed name here - existing seeds already have names
     */
    fun createAuthorizeSeedIntent(purpose: Int): Intent {
        Log.d(TAG, "Creating authorize intent for existing seed with purpose: $purpose")
        return Intent(ACTION_AUTHORIZE_SEED_ACCESS).apply {
            setPackage("com.solanamobile.seedvaultimpl") // Explicitly target the simulator
            putExtra(EXTRA_PURPOSE, purpose)
            // Do NOT include EXTRA_SEED_NAME - that's only for creating new seeds
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