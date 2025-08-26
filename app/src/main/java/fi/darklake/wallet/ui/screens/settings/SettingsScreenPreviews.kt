package fi.darklake.wallet.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.ui.design.DarklakeWalletTheme

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewSettingsScreen() {
    DarklakeWalletTheme {
        val context = LocalContext.current
        val settingsManager = remember { SettingsManager(context) }
        val storageManager = remember { fi.darklake.wallet.storage.WalletStorageManager(context) }
        
        SettingsScreen(
            settingsManager = settingsManager,
            storageManager = storageManager
        )
    }
}