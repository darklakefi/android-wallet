package fi.darklake.wallet.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.data.model.DisplayToken
import fi.darklake.wallet.data.model.DisplayNft
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.*
import fi.darklake.wallet.ui.design.*
import java.text.DecimalFormat

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarklakeBackground)
    ) {
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
                DarklakeLogo(size = 40.dp)
                
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

@Composable
private fun TabSelector(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarklakeCardBackgroundAlt)
    ) {
        // Tokens Tab
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onTabSelected(0) }
                .background(if (selectedTab == 0) DarklakeTabActive else Color.Transparent)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "[ TOKENS ]",
                style = TabTextStyle,
                color = if (selectedTab == 0) DarklakeTabTextActive else DarklakeTabTextInactive
            )
        }
        
        // NFTs Tab
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onTabSelected(1) }
                .background(if (selectedTab == 1) DarklakeTabActive else Color.Transparent)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "[ NFTS ]",
                style = TabTextStyle,
                color = if (selectedTab == 1) DarklakeTabTextActive else DarklakeTabTextInactive
            )
        }
    }
}


@Composable
private fun TokensList(
    tokens: List<DisplayToken>,
    isLoading: Boolean,
    onTokenClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarklakeCardBackground)
            .padding(vertical = 12.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tokens) { token ->
                TokenBalanceCard(
                    tokenSymbol = token.symbol,
                    tokenName = token.name,
                    tokenAddress = if (token.mint.length > 10) {
                        "${token.mint.take(4)}...${token.mint.takeLast(4)}"
                    } else null,
                    balance = token.balance,
                    balanceUsd = null,
                    onClick = { onTokenClick(token.mint) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            if (tokens.isEmpty() && !isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tokens found",
                            style = TerminalTextStyle,
                            color = DarklakeTextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun NftsGrid(
    nfts: List<DisplayNft>,
    isLoading: Boolean,
    onNftClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarklakeCardBackground)
            .padding(12.dp)
    ) {
        if (nfts.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No NFTs found",
                    style = TerminalTextStyle,
                    color = DarklakeTextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(nfts) { nft ->
                    NftCard(
                        nft = nft,
                        onClick = { onNftClick(nft.mint) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NftCard(
    nft: DisplayNft,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = DarklakeCardBackgroundAlt
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = nft.name.take(20),
                    style = NftTitleStyle,
                    color = DarklakeTextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (nft.compressed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "COMPRESSED",
                        style = CompressedLabelStyle,
                        color = DarklakeTextTertiary
                    )
                }
            }
        }
    }
}

// Preview functions
@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewWalletScreen() {
    val mockTokens = listOf(
        DisplayToken(
            mint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            symbol = "USDC",
            name = "USD Coin",
            balance = "123,333.12",
            imageUrl = null,
            usdValue = null
        ),
        DisplayToken(
            mint = "So11111111111111111111111111111111111111112",
            symbol = "SOL",
            name = "Solana",
            balance = "123,333.12",
            imageUrl = null,
            usdValue = null
        ),
        DisplayToken(
            mint = "DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB263",
            symbol = "BONK",
            name = "Bonk",
            balance = "123,333.12",
            imageUrl = null,
            usdValue = null
        )
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarklakeBackground)
    ) {
        TokensList(
            tokens = mockTokens,
            isLoading = false,
            onTokenClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewMainBalanceCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarklakeBackground)
            .padding(16.dp)
    ) {
        MainBalanceCard(
            balance = "123,553.12 SOL",
            onReceiveClick = {},
            onSendClick = {},
            onRefreshClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewTokenBalanceCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarklakeBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TokenBalanceCard(
            tokenSymbol = "USDC",
            tokenName = "USD Coin",
            tokenAddress = null,
            balance = "123,333.12",
            balanceUsd = null,
            onClick = {}
        )
        TokenBalanceCard(
            tokenSymbol = "SOL",
            tokenName = "Solana",
            tokenAddress = null,
            balance = "123,333.12",
            balanceUsd = null,
            onClick = {}
        )
        TokenBalanceCard(
            tokenSymbol = "BONK",
            tokenName = "Bonk",
            tokenAddress = null,
            balance = "123,333.12",
            balanceUsd = null,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewNftCard() {
    val mockNft = DisplayNft(
        mint = "mock123",
        name = "Cool NFT #1234",
        imageUrl = null,
        collectionName = "Cool Collection",
        compressed = false
    )
    
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(DarklakeBackground)
            .padding(16.dp)
    ) {
        NftCard(nft = mockNft, onClick = {})
    }
}

