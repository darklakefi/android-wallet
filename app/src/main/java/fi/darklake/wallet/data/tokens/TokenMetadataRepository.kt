package fi.darklake.wallet.data.tokens

import android.util.Log
import fi.darklake.wallet.data.model.NetworkSettings
import fi.darklake.wallet.data.swap.grpc.DexGatewayClient
import fi.darklake.wallet.grpc.TokenMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for managing token metadata with caching
 */
class TokenMetadataRepository(
    private val networkSettings: NetworkSettings
) {
    companion object {
        private const val TAG = "TokenMetadataRepository"
        private const val CACHE_EXPIRY_MS = 1000L * 60 * 60 * 24 // 24 hours
        
        // Common token addresses on Solana mainnet
        private val COMMON_TOKEN_ADDRESSES = mapOf(
            "SOL" to "So11111111111111111111111111111111111111112",
            "USDC" to "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            "USDT" to "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB",
            "BONK" to "DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB263"
        )
    }
    
    private val dexGatewayClient = DexGatewayClient(networkSettings)
    private val cache = ConcurrentHashMap<String, CachedTokenMetadata>()
    private val cacheMutex = Mutex()
    
    data class CachedTokenMetadata(
        val metadata: TokenMetadata,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = 
            System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS
    }
    
    /**
     * Get token metadata by address with caching
     */
    suspend fun getTokenMetadata(tokenAddress: String): TokenMetadata? = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            cache[tokenAddress]?.let { cached ->
                if (!cached.isExpired()) {
                    Log.d(TAG, "Returning cached metadata for $tokenAddress")
                    return@withContext cached.metadata
                }
            }
            
            // Fetch from server
            Log.d(TAG, "Fetching metadata from server for $tokenAddress")
            val response = dexGatewayClient.getTokenMetadata(tokenAddress = tokenAddress)
            
            response.tokenMetadata?.let { metadata ->
                // Update cache
                cacheMutex.withLock {
                    cache[tokenAddress] = CachedTokenMetadata(metadata)
                }
                Log.d(TAG, "Cached metadata for ${metadata.symbol} (${tokenAddress})")
                metadata
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch token metadata for $tokenAddress", e)
            // Return cached data even if expired on error
            cache[tokenAddress]?.metadata
        }
    }
    
    /**
     * Get token metadata by symbol with caching
     */
    suspend fun getTokenMetadataBySymbol(symbol: String): TokenMetadata? = withContext(Dispatchers.IO) {
        try {
            // Try to get by known address first
            COMMON_TOKEN_ADDRESSES[symbol]?.let { address ->
                return@withContext getTokenMetadata(address)
            }
            
            // Check if we have it cached by symbol
            cache.values.find { it.metadata.symbol == symbol && !it.isExpired() }?.let {
                Log.d(TAG, "Returning cached metadata for symbol $symbol")
                return@withContext it.metadata
            }
            
            // Fetch from server
            Log.d(TAG, "Fetching metadata from server for symbol $symbol")
            val response = dexGatewayClient.getTokenMetadata(tokenSymbol = symbol)
            
            response.tokenMetadata?.let { metadata ->
                // Update cache using address as key
                if (metadata.address.isNotEmpty()) {
                    cacheMutex.withLock {
                        cache[metadata.address] = CachedTokenMetadata(metadata)
                    }
                    Log.d(TAG, "Cached metadata for ${metadata.symbol}")
                }
                metadata
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch token metadata for symbol $symbol", e)
            // Return cached data even if expired on error
            cache.values.find { it.metadata.symbol == symbol }?.metadata
        }
    }
    
    /**
     * Get metadata for multiple tokens at once
     */
    suspend fun getTokenMetadataList(
        addresses: List<String>? = null,
        symbols: List<String>? = null
    ): List<TokenMetadata> = withContext(Dispatchers.IO) {
        try {
            val result = mutableListOf<TokenMetadata>()
            
            // Check cache first
            val uncachedAddresses = mutableListOf<String>()
            val uncachedSymbols = mutableListOf<String>()
            
            addresses?.forEach { address ->
                cache[address]?.let { cached ->
                    if (!cached.isExpired()) {
                        result.add(cached.metadata)
                    } else {
                        uncachedAddresses.add(address)
                    }
                } ?: uncachedAddresses.add(address)
            }
            
            symbols?.forEach { symbol ->
                val cached = cache.values.find { 
                    it.metadata.symbol == symbol && !it.isExpired() 
                }
                if (cached != null) {
                    result.add(cached.metadata)
                } else {
                    uncachedSymbols.add(symbol)
                }
            }
            
            // Fetch uncached tokens
            if (uncachedAddresses.isNotEmpty() || uncachedSymbols.isNotEmpty()) {
                Log.d(TAG, "Fetching ${uncachedAddresses.size} addresses and ${uncachedSymbols.size} symbols from server")
                
                val response = when {
                    uncachedAddresses.isNotEmpty() -> 
                        dexGatewayClient.getTokenMetadataList(addressesList = uncachedAddresses)
                    uncachedSymbols.isNotEmpty() -> 
                        dexGatewayClient.getTokenMetadataList(symbolsList = uncachedSymbols)
                    else -> null
                }
                
                response?.tokensList?.forEach { metadata ->
                    if (metadata.address.isNotEmpty()) {
                        cacheMutex.withLock {
                            cache[metadata.address] = CachedTokenMetadata(metadata)
                        }
                        result.add(metadata)
                    }
                }
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch token metadata list", e)
            // Return cached data on error
            val result = mutableListOf<TokenMetadata>()
            addresses?.forEach { address ->
                cache[address]?.metadata?.let { result.add(it) }
            }
            symbols?.forEach { symbol ->
                cache.values.find { it.metadata.symbol == symbol }?.metadata?.let { 
                    result.add(it) 
                }
            }
            result
        }
    }
    
    /**
     * Prefetch common tokens metadata
     */
    suspend fun prefetchCommonTokens() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Prefetching common token metadata")
            val addresses = COMMON_TOKEN_ADDRESSES.values.toList()
            getTokenMetadataList(addresses = addresses)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prefetch common tokens", e)
        }
    }
    
    /**
     * Clear expired cache entries
     */
    fun clearExpiredCache() {
        cache.entries.removeIf { it.value.isExpired() }
        Log.d(TAG, "Cleared expired cache entries, ${cache.size} entries remaining")
    }
    
    /**
     * Clear all cache
     */
    fun clearCache() {
        cache.clear()
        Log.d(TAG, "Cleared all cache")
    }
    
    /**
     * Get cached token metadata without fetching
     */
    fun getCachedTokenMetadata(tokenAddress: String): TokenMetadata? {
        return cache[tokenAddress]?.takeIf { !it.isExpired() }?.metadata
    }
    
    /**
     * Check if token metadata is cached
     */
    fun isTokenCached(tokenAddress: String): Boolean {
        return cache[tokenAddress]?.let { !it.isExpired() } ?: false
    }
}