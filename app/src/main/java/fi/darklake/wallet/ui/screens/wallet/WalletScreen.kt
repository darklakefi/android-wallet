package fi.darklake.wallet.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.data.model.DisplayToken
import fi.darklake.wallet.data.model.DisplayNft
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.*
import fi.darklake.wallet.ui.design.*
import fi.darklake.wallet.R

@Composable
fun WalletScreen(
    storageManager: WalletStorageManager,
    settingsManager: SettingsManager,
    onNavigateToSettings: () -> Unit,
    onNavigateToSendSol: () -> Unit = {},
    onNavigateToSendToken: (String) -> Unit = {},
    onNavigateToSendNft: (String) -> Unit = {},
    viewModel: WalletViewModel = viewModel { WalletViewModel(storageManager, settingsManager) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 for Tokens, 1 for NFTs

    LaunchedEffect(Unit) {
        viewModel.loadWalletData()
    }

    BackgroundWithOverlay {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header with Logo and Wallet Address
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppLogo(
                    logoResId = R.drawable.darklake_logo,
                    size = 40.dp,
                    contentDescription = "Darklake Logo",
                    tint = DarklakePrimary
                )
                
                WalletAddress(
                    address = uiState.publicKey ?: "Not connected"
                )
            }
            
            // Main Balance Card with Send/Receive/Refresh
            MainBalanceCard(
                balance = String.format("%.2f SOL", uiState.solBalance),
                onSendClick = onNavigateToSendSol,
                onReceiveClick = { /* TODO: Implement receive */ },
                onRefreshClick = { viewModel.refresh() }
            )
            
            // Spacer
            Spacer(modifier = Modifier.height(20.dp))
            
            // Tab Selection and Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Tab Selector
                TabSelector(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
                
                // Content Container with no extra background since tabs handle it
                when (selectedTab) {
                    0 -> TokensList(
                        tokens = uiState.tokens,
                        isLoading = uiState.isLoading,
                        onTokenClick = onNavigateToSendToken
                    )
                    1 -> NftsGrid(
                        nfts = uiState.nfts,
                        isLoading = uiState.isLoading,
                        onNftClick = onNavigateToSendNft
                    )
                }
            }
        }
    }
}


