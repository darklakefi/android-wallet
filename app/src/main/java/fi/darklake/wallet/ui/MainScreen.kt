package fi.darklake.wallet.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.ui.screens.swap.SwapScreen
import fi.darklake.wallet.ui.screens.lp.LpScreen
import fi.darklake.wallet.ui.wallet.WalletScreen
import fi.darklake.wallet.ui.wallet.WalletViewModel
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.navigation.Screen

sealed class MainTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Wallet : MainTab("main_wallet", "Wallet", Icons.Default.AccountBalanceWallet)
    data object Swap : MainTab("main_swap", "Swap", Icons.Default.SwapHoriz)
    data object LP : MainTab("main_lp", "LP", Icons.Default.WaterDrop)
}

@Composable
fun MainScreen(
    storageManager: WalletStorageManager,
    settingsManager: SettingsManager,
    onNavigateToSettings: () -> Unit,
    onNavigateToSendSol: () -> Unit,
    onNavigateToSendToken: (String) -> Unit,
    onNavigateToSendNft: (String) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val tabs = listOf(MainTab.Wallet, MainTab.Swap, MainTab.LP)
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(tab.title)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = MainTab.Wallet.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(MainTab.Wallet.route) {
                val walletViewModel: WalletViewModel = viewModel { 
                    WalletViewModel(storageManager, settingsManager) 
                }
                WalletScreen(
                    storageManager = storageManager,
                    settingsManager = settingsManager,
                    viewModel = walletViewModel,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToSendSol = onNavigateToSendSol,
                    onNavigateToSendToken = onNavigateToSendToken,
                    onNavigateToSendNft = onNavigateToSendNft
                )
            }
            
            composable(MainTab.Swap.route) {
                SwapScreen(
                    storageManager = storageManager,
                    settingsManager = settingsManager
                )
            }
            
            composable(MainTab.LP.route) {
                LpScreen(
                    storageManager = storageManager,
                    settingsManager = settingsManager
                )
            }
        }
    }
}