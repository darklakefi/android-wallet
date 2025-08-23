package fi.darklake.wallet.ui.screens.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
            
            // Validate mnemonic format
            if (mnemonic.size != 12) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    validationError = "Recovery phrase must be exactly 12 words"
                )
                return@launch
            }
            
            // Validate words
            if (!SolanaWalletManager.validateMnemonic(mnemonic)) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    validationError = "Invalid recovery phrase. Please check your words."
                )
                return@launch
            }
            
            try {
                val wallet = SolanaWalletManager.createWalletFromMnemonic(mnemonic)
                
                // Store wallet securely
                val saveResult = repository.saveWallet(wallet)
                
                if (saveResult.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        walletImported = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to save wallet: ${saveResult.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to import wallet: ${e.message}"
                )
            }
        }
    }
}