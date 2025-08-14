package fi.darklake.wallet.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.ui.theme.DarklakeWalletTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportWalletScreen(
    onWalletImported: () -> Unit,
    onBack: () -> Unit,
    viewModel: ImportWalletViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var mnemonicInput by remember { mutableStateOf("") }
    var showMnemonic by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.walletImported) {
        if (uiState.walletImported) {
            onWalletImported()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Wallet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enter Recovery Phrase",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Enter your 12-word recovery phrase to restore your wallet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = mnemonicInput,
                onValueChange = { mnemonicInput = it },
                label = { Text("Recovery phrase") },
                placeholder = { Text("Enter 12 words separated by spaces") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                visualTransformation = if (showMnemonic) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showMnemonic = !showMnemonic }) {
                        Icon(
                            imageVector = if (showMnemonic) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showMnemonic) "Hide" else "Show"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                isError = uiState.validationError != null
            )
            
            uiState.validationError?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { 
                    val words = mnemonicInput.trim().split("\\s+".toRegex())
                    viewModel.importWallet(words)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading && mnemonicInput.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Import Wallet",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
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
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ImportWalletScreenPreview() {
    DarklakeWalletTheme {
        ImportWalletScreen(
            onWalletImported = {},
            onBack = {}
        )
    }
}