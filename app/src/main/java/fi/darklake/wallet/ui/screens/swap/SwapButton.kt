package fi.darklake.wallet.ui.screens.swap

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.R

@Composable
internal fun SwapButton(
    uiState: SwapUiState,
    onSwap: () -> Unit,
    onReset: () -> Unit
) {
    val buttonText = when (uiState.swapStep) {
        SwapStep.IDLE -> {
            when {
                uiState.tokenAAmount.isEmpty() -> stringResource(R.string.swap_button_enter_amount)
                uiState.insufficientBalance -> stringResource(R.string.swap_button_insufficient, uiState.tokenA?.symbol ?: "")
                !uiState.poolExists -> stringResource(R.string.swap_button_no_pool)
                uiState.isLoadingQuote -> stringResource(R.string.swap_button_loading)
                uiState.priceImpactWarning -> stringResource(R.string.swap_button_high_impact)
                else -> stringResource(R.string.swap_button_swap)
            }
        }
        SwapStep.GENERATING_PROOF -> stringResource(R.string.swap_generating_proof)
        SwapStep.CONFIRM_TRANSACTION -> stringResource(R.string.swap_confirm_wallet)
        SwapStep.PROCESSING -> stringResource(R.string.swap_processing)
        SwapStep.COMPLETED -> stringResource(R.string.swap_completed)
        SwapStep.FAILED -> stringResource(R.string.swap_failed)
    }
    
    val isEnabled = when (uiState.swapStep) {
        SwapStep.IDLE -> {
            !uiState.insufficientBalance &&
            uiState.tokenAAmount.isNotEmpty() &&
            uiState.tokenA != null &&
            uiState.tokenB != null &&
            uiState.poolExists &&
            !uiState.isLoadingQuote
        }
        SwapStep.COMPLETED, SwapStep.FAILED -> true
        else -> false
    }
    
    Button(
        onClick = {
            when (uiState.swapStep) {
                SwapStep.IDLE -> onSwap()
                SwapStep.COMPLETED, SwapStep.FAILED -> onReset()
                else -> {}
            }
        },
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = when (uiState.swapStep) {
                SwapStep.COMPLETED -> Color(0xFF4CAF50)
                SwapStep.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
        )
    ) {
        if (uiState.isSwapping && uiState.swapStep != SwapStep.COMPLETED && uiState.swapStep != SwapStep.FAILED) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(buttonText)
    }
}