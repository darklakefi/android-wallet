package fi.darklake.wallet.ui.screens.swap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.R
import fi.darklake.wallet.data.swap.models.TokenInfo
import fi.darklake.wallet.ui.utils.FormatUtils
import java.math.BigDecimal

@Composable
internal fun TokenInputSection(
    label: String,
    token: TokenInfo?,
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
                            text = stringResource(R.string.swap_select_token),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.swap_select_token),
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
                    placeholder = { Text(stringResource(R.string.swap_amount_placeholder)) },
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
                        text = stringResource(R.string.swap_balance, FormatUtils.formatBalance(balance)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (showInsufficientBalance)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (!isReadOnly && balance > BigDecimal.ZERO) {
                        Text(
                            text = stringResource(R.string.swap_max),
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