package fi.darklake.wallet.ui.send

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.data.model.getHeliusRpcUrl
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.RetroGridBackground
import fi.darklake.wallet.ui.components.TerminalButton
import fi.darklake.wallet.ui.components.TerminalCard
import fi.darklake.wallet.ui.design.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendNftScreen(
    nftMint: String,
    onBack: () -> Unit,
    onSuccess: (() -> Unit)? = null,
    storageManager: WalletStorageManager,
    settingsManager: SettingsManager,
    viewModel: SendViewModel = viewModel { SendViewModel(storageManager, settingsManager) }
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle success state - navigate back and trigger refresh
    LaunchedEffect(uiState.transactionSuccess) {
        if (uiState.transactionSuccess) {
            onSuccess?.invoke()
            onBack()
        }
    }
    
    // Initialize NFT data when screen loads
    LaunchedEffect(nftMint) {
        // Load NFT from wallet assets to get metadata
        val wallet = storageManager.getWallet().getOrNull()
        if (wallet != null) {
            val networkSettings = settingsManager.networkSettings.value
            val solanaApiService = fi.darklake.wallet.data.api.SolanaApiService { networkSettings.getHeliusRpcUrl() }
            
            val nftsResult = solanaApiService.getNftsByOwner(wallet.publicKey)
            if (nftsResult.isSuccess) {
                val nft = nftsResult.getOrNull()?.find { it.mint == nftMint }
                if (nft != null) {
                    viewModel.initializeNftSend(
                        nftMint = nftMint,
                        nftName = nft.name ?: "Unknown NFT",
                        nftImageUrl = nft.image
                    )
                } else {
                    // Fallback for unknown NFT
                    viewModel.initializeNftSend(
                        nftMint = nftMint,
                        nftName = "Unknown NFT",
                        nftImageUrl = null
                    )
                }
            }
            solanaApiService.close()
        }
    }
    
    RetroGridBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "SEND NFT",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // NFT preview card
            TerminalCard(
                title = "NFT_ASSET",
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // NFT image
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                color = TerminalBlack,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = BrightCyan.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.nftImageUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(uiState.nftImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = uiState.nftName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = "[NFT]",
                                style = TerminalHeaderStyle,
                                color = BrightCyan
                            )
                        }
                    }
                    
                    // NFT details
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "[${uiState.nftName ?: "UNKNOWN_NFT"}]",
                            style = TerminalHeaderStyle,
                            color = BrightCyan,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = "MINT: ${nftMint.take(8)}...${nftMint.takeLast(8)}",
                            style = TerminalTextStyle,
                            color = TerminalGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = "✓ UNIQUE ASSET",
                                modifier = Modifier.padding(8.dp),
                                style = TerminalTextStyle,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Send form
            TerminalCard(
                title = "TRANSFER_PARAMETERS",
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Warning about NFT transfers
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = "⚠️ NFT transfers are irreversible. Please verify the recipient address carefully.",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = TerminalTextStyle
                        )
                    }
                    
                    // Recipient address
                    OutlinedTextField(
                        value = uiState.recipientAddress,
                        onValueChange = viewModel::updateRecipientAddress,
                        label = { Text("Recipient Address") },
                        placeholder = { Text("Enter Solana wallet address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = uiState.recipientAddressError != null,
                        supportingText = uiState.recipientAddressError?.let { error ->
                            { Text(error, color = MaterialTheme.colorScheme.error) }
                        }
                    )
                    
                    // Transaction fee estimate (in SOL)
                    Text(
                        text = "ESTIMATED_FEE: ${String.format(Locale.US, "%.6f", uiState.estimatedFee)} SOL",
                        style = TerminalTextStyle,
                        color = TerminalGray
                    )
                    
                    // Balance check warning
                    if (uiState.estimatedFee > uiState.solBalance) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = "⚠️ Insufficient SOL for transaction fees",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.error,
                                style = TerminalTextStyle
                            )
                        }
                    }
                }
            }
            
            // Error display
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Send button
            TerminalButton(
                onClick = { viewModel.sendNft() },
                enabled = uiState.recipientAddress.isNotBlank() && 
                         uiState.recipientAddressError == null && 
                         !uiState.isLoading && 
                         uiState.estimatedFee <= uiState.solBalance,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = TerminalGray
                        )
                        Text("BROADCASTING...")
                    }
                } else {
                    Text("SEND NFT")
                }
            }
        }
    }
}