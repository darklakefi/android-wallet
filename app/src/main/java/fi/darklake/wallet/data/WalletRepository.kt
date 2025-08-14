package fi.darklake.wallet.data

import android.content.Context
import fi.darklake.wallet.crypto.SolanaWallet
import fi.darklake.wallet.storage.WalletStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WalletRepository(context: Context) {
    
    private val storageManager = WalletStorageManager(context)
    
    private val _currentWallet = MutableStateFlow<SolanaWallet?>(null)
    val currentWallet: StateFlow<SolanaWallet?> = _currentWallet.asStateFlow()
    
    private val _isWalletLoaded = MutableStateFlow(false)
    val isWalletLoaded: StateFlow<Boolean> = _isWalletLoaded.asStateFlow()
    
    suspend fun loadWallet(): Result<SolanaWallet?> {
        return try {
            val result = storageManager.getWallet()
            if (result.isSuccess) {
                _currentWallet.value = result.getOrNull()
                _isWalletLoaded.value = true
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun saveWallet(wallet: SolanaWallet): Result<Unit> {
        return try {
            val result = storageManager.storeWallet(wallet)
            if (result.isSuccess) {
                _currentWallet.value = wallet
                _isWalletLoaded.value = true
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteWallet(): Result<Unit> {
        return try {
            val result = storageManager.deleteWallet()
            if (result.isSuccess) {
                _currentWallet.value = null
                _isWalletLoaded.value = false
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun hasWallet(): Boolean {
        return storageManager.hasWallet()
    }
    
    fun getAvailableStorageProviders(): List<String> {
        return storageManager.getAvailableProviders()
    }
    
    fun getCurrentStorageProvider(): String? {
        return storageManager.currentProvider.value?.providerName
    }
}

// Singleton instance
object WalletRepositoryProvider {
    @Volatile
    private var INSTANCE: WalletRepository? = null
    
    fun getInstance(context: Context): WalletRepository {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: WalletRepository(context.applicationContext).also { INSTANCE = it }
        }
    }
}