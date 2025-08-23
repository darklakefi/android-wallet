package fi.darklake.wallet.ui.screens.swap

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.R
import fi.darklake.wallet.data.swap.models.SwapQuoteResponse
import fi.darklake.wallet.data.swap.models.TokenInfo
import fi.darklake.wallet.ui.utils.FormatUtils

@Composable
internal fun QuoteDetails(
    quote: SwapQuoteResponse,
    tokenA: TokenInfo?,
    tokenB: TokenInfo?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.swap_rate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "1 ${tokenA?.symbol ?: ""} = ${FormatUtils.formatAmount(quote.rate)} ${tokenB?.symbol ?: ""}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Price Impact
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.swap_price_impact),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FormatUtils.formatPercentage(quote.priceImpactPercentage),
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        quote.priceImpactPercentage < 1 -> Color(0xFF4CAF50)
                        quote.priceImpactPercentage < 5 -> Color(0xFFFFA726)
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
            
            // Estimated Fee
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.swap_network_fee),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "~$${FormatUtils.formatAmount(quote.estimatedFeesUsd)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}