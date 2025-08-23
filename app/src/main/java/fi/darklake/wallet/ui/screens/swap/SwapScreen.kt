package fi.darklake.wallet.ui.screens.swap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import fi.darklake.wallet.R
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.*
import fi.darklake.wallet.ui.design.*
import fi.darklake.wallet.ui.utils.FormatUtils
import fi.darklake.wallet.data.swap.models.TokenInfo
import kotlinx.coroutines.launch

@Composable
fun SwapScreen(
    storageManager: WalletStorageManager,
    settingsManager: SettingsManager,
    onNavigateToSlippageSettings: () -> Unit = {}
) {
    val viewModel: SwapViewModel = viewModel {
        SwapViewModel(storageManager, settingsManager)
    }
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var hasWallet by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        hasWallet = storageManager.hasWallet()
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
            // Header with title and action buttons
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
                // Swap Input Group
                TerminalCard(
                    modifier = Modifier.fillMaxWidth(),
                    glowEffect = uiState.isLoadingQuote || uiState.isSwapping
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // From Token Section
                        SwapTokenInput(
                            label = "FROM",
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
                        
                        // Swap Direction Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { viewModel.swapTokens() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = DarklakeCardBackground,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Swap tokens",
                                    tint = DarklakePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        // To Token Section
                        SwapTokenInput(
                            label = "TO",
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = DarklakeCardBackground,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Slippage
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Slippage Tolerance",
                                    style = TerminalTextStyle,
                                    color = DarklakeTextTertiary,
                                    fontSize = 12.sp
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.clickable {
                                        val nextSlippage = when (uiState.slippagePercent) {
                                            0.5 -> 1.0
                                            1.0 -> 2.0
                                            2.0 -> 0.5
                                            else -> 0.5
                                        }
                                        viewModel.updateSlippage(nextSlippage, false)
                                    }
                                ) {
                                    Text(
                                        text = "${uiState.slippagePercent}%",
                                        style = TerminalTextStyle,
                                        color = DarklakeTextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            
                            // Quote details
                            uiState.quote?.let { quote ->
                                // Exchange Rate
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Rate",
                                        style = TerminalTextStyle,
                                        color = DarklakeTextTertiary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "1 ${uiState.tokenA?.symbol ?: ""} = ${quote.rate} ${uiState.tokenB?.symbol ?: ""}",
                                        style = TerminalTextStyle,
                                        color = DarklakeTextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                                
                                // Price Impact
                                if (quote.priceImpactPercentage > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Price Impact",
                                            style = TerminalTextStyle,
                                            color = DarklakeTextTertiary,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "${String.format("%.2f", quote.priceImpactPercentage)}%",
                                            style = TerminalTextStyle,
                                            color = when {
                                                quote.priceImpactPercentage < 1 -> SwapPriceImpactLow
                                                quote.priceImpactPercentage < 5 -> SwapPriceImpactMedium
                                                else -> SwapPriceImpactHigh
                                            },
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                
                                // Network Fee
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Network Fee",
                                        style = TerminalTextStyle,
                                        color = DarklakeTextTertiary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${quote.estimatedFee} SOL",
                                        style = TerminalTextStyle,
                                        color = DarklakeTextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                                
                                // Route
                                if (quote.routePlan.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Route",
                                            style = TerminalTextStyle,
                                            color = DarklakeTextTertiary,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "${quote.routePlan.size} hop(s)",
                                            style = TerminalTextStyle,
                                            color = DarklakeTextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
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

@Composable
private fun SwapTokenInput(
    label: String,
    token: TokenInfo?,
    amount: String,
    balance: java.math.BigDecimal,
    onAmountChange: (String) -> Unit,
    onTokenSelect: () -> Unit,
    isReadOnly: Boolean,
    showInsufficientBalance: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label
        Text(
            text = label,
            style = TerminalTextStyle,
            color = DarklakeTextTertiary,
            fontSize = 10.sp
        )
        
        // Input Row
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
                    .padding(horizontal = 12.dp, vertical = 8.dp)
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
            Column(
                modifier = Modifier.weight(1f)
            ) {
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
                    modifier = Modifier.fillMaxWidth(),
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
                
                // Balance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Balance: ${FormatUtils.formatBalance(balance)}",
                        style = TerminalTextStyle,
                        color = if (showInsufficientBalance)
                            DarklakeError
                        else DarklakeTextTertiary,
                        fontSize = 10.sp
                    )
                    
                    if (!isReadOnly && balance > java.math.BigDecimal.ZERO) {
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
                }
            }
        }
    }
}