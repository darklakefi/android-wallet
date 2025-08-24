package fi.darklake.wallet.ui.screens.onboarding

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fi.darklake.wallet.crypto.SolanaWalletManager
import fi.darklake.wallet.data.WalletRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ImportWalletUiState(
    val isLoading: Boolean = false,
    val walletImported: Boolean = false,
    val validationError: String? = null,
    val error: String? = null
)

class ImportWalletViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "ImportWalletViewModel"
    }
    
    private val repository = WalletRepositoryProvider.getInstance(application)
    
    private val _uiState = MutableStateFlow(ImportWalletUiState())
    val uiState: StateFlow<ImportWalletUiState> = _uiState.asStateFlow()
    
    fun importWallet(mnemonic: List<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                error = null,
                validationError = null
            )
            
            Log.d(TAG, "Validating mnemonic format...")
            // Validate mnemonic format
            if (mnemonic.size != 12) {
                Log.w(TAG, "Mnemonic has ${mnemonic.size} words, expected 12")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    validationError = "Recovery phrase must be exactly 12 words"
                )
                return@launch
            }
            
            // Validate words
            if (!SolanaWalletManager.validateMnemonic(mnemonic)) {
                Log.w(TAG, "Invalid mnemonic words")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    validationError = "Invalid recovery phrase. Please check your words."
                )
                return@launch
            }
            
            try {
                Log.d(TAG, "Creating wallet from mnemonic...")
                val wallet = SolanaWalletManager.createWalletFromMnemonic(mnemonic)
                
                Log.d(TAG, "Attempting to save wallet...")
                // Store wallet securely
                val saveResult = repository.saveWallet(wallet)
                
                if (saveResult.isSuccess) {
                    Log.d(TAG, "Wallet imported successfully")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        walletImported = true
                    )
                } else {
                    val error = saveResult.exceptionOrNull()
                    Log.e(TAG, "Failed to save wallet", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to save wallet: ${error?.message ?: "Unknown error"}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during wallet import", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to import wallet: ${e.message}"
                )
            }
        }
    }
}

// ViewModelFactory for proper Application context injection
class ImportWalletViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImportWalletViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ImportWalletViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}