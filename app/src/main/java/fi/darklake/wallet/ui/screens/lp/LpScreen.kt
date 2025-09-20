package fi.darklake.wallet.ui.screens.lp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.R
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.*
import fi.darklake.wallet.ui.design.*
import fi.darklake.wallet.ui.subscreens.SlippageSettingsScreen
import fi.darklake.wallet.ui.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LpScreen(
    settingsManager: SettingsManager,
    storageManager: WalletStorageManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: LpViewModel = viewModel {
        LpViewModel(storageManager, settingsManager, context)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    var hasWallet by remember { mutableStateOf(false) }
    var walletAddress by remember { mutableStateOf("") }
    var showSlippageSettings by remember { mutableStateOf(false) }
    
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
        viewModel.loadPositions()
    }
    
    BackgroundWithOverlay {
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Logo and Wallet Address
            AppHeader(
                walletAddress = if (hasWallet) walletAddress else "Not connected"
            )
            
            // Liquidity Title and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Liquidity Title
                Text(
                    text = "LIQUIDITY",
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
                        onClick = { showSlippageSettings = true },
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
                            viewModel.refreshPoolDetails()
                            viewModel.loadPositions()
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
            
            // Liquidity Component Wrapper
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Liquidity Input Group - separate cards like Swap
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Token A Input Card
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
                            label = "TOKEN A",
                            token = uiState.tokenA,
                            amount = uiState.tokenAAmount,
                            balance = uiState.tokenABalance,
                            onAmountChange = { 
                                val filteredInput = FormatUtils.filterNumericInput(it)
                                viewModel.updateTokenAAmount(filteredInput)
                            },
                            onTokenSelect = { viewModel.showTokenSelection(TokenSelectionType.TOKEN_A) },
                            isReadOnly = false,
                            showInsufficientBalance = uiState.insufficientBalanceA
                        )
                    }
                    
                    // Add/Swap Icon Button (between cards)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
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
                                imageVector =  ImageVector.vectorResource(id = R.drawable.ic_plus),
                                contentDescription =
                                    "Add liquidity",
                                tint = DarklakeSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Token B Input Card
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
                            label = "TOKEN B",
                            token = uiState.tokenB,
                            amount = uiState.tokenBAmount,
                            balance = uiState.tokenBBalance,
                            onAmountChange = { 
                                val filteredInput = FormatUtils.filterNumericInput(it)
                                viewModel.updateTokenBAmount(filteredInput)
                            },
                            onTokenSelect = { viewModel.showTokenSelection(TokenSelectionType.TOKEN_B) },
                            isReadOnly = false,
                            showInsufficientBalance = uiState.insufficientBalanceB
                        )
                    }
                    
                    // Initial Price Input (for new pools only - when pool doesn't exist)
                    AnimatedVisibility(visible = uiState.poolDetails?.exists == false) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SET INITIAL PRICE",
                                style = TerminalTextStyle,
                                color = DarklakeTextSecondary,
                                fontSize = 12.sp
                            )
                            
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
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "1 ${uiState.tokenA?.symbol ?: "TOKEN A"} =",
                                        style = Typography.bodyMedium,
                                        color = DarklakeTextPrimary
                                    )
                                    
                                    BasicTextField(
                                        value = uiState.initialPrice,
                                        onValueChange = { 
                                            val filteredInput = FormatUtils.filterNumericInput(it)
                                            viewModel.updateInitialPrice(filteredInput) 
                                        },
                                        modifier = Modifier
                                            .width(120.dp)
                                            .background(
                                                color = DarklakeInputBackground,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        textStyle = Typography.bodyMedium.copy(
                                            color = DarklakeTextPrimary,
                                            textAlign = TextAlign.End
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal
                                        ),
                                        singleLine = true,
                                        decorationBox = { innerTextField ->
                                            Box(contentAlignment = Alignment.CenterEnd) {
                                                if (uiState.initialPrice.isEmpty()) {
                                                    Text(
                                                        text = "0.0",
                                                        style = Typography.bodyMedium,
                                                        color = DarklakeTextSecondary
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )
                                    
                                    Text(
                                        text = uiState.tokenB?.symbol ?: "TOKEN B",
                                        style = Typography.bodyMedium,
                                        color = DarklakeTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Button
                AppButton(
                    text = when {
                        !hasWallet -> "CONNECT WALLET"
                        uiState.insufficientBalanceA || uiState.insufficientBalanceB -> "INSUFFICIENT BALANCE"
                        uiState.tokenA == null || uiState.tokenB == null -> "SELECT TOKENS"
                        uiState.tokenAAmount.isEmpty() || uiState.tokenBAmount.isEmpty() -> "ENTER AMOUNTS"
                        uiState.isProcessing -> if (uiState.poolDetails?.exists == true) "ADDING..." else "CREATING..."
                        uiState.poolDetails?.exists == true -> "DEPOSIT"
                        else -> "CREATE POOL"
                    },
                    onClick = {
                        when {
                            !hasWallet -> {
                                // Handle wallet connection
                            }
                            uiState.poolDetails?.exists == true -> {
                                viewModel.addLiquidity()
                            }
                            else -> {
                                viewModel.createPool()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = when {
                        !hasWallet -> true
                        uiState.insufficientBalanceA || uiState.insufficientBalanceB -> false
                        uiState.tokenA == null || uiState.tokenB == null -> false
                        uiState.tokenAAmount.isEmpty() || uiState.tokenBAmount.isEmpty() -> false
                        uiState.isProcessing -> false
                        else -> true
                    },
                    isLoading = uiState.isProcessing
                )
                
                // Pool Details
                AnimatedVisibility(visible = uiState.poolDetails != null) {
                    uiState.poolDetails?.let { pool ->
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
                        
                        if (pool.exists) {
                            informationEntries.add(
                                InformationCardEntry(
                                    label = "Current Price",
                                    value = "${pool.currentPrice} ${uiState.tokenB?.symbol ?: ""} per ${uiState.tokenA?.symbol ?: ""}"
                                )
                            )
                            
                            if (uiState.tokenAAmount.isNotEmpty() && uiState.tokenBAmount.isNotEmpty()) {
                                informationEntries.add(
                                    InformationCardEntry(
                                        label = "Pool Share",
                                        value = "${String.format("%.2f", pool.poolShare)}%"
                                    )
                                )
                            }
                            
                            informationEntries.add(
                                InformationCardEntry(
                                    label = "APR",
                                    value = "${pool.apr}%"
                                )
                            )
                        }
                        
                        InformationCard(entries = informationEntries)
                    }
                }
                
                // Your Positions
                AnimatedVisibility(visible = uiState.hasLiquidityPositions) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "YOUR POSITIONS",
                            style = TerminalTextStyle,
                            color = DarklakeTextSecondary,
                            fontSize = 14.sp
                        )
                        
                        uiState.liquidityPositions.forEach { position ->
                            LiquidityPositionCard(
                                position = position,
                                onWithdraw = { viewModel.withdrawLiquidity(position) }
                            )
                        }
                    }
                }
                
                // Messages
                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    uiState.errorMessage?.let { error ->
                        ErrorMessageCard(
                            message = error,
                            onDismiss = { viewModel.clearError() }
                        )
                    }
                }
                
                AnimatedVisibility(visible = uiState.successMessage != null) {
                    uiState.successMessage?.let { message ->
                        InfoMessageCard(
                            message = message,
                            containerColor = DarklakePrimary.copy(alpha = 0.1f),
                            contentColor = DarklakePrimary
                        )
                    }
                }
            }
        }
    }
    
    // Token Selection Sheet
    if (uiState.showTokenSelection) {
        TokenSelectionSheet(
            tokens = uiState.availableTokens,
            selectedTokenAddress = when (uiState.tokenSelectionType) {
                TokenSelectionType.TOKEN_A -> uiState.tokenA?.address
                TokenSelectionType.TOKEN_B -> uiState.tokenB?.address
                null -> null
            },
            onTokenSelected = { token -> viewModel.selectToken(token) },
            onDismiss = { viewModel.hideTokenSelection() }
        )
    }
    
    // Slippage Settings Modal
    if (showSlippageSettings) {
        SlippageSettingsScreen(
            settingsManager = settingsManager,
            onBack = { showSlippageSettings = false }
        )
    }
}

@Composable
private fun LiquidityPositionCard(
    position: LiquidityPosition,
    onWithdraw: () -> Unit
) {
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
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${position.tokenA.symbol} / ${position.tokenB.symbol}",
                    style = TerminalTextStyle,
                    color = DarklakeTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${FormatUtils.formatBalance(position.lpTokenBalance)} LP",
                    style = TerminalTextStyle,
                    color = DarklakePrimary,
                    fontSize = 14.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = position.tokenA.symbol,
                        style = TerminalTextStyle,
                        color = DarklakeTextTertiary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = FormatUtils.formatBalance(position.amountA),
                        style = TerminalTextStyle,
                        color = DarklakeTextPrimary,
                        fontSize = 12.sp
                    )
                }

                Column {
                    Text(
                        text = position.tokenB.symbol,
                        style = TerminalTextStyle,
                        color = DarklakeTextTertiary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = FormatUtils.formatBalance(position.amountB),
                        style = TerminalTextStyle,
                        color = DarklakeTextPrimary,
                        fontSize = 12.sp
                    )
                }

                Column {
                    Text(
                        text = "POOL SHARE",
                        style = TerminalTextStyle,
                        color = DarklakeTextTertiary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${position.poolShare}%",
                        style = TerminalTextStyle,
                        color = DarklakeTextPrimary,
                        fontSize = 12.sp
                    )
                }
            }

            TextButton(
                onClick = onWithdraw,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = DarklakeError
                )
            ) {
                Text(
                    text = "WITHDRAW",
                    style = TerminalTextStyle,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewLpScreen() {
    DarklakeWalletTheme {
        val context = LocalContext.current
        val settingsManager = remember { SettingsManager(context) }
        val storageManager = remember { WalletStorageManager(context) }
        
        LpScreen(
            settingsManager = settingsManager,
            storageManager = storageManager
        )
    }
}