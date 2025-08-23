package fi.darklake.wallet.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.ui.components.RetroGridBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    viewModel: SettingsViewModel = viewModel { SettingsViewModel(settingsManager) }
) {
    val uiState by viewModel.uiState.collectAsState()

    RetroGridBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            SettingsHeader()

            // Network Settings Card
            NetworkSettingsCard(
                networkSettings = uiState.networkSettings,
                onNetworkChange = viewModel::updateNetwork,
                onApiKeyChange = viewModel::updateHeliusApiKey
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}


