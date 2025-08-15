package fi.darklake.wallet.ui.send

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.RetroGridBackground
import fi.darklake.wallet.ui.components.TerminalButton
import fi.darklake.wallet.ui.components.TerminalCard
import fi.darklake.wallet.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendSolScreen(
    onBack: () -> Unit,
    storageManager: WalletStorageManager,
    settingsManager: SettingsManager,
    viewModel: SendViewModel = viewModel { SendViewModel(storageManager, settingsManager) }
) {
    val uiState by viewModel.uiState.collectAsState()
    
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
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "SEND SOL",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
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
                    // Available balance display
                    Text(
                        text = "AVAILABLE: ${String.format(Locale.US, "%.4f", uiState.solBalance)} SOL",
                        style = TerminalTextStyle,
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold
                    )
                    
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
                        label = { Text("Amount (SOL)") },
                        placeholder = { Text("0.0000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = uiState.amountError != null,
                        supportingText = uiState.amountError?.let { error ->
                            { Text(error, color = MaterialTheme.colorScheme.error) }
                        },
                        trailingIcon = {
                            TextButton(
                                onClick = { viewModel.setMaxAmount() }
                            ) {
                                Text("MAX", style = TerminalTextStyle, color = BrightCyan)
                            }
                        }
                    )
                    
                    // Transaction fee estimate
                    if (uiState.estimatedFee > 0.0) {
                        Text(
                            text = "ESTIMATED_FEE: ${String.format(Locale.US, "%.6f", uiState.estimatedFee)} SOL",
                            style = TerminalTextStyle,
                            color = TerminalGray
                        )
                    }
                    
                    // Total to be deducted
                    val totalDeduction = uiState.amount + uiState.estimatedFee
                    if (totalDeduction > 0.0) {
                        Text(
                            text = "TOTAL_DEDUCTION: ${String.format(Locale.US, "%.6f", totalDeduction)} SOL",
                            style = TerminalTextStyle,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
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
                onClick = { viewModel.sendSol() },
                enabled = uiState.canSend && !uiState.isLoading,
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
                    Text("SEND SOL")
                }
            }
        }
    }
}