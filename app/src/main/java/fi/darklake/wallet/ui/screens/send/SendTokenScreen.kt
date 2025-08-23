package fi.darklake.wallet.ui.screens.send

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
fun SendTokenScreen(
    tokenMint: String,
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
    
    // Initialize token data when screen loads
    LaunchedEffect(tokenMint) {
        // Load token from wallet assets to get balance
        val wallet = storageManager.getWallet().getOrNull()
        if (wallet != null) {
            val networkSettings = settingsManager.networkSettings.value
            val solanaApiService = fi.darklake.wallet.data.api.SolanaApiService { networkSettings.getHeliusRpcUrl() }
            val assetsRepository = fi.darklake.wallet.data.api.WalletAssetsRepository(solanaApiService)
            
            val assetsResult = assetsRepository.getWalletAssets(wallet.publicKey)
            if (assetsResult.isSuccess) {
                val assets = assetsResult.getOrNull()!!
                val token = assets.tokens.find { it.balance.mint == tokenMint }
                if (token != null) {
                    viewModel.initializeTokenSend(
                        tokenMint = tokenMint,
                        tokenSymbol = token.metadata?.symbol ?: "TOKEN",
                        tokenBalance = token.balance.uiAmountString ?: "0",
                        decimals = token.balance.decimals
                    )
                } else {
                    // Fallback for unknown token
                    viewModel.initializeTokenSend(
                        tokenMint = tokenMint,
                        tokenSymbol = "TOKEN",
                        tokenBalance = "0",
                        decimals = 0
                    )
                }
            }
            solanaApiService.close()
            assetsRepository.close()
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
            SendHeader(
                title = "SEND ${uiState.tokenSymbol ?: "TOKEN"}",
                icon = Icons.AutoMirrored.Filled.Send,
                onBack = onBack
            )

            // Token info card
            TokenInfoCard(
                tokenSymbol = uiState.tokenSymbol,
                tokenName = uiState.tokenName,
                tokenBalance = uiState.tokenBalance,
                tokenImageUrl = uiState.tokenImageUrl
            )

            // Send form
            TerminalCard(
                title = "TRANSFER_PARAMETERS",
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                    
                    // Amount input
                    OutlinedTextField(
                        value = uiState.amountInput,
                        onValueChange = viewModel::updateAmount,
                        label = { Text("Amount (${uiState.tokenSymbol})") },
                        placeholder = { Text("0.00") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = uiState.amountError != null,
                        supportingText = uiState.amountError?.let { error ->
                            { Text(error, color = MaterialTheme.colorScheme.error) }
                        },
                        trailingIcon = {
                            TextButton(
                                onClick = { 
                                    // Set max token amount
                                    val maxBalance = uiState.tokenBalance?.toDoubleOrNull() ?: 0.0
                                    viewModel.updateAmount(maxBalance.toString())
                                }
                            ) {
                                Text("MAX", style = TerminalTextStyle, color = BrightCyan)
                            }
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
                onClick = { viewModel.sendToken() },
                enabled = uiState.canSend && !uiState.isLoading && uiState.estimatedFee <= uiState.solBalance,
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
                    Text("SEND ${uiState.tokenSymbol}")
                }
            }
        }
    }
}