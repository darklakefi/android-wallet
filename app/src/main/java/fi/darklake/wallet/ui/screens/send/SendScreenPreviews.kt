package fi.darklake.wallet.ui.screens.send

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.TerminalButton
import fi.darklake.wallet.ui.design.*
import kotlinx.coroutines.flow.MutableStateFlow

// Create mock ViewModel for previews
@Composable
fun createMockSendViewModel(): SendViewModel {
    val context = LocalContext.current
    return remember {
        SendViewModel(
            storageManager = WalletStorageManager(context),
            settingsManager = SettingsManager(context)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewSendHeader() {
    DarklakeWalletTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            SendHeader(
                title = "SEND SOL",
                icon = Icons.AutoMirrored.Filled.Send,
                onBack = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewTokenInfoCard() {
    DarklakeWalletTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            TokenInfoCard(
                tokenSymbol = "USDC",
                tokenName = "USD Coin",
                tokenBalance = "1,234.56",
                tokenImageUrl = null
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewNftPreviewCard() {
    DarklakeWalletTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            NftPreviewCard(
                nftName = "Bored Ape #1234",
                nftImageUrl = null,
                nftMint = "DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB263"
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewTransferForm() {
    DarklakeWalletTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            var recipientAddress by remember { mutableStateOf("") }
            var amountInput by remember { mutableStateOf("") }
            
            TransferForm(
                recipientAddress = recipientAddress,
                recipientAddressError = null,
                onRecipientAddressChange = { recipientAddress = it },
                showAmountInput = true,
                amountInput = amountInput,
                amountLabel = "Amount (SOL)",
                amountError = null,
                onAmountChange = { amountInput = it },
                onMaxClick = { amountInput = "10.5" },
                estimatedFee = 0.000005,
                solBalance = 10.5,
                showWarning = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewTransferFormWithWarning() {
    DarklakeWalletTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            TransferForm(
                recipientAddress = "7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU",
                recipientAddressError = null,
                onRecipientAddressChange = {},
                showAmountInput = false,
                estimatedFee = 0.000005,
                solBalance = 10.5,
                showWarning = true,
                warningText = "⚠️ NFT transfers are irreversible. Please verify the recipient address carefully."
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewTransferFormInsufficientBalance() {
    DarklakeWalletTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp)
        ) {
            TransferForm(
                recipientAddress = "",
                recipientAddressError = "Invalid address",
                onRecipientAddressChange = {},
                showAmountInput = true,
                amountInput = "100",
                amountLabel = "Amount (SOL)",
                amountError = "Insufficient balance",
                onAmountChange = {},
                estimatedFee = 0.001,
                solBalance = 0.0005
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewSendButton() {
    DarklakeWalletTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Enabled state
            TerminalButton(
                onClick = {},
                enabled = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SEND SOL")
            }
            
            // Loading state
            TerminalButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
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
            }
            
            // Disabled state
            TerminalButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SEND TOKEN")
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewSendSolScreen() {
    DarklakeWalletTheme {
        val context = LocalContext.current
        val storageManager = remember { WalletStorageManager(context) }
        val settingsManager = remember { SettingsManager(context) }
        
        SendSolScreen(
            onBack = {},
            onSuccess = {},
            storageManager = storageManager,
            settingsManager = settingsManager,
            viewModel = createMockSendViewModel()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewSendTokenScreen() {
    DarklakeWalletTheme {
        val context = LocalContext.current
        val storageManager = remember { WalletStorageManager(context) }
        val settingsManager = remember { SettingsManager(context) }
        
        SendTokenScreen(
            tokenMint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            onBack = {},
            onSuccess = {},
            storageManager = storageManager,
            settingsManager = settingsManager,
            viewModel = createMockSendViewModel()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewSendNftScreen() {
    DarklakeWalletTheme {
        val context = LocalContext.current
        val storageManager = remember { WalletStorageManager(context) }
        val settingsManager = remember { SettingsManager(context) }
        
        SendNftScreen(
            nftMint = "DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB263",
            onBack = {},
            onSuccess = {},
            storageManager = storageManager,
            settingsManager = settingsManager,
            viewModel = createMockSendViewModel()
        )
    }
}