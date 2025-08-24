package fi.darklake.wallet.ui.screens.lp

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.R
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.*
import fi.darklake.wallet.ui.design.*
import fi.darklake.wallet.ui.utils.FormatUtils
import fi.darklake.wallet.data.swap.models.TokenInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LpScreen(
    settingsManager: SettingsManager,
    storageManager: WalletStorageManager,
    modifier: Modifier = Modifier
) {
    val viewModel: LpViewModel = viewModel {
        LpViewModel(storageManager, settingsManager)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
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
                        onClick = { /* TODO: Open liquidity settings */ },
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
                // Liquidity Input Group
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = SurfaceContainer,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = DarklakeBorder,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Pool Status Indicator
                        uiState.poolDetails?.let { poolDetails ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (poolDetails.exists) "EXISTING POOL" else "NEW POOL",
                                    style = TerminalTextStyle,
                                    color = if (poolDetails.exists) DarklakePrimary else DarklakeWarning,
                                    fontSize = 10.sp
                                )
                                
                                if (poolDetails.exists) {
                                    Text(
                                        text = "TVL: $${FormatUtils.formatBalance(poolDetails.tvl)}",
                                        style = TerminalTextStyle,
                                        color = DarklakeTextTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        
                        // Token A Input
                        LiquidityTokenInput(
                            label = "TOKEN A",
                            token = uiState.tokenA,
                            amount = uiState.tokenAAmount,
                            balance = uiState.tokenABalance,
                            onAmountChange = { 
                                val filteredInput = FormatUtils.filterNumericInput(it)
                                viewModel.updateTokenAAmount(filteredInput)
                            },
                            onTokenSelect = { viewModel.showTokenSelection(TokenSelectionType.TOKEN_A) },
                            insufficientBalance = uiState.insufficientBalanceA
                        )
                        
                        // Add/Swap Icon
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
                                    imageVector = if (uiState.poolDetails?.exists == true) 
                                        Icons.Default.Add else Icons.Default.SwapVert,
                                    contentDescription = if (uiState.poolDetails?.exists == true) 
                                        "Add liquidity" else "Swap tokens",
                                    tint = DarklakePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        // Token B Input
                        LiquidityTokenInput(
                            label = "TOKEN B",
                            token = uiState.tokenB,
                            amount = uiState.tokenBAmount,
                            balance = uiState.tokenBBalance,
                            onAmountChange = { 
                                val filteredInput = FormatUtils.filterNumericInput(it)
                                viewModel.updateTokenBAmount(filteredInput)
                            },
                            onTokenSelect = { viewModel.showTokenSelection(TokenSelectionType.TOKEN_B) },
                            insufficientBalance = uiState.insufficientBalanceB
                        )
                        
                        // Initial Price Input (for new pools)
                        AnimatedVisibility(visible = uiState.poolDetails?.exists == false) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "INITIAL PRICE",
                                    style = TerminalTextStyle,
                                    color = DarklakeTextTertiary,
                                    fontSize = 10.sp
                                )
                                OutlinedTextField(
                                    value = uiState.initialPrice,
                                    onValueChange = { viewModel.updateInitialPrice(it) },
                                    placeholder = { 
                                        Text(
                                            "0.00",
                                            color = DarklakeTextTertiary.copy(alpha = 0.5f)
                                        )
                                    },
                                    textStyle = TerminalTextStyle.copy(
                                        fontSize = 14.sp,
                                        color = DarklakeTextPrimary
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DarklakePrimary,
                                        unfocusedBorderColor = DarklakeBorder,
                                        cursorColor = DarklakePrimary,
                                        focusedContainerColor = DarklakeInputBackground,
                                        unfocusedContainerColor = DarklakeInputBackground
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    supportingText = {
                                        Text(
                                            text = "Price of Token B per Token A",
                                            style = TerminalTextStyle,
                                            color = DarklakeTextTertiary,
                                            fontSize = 10.sp
                                        )
                                    }
                                )
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
                                // Slippage Tolerance
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
                                
                                if (pool.exists) {
                                    // Current Price
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Current Price",
                                            style = TerminalTextStyle,
                                            color = DarklakeTextTertiary,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "${pool.currentPrice} ${uiState.tokenB?.symbol ?: ""} per ${uiState.tokenA?.symbol ?: ""}",
                                            style = TerminalTextStyle,
                                            color = DarklakeTextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    
                                    // Pool Share
                                    if (uiState.tokenAAmount.isNotEmpty() && uiState.tokenBAmount.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Pool Share",
                                                style = TerminalTextStyle,
                                                color = DarklakeTextTertiary,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "${String.format("%.2f", pool.poolShare)}%",
                                                style = TerminalTextStyle,
                                                color = DarklakeTextPrimary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    
                                    // APR
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "APR",
                                            style = TerminalTextStyle,
                                            color = DarklakeTextTertiary,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "${pool.apr}%",
                                            style = TerminalTextStyle,
                                            color = DarklakePrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
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
}

@Composable
private fun LiquidityTokenInput(
    label: String,
    token: TokenInfo?,
    amount: String,
    balance: java.math.BigDecimal,
    onAmountChange: (String) -> Unit,
    onTokenSelect: () -> Unit,
    insufficientBalance: Boolean
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
                    isError = insufficientBalance,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (insufficientBalance) 
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
                        color = if (insufficientBalance)
                            DarklakeError
                        else DarklakeTextTertiary,
                        fontSize = 10.sp
                    )
                    
                    if (balance > java.math.BigDecimal.ZERO) {
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
                    text = "$${FormatUtils.formatBalance(position.totalValue)}",
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