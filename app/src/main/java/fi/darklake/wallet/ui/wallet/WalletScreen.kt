package fi.darklake.wallet.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fi.darklake.wallet.data.model.DisplayNft
import fi.darklake.wallet.data.model.DisplayToken
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.RetroGridBackground
import fi.darklake.wallet.ui.components.TerminalBalanceDisplay
import fi.darklake.wallet.ui.components.TerminalAddressDisplay
import fi.darklake.wallet.ui.components.TerminalCard
import fi.darklake.wallet.ui.components.TerminalButton
import fi.darklake.wallet.ui.components.TerminalNetworkStatus
import fi.darklake.wallet.ui.components.neonGlow
import fi.darklake.wallet.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(Unit) {
        viewModel.loadWalletData()
    }

    RetroGridBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Terminal header with network status
            item {
                TerminalNetworkStatus(
                    isConnected = !uiState.isLoading,
                    networkName = "DEVNET",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Header with balance - now using terminal style
            item {
                TerminalWalletCard(
                    solBalance = uiState.solBalance,
                    publicKey = uiState.publicKey,
                    isLoading = uiState.isLoading,
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    onSettings = onNavigateToSettings,
                    onSendSol = onNavigateToSendSol
                )
            }

            // Error display
            uiState.error?.let { error ->
                item {
                    ErrorCard(
                        error = error,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }

            // Tokens section - terminal style
            if (uiState.tokens.isNotEmpty() || !uiState.isLoading) {
                item {
                    TerminalTokensSection(
                        tokens = uiState.tokens,
                        isLoading = uiState.isLoading,
                        onTokenClick = onNavigateToSendToken
                    )
                }
            }

            // NFTs section - terminal style
            if (uiState.nfts.isNotEmpty() || !uiState.isLoading) {
                item {
                    TerminalNftsSection(
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
private fun TerminalWalletCard(
    solBalance: Double,
    publicKey: String?,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onSendSol: () -> Unit
) {
    TerminalCard(
        title = "WALLET_STATUS",
        glowEffect = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .neonGlow()
    ) {
        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[SYSTEM_READY]",
                style = TerminalHeaderStyle,
                color = NeonGreen,
                fontSize = 12.sp
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TerminalButton(
                    onClick = onSendSol,
                    enabled = solBalance > 0.0 && !isLoading,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send SOL",
                        tint = if (solBalance > 0.0 && !isLoading) NeonGreen else TerminalGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                TerminalButton(
                    onClick = onSettings,
                    modifier = Modifier.size(32.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("file:///android_asset/icons/png/dark/bullet-list.png")
                            .build(),
                        contentDescription = "Settings",
                        modifier = Modifier.size(20.dp) // Adjust size as needed
                    )
                }
                
                TerminalButton(
                    onClick = onRefresh,
                    enabled = !isLoading && !isRefreshing,
                    isLoading = isRefreshing,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp), // Adjust size as needed
                            color = TerminalGray,
                            strokeWidth = 2.dp
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("file:///android_asset/icons/png/dark/refresh.png")
                                .build(),
                            contentDescription = "Refresh",
                            modifier = Modifier.size(20.dp) // Adjust size as needed
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Balance display
        TerminalBalanceDisplay(
            balance = if (solBalance > 0) String.format(Locale.US, "%.4f", solBalance) else "0.0000",
            currency = "SOL",
            isLoading = isLoading && solBalance == 0.0,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Address display
        publicKey?.let { key ->
            TerminalAddressDisplay(
                address = key,
                label = "WALLET_ADDRESS",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            TextButton(onClick = onDismiss) {
                Text("DISMISS")
            }
        }
    }
}

@Composable
private fun TerminalTokensSection(
    tokens: List<DisplayToken>,
    isLoading: Boolean,
    onTokenClick: (String) -> Unit = {}
) {
    TerminalCard(
        title = "TOKEN_INVENTORY",
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading && tokens.isEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(3) {
                    TerminalTokenSkeleton()
                }
            }
        } else if (tokens.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "[NO_TOKENS_DETECTED]",
                    style = TerminalTextStyle,
                    color = TerminalGray,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(tokens) { token ->
                    TerminalTokenItem(
                        token = token,
                        onClick = { onTokenClick(token.mint) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalTokenItem(
    token: DisplayToken,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .background(
                color = TerminalBlack,
                shape = RoundedCornerShape(2.dp)
            )
            .border(
                width = 1.dp,
                color = NeonGreen.copy(alpha = 0.6f),
                shape = RoundedCornerShape(2.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Token symbol/name header
            Text(
                text = "[${token.symbol}]",
                style = TerminalHeaderStyle,
                color = NeonGreen,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Balance
            Text(
                text = token.balance,
                style = TerminalTextStyle,
                color = OnSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Token name
            Text(
                text = token.name,
                style = TerminalTextStyle,
                color = TerminalGray,
                fontSize = 9.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TerminalTokenSkeleton() {
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .background(
                color = TerminalBlack,
                shape = RoundedCornerShape(2.dp)
            )
            .border(
                width = 1.dp,
                color = OutlineVariant,
                shape = RoundedCornerShape(2.dp)
            )
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp)
                    .background(
                        color = OutlineVariant,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(10.dp)
                    .background(
                        color = OutlineVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(8.dp)
                    .background(
                        color = OutlineVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
private fun TokenItemSkeleton() {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )
            }
        }
    }
}

@Composable
private fun TerminalNftsSection(
    nfts: List<DisplayNft>,
    isLoading: Boolean,
    onNftClick: (String) -> Unit = {}
) {
    TerminalCard(
        title = "NFT_COLLECTION",
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading && nfts.isEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(4) {
                    TerminalNftSkeleton()
                }
            }
        } else if (nfts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "[NO_NFT_ASSETS_FOUND]",
                    style = TerminalTextStyle,
                    color = TerminalGray,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(320.dp) // Adjusted height for better display
            ) {
                items(nfts) { nft ->
                    TerminalNftItem(
                        nft = nft,
                        onClick = { onNftClick(nft.mint) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalNftItem(
    nft: DisplayNft,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(
                color = TerminalBlack,
                shape = RoundedCornerShape(2.dp)
            )
            .border(
                width = 1.dp,
                color = BrightCyan.copy(alpha = 0.6f),
                shape = RoundedCornerShape(2.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // NFT Image area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SurfaceContainer), // Placeholder background
                contentAlignment = Alignment.Center
            ) {
                if (nft.imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(nft.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = nft.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text( // Placeholder text if no image
                        text = "[IMG]",
                        style = TerminalTextStyle,
                        color = TerminalGray,
                        fontSize = 20.sp
                    )
                }
            }
            
            // NFT Info
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "[${nft.name}]", // Displaying NFT name in brackets
                    style = TerminalTextStyle,
                    color = BrightCyan,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                
                nft.collectionName?.let { collection ->
                    Text(
                        text = collection,
                        style = TerminalTextStyle,
                        color = TerminalGray,
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalNftSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(
                color = TerminalBlack,
                shape = RoundedCornerShape(2.dp)
            )
            .border(
                width = 1.dp,
                color = OutlineVariant, // Using a dimmer color for skeleton
                shape = RoundedCornerShape(2.dp)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(OutlineVariant.copy(alpha = 0.3f)) // Dimmer background for skeleton
            )
            
            // Text skeletons
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(10.dp)
                        .background(
                            color = OutlineVariant,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(8.dp)
                        .background(
                            color = OutlineVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun NftItemSkeleton() { // This seems to be a duplicate or an alternative skeleton
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            )
            
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WalletScreenPreview() {
    DarklakeWalletTheme {
        // Preview would need mock data
    }
}
