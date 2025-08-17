package fi.darklake.wallet.ui.screens.swap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import java.math.BigDecimal
import java.text.DecimalFormat

@Composable
fun SwapScreen(
    storageManager: WalletStorageManager,
    settingsManager: SettingsManager
) {
    val viewModel: SwapViewModel = viewModel {
        SwapViewModel(storageManager, settingsManager)
    }
    val uiState by viewModel.uiState.collectAsState()
    
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
                    label = "From",
                    token = uiState.tokenA,
                    amount = uiState.tokenAAmount,
                    balance = uiState.tokenABalance,
                    onAmountChange = { viewModel.updateTokenAAmount(it) },
                    onTokenSelect = { /* TODO: Open token selector */ },
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
                            contentDescription = "Swap tokens",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // To Token Section
                TokenInputSection(
                    label = "To",
                    token = uiState.tokenB,
                    amount = uiState.tokenBAmount,
                    balance = uiState.tokenBBalance,
                    onAmountChange = { },
                    onTokenSelect = { /* TODO: Open token selector */ },
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
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "High price impact! Your swap may result in significant loss.",
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                AnimatedVisibility(visible = uiState.successMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = uiState.successMessage ?: "",
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
                    text = "No liquidity pool exists for this pair. Create a pool to enable swapping.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SwapHeader(
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Swap",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }
    }
}

@Composable
private fun TokenInputSection(
    label: String,
    token: fi.darklake.wallet.data.swap.models.TokenInfo?,
    amount: String,
    balance: BigDecimal,
    onAmountChange: (String) -> Unit,
    onTokenSelect: () -> Unit,
    isReadOnly: Boolean,
    showInsufficientBalance: Boolean
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Token Selector
            Card(
                modifier = Modifier.clickable { onTokenSelect() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (token != null) {
                        // Token icon placeholder
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = token.symbol.first().toString(),
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = token.symbol,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Select",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select token",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Amount Input
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    readOnly = isReadOnly,
                    placeholder = { Text("0.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showInsufficientBalance,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (showInsufficientBalance) 
                            MaterialTheme.colorScheme.error 
                        else MaterialTheme.colorScheme.primary
                    )
                )
                
                // Balance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Balance: ${formatBalance(balance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (showInsufficientBalance)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (!isReadOnly && balance > BigDecimal.ZERO) {
                        Text(
                            text = "MAX",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                onAmountChange(balance.toPlainString())
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlippageSettings(
    slippagePercent: Double,
    onSlippageChange: (Double, Boolean) -> Unit
) {
    var showCustom by remember { mutableStateOf(false) }
    var customSlippage by remember { mutableStateOf("") }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Slippage Tolerance",
                style = MaterialTheme.typography.labelMedium
            )
            
            Text(
                text = "$slippagePercent%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preset options
            listOf(0.1, 0.5, 1.0).forEach { slippage ->
                FilterChip(
                    selected = slippagePercent == slippage && !showCustom,
                    onClick = {
                        showCustom = false
                        onSlippageChange(slippage, false)
                    },
                    label = { Text("$slippage%") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Custom option
            FilterChip(
                selected = showCustom,
                onClick = { showCustom = !showCustom },
                label = { Text("Custom") },
                modifier = Modifier.weight(1f)
            )
        }
        
        AnimatedVisibility(visible = showCustom) {
            OutlinedTextField(
                value = customSlippage,
                onValueChange = { value ->
                    customSlippage = value
                    value.toDoubleOrNull()?.let { slippage ->
                        if (slippage in 0.0..50.0) {
                            onSlippageChange(slippage, true)
                        }
                    }
                },
                label = { Text("Custom slippage (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true
            )
        }
    }
}

@Composable
private fun QuoteDetails(
    quote: fi.darklake.wallet.data.swap.models.SwapQuoteResponse,
    tokenA: fi.darklake.wallet.data.swap.models.TokenInfo?,
    tokenB: fi.darklake.wallet.data.swap.models.TokenInfo?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "1 ${tokenA?.symbol ?: ""} = ${formatAmount(quote.rate)} ${tokenB?.symbol ?: ""}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Price Impact
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Price Impact",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${formatAmount(quote.priceImpactPercentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        quote.priceImpactPercentage < 1 -> Color(0xFF4CAF50)
                        quote.priceImpactPercentage < 5 -> Color(0xFFFFA726)
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
            
            // Estimated Fee
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Network Fee",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "~$${formatAmount(quote.estimatedFeesUsd)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SwapButton(
    uiState: SwapUiState,
    onSwap: () -> Unit,
    onReset: () -> Unit
) {
    val buttonText = when (uiState.swapStep) {
        SwapStep.IDLE -> {
            when {
                uiState.tokenAAmount.isEmpty() -> "Enter an amount"
                uiState.insufficientBalance -> "Insufficient ${uiState.tokenA?.symbol ?: "balance"}"
                !uiState.poolExists -> "No liquidity pool"
                uiState.isLoadingQuote -> "Loading..."
                uiState.priceImpactWarning -> "Swap anyway (High impact!)"
                else -> "Swap"
            }
        }
        SwapStep.GENERATING_PROOF -> "Generating proof [1/3]..."
        SwapStep.CONFIRM_TRANSACTION -> "Confirm in wallet [2/3]..."
        SwapStep.PROCESSING -> "Processing [3/3]..."
        SwapStep.COMPLETED -> "Swap completed!"
        SwapStep.FAILED -> "Swap failed - Try again"
    }
    
    val isEnabled = when (uiState.swapStep) {
        SwapStep.IDLE -> {
            !uiState.insufficientBalance &&
            uiState.tokenAAmount.isNotEmpty() &&
            uiState.tokenA != null &&
            uiState.tokenB != null &&
            uiState.poolExists &&
            !uiState.isLoadingQuote
        }
        SwapStep.COMPLETED, SwapStep.FAILED -> true
        else -> false
    }
    
    Button(
        onClick = {
            when (uiState.swapStep) {
                SwapStep.IDLE -> onSwap()
                SwapStep.COMPLETED, SwapStep.FAILED -> onReset()
                else -> {}
            }
        },
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = when (uiState.swapStep) {
                SwapStep.COMPLETED -> Color(0xFF4CAF50)
                SwapStep.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
        )
    ) {
        if (uiState.isSwapping && uiState.swapStep != SwapStep.COMPLETED && uiState.swapStep != SwapStep.FAILED) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(buttonText)
    }
}

private fun formatBalance(balance: BigDecimal): String {
    return if (balance == BigDecimal.ZERO) {
        "0"
    } else if (balance < BigDecimal("0.0001")) {
        "<0.0001"
    } else {
        DecimalFormat("#,##0.####").format(balance)
    }
}

private fun formatAmount(amount: Double): String {
    return DecimalFormat("#,##0.######").format(amount)
}