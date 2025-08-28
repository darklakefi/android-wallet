package fi.darklake.wallet.ui.screens.onboarding

import android.app.Application
import android.util.Log
import fi.darklake.wallet.diagnostics.DiagnosticLogger
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

data class CreateWalletUiState(
    val isLoading: Boolean = false,
    val mnemonic: List<String>? = null,
    val walletCreated: Boolean = false,
    val error: String? = null
)

class CreateWalletViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "CreateWalletViewModel"
    }
    
    private val repository = WalletRepositoryProvider.getInstance(application)
    
    private val _uiState = MutableStateFlow(CreateWalletUiState())
    val uiState: StateFlow<CreateWalletUiState> = _uiState.asStateFlow()
    
    fun createWallet() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Log.d(TAG, "Generating mnemonic...")
                val mnemonic = SolanaWalletManager.generateMnemonic()
                
                Log.d(TAG, "Creating wallet from mnemonic...")
                val wallet = SolanaWalletManager.createWalletFromMnemonic(mnemonic)
                
                Log.d(TAG, "Attempting to save wallet...")
                // Store wallet securely
                val saveResult = repository.saveWallet(wallet)
                
                if (saveResult.isSuccess) {
                    Log.d(TAG, "Wallet saved successfully")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        mnemonic = mnemonic,
                        walletCreated = true
                    )
                } else {
                    val error = saveResult.exceptionOrNull()
                    DiagnosticLogger.getInstance().error(TAG, "Failed to save wallet", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to save wallet: ${error?.message ?: "Unknown error"}"
                    )
                }
            } catch (e: Exception) {
                DiagnosticLogger.getInstance().error(TAG, "Exception during wallet creation", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to create wallet: ${e.message}"
                )
            }
        }
    }
}

// ViewModelFactory for proper Application context injection
class CreateWalletViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateWalletViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateWalletViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}