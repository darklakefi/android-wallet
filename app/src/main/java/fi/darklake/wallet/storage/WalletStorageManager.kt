package fi.darklake.wallet.storage

import android.content.Context
import android.util.Log
import fi.darklake.wallet.crypto.SolanaWallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WalletStorageManager(context: Context) {
    
    companion object {
        private const val TAG = "WalletStorageManager"
    }
    
    private val providers: List<WalletStorageProvider> = listOf(
        // Seed Vault has highest priority when available
        SeedVaultStorageProvider(context),
        KeystoreStorageProvider(context, useStrongBox = true),
        KeystoreStorageProvider(context, useStrongBox = false),
        // Add basic file encryption as final fallback
        BasicFileStorageProvider(context)
    )
    
    private val _currentProvider = MutableStateFlow<WalletStorageProvider?>(null)
    val currentProvider: StateFlow<WalletStorageProvider?> = _currentProvider.asStateFlow()
    
    init {
        selectBestProvider()
    }
    
    private fun selectBestProvider() {
        Log.d(TAG, "Selecting best storage provider...")
        
        // Try each provider and log availability
        providers.forEach { provider ->
            Log.d(TAG, "Provider ${provider.providerName}: available=${provider.isAvailable}")
        }
        
        _currentProvider.value = providers.firstOrNull { it.isAvailable }
        
        if (_currentProvider.value == null) {
            Log.e(TAG, "No storage provider available!")
        } else {
            Log.d(TAG, "Selected storage provider: ${_currentProvider.value?.providerName}")
        }
    }
    
    suspend fun storeWallet(wallet: SolanaWallet): Result<Unit> {
        val provider = _currentProvider.value
            ?: return Result.failure(StorageError.NotAvailable("No storage provider available"))
        
        Log.d(TAG, "Storing wallet using ${provider.providerName}")
        
        return provider.storeWallet(wallet).also { result ->
            if (result.isFailure) {
                Log.e(TAG, "Failed to store wallet with ${provider.providerName}", result.exceptionOrNull())
                // Try next available provider
                tryNextProvider(provider)?.let { nextProvider ->
                    Log.d(TAG, "Retrying with ${nextProvider.providerName}")
                    return nextProvider.storeWallet(wallet)
                }
            }
        }
    }
    
    suspend fun getWallet(): Result<SolanaWallet?> {
        // First try to get from the current provider
        _currentProvider.value?.let { provider ->
            val result = provider.getWallet()
            if (result.isSuccess) {
                return result
            }
        }
        
        // If current provider failed, try all providers
        for (provider in providers) {
            if (provider.isAvailable) {
                Log.d(TAG, "Trying to retrieve wallet from ${provider.providerName}")
                val result = provider.getWallet()
                if (result.isSuccess && result.getOrNull() != null) {
                    _currentProvider.value = provider
                    return result
                }
            }
        }
        
        return Result.success(null)
    }
    
    suspend fun deleteWallet(): Result<Unit> {
        val results = mutableListOf<Result<Unit>>()
        
        // Try to delete from all available providers
        for (provider in providers) {
            if (provider.isAvailable) {
                Log.d(TAG, "Deleting wallet from ${provider.providerName}")
                results.add(provider.deleteWallet())
            }
        }
        
        // If at least one succeeded, consider it a success
        return if (results.any { it.isSuccess }) {
            Result.success(Unit)
        } else {
            Result.failure(StorageError.DeletionFailed("Failed to delete wallet from all providers"))
        }
    }
    
    suspend fun hasWallet(): Boolean {
        // Check all available providers
        for (provider in providers) {
            if (provider.isAvailable && provider.hasWallet()) {
                _currentProvider.value = provider
                return true
            }
        }
        return false
    }
    
    private fun tryNextProvider(currentProvider: WalletStorageProvider): WalletStorageProvider? {
        val currentIndex = providers.indexOf(currentProvider)
        return providers.drop(currentIndex + 1).firstOrNull { it.isAvailable }?.also {
            _currentProvider.value = it
        }
    }
    
    fun getAvailableProviders(): List<String> {
        return providers.filter { it.isAvailable }.map { it.providerName }
    }
}
