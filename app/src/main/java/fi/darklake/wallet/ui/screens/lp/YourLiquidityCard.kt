package fi.darklake.wallet.ui.screens.lp

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

@Composable
fun YourLiquidityCard(
    positions: List<LiquidityPosition>,
    onWithdrawLiquidity: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPosition by remember { mutableStateOf<LiquidityPosition?>(null) }
    
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Liquidity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "All Positions (${positions.size})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Liquidity Positions
            positions.forEach { position ->
                LiquidityPositionItem(
                    position = position,
                    onWithdraw = { onWithdrawLiquidity(position.id) },
                    onShowDetails = { selectedPosition = position }
                )
            }
        }
    }
    
    // Position Details Dialog
    selectedPosition?.let { position ->
        LiquidityPositionDialog(
            position = position,
            onDismiss = { selectedPosition = null },
            onWithdraw = { 
                onWithdrawLiquidity(position.id)
                selectedPosition = null
            }
        )
    }
}

@Composable
private fun LiquidityPositionItem(
    position: LiquidityPosition,
    onWithdraw: () -> Unit,
    onShowDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onShowDetails
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Token pair and value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = "Liquidity pool",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${position.tokenA.symbol}/${position.tokenB.symbol}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${position.usdValue.stripTrailingZeros().toPlainString()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${position.sharePercentage}% of pool",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Token amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${position.tokenAAmount.stripTrailingZeros().toPlainString()} ${position.tokenA.symbol}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${position.tokenBAmount.stripTrailingZeros().toPlainString()} ${position.tokenB.symbol}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier.size(width = 100.dp, height = 36.dp)
                ) {
                    Text(
                        text = "Withdraw",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidityPositionDialog(
    position: LiquidityPosition,
    onDismiss: () -> Unit,
    onWithdraw: () -> Unit
) {
    var withdrawPercentage by remember { mutableFloatStateOf(100f) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${position.tokenA.symbol}/${position.tokenB.symbol} Position")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Position summary
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Value")
                            Text(
                                text = "$${position.usdValue.stripTrailingZeros().toPlainString()}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pool Share")
                            Text("${position.sharePercentage}%")
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("LP Tokens")
                            Text(position.lpTokenBalance.stripTrailingZeros().toPlainString())
                        }
                    }
                }
                
                // Withdraw percentage slider
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Withdraw Amount: ${withdrawPercentage.toInt()}%",
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    Slider(
                        value = withdrawPercentage,
                        onValueChange = { withdrawPercentage = it },
                        valueRange = 0f..100f,
                        steps = 19 // 5% increments
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(25, 50, 75, 100).forEach { percentage ->
                            FilterChip(
                                selected = withdrawPercentage.toInt() == percentage,
                                onClick = { withdrawPercentage = percentage.toFloat() },
                                label = { Text("$percentage%") }
                            )
                        }
                    }
                }
                
                // Withdrawal preview
                if (withdrawPercentage > 0) {
                    val withdrawTokenA = position.tokenAAmount.multiply(BigDecimal((withdrawPercentage / 100).toDouble()))
                    val withdrawTokenB = position.tokenBAmount.multiply(BigDecimal((withdrawPercentage / 100).toDouble()))
                    val withdrawValue = position.usdValue.multiply(BigDecimal((withdrawPercentage / 100).toDouble()))
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "You will receive:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "${withdrawTokenA.stripTrailingZeros().toPlainString()} ${position.tokenA.symbol}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${withdrawTokenB.stripTrailingZeros().toPlainString()} ${position.tokenB.symbol}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "≈ $${withdrawValue.stripTrailingZeros().toPlainString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onWithdraw,
                enabled = withdrawPercentage > 0
            ) {
                Text("Withdraw ${withdrawPercentage.toInt()}%")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}