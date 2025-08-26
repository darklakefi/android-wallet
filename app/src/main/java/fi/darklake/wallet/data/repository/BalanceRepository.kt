package fi.darklake.wallet.data.repository

import android.content.Context
import android.content.SharedPreferences
import fi.darklake.wallet.data.api.SolanaApiService
import fi.darklake.wallet.data.model.TokenInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Repository for managing wallet balances and token information
 * Provides centralized caching to avoid redundant API calls
 * Includes persistent storage for instant loading on app startup
 */
class BalanceRepository(
    private val solanaApiService: SolanaApiService,
    private val context: Context? = null
) {
    private val sharedPrefs: SharedPreferences? = context?.getSharedPreferences(
        "balance_cache",
        Context.MODE_PRIVATE
    )
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val _solBalance = MutableStateFlow(0.0)
    val solBalance: StateFlow<Double> = _solBalance.asStateFlow()
    
    private val _tokens = MutableStateFlow<List<TokenInfo>>(emptyList())
    val tokens: StateFlow<List<TokenInfo>> = _tokens.asStateFlow()
    
    private val mutex = Mutex()
    private var lastFetchTime = 0L
    private val CACHE_DURATION = 30_000L // 30 seconds cache
    
    companion object {
        private const val KEY_SOL_BALANCE = "sol_balance"
        private const val KEY_TOKENS = "tokens"
        private const val KEY_LAST_FETCH = "last_fetch"
        private const val KEY_LAST_PUBLIC_KEY = "last_public_key"
    }
    
    init {
        // Load cached data on initialization
        loadCachedData()
    }
    
    /**
     * Load cached data from SharedPreferences
     */
    private fun loadCachedData() {
        sharedPrefs?.let { prefs ->
            try {
                // Load SOL balance
                _solBalance.value = prefs.getString(KEY_SOL_BALANCE, null)?.toDoubleOrNull() ?: 0.0
                
                // Load tokens
                val tokensJson = prefs.getString(KEY_TOKENS, null)
                if (!tokensJson.isNullOrEmpty()) {
                    try {
                        _tokens.value = json.decodeFromString<List<TokenInfo>>(tokensJson)
                    } catch (e: Exception) {
                        // If deserialization fails, start with empty list
                        _tokens.value = emptyList()
                    }
                }
                
                // Load last fetch time
                lastFetchTime = prefs.getLong(KEY_LAST_FETCH, 0L)
                
            } catch (e: Exception) {
                // If loading fails, just start fresh
                println("Failed to load cached balance data: ${e.message}")
            }
        }
    }
    
    /**
     * Save current data to SharedPreferences
     */
    private fun saveCachedData(publicKey: String) {
        sharedPrefs?.edit()?.apply {
            putString(KEY_SOL_BALANCE, _solBalance.value.toString())
            putString(KEY_TOKENS, try {
                json.encodeToString(_tokens.value)
            } catch (e: Exception) {
                "[]"
            })
            putLong(KEY_LAST_FETCH, lastFetchTime)
            putString(KEY_LAST_PUBLIC_KEY, publicKey)
            apply()
        }
    }
    
    /**
     * Check if cached data is for the same wallet
     */
    private fun isCacheValidForWallet(publicKey: String): Boolean {
        val lastPublicKey = sharedPrefs?.getString(KEY_LAST_PUBLIC_KEY, null)
        return lastPublicKey == publicKey
    }
    
    /**
     * Fetch SOL balance for a wallet
     */
    suspend fun fetchSolBalance(publicKey: String): Result<Double> {
        return mutex.withLock {
            // If cached data is not for this wallet, clear it
            if (!isCacheValidForWallet(publicKey)) {
                clearCache()
            }
            
            try {
                val result = solanaApiService.getBalance(publicKey)
                if (result.isSuccess) {
                    val balance = result.getOrNull() ?: 0.0
                    _solBalance.value = balance
                    saveCachedData(publicKey) // Save to persistent storage
                    Result.success(balance)
                } else {
                    result
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Fetch token accounts for a wallet
     */
    suspend fun fetchTokens(publicKey: String, forceRefresh: Boolean = false): Result<List<TokenInfo>> {
        return mutex.withLock {
            // If cached data is not for this wallet, clear it
            if (!isCacheValidForWallet(publicKey)) {
                clearCache()
            }
            
            val currentTime = System.currentTimeMillis()
            
            // Return cached data if still valid and not forcing refresh
            if (!forceRefresh && 
                _tokens.value.isNotEmpty() && 
                (currentTime - lastFetchTime) < CACHE_DURATION) {
                return Result.success(_tokens.value)
            }
            
            try {
                val result = solanaApiService.getTokenAccounts(publicKey)
                if (result.isSuccess) {
                    val tokens = result.getOrNull() ?: emptyList()
                    _tokens.value = tokens
                    lastFetchTime = currentTime
                    saveCachedData(publicKey) // Save to persistent storage
                    Result.success(tokens)
                } else {
                    result
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get a specific token by mint address
     */
    fun getTokenByMint(mint: String): TokenInfo? {
        return _tokens.value.find { it.balance.mint == mint }
    }
    
    /**
     * Clear all cached data
     */
    fun clearCache() {
        _solBalance.value = 0.0
        _tokens.value = emptyList()
        lastFetchTime = 0L
    }
}