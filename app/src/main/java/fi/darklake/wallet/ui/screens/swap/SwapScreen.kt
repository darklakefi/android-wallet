package fi.darklake.wallet.ui.screens.swap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import fi.darklake.wallet.R
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.TokenSelectionSheet
import fi.darklake.wallet.ui.components.SuccessMessageCard
import fi.darklake.wallet.ui.components.BackgroundWithOverlay
import fi.darklake.wallet.ui.utils.FormatUtils

@Composable
fun SwapScreen(
    storageManager: WalletStorageManager,
    settingsManager: SettingsManager
) {
    val viewModel: SwapViewModel = viewModel {
        SwapViewModel(storageManager, settingsManager)
    }
    val uiState by viewModel.uiState.collectAsState()
    
    BackgroundWithOverlay {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // Header
        SwapHeader(
            onSettingsClick = { /* TODO: Open swap settings */ }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Swap Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // From Token Section
                TokenInputSection(
                    label = stringResource(R.string.swap_from),
                    token = uiState.tokenA,
                    amount = uiState.tokenAAmount,
                    balance = uiState.tokenABalance,
                    onAmountChange = { 
                        val filteredInput = FormatUtils.filterNumericInput(it)
                        viewModel.updateTokenAAmount(filteredInput) 
                    },
                    onTokenSelect = { viewModel.showTokenSelection(TokenSelectionType.TOKEN_A) },
                    isReadOnly = false,
                    showInsufficientBalance = uiState.insufficientBalance
                )
                
                // Swap Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { viewModel.swapTokens() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = stringResource(R.string.swap_tokens),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // To Token Section
                TokenInputSection(
                    label = stringResource(R.string.swap_to),
                    token = uiState.tokenB,
                    amount = uiState.tokenBAmount,
                    balance = uiState.tokenBBalance,
                    onAmountChange = { },
                    onTokenSelect = { viewModel.showTokenSelection(TokenSelectionType.TOKEN_B) },
                    isReadOnly = true,
                    showInsufficientBalance = false
                )
                
                // Slippage Settings
                Spacer(modifier = Modifier.height(16.dp))
                SlippageSettings(
                    slippagePercent = uiState.slippagePercent,
                    onSlippageChange = { slippage, isCustom ->
                        viewModel.updateSlippage(slippage, isCustom)
                    }
                )
                
                // Quote Details
                AnimatedVisibility(visible = uiState.quote != null) {
                    uiState.quote?.let { quote ->
                        QuoteDetails(
                            quote = quote,
                            tokenA = uiState.tokenA,
                            tokenB = uiState.tokenB
                        )
                    }
                }
                
                // Warning Messages
                AnimatedVisibility(visible = uiState.priceImpactWarning) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = stringResource(R.string.warning),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.swap_high_price_impact),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                // Swap Button
                Spacer(modifier = Modifier.height(16.dp))
                SwapButton(
                    uiState = uiState,
                    onSwap = { viewModel.executeSwap() },
                    onReset = { viewModel.resetSwap() }
                )
                
                // Error/Success Messages
                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    val clipboardManager = LocalClipboardManager.current
                    val context = LocalContext.current
                    val trackingCopiedMessage = uiState.trackingDetails?.trackingId?.let { trackingId ->
                        stringResource(R.string.swap_tracking_copied, trackingId)
                    } ?: ""
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                // Extract tracking ID from error message if present
                                uiState.trackingDetails?.trackingId?.let { trackingId ->
                                    clipboardManager.setText(AnnotatedString(trackingId))
                                    Toast.makeText(context, trackingCopiedMessage, Toast.LENGTH_SHORT).show()
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            // Show tracking ID separately if available
                            uiState.trackingDetails?.trackingId?.let { trackingId ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = stringResource(R.string.copy),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.swap_tracking_id, trackingId),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
                
                AnimatedVisibility(visible = uiState.successMessage != null) {
                    SuccessMessageCard(
                        message = uiState.successMessage ?: "",
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
        
        // Pool Info
        if (!uiState.poolExists) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = stringResource(R.string.swap_no_pool_message),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Token Selection Sheet
        if (uiState.showTokenSelection) {
            TokenSelectionSheet(
                tokens = uiState.availableTokens,
                selectedTokenAddress = when (uiState.tokenSelectionType) {
                    TokenSelectionType.TOKEN_A -> uiState.tokenA?.address
                    TokenSelectionType.TOKEN_B -> uiState.tokenB?.address
                    null -> null
                },
                onTokenSelected = { token -> viewModel.selectToken(token) },
                onDismiss = { viewModel.hideTokenSelection() }
            )
        }
        }
    }
}

