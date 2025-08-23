package fi.darklake.wallet.ui.screens.lp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.R
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.TokenSelectionSheet
import fi.darklake.wallet.ui.components.ErrorMessageCard
import fi.darklake.wallet.ui.components.InfoMessageCard
import fi.darklake.wallet.ui.components.BackgroundWithOverlay

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
    
    BackgroundWithOverlay {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.lp_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.lp_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.lp_subtitle2),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Liquidity Form
        LiquidityFormCard(
            uiState = uiState,
            onTokenAAmountChange = viewModel::updateTokenAAmount,
            onTokenBAmountChange = viewModel::updateTokenBAmount,
            onTokenASelect = { viewModel.showTokenSelection(TokenSelectionType.TOKEN_A) },
            onTokenBSelect = { viewModel.showTokenSelection(TokenSelectionType.TOKEN_B) },
            onSwapTokens = viewModel::swapTokens,
            onAddLiquidity = viewModel::addLiquidity,
            onCreatePool = viewModel::createPool,
            onSlippageChange = viewModel::updateSlippage,
            onInitialPriceChange = viewModel::updateInitialPrice
        )
        
        // Your Liquidity Positions
        if (uiState.hasLiquidityPositions) {
            YourLiquidityCard(
                positions = uiState.liquidityPositions,
                onWithdrawLiquidity = viewModel::withdrawLiquidity
            )
        }
        
        // Error/Success Messages
        uiState.errorMessage?.let { message ->
            ErrorMessageCard(message = message)
        }
        
        uiState.successMessage?.let { message ->
            InfoMessageCard(message = message)
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