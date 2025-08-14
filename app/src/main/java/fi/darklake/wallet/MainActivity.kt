package fi.darklake.wallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import fi.darklake.wallet.data.WalletRepositoryProvider
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.navigation.DarklakeNavigation
import fi.darklake.wallet.navigation.Screen
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.theme.DarklakeWalletTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val repository = WalletRepositoryProvider.getInstance(this)
        val storageManager = WalletStorageManager(this)
        val settingsManager = SettingsManager(this)
        
        setContent {
            var startDestination by remember { mutableStateOf<String?>(null) }
            
            LaunchedEffect(Unit) {
                lifecycleScope.launch {
                    startDestination = if (repository.hasWallet()) {
                        Screen.Wallet.route
                    } else {
                        Screen.Welcome.route
                    }
                }
            }
            
            DarklakeWalletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    startDestination?.let { destination ->
                        DarklakeNavigation(
                            startDestination = destination,
                            storageManager = storageManager,
                            settingsManager = settingsManager
                        )
                    }
                }
            }
        }
    }
}