package fi.darklake.wallet.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.graphics.vector.ImageVector

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