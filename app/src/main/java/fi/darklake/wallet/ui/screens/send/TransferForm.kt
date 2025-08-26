package fi.darklake.wallet.ui.screens.send

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import fi.darklake.wallet.ui.design.*
import java.util.Locale

@Composable
fun TransferForm(
    recipientAddress: String,
    recipientAddressError: String?,
    onRecipientAddressChange: (String) -> Unit,
    showAmountInput: Boolean = false,
    amountInput: String = "",
    amountLabel: String = "Amount",
    amountError: String? = null,
    onAmountChange: (String) -> Unit = {},
    onMaxClick: (() -> Unit)? = null,
    estimatedFee: Double,
    solBalance: Double,
    showWarning: Boolean = false,
    warningText: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = SurfaceContainer,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = NeonGreen.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            // Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "╭─ TRANSFER_PARAMETERS",
                    style = TerminalHeaderStyle,
                    color = NeonGreen,
                    fontSize = 12.sp
                )
                Text(
                    text = "─╮",
                    style = TerminalHeaderStyle,
                    color = NeonGreen,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Optional warning
                if (showWarning && warningText != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = warningText,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = TerminalTextStyle
                        )
                    }
                }
                
                // Recipient address
                OutlinedTextField(
                    value = recipientAddress,
                    onValueChange = onRecipientAddressChange,
                    label = { Text("Recipient Address") },
                    placeholder = { Text("Enter Solana wallet address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = recipientAddressError != null,
                    supportingText = recipientAddressError?.let { error ->
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    }
                )
                
                // Optional amount input
                if (showAmountInput) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = onAmountChange,
                        label = { Text(amountLabel) },
                        placeholder = { Text("0.00") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = amountError != null,
                        supportingText = amountError?.let { error ->
                            { Text(error, color = MaterialTheme.colorScheme.error) }
                        },
                        trailingIcon = if (onMaxClick != null) {
                            {
                                TextButton(onClick = onMaxClick) {
                                    Text("MAX", style = TerminalTextStyle, color = BrightCyan)
                                }
                            }
                        } else null
                    )
                }
                
                // Transaction fee estimate
                Text(
                    text = "ESTIMATED_FEE: ${String.format(Locale.US, "%.6f", estimatedFee)} SOL",
                    style = TerminalTextStyle,
                    color = TerminalGray
                )
                
                // Balance check warning
                if (estimatedFee > solBalance) {
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
            
            // Bottom border
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "╰─",
                    style = TerminalHeaderStyle,
                    color = NeonGreen,
                    fontSize = 12.sp
                )
                Text(
                    text = "─╯",
                    style = TerminalHeaderStyle,
                    color = NeonGreen,
                    fontSize = 12.sp
                )
            }
        }
    }
}