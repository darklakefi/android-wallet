package fi.darklake.wallet.data.preferences

import android.content.Context
import android.content.SharedPreferences
import fi.darklake.wallet.data.model.NetworkSettings
import fi.darklake.wallet.data.model.SolanaNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _networkSettings = MutableStateFlow(loadNetworkSettings())
    val networkSettings: StateFlow<NetworkSettings> = _networkSettings.asStateFlow()
    
    private fun loadNetworkSettings(): NetworkSettings {
        val settingsJson = prefs.getString(KEY_NETWORK_SETTINGS, null)
        return if (settingsJson != null) {
            try {
                json.decodeFromString<NetworkSettings>(settingsJson)
            } catch (e: Exception) {
                NetworkSettings() // Return default if parsing fails
            }
        } else {
            NetworkSettings() // Return default if no settings found
        }
    }
    
    fun updateNetworkSettings(settings: NetworkSettings) {
        val settingsJson = json.encodeToString(settings)
        prefs.edit()
            .putString(KEY_NETWORK_SETTINGS, settingsJson)
            .apply()
        _networkSettings.value = settings
    }
    
    fun switchNetwork(network: SolanaNetwork) {
        val currentSettings = _networkSettings.value
        val newSettings = currentSettings.copy(network = network)
        updateNetworkSettings(newSettings)
    }
    
    fun getCurrentRpcUrl(): String {
        val settings = _networkSettings.value
        return settings.customRpcUrl ?: settings.network.rpcUrl
    }
    
    companion object {
        private const val PREFS_NAME = "darklake_wallet_settings"
        private const val KEY_NETWORK_SETTINGS = "network_settings"
    }
}