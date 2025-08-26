package fi.darklake.wallet.ui.screens.swap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import fi.darklake.wallet.R
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.*
import fi.darklake.wallet.ui.design.*
import fi.darklake.wallet.ui.utils.FormatUtils

@Composable
fun SwapScreen(
    storageManager: WalletStorageManager,
    settingsManager: SettingsManager,
    onNavigateToSlippageSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: SwapViewModel = viewModel {
        SwapViewModel(storageManager, settingsManager, context)
    }
    val uiState by viewModel.uiState.collectAsState()
    
    var hasWallet by remember { mutableStateOf(false) }
    var walletAddress by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        hasWallet = storageManager.hasWallet()
        if (hasWallet) {
            val result = storageManager.getWallet()
            if (result.isSuccess) {
                result.getOrNull()?.let { wallet ->
                    walletAddress = wallet.publicKey
                }
            }
        }
        viewModel.loadTokens()
    }
    
    BackgroundWithOverlay {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Logo and Wallet Address
            AppHeader(
                walletAddress = if (hasWallet) walletAddress else "Not connected"
            )
            
            // Swap Title and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Swap Title
                Text(
                    text = "SWAP",
                    style = Typography.headlineMedium.copy(
                        fontFamily = BitsumishiFontFamily,
                        fontSize = 28.sp
                    ),
                    color = DarklakeTextSecondary
                )
                
                // Action buttons (Settings and Refresh)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Settings button
                    IconButton(
                        onClick = onNavigateToSlippageSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = DarklakeCardBackgroundAlt,
                                shape = RoundedCornerShape(4.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = DarklakeTextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Refresh button
                    IconButton(
                        onClick = { 
                            if (uiState.tokenA != null && uiState.tokenB != null && uiState.tokenAAmount.isNotEmpty()) {
                                viewModel.refreshQuote()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = DarklakeCardBackgroundAlt,
                                shape = RoundedCornerShape(4.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = DarklakeTextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Swap Component Wrapper
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Swap Input Group - separate cards for FROM and TO
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // From Token Section (Selling)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = DarklakeCardBackground,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = DarklakeBorder,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        TokenAmountInput(
                            label = "SELLING",
                            token = uiState.tokenA,
                            amount = uiState.tokenAAmount,
                            balance = uiState.tokenABalance,
                            onAmountChange = { 
                                val filteredInput = FormatUtils.filterNumericInput(it)
                                viewModel.updateTokenAAmount(filteredInput) 
                            },
                            onTokenSelect = { viewModel.showTokenSelection(TokenSelectionType.TOKEN_A) },
                            isReadOnly = false,
                            showInsufficientBalance = uiState.insufficientBalance
                        )
                    }
                    
                    // Swap Direction Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp, 32.dp)
                                .background(
                                    color = Color(0xFF03160B),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = DarklakeCardBackground,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { viewModel.swapTokens() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_swap_arrows),
                                contentDescription = "Swap tokens",
                                tint = DarklakePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // To Token Section (Buying)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = DarklakeCardBackground,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = DarklakeBorder,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        TokenAmountInput(
                            label = "BUYING",
                            token = uiState.tokenB,
                            amount = uiState.tokenBAmount,
                            balance = uiState.tokenBBalance,
                            onAmountChange = { },
                            onTokenSelect = { viewModel.showTokenSelection(TokenSelectionType.TOKEN_B) },
                            isReadOnly = true,
                            showInsufficientBalance = false
                        )
                    }
                }
                
                // Swap Button
                AppButton(
                    text = when {
                        !hasWallet -> "CONNECT WALLET"
                        uiState.insufficientBalance -> "INSUFFICIENT BALANCE"
                        uiState.tokenA == null || uiState.tokenB == null -> "SELECT TOKENS"
                        uiState.tokenAAmount.isEmpty() || uiState.tokenAAmount == "0" -> "ENTER AMOUNT"
                        uiState.isSwapping -> "SWAPPING..."
                        uiState.isLoadingQuote -> "FETCHING QUOTE..."
                        else -> "SWAP"
                    },
                    onClick = {
                        when {
                            !hasWallet -> {
                                Toast.makeText(context, "Please create or import a wallet first", Toast.LENGTH_SHORT).show()
                            }
                            !uiState.insufficientBalance && 
                            uiState.tokenA != null && 
                            uiState.tokenB != null &&
                            uiState.tokenAAmount.isNotEmpty() &&
                            uiState.tokenAAmount != "0" &&
                            !uiState.isSwapping &&
                            !uiState.isLoadingQuote -> {
                                viewModel.executeSwap()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = when {
                        !hasWallet -> true
                        uiState.insufficientBalance -> false
                        uiState.tokenA == null || uiState.tokenB == null -> false
                        uiState.tokenAAmount.isEmpty() || uiState.tokenAAmount == "0" -> false
                        uiState.isSwapping || uiState.isLoadingQuote -> false
                        else -> true
                    },
                    isLoading = uiState.isSwapping || uiState.isLoadingQuote
                )
                
                // Swap Details
                AnimatedVisibility(visible = uiState.quote != null || uiState.slippagePercent > 0) {
                    val informationEntries = mutableListOf(
                        InformationCardEntry(
                            label = "Slippage Tolerance",
                            value = "${uiState.slippagePercent}%",
                            isClickable = true,
                            onClick = {
                                val nextSlippage = when (uiState.slippagePercent) {
                                    0.5 -> 1.0
                                    1.0 -> 2.0
                                    2.0 -> 0.5
                                    else -> 0.5
                                }
                                viewModel.updateSlippage(nextSlippage, false)
                            }
                        )
                    )
                    
                    uiState.quote?.let { quote ->
                        informationEntries.add(
                            InformationCardEntry(
                                label = "Rate",
                                value = "1 ${uiState.tokenA?.symbol ?: ""} = ${quote.rate} ${uiState.tokenB?.symbol ?: ""}"
                            )
                        )
                        
                        if (quote.priceImpactPercentage > 0) {
                            informationEntries.add(
                                InformationCardEntry(
                                    label = "Price Impact",
                                    value = "${String.format("%.2f", quote.priceImpactPercentage)}%"
                                )
                            )
                        }
                        
                        informationEntries.add(
                            InformationCardEntry(
                                label = "Network Fee",
                                value = "${quote.estimatedFee} SOL"
                            )
                        )
                        
                        if (quote.routePlan.isNotEmpty()) {
                            informationEntries.add(
                                InformationCardEntry(
                                    label = "Route",
                                    value = "${quote.routePlan.size} hop(s)"
                                )
                            )
                        }
                    }
                    
                    InformationCard(entries = informationEntries)
                }
                
                // Warning Messages
                AnimatedVisibility(visible = uiState.priceImpactWarning) {
                    InfoMessageCard(
                        message = "High price impact! Consider reducing the swap amount.",
                        containerColor = DarklakeWarning.copy(alpha = 0.1f),
                        contentColor = DarklakeWarning
                    )
                }
                
                // Error Messages
                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    uiState.errorMessage?.let { error ->
                        ErrorMessageCard(
                            message = error,
                            onDismiss = { viewModel.clearError() }
                        )
                    }
                }
                
                // Success Message
                AnimatedVisibility(visible = uiState.successMessage != null) {
                    uiState.successMessage?.let { message ->
                        SuccessMessageCard(
                            message = message,
                            onDismiss = { viewModel.clearSuccess() }
                        )
                    }
                }
            }
            
            // Token Selection Sheet
            if (uiState.showTokenSelection) {
                TokenSelectionSheet(
                    tokens = uiState.availableTokens,
                    selectedTokenAddress = when(uiState.tokenSelectionType) {
                        TokenSelectionType.TOKEN_A -> uiState.tokenA?.address
                        TokenSelectionType.TOKEN_B -> uiState.tokenB?.address
                        else -> null
                    },
                    onTokenSelected = { token ->
                        viewModel.selectToken(token)
                    },
                    onDismiss = { viewModel.hideTokenSelection() }
                )
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewSwapScreen() {
    DarklakeWalletTheme {
        val context = LocalContext.current
        val settingsManager = remember { SettingsManager(context) }
        val storageManager = remember { WalletStorageManager(context) }
        
        SwapScreen(
            storageManager = storageManager,
            settingsManager = settingsManager,
            onNavigateToSlippageSettings = {}
        )
    }
}