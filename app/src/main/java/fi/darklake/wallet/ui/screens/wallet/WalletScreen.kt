package fi.darklake.wallet.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
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
    onNavigateToReceive: () -> Unit = {},
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
                .statusBarsPadding()
        ) {
            // Header with Logo and Wallet Address
            AppHeader(
                walletAddress = uiState.publicKey ?: "Not connected"
            )
            
            // Main Balance Card with Send/Receive/Refresh
            MainBalanceCard(
                balance = String.format("%.2f SOL", uiState.solBalance),
                onSendClick = onNavigateToSendSol,
                onReceiveClick = onNavigateToReceive,
                onRefreshClick = { viewModel.refresh() },
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            // No spacer needed - spacing handled by TabSelector padding
            
            // Tab Selection and Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 20.dp)
            ) {
                // Tab Selector
                TabSelector(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.padding(horizontal = 24.dp)
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

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewWalletScreen() {
    DarklakeWalletTheme {
        // Create a static preview state
        val sampleState = WalletUiState(
            publicKey = "7xKXtpZfyVtzPE9PqRz8g9hNmDwrDgYm5x3TJK9PqRz",
            solBalance = 12.456789,
            tokens = listOf(
                DisplayToken(
                    mint = "So11111111111111111111111111111111111111112",
                    symbol = "SOL",
                    name = "Solana",
                    balance = "12.456789",
                    imageUrl = null,
                    usdValue = 1234.56
                ),
                DisplayToken(
                    mint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
                    symbol = "USDC",
                    name = "USD Coin",
                    balance = "500.25",
                    imageUrl = null,
                    usdValue = 500.25
                ),
                DisplayToken(
                    mint = "DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB263",
                    symbol = "BONK",
                    name = "Bonk",
                    balance = "1,000,000",
                    imageUrl = null,
                    usdValue = 25.50
                ),
                DisplayToken(
                    mint = "7vfCXTUXx5WJV5JADk17DUJ4ksgau7utNKj4b963voxs",
                    symbol = "RAY",
                    name = "Raydium",
                    balance = "25.5",
                    imageUrl = null,
                    usdValue = 125.75
                )
            ),
            nfts = listOf(
                DisplayNft(
                    mint = "NFT1xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
                    name = "Darklake Genesis #001",
                    imageUrl = null,
                    collectionName = "Darklake Genesis",
                    compressed = false
                ),
                DisplayNft(
                    mint = "NFT2xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
                    name = "Solana Monkey #1234",
                    imageUrl = null,
                    collectionName = "Solana Monkey Business",
                    compressed = false
                ),
                DisplayNft(
                    mint = "NFT3xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
                    name = "DeGod #5678",
                    imageUrl = null,
                    collectionName = "DeGods",
                    compressed = false
                ),
                DisplayNft(
                    mint = "NFT4xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
                    name = "Okay Bear #999",
                    imageUrl = null,
                    collectionName = "Okay Bears",
                    compressed = false
                )
            ),
            isLoading = false,
            isRefreshing = false,
            error = null
        )
        
        var selectedTab by remember { mutableStateOf(0) }
        
        BackgroundWithOverlay {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Header with Logo and Wallet Address
                AppHeader(
                    walletAddress = sampleState.publicKey ?: "Not connected"
                )
                
                // Main Balance Card with Send/Receive/Refresh
                MainBalanceCard(
                    balance = String.format("%.2f SOL", sampleState.solBalance),
                    onSendClick = {},
                    onReceiveClick = {},
                    onRefreshClick = {},
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                
                // Tab Selection and Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 20.dp)
                ) {
                    // Tab Selector
                    TabSelector(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    
                    // Content Container
                    when (selectedTab) {
                        0 -> TokensList(
                            tokens = sampleState.tokens,
                            isLoading = sampleState.isLoading,
                            onTokenClick = {}
                        )
                        1 -> NftsGrid(
                            nfts = sampleState.nfts,
                            isLoading = sampleState.isLoading,
                            onNftClick = {}
                        )
                    }
                }
            }
        }
    }
}
