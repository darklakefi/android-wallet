package fi.darklake.wallet.ui.wallet

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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.R
import fi.darklake.wallet.data.model.DisplayToken
import fi.darklake.wallet.data.model.DisplayNft
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.*
import fi.darklake.wallet.ui.theme.*
import java.text.DecimalFormat

val BitsumishiFontWallet = FontFamily(
    Font(R.font.bitsumishi)
)

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
                color = if (selectedTab == 0) DarklakeTabTextActive else DarklakeTabTextInactive,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                fontFamily = BitsumishiFontWallet
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
                color = if (selectedTab == 1) DarklakeTabTextActive else DarklakeTabTextInactive,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                fontFamily = BitsumishiFontWallet
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
                            color = DarklakeTextTertiary,
                            fontSize = 14.sp,
                            fontFamily = BitsumishiFontWallet
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
                    color = Color(0xFF1A9A56),
                    fontSize = 14.sp,
                    fontFamily = BitsumishiFontWallet
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
                    color = DarklakeTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = BitsumishiFontWallet
                )
                if (nft.compressed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "COMPRESSED",
                        color = DarklakeTextTertiary,
                        fontSize = 8.sp,
                        fontFamily = BitsumishiFontWallet
                    )
                }
            }
        }
    }
}

