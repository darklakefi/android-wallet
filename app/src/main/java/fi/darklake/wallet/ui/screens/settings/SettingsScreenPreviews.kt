package fi.darklake.wallet.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.data.model.NetworkSettings
import fi.darklake.wallet.data.model.SolanaNetwork
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.ui.design.DarklakeBackground
import fi.darklake.wallet.ui.design.DarklakeWalletTheme

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewNetworkOption() {
    DarklakeWalletTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NetworkOption(
                network = SolanaNetwork.MAINNET,
                isSelected = true,
                onSelect = {}
            )
            NetworkOption(
                network = SolanaNetwork.DEVNET,
                isSelected = false,
                onSelect = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewNetworkSettingsCard() {
    DarklakeWalletTheme {
        val mockNetworkSettings = NetworkSettings(
            network = SolanaNetwork.MAINNET,
            customRpcUrl = null,
            heliusApiKey = "test-api-key-12345678"
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            NetworkSettingsCard(
                networkSettings = mockNetworkSettings,
                onNetworkChange = {},
                onApiKeyChange = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewNetworkSettingsCardNoApiKey() {
    DarklakeWalletTheme {
        val mockNetworkSettings = NetworkSettings(
            network = SolanaNetwork.DEVNET,
            customRpcUrl = null,
            heliusApiKey = null
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            NetworkSettingsCard(
                networkSettings = mockNetworkSettings,
                onNetworkChange = {},
                onApiKeyChange = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewNetworkSettingsCardWithCustomRpc() {
    DarklakeWalletTheme {
        val mockNetworkSettings = NetworkSettings(
            network = SolanaNetwork.MAINNET,
            customRpcUrl = "https://custom-rpc.example.com",
            heliusApiKey = null
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            NetworkSettingsCard(
                networkSettings = mockNetworkSettings,
                onNetworkChange = {},
                onApiKeyChange = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewSettingsHeader() {
    DarklakeWalletTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            SettingsHeader(
                onBack = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewSettingsScreen() {
    DarklakeWalletTheme {
        val context = LocalContext.current
        val settingsManager = remember { SettingsManager(context) }
        
        SettingsScreen(
            settingsManager = settingsManager,
            onBack = {}
        )
    }
}