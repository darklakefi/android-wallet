package fi.darklake.wallet.ui.screens.send

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.data.model.getHeliusRpcUrl
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.AppButton
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // Header
            SendHeader(
                title = "SEND NFT",
                icon = Icons.AutoMirrored.Filled.Send,
                onBack = onBack
            )

            // NFT preview card
            NftPreviewCard(
                nftName = uiState.nftName,
                nftImageUrl = uiState.nftImageUrl,
                nftMint = nftMint
            )

            // Send form - Using TransferForm component
            TransferForm(
                recipientAddress = uiState.recipientAddress,
                recipientAddressError = uiState.recipientAddressError,
                onRecipientAddressChange = viewModel::updateRecipientAddress,
                estimatedFee = uiState.estimatedFee,
                solBalance = uiState.solBalance,
                showWarning = true,
                warningText = "⚠️ NFT transfers are irreversible. Please verify the recipient address carefully.",
                modifier = Modifier.fillMaxWidth()
            )
            
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
            AppButton(
                text = if (uiState.isLoading) "BROADCASTING..." else "SEND NFT",
                onClick = { viewModel.sendNft() },
                enabled = uiState.recipientAddress.isNotBlank() && 
                         uiState.recipientAddressError == null && 
                         !uiState.isLoading && 
                         uiState.estimatedFee <= uiState.solBalance,
                modifier = Modifier.fillMaxWidth(),
                isLoading = uiState.isLoading
            )
        }
    }