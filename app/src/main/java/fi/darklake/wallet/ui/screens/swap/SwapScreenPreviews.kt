package fi.darklake.wallet.ui.screens.swap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.data.swap.models.SwapQuoteResponse
import fi.darklake.wallet.data.swap.models.TokenInfo
import fi.darklake.wallet.ui.design.DarklakeBackground
import java.math.BigDecimal

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewSwapHeader() {
    SwapHeader(
        onSettingsClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewTokenInputSection() {
    val mockToken = TokenInfo(
        address = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
        symbol = "USDC",
        name = "USD Coin",
        decimals = 6,
        logoURI = null
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarklakeBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // From section with token selected
        TokenInputSection(
            label = "From",
            token = mockToken,
            amount = "100.50",
            balance = BigDecimal("1234.56"),
            onAmountChange = {},
            onTokenSelect = {},
            isReadOnly = false,
            showInsufficientBalance = false
        )
        
        // To section without token
        TokenInputSection(
            label = "To",
            token = null,
            amount = "",
            balance = BigDecimal.ZERO,
            onAmountChange = {},
            onTokenSelect = {},
            isReadOnly = true,
            showInsufficientBalance = false
        )
        
        // With insufficient balance error
        TokenInputSection(
            label = "From",
            token = mockToken,
            amount = "5000",
            balance = BigDecimal("1234.56"),
            onAmountChange = {},
            onTokenSelect = {},
            isReadOnly = false,
            showInsufficientBalance = true
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewSlippageSettings() {
    var slippage by remember { mutableStateOf(0.5) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarklakeBackground)
            .padding(16.dp)
    ) {
        SlippageSettings(
            slippagePercent = slippage,
            onSlippageChange = { newSlippage, _ ->
                slippage = newSlippage
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewQuoteDetails() {
    val mockQuote = SwapQuoteResponse(
        amountIn = 100.0,
        amountInRaw = "100000000",
        amountOut = 98.5,
        amountOutRaw = "98500000",
        estimatedFee = 0.002,
        estimatedFeesUsd = 0.25,
        isXtoY = true,
        priceImpactPercentage = 2.5,
        rate = 0.985,
        routePlan = emptyList(),
        slippage = 0.5,
        tokenX = null,
        tokenXMint = "So11111111111111111111111111111111111111112",
        tokenY = null,
        tokenYMint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
    )
    
    val tokenA = TokenInfo(
        address = "So11111111111111111111111111111111111111112",
        symbol = "SOL",
        name = "Solana",
        decimals = 9,
        logoURI = null
    )
    
    val tokenB = TokenInfo(
        address = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
        symbol = "USDC",
        name = "USD Coin",
        decimals = 6,
        logoURI = null
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarklakeBackground)
            .padding(16.dp)
    ) {
        QuoteDetails(
            quote = mockQuote,
            tokenA = tokenA,
            tokenB = tokenB
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewSwapButtonIdle() {
    val mockToken = TokenInfo(
        address = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
        symbol = "USDC",
        name = "USD Coin",
        decimals = 6,
        logoURI = null
    )
    
    val uiState = SwapUiState(
        swapStep = SwapStep.IDLE,
        tokenA = mockToken,
        tokenB = mockToken,
        tokenAAmount = "100",
        poolExists = true,
        insufficientBalance = false,
        isLoadingQuote = false,
        priceImpactWarning = false
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarklakeBackground)
            .padding(16.dp)
    ) {
        SwapButton(
            uiState = uiState,
            onSwap = {},
            onReset = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewSwapButtonProcessing() {
    val mockToken = TokenInfo(
        address = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
        symbol = "USDC",
        name = "USD Coin",
        decimals = 6,
        logoURI = null
    )
    
    val uiState = SwapUiState(
        swapStep = SwapStep.PROCESSING,
        tokenA = mockToken,
        tokenB = mockToken,
        tokenAAmount = "100",
        poolExists = true,
        isSwapping = true
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarklakeBackground)
            .padding(16.dp)
    ) {
        SwapButton(
            uiState = uiState,
            onSwap = {},
            onReset = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewSwapButtonCompleted() {
    val mockToken = TokenInfo(
        address = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
        symbol = "USDC",
        name = "USD Coin",
        decimals = 6,
        logoURI = null
    )
    
    val uiState = SwapUiState(
        swapStep = SwapStep.COMPLETED,
        tokenA = mockToken,
        tokenB = mockToken,
        tokenAAmount = "100",
        poolExists = true
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarklakeBackground)
            .padding(16.dp)
    ) {
        SwapButton(
            uiState = uiState,
            onSwap = {},
            onReset = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewSwapButtonInsufficientBalance() {
    val mockToken = TokenInfo(
        address = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
        symbol = "USDC",
        name = "USD Coin",
        decimals = 6,
        logoURI = null
    )
    
    val uiState = SwapUiState(
        swapStep = SwapStep.IDLE,
        tokenA = mockToken,
        tokenB = mockToken,
        tokenAAmount = "5000",
        poolExists = true,
        insufficientBalance = true
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarklakeBackground)
            .padding(16.dp)
    ) {
        SwapButton(
            uiState = uiState,
            onSwap = {},
            onReset = {}
        )
    }
}