package fi.darklake.wallet.data.tokens

import android.content.Context
import fi.darklake.wallet.data.model.NetworkSettings
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.grpc.TokenMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Service for managing token metadata across the application
 * This is a singleton that should be initialized once and reused
 */
class TokenMetadataService private constructor(
    private val settingsManager: SettingsManager
) {
    companion object {
        @Volatile
        private var INSTANCE: TokenMetadataService? = null
        
        fun getInstance(context: Context): TokenMetadataService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenMetadataService(
                    SettingsManager(context)
                ).also { INSTANCE = it }
            }
        }
    }
    
    // Get network settings from SettingsManager (using current value from StateFlow)
    private val networkSettings = settingsManager.networkSettings.value
    private val repository = TokenMetadataRepository(networkSettings)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Observable state for UI components
    private val _tokenMetadataState = MutableStateFlow<Map<String, TokenMetadata>>(emptyMap())
    val tokenMetadataState: StateFlow<Map<String, TokenMetadata>> = _tokenMetadataState.asStateFlow()
    
    init {
        // Prefetch common tokens on initialization
        serviceScope.launch {
            repository.prefetchCommonTokens()
        }
        
        // Periodically clean expired cache
        serviceScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000L * 60 * 60) // Every hour
                repository.clearExpiredCache()
            }
        }
    }
    
    /**
     * Get token metadata with automatic caching
     */
    suspend fun getTokenMetadata(tokenAddress: String): TokenMetadata? {
        // Check if already in state
        _tokenMetadataState.value[tokenAddress]?.let { return it }
        
        // Fetch from repository
        return repository.getTokenMetadata(tokenAddress)?.also { metadata ->
            updateState(tokenAddress, metadata)
        }
    }
    
    /**
     * Get token metadata by symbol
     */
    suspend fun getTokenMetadataBySymbol(symbol: String): TokenMetadata? {
        // Check if already in state
        _tokenMetadataState.value.values.find { it.symbol == symbol }?.let { return it }
        
        // Fetch from repository
        return repository.getTokenMetadataBySymbol(symbol)?.also { metadata ->
            if (metadata.address.isNotEmpty()) {
                updateState(metadata.address, metadata)
            }
        }
    }
    
    /**
     * Get metadata for multiple tokens
     */
    suspend fun getTokenMetadataList(addresses: List<String>): List<TokenMetadata> {
        return repository.getTokenMetadataList(addresses = addresses).also { metadataList ->
            metadataList.forEach { metadata ->
                if (metadata.address.isNotEmpty()) {
                    updateState(metadata.address, metadata)
                }
            }
        }
    }
    
    /**
     * Get cached metadata without fetching
     */
    fun getCachedMetadata(tokenAddress: String): TokenMetadata? {
        return _tokenMetadataState.value[tokenAddress] 
            ?: repository.getCachedTokenMetadata(tokenAddress)
    }
    
    /**
     * Get logo URL for a token
     */
    fun getTokenLogoUrl(tokenAddress: String): String? {
        return getCachedMetadata(tokenAddress)?.logoUri?.takeIf { it.isNotEmpty() }
    }
    
    /**
     * Get token color based on symbol or address
     * Returns a suggested background color for the token icon
     */
    fun getTokenColor(tokenAddress: String): TokenColor {
        val metadata = getCachedMetadata(tokenAddress)
        return when (metadata?.symbol) {
            "SOL" -> TokenColor.SOL
            "USDC" -> TokenColor.USDC
            "USDT" -> TokenColor.USDT
            "BONK" -> TokenColor.BONK
            "PYTH" -> TokenColor.PYTH
            "JUP" -> TokenColor.JUP
            "RAY" -> TokenColor.RAY
            "ORCA" -> TokenColor.ORCA
            else -> TokenColor.DEFAULT
        }
    }
    
    /**
     * Refresh metadata for a specific token
     */
    fun refreshTokenMetadata(tokenAddress: String) {
        serviceScope.launch {
            repository.getTokenMetadata(tokenAddress)?.let { metadata ->
                updateState(tokenAddress, metadata)
            }
        }
    }
    
    /**
     * Clear all cached data
     */
    fun clearCache() {
        repository.clearCache()
        _tokenMetadataState.value = emptyMap()
    }
    
    private fun updateState(tokenAddress: String, metadata: TokenMetadata) {
        _tokenMetadataState.value = _tokenMetadataState.value + (tokenAddress to metadata)
    }
}

/**
 * Token color definitions
 */
enum class TokenColor(val hex: String) {
    SOL("#14F195"),
    USDC("#2775CA"),
    USDT("#26A17B"),
    BONK("#FF5722"),
    PYTH("#6528CC"),
    JUP("#3E3E3E"),
    RAY("#5AC4BE"),
    ORCA("#FFD15C"),
    DEFAULT("#6B7280")
}