package fi.darklake.wallet.ui.screens.lp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import fi.darklake.wallet.R
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidityFormCard(
    uiState: LpUiState,
    onTokenAAmountChange: (String) -> Unit,
    onTokenBAmountChange: (String) -> Unit,
    onTokenASelect: () -> Unit,
    onTokenBSelect: () -> Unit,
    onSwapTokens: () -> Unit,
    onAddLiquidity: () -> Unit,
    onCreatePool: () -> Unit,
    onSlippageChange: (Double, Boolean) -> Unit,
    onInitialPriceChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSlippageDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.poolDetails?.exists == true) stringResource(R.string.lp_add_liquidity) else stringResource(R.string.lp_create_pool),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = { showSlippageDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                }
            }
            
            // Token A Input
            TokenInputSection(
                label = stringResource(R.string.lp_token_a) + " " + stringResource(R.string.lp_amount),
                token = uiState.tokenA,
                amount = uiState.tokenAAmount,
                balance = uiState.tokenABalance,
                onAmountChange = onTokenAAmountChange,
                onTokenSelect = onTokenASelect,
                insufficientBalance = uiState.insufficientBalanceA,
                isLoading = false
            )
            
            // Swap/Add Icon
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onSwapTokens,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        if (uiState.poolDetails?.exists == true) Icons.Default.SwapVert else Icons.Default.Add,
                        contentDescription = stringResource(R.string.swap_tokens),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            // Token B Input
            TokenInputSection(
                label = stringResource(R.string.lp_token_b) + " " + stringResource(R.string.lp_amount),
                token = uiState.tokenB,
                amount = uiState.tokenBAmount,
                balance = uiState.tokenBBalance,
                onAmountChange = onTokenBAmountChange,
                onTokenSelect = onTokenBSelect,
                insufficientBalance = uiState.insufficientBalanceB,
                isLoading = false
            )
            
            // Initial Price Section (only for new pools)
            if (uiState.poolDetails?.exists != true) {
                InitialPriceSection(
                    tokenA = uiState.tokenA,
                    tokenB = uiState.tokenB,
                    initialPrice = uiState.initialPrice,
                    onInitialPriceChange = onInitialPriceChange
                )
            }
            
            // Pool Details (if exists)
            if (uiState.poolDetails?.exists == true && 
                uiState.tokenAAmount.isNotEmpty() && uiState.tokenBAmount.isNotEmpty()) {
                PoolDetailsSection(
                    tokenA = uiState.tokenA,
                    tokenB = uiState.tokenB,
                    tokenAAmount = uiState.tokenAAmount,
                    tokenBAmount = uiState.tokenBAmount,
                    slippage = uiState.slippagePercent
                )
            }
            
            // Action Button
            val isLoading = uiState.isAddingLiquidity || uiState.isCreatingPool
            val canSubmit = uiState.tokenA != null && uiState.tokenB != null &&
                          uiState.tokenAAmount.isNotEmpty() && uiState.tokenBAmount.isNotEmpty() &&
                          !uiState.insufficientBalanceA && !uiState.insufficientBalanceB
            
            Button(
                onClick = {
                    if (uiState.poolDetails?.exists == true) {
                        onAddLiquidity()
                    } else {
                        onCreatePool()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSubmit && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Text(
                    text = when {
                        isLoading && uiState.liquidityStep == LiquidityStep.GENERATING_PROOF -> 
                            stringResource(R.string.swap_generating_proof)
                        isLoading && uiState.liquidityStep == LiquidityStep.CONFIRM_TRANSACTION -> 
                            stringResource(R.string.swap_confirm_wallet)
                        isLoading && uiState.liquidityStep == LiquidityStep.PROCESSING -> 
                            stringResource(R.string.swap_processing)
                        uiState.poolDetails?.exists == true -> stringResource(R.string.lp_add_liquidity)
                        else -> stringResource(R.string.lp_create_pool)
                    }
                )
            }
        }
    }
    
    // Slippage Settings Dialog
    if (showSlippageDialog) {
        SlippageDialog(
            currentSlippage = uiState.slippagePercent,
            isCustom = uiState.useCustomSlippage,
            onSlippageChange = { slippage, isCustom ->
                onSlippageChange(slippage, isCustom)
                showSlippageDialog = false
            },
            onDismiss = { showSlippageDialog = false }
        )
    }
}

@Composable
private fun TokenInputSection(
    label: String,
    token: TokenInfo?,
    amount: String,
    balance: BigDecimal,
    onAmountChange: (String) -> Unit,
    onTokenSelect: () -> Unit,
    insufficientBalance: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (insufficientBalance) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = stringResource(R.string.swap_balance, balance.stripTrailingZeros().toPlainString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Token selector
                OutlinedButton(
                    onClick = onTokenSelect,
                    modifier = Modifier.weight(1f)
                ) {
                    if (token != null) {
                        Text(token.symbol)
                    } else {
                        Text(stringResource(R.string.lp_select_token))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.lp_select_token),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Amount input
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    modifier = Modifier.weight(2f),
                    placeholder = { Text(stringResource(R.string.swap_amount_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = insufficientBalance,
                    enabled = !isLoading
                )
            }
            
            if (insufficientBalance) {
                Text(
                    text = stringResource(R.string.swap_button_insufficient, token?.symbol ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun InitialPriceSection(
    tokenA: TokenInfo?,
    tokenB: TokenInfo?,
    initialPrice: String,
    onInitialPriceChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.lp_initial_price),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "1 ${tokenA?.symbol ?: stringResource(R.string.lp_token_a)} =",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = initialPrice,
                    onValueChange = onInitialPriceChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("1.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    suffix = { Text(tokenB?.symbol ?: stringResource(R.string.lp_token_b)) }
                )
            }
            
            Text(
                text = stringResource(R.string.lp_initial_price_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PoolDetailsSection(
    tokenA: TokenInfo?,
    tokenB: TokenInfo?,
    tokenAAmount: String,
    tokenBAmount: String,
    slippage: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Liquidity Details",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Deposit",
                    style = MaterialTheme.typography.bodyMedium
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$tokenAAmount ${tokenA?.symbol}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$tokenBAmount ${tokenB?.symbol}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Slippage Tolerance",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$slippage%",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SlippageDialog(
    currentSlippage: Double,
    isCustom: Boolean,
    onSlippageChange: (Double, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var customSlippage by remember { mutableStateOf(currentSlippage.toString()) }
    val presetOptions = listOf(0.1, 0.5, 1.0)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Slippage Tolerance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetOptions.forEach { preset ->
                        FilterChip(
                            selected = !isCustom && currentSlippage == preset,
                            onClick = { onSlippageChange(preset, false) },
                            label = { Text("$preset%") }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = customSlippage,
                    onValueChange = { customSlippage = it },
                    label = { Text("Custom") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val slippage = customSlippage.toDoubleOrNull() ?: currentSlippage
                    onSlippageChange(slippage, true)
                }
            ) {
                Text("Apply Custom")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}