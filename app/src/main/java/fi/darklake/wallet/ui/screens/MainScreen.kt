package fi.darklake.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.ui.screens.swap.SwapScreen
import fi.darklake.wallet.ui.screens.lp.LpScreen
import fi.darklake.wallet.ui.screens.wallet.WalletScreen
import fi.darklake.wallet.ui.screens.wallet.WalletViewModel
import fi.darklake.wallet.ui.screens.settings.SettingsScreen
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.ui.design.*

@Composable
fun MainScreen(
    storageManager: WalletStorageManager,
    settingsManager: SettingsManager,
    onNavigateToSendSol: () -> Unit,
    onNavigateToSendToken: (String) -> Unit,
    onNavigateToSendNft: (String) -> Unit,
    onNavigateToReceive: () -> Unit,
    onNavigateToSlippageSettings: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val tabs = listOf(MainTab.Wallet, MainTab.Swap, MainTab.LP, MainTab.More)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarklakeBackground)
    ) {
        // Main content without bottom navigation (it's included in each screen)
        NavHost(
            navController = navController,
            startDestination = MainTab.Wallet.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(MainTab.Wallet.route) {
                val walletViewModel: WalletViewModel = viewModel { 
                    WalletViewModel(storageManager, settingsManager) 
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    WalletScreen(
                        storageManager = storageManager,
                        settingsManager = settingsManager,
                        viewModel = walletViewModel,
                        onNavigateToSettings = {
                            navController.navigate(MainTab.More.route)
                        },
                        onNavigateToSendSol = onNavigateToSendSol,
                        onNavigateToSendToken = onNavigateToSendToken,
                        onNavigateToSendNft = onNavigateToSendNft,
                        onNavigateToReceive = onNavigateToReceive
                    )
                    
                    // Override bottom navigation
                    DarklakeBottomNavigation(
                        currentRoute = currentRoute,
                        tabs = tabs,
                        onTabSelected = { tab ->
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
            
            composable(MainTab.Swap.route) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SwapScreen(
                        storageManager = storageManager,
                        settingsManager = settingsManager,
                        onNavigateToSlippageSettings = onNavigateToSlippageSettings
                    )
                    
                    DarklakeBottomNavigation(
                        currentRoute = currentRoute,
                        tabs = tabs,
                        onTabSelected = { tab ->
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
            
            composable(MainTab.LP.route) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LpScreen(
                        settingsManager = settingsManager,
                        storageManager = storageManager
                    )
                    
                    DarklakeBottomNavigation(
                        currentRoute = currentRoute,
                        tabs = tabs,
                        onTabSelected = { tab ->
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
            
            composable(MainTab.More.route) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        settingsManager = settingsManager
                    )
                    
                    DarklakeBottomNavigation(
                        currentRoute = currentRoute,
                        tabs = tabs,
                        onTabSelected = { tab ->
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

