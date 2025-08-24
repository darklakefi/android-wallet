package fi.darklake.wallet.ui.components

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.darklake.wallet.data.swap.models.TokenInfo
import fi.darklake.wallet.ui.design.*
import fi.darklake.wallet.ui.utils.FormatUtils
import java.math.BigDecimal

/**
 * Token amount input component matching Figma design
 * Balance is shown in top right with HALF/MAX buttons
 * Used in Swap and Liquidity screens
 */
@Composable
fun TokenAmountInput(
    label: String,
    token: TokenInfo?,
    amount: String,
    balance: BigDecimal,
    onAmountChange: (String) -> Unit,
    onTokenSelect: () -> Unit,
    isReadOnly: Boolean,
    showInsufficientBalance: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Row: Label and Balance with HALF/MAX
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label (SELLING/BUYING)
            Text(
                text = label,
                style = TerminalTextStyle,
                color = DarklakeTextTertiary,
                fontSize = 10.sp
            )
            
            // Balance and action buttons
            if (!isReadOnly && balance > BigDecimal.ZERO) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Balance text
                    Text(
                        text = "Balance: ${FormatUtils.formatBalance(balance)}",
                        style = TerminalTextStyle,
                        color = if (showInsufficientBalance) DarklakeError else DarklakeTextTertiary,
                        fontSize = 10.sp
                    )
                    
                    // HALF button
                    Text(
                        text = "HALF",
                        style = TerminalTextStyle,
                        color = DarklakePrimary,
                        fontSize = 10.sp,
                        modifier = Modifier.clickable {
                            val halfAmount = balance.divide(BigDecimal(2))
                            onAmountChange(FormatUtils.formatBalance(halfAmount))
                        }
                    )
                    
                    // MAX button
                    Text(
                        text = "MAX",
                        style = TerminalTextStyle,
                        color = DarklakePrimary,
                        fontSize = 10.sp,
                        modifier = Modifier.clickable {
                            onAmountChange(balance.toPlainString())
                        }
                    )
                }
            } else if (isReadOnly) {
                // Just show balance for readonly (BUYING)
                Text(
                    text = "Balance: ${FormatUtils.formatBalance(balance)}",
                    style = TerminalTextStyle,
                    color = DarklakeTextTertiary,
                    fontSize = 10.sp
                )
            }
        }
        
        // Bottom Row: Token Selector and Amount Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Token Selector
            Box(
                modifier = Modifier
                    .clickable { onTokenSelect() }
                    .background(
                        color = DarklakeCardBackgroundAlt,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (token != null) {
                        // Token icon as first letter
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when(token.symbol) {
                                        "SOL" -> TokenSolBackground
                                        "USDC" -> TokenUsdcBackground
                                        "BONK" -> TokenBonkBackground
                                        else -> DarklakeTokenDefaultBg
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = token.symbol.first().toString(),
                                style = TerminalTextStyle,
                                color = DarklakeTextPrimary,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = token.symbol,
                            style = ButtonTextStyle,
                            color = DarklakeTextPrimary,
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = "SELECT",
                            style = ButtonTextStyle,
                            color = DarklakeTextTertiary,
                            fontSize = 14.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select token",
                        tint = DarklakeTextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Amount Input
            Box(
                modifier = Modifier.weight(1f)
            ) {
                if (isReadOnly) {
                    // Read-only display for BUYING section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(
                                color = DarklakeInputBackground,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = amount.ifEmpty { "0.00" },
                            style = TerminalTextStyle.copy(
                                fontSize = 16.sp,
                                color = if (amount.isEmpty()) DarklakeTextTertiary.copy(alpha = 0.5f) else DarklakeTextPrimary
                            )
                        )
                    }
                } else {
                    // Editable input for SELLING section
                    OutlinedTextField(
                        value = amount,
                        onValueChange = onAmountChange,
                        readOnly = isReadOnly,
                        placeholder = { 
                            Text(
                                "0.00",
                                color = DarklakeTextTertiary.copy(alpha = 0.5f)
                            )
                        },
                        textStyle = TerminalTextStyle.copy(
                            fontSize = 16.sp,
                            color = DarklakeTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        isError = showInsufficientBalance,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (showInsufficientBalance) 
                                DarklakeError 
                            else DarklakePrimary,
                            unfocusedBorderColor = DarklakeBorder,
                            errorBorderColor = DarklakeError,
                            cursorColor = DarklakePrimary,
                            focusedContainerColor = DarklakeInputBackground,
                            unfocusedContainerColor = DarklakeInputBackground,
                            errorContainerColor = DarklakeInputBackground
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        }
    }
}