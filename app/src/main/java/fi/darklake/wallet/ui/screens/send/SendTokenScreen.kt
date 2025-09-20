package fi.darklake.wallet.ui.screens.send

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.R
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.AppButton
import fi.darklake.wallet.ui.components.AppLogo
import fi.darklake.wallet.ui.components.BackgroundWithOverlay
import fi.darklake.wallet.ui.components.TokenIconByAddress
import fi.darklake.wallet.ui.design.BitsumishiFontFamily
import fi.darklake.wallet.ui.design.DarklakeCardBackground
import fi.darklake.wallet.ui.design.DarklakeInputBackground
import fi.darklake.wallet.ui.design.DarklakePrimary
import fi.darklake.wallet.ui.design.DarklakeTextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendTokenScreen(
    tokenMint: String? = null,
    onBack: () -> Unit,
    onSuccess: (() -> Unit)? = null,
    storageManager: WalletStorageManager,
    settingsManager: SettingsManager,
    viewModel: SendViewModel = viewModel { SendViewModel(storageManager, settingsManager) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Activity result launcher for signing (e.g., Seed Vault)
    val signingLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        println("SendTokenScreen: Activity result received - resultCode=${result.resultCode}")
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            // Get the signing responses from the result
            val signingResponses = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableArrayListExtra(
                        com.solanamobile.seedvault.WalletContractV1.EXTRA_SIGNING_RESPONSE,
                        com.solanamobile.seedvault.SigningResponse::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableArrayListExtra(
                        com.solanamobile.seedvault.WalletContractV1.EXTRA_SIGNING_RESPONSE
                    )
                }
            } catch (e: Exception) {
                println("SendTokenScreen: Error getting signing responses: ${e.message}")
                null
            }

            println("SendTokenScreen: Got ${signingResponses?.size} signing responses")

            if (!signingResponses.isNullOrEmpty()) {
                // Get the first signature from the first response
                val firstResponse = signingResponses[0]
                val signatures = firstResponse.signatures
                println("SendTokenScreen: Got ${signatures.size} signatures in first response")

                if (signatures.isNotEmpty()) {
                    // Pass the first signature to the view model
                    val signatureBytes = signatures[0]
                    println("SendTokenScreen: Signature bytes: ${signatureBytes.size}")
                    viewModel.onSigningComplete(signatureBytes)
                } else {
                    println("SendTokenScreen: No signatures in response")
                    viewModel.onSigningCancelled()
                }
            } else {
                println("SendTokenScreen: No signing responses in result")
                println("SendTokenScreen: Result extras: ${result.data?.extras?.keySet()}")
                viewModel.onSigningCancelled()
            }
        } else {
            println("SendTokenScreen: Signing cancelled or failed with code: ${result.resultCode}")
            viewModel.onSigningCancelled()
        }
    }

    // Launch signing when we have a pending signing request
    LaunchedEffect(uiState.pendingSigningRequest) {
        val signingRequest = uiState.pendingSigningRequest
        if (signingRequest != null) {
            println("SendTokenScreen: Launching signing for pending request")
            when (val method = signingRequest.signingMethod) {
                is fi.darklake.wallet.crypto.SigningMethod.SeedVault -> {
                    println("SendTokenScreen: Launching Seed Vault intent")
                    // Launch the signing intent for Seed Vault
                    signingLauncher.launch(method.signingIntent)
                }
                is fi.darklake.wallet.crypto.SigningMethod.Local -> {
                    // This shouldn't happen for local wallets as they sign immediately
                    println("SendTokenScreen: Unexpected local wallet in pending signing")
                }
            }
        }
    }

    // Set the token mint in the view model if provided
    LaunchedEffect(tokenMint) {
        tokenMint?.let { viewModel.setTokenMint(it) }
    }
    
    // Determine if we're sending SOL or a token
    val isSol = tokenMint == null
    val tokenInfo = if (!isSol) uiState.selectedToken else null
    val tokenSymbol = if (!isSol) uiState.tokenSymbol ?: "TOKEN" else "SOL"
    val tokenName = if (!isSol) uiState.tokenName ?: "Unknown Token" else "SOLANA"
    val displayMint = if (isSol) "So11...1112" else tokenMint.let {
        "${it.take(4)}...${it.takeLast(4)}"
    }
    val balance = if (isSol) uiState.solBalance else (tokenInfo?.balance?.uiAmount ?: 0.0)
    
    // Handle success state - navigate back and trigger refresh
    LaunchedEffect(uiState.transactionSuccess) {
        if (uiState.transactionSuccess) {
            onSuccess?.invoke()
            onBack()
        }
    }
    
    BackgroundWithOverlay {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header with logo and back arrow
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            println("SendTokenScreen: Back button clicked")
                            onBack()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarklakePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        AppLogo(
                            logoResId = R.drawable.darklake_logo,
                            size = 40.dp,
                            contentDescription = "Darklake Logo",
                            tint = DarklakePrimary
                        )
                    }
                    
                    // Spacer to balance the layout
                    Spacer(modifier = Modifier.size(48.dp))
                }
                
                // Token Info Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Token details
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Token Logo
                        if (isSol) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "S",
                                    color = DarklakePrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            TokenIconByAddress(
                                tokenAddress = tokenMint,
                                size = 32.dp
                            )
                        }

                        Column {
                            Text(
                                text = tokenSymbol,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 28.sp,
                                    fontFamily = BitsumishiFontFamily
                                ),
                                color = DarklakePrimary
                            )
                            Text(
                                text = tokenName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 18.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = DarklakeTextSecondary
                            )
                        }
                    }
                    
                    // Contract address badge
                    Row(
                        modifier = Modifier
                            .background(
                                color = DarklakeCardBackground,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayMint,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                            color = DarklakePrimary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in explorer",
                            modifier = Modifier.size(10.dp),
                            tint = DarklakePrimary
                        )
                    }
                }
                
                // Balance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = DarklakeCardBackground
                    ),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "BALANCE",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 18.sp,
                                letterSpacing = 1.sp
                            ),
                            color = DarklakePrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = String.format(Locale.US, "%.4f", balance),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                letterSpacing = 2.sp
                            ),
                            color = DarklakePrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Send Form Section
                Column(
                    modifier = Modifier.padding(top = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Send $tokenSymbol",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 28.sp,
                            fontFamily = BitsumishiFontFamily
                        ),
                        color = DarklakePrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = DarklakeCardBackground
                        ),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Recipient address field
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "RECIPIENT ADDRESS",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 18.sp,
                                        letterSpacing = 1.sp
                                    ),
                                    color = DarklakePrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = uiState.recipientAddress,
                                    onValueChange = viewModel::updateRecipientAddress,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    isError = uiState.recipientAddressError != null,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = DarklakeTextSecondary,
                                        focusedBorderColor = DarklakePrimary,
                                        errorBorderColor = MaterialTheme.colorScheme.error,
                                        unfocusedContainerColor = DarklakeInputBackground,
                                        focusedContainerColor = DarklakeInputBackground
                                    ),
                                    shape = RoundedCornerShape(0.dp),
                                    supportingText = uiState.recipientAddressError?.let { error ->
                                        { Text(error, color = MaterialTheme.colorScheme.error) }
                                    }
                                )
                            }
                            
                            // Amount field
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AMOUNT ($tokenSymbol)",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 18.sp,
                                            letterSpacing = 1.sp
                                        ),
                                        color = DarklakePrimary
                                    )
                                    Text(
                                        text = "MAX",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 18.sp,
                                            letterSpacing = 1.sp
                                        ),
                                        color = DarklakePrimary,
                                        modifier = Modifier.clickable { viewModel.setMaxAmount() }
                                    )
                                }
                                OutlinedTextField(
                                    value = uiState.amountInput,
                                    onValueChange = viewModel::updateAmount,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    isError = uiState.amountError != null,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = DarklakeTextSecondary,
                                        focusedBorderColor = DarklakePrimary,
                                        errorBorderColor = MaterialTheme.colorScheme.error,
                                        unfocusedContainerColor = DarklakeInputBackground,
                                        focusedContainerColor = DarklakeInputBackground
                                    ),
                                    shape = RoundedCornerShape(0.dp),
                                    supportingText = uiState.amountError?.let { error ->
                                        { Text(error, color = MaterialTheme.colorScheme.error) }
                                    }
                                )
                            }
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
            }
            
            // Send button at bottom
            AppButton(
                text = if (uiState.isLoading) "BROADCASTING..." else "SEND $tokenSymbol",
                onClick = { 
                    if (isSol) viewModel.sendSol() else viewModel.sendToken()
                },
                enabled = uiState.canSend && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                isLoading = uiState.isLoading
            )
        }
    }
}