package fi.darklake.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.R
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.design.DarklakeBackground
import fi.darklake.wallet.ui.design.DarklakeWalletTheme

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewMainScreen() {
    DarklakeWalletTheme {
        val context = LocalContext.current
        val storageManager = remember { WalletStorageManager(context) }
        val settingsManager = remember { SettingsManager(context) }
        
        MainScreen(
            storageManager = storageManager,
            settingsManager = settingsManager,
            onNavigateToSendSol = {},
            onNavigateToSendToken = {},
            onNavigateToSendNft = {},
            onNavigateToReceive = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewDarklakeBottomNavigation() {
    DarklakeWalletTheme {
        val tabs = listOf(MainTab.Wallet, MainTab.Swap, MainTab.LP, MainTab.More)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            DarklakeBottomNavigation(
                currentRoute = MainTab.Wallet.route,
                tabs = tabs,
                onTabSelected = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewDarklakeBottomNavigationSwapSelected() {
    DarklakeWalletTheme {
        val tabs = listOf(MainTab.Wallet, MainTab.Swap, MainTab.LP, MainTab.More)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            DarklakeBottomNavigation(
                currentRoute = MainTab.Swap.route,
                tabs = tabs,
                onTabSelected = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewDarklakeBottomNavigationLPSelected() {
    DarklakeWalletTheme {
        val tabs = listOf(MainTab.Wallet, MainTab.Swap, MainTab.LP, MainTab.More)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            DarklakeBottomNavigation(
                currentRoute = MainTab.LP.route,
                tabs = tabs,
                onTabSelected = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewDarklakeBottomNavigationMoreSelected() {
    DarklakeWalletTheme {
        val tabs = listOf(MainTab.Wallet, MainTab.Swap, MainTab.LP, MainTab.More)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            DarklakeBottomNavigation(
                currentRoute = MainTab.More.route,
                tabs = tabs,
                onTabSelected = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewDarklakeNavItemSelected() {
    DarklakeWalletTheme {
        Box(
            modifier = Modifier
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            DarklakeNavItem(
                iconRes = R.drawable.ic_nav_wallet,
                label = "WALLET",
                selected = true,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewDarklakeNavItemUnselected() {
    DarklakeWalletTheme {
        Box(
            modifier = Modifier
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            DarklakeNavItem(
                iconRes = R.drawable.ic_nav_swap,
                label = "SWAP",
                selected = false,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewAllNavItems() {
    DarklakeWalletTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DarklakeNavItem(
                iconRes = R.drawable.ic_nav_wallet,
                label = "WALLET",
                selected = true,
                onClick = {}
            )
            DarklakeNavItem(
                iconRes = R.drawable.ic_nav_swap,
                label = "SWAP",
                selected = false,
                onClick = {}
            )
            DarklakeNavItem(
                iconRes = R.drawable.ic_nav_liquidity,
                label = "LIQUIDITY",
                selected = false,
                onClick = {}
            )
            DarklakeNavItem(
                iconRes = R.drawable.ic_nav_settings,
                label = "SETTINGS",
                selected = false,
                onClick = {}
            )
        }
    }
}