package fi.darklake.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.ui.design.*

sealed class MainTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Wallet : MainTab("main_wallet", "WALLET", Icons.Default.AccountBalanceWallet)
    data object Swap : MainTab("main_swap", "SWAP", Icons.Default.SwapHoriz)
    data object LP : MainTab("main_lp", "LIQUIDITY", Icons.Default.Layers)
    data object More : MainTab("main_more", "SETTINGS", Icons.Default.MoreHoriz)
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
                            if (currentRoute == MainTab.More.route) {
                                onNavigateToSettings()
                            } else {
                                navController.navigate(MainTab.More.route)
                            }
                        },
                        onNavigateToSendSol = onNavigateToSendSol,
                        onNavigateToSendToken = onNavigateToSendToken,
                        onNavigateToSendNft = onNavigateToSendNft
                    )
                    
                    // Override bottom navigation
                    DarklakeBottomNavigation(
                        currentRoute = currentRoute,
                        tabs = tabs,
                        onTabSelected = { tab ->
                            if (tab == MainTab.More) {
                                onNavigateToSettings()
                            } else {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
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
                        settingsManager = settingsManager
                    )
                    
                    DarklakeBottomNavigation(
                        currentRoute = currentRoute,
                        tabs = tabs,
                        onTabSelected = { tab ->
                            if (tab == MainTab.More) {
                                onNavigateToSettings()
                            } else {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
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
                            if (tab == MainTab.More) {
                                onNavigateToSettings()
                            } else {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun DarklakeBottomNavigation(
    currentRoute: String?,
    tabs: List<MainTab>,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarklakeBackground)
            .border(
                width = 1.dp,
                color = DarklakeBorder,
                shape = RoundedCornerShape(0.dp)
            )
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { tab ->
            DarklakeNavItem(
                icon = tab.icon,
                label = tab.title,
                selected = currentRoute == tab.route,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

@Composable
private fun DarklakeNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .wrapContentSize()
            .noRippleClickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) DarklakePrimary else TerminalGray,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = if (selected) DarklakePrimary else TerminalGray,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// Extension for clickable without ripple
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
    ) {
        onClick()
    }
}

// Preview functions
@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewDarklakeBottomNavigation() {
    val tabs = listOf(MainTab.Wallet, MainTab.Swap, MainTab.LP, MainTab.More)
    DarklakeBottomNavigation(
        currentRoute = MainTab.Wallet.route,
        tabs = tabs,
        onTabSelected = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewDarklakeBottomNavigationSwapSelected() {
    val tabs = listOf(MainTab.Wallet, MainTab.Swap, MainTab.LP, MainTab.More)
    DarklakeBottomNavigation(
        currentRoute = MainTab.Swap.route,
        tabs = tabs,
        onTabSelected = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewDarklakeNavItemSelected() {
    DarklakeNavItem(
        icon = Icons.Default.AccountBalanceWallet,
        label = "WALLET",
        selected = true,
        onClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewDarklakeNavItemUnselected() {
    DarklakeNavItem(
        icon = Icons.Default.SwapHoriz,
        label = "SWAP",
        selected = false,
        onClick = {}
    )
}