package fi.darklake.wallet.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.darklake.wallet.data.model.NetworkSettings
import fi.darklake.wallet.data.model.SolanaNetwork
import fi.darklake.wallet.data.preferences.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val networkSettings: NetworkSettings = NetworkSettings()
)

class SettingsViewModel(
    private val settingsManager: SettingsManager
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
}