package fi.darklake.wallet.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.darklake.wallet.data.model.NetworkSettings
import fi.darklake.wallet.data.model.SolanaNetwork
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val networkSettings: NetworkSettings = NetworkSettings(),
    val walletAddress: String = ""
)

class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val storageManager: WalletStorageManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            networkSettings = settingsManager.networkSettings.value
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsManager.networkSettings.collect { networkSettings ->
                _uiState.value = _uiState.value.copy(
                    networkSettings = networkSettings
                )
            }
        }
    }

    fun updateNetwork(network: SolanaNetwork) {
        settingsManager.switchNetwork(network)
    }
    
    fun updateHeliusApiKey(apiKey: String) {
        val key = apiKey.trim().ifBlank { null }
        settingsManager.updateHeliusApiKey(key)
    }
    
    fun loadWalletAddress() {
        storageManager?.let { manager ->
            viewModelScope.launch {
                if (manager.hasWallet()) {
                    val result = manager.getWallet()
                    if (result.isSuccess) {
                        result.getOrNull()?.let { wallet ->
                            _uiState.value = _uiState.value.copy(
                                walletAddress = wallet.publicKey
                            )
                        }
                    }
                }
            }
        }
    }
    
    fun getWalletAddress(): String {
        return _uiState.value.walletAddress
    }
}