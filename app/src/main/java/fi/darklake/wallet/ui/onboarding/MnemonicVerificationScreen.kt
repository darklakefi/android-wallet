package fi.darklake.wallet.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.ui.design.DarklakeWalletTheme
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MnemonicVerificationScreen(
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: SharedWalletViewModel = viewModel()
) {
    val mnemonic = viewModel.currentMnemonic ?: listOf()
    
    // Select random words to verify (e.g., 3rd, 7th, and 11th)
    val verificationIndices = remember {
        listOf(2, 6, 10).shuffled(Random(System.currentTimeMillis())).take(3).sorted()
    }
    
    var wordInputs by remember { 
        mutableStateOf(verificationIndices.associateWith { "" })
    }
    
    var verificationError by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Recovery Phrase") },
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
                text = "Verify Your Recovery Phrase",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Please enter the following words from your recovery phrase",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            verificationIndices.forEach { index ->
                OutlinedTextField(
                    value = wordInputs[index] ?: "",
                    onValueChange = { newValue ->
                        wordInputs = wordInputs + (index to newValue.trim().lowercase())
                        verificationError = false
                    },
                    label = { Text("Word #${index + 1}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isError = verificationError
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (verificationError) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Incorrect words. Please check your recovery phrase.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    val allCorrect = verificationIndices.all { index ->
                        wordInputs[index]?.lowercase()?.trim() == mnemonic[index].lowercase()
                    }
                    
                    if (allCorrect) {
                        viewModel.confirmWallet()
                        onVerified()
                    } else {
                        verificationError = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = verificationIndices.all { index -> 
                    !wordInputs[index].isNullOrBlank() 
                }
            ) {
                Text(
                    text = "Verify & Continue",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MnemonicVerificationScreenPreview() {
    DarklakeWalletTheme {
        MnemonicVerificationScreen(
            onVerified = {},
            onBack = {}
        )
    }
}