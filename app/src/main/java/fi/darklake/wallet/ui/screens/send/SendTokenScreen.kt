package fi.darklake.wallet.ui.screens.send

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.AppButton
import fi.darklake.wallet.ui.components.AppLogo
import fi.darklake.wallet.ui.components.BackgroundWithOverlay
import fi.darklake.wallet.ui.design.*
import fi.darklake.wallet.R
import fi.darklake.wallet.ui.components.TokenIconByAddress
import fi.darklake.wallet.data.tokens.TokenMetadataService
import java.util.Locale
import androidx.compose.ui.platform.LocalContext

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
    
    // Set the token mint in the view model if provided
    LaunchedEffect(tokenMint) {
        tokenMint?.let { viewModel.setTokenMint(it) }
    }
    
    // Determine if we're sending SOL or a token
    val isSol = tokenMint == null
    val tokenInfo = if (!isSol) uiState.selectedToken else null
    val tokenSymbol = if (!isSol) uiState.tokenSymbol ?: "TOKEN" else "SOL"
    val tokenName = if (!isSol) uiState.tokenName ?: "Unknown Token" else "SOLANA"
    val displayMint = if (isSol) "So11...1112" else tokenMint?.let { 
        "${it.take(4)}...${it.takeLast(4)}"
    } ?: ""
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
                        } else if (tokenMint != null) {
                            TokenIconByAddress(
                                tokenAddress = tokenMint,
                                size = 32.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(DarklakeCardBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "?",
                                    color = DarklakeTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
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