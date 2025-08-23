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

data class CreateWalletUiState(
    val isLoading: Boolean = false,
    val mnemonic: List<String>? = null,
    val walletCreated: Boolean = false,
    val error: String? = null
)

class CreateWalletViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WalletRepositoryProvider.getInstance(application)
    
    private val _uiState = MutableStateFlow(CreateWalletUiState())
    val uiState: StateFlow<CreateWalletUiState> = _uiState.asStateFlow()
    
    fun createWallet() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val mnemonic = SolanaWalletManager.generateMnemonic()
                val wallet = SolanaWalletManager.createWalletFromMnemonic(mnemonic)
                
                // Store wallet securely
                val saveResult = repository.saveWallet(wallet)
                
                if (saveResult.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        mnemonic = mnemonic,
                        walletCreated = true
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
                    error = "Failed to create wallet: ${e.message}"
                )
            }
        }
    }
}