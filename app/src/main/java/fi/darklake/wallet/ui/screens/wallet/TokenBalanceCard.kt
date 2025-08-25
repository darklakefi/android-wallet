package fi.darklake.wallet.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.data.swap.models.TokenInfo
import fi.darklake.wallet.ui.components.TokenIcon
import fi.darklake.wallet.ui.components.TokenIconByAddress
import fi.darklake.wallet.ui.design.*

@Composable
fun TokenBalanceCard(
    tokenSymbol: String,
    tokenName: String,
    tokenAddress: String? = null,
    balance: String,
    balanceUsd: String? = null,
    iconText: String? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarklakeCardBackground)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Token Info (simplified)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Token Icon with metadata fetching
                if (!tokenAddress.isNullOrEmpty()) {
                    // Use address-based icon if available
                    TokenIconByAddress(
                        tokenAddress = tokenAddress,
                        size = 32.dp
                    )
                } else {
                    // Fall back to symbol-based icon
                    TokenIcon(
                        token = TokenInfo(
                            address = "",
                            symbol = tokenSymbol,
                            name = tokenName,
                            decimals = 0
                        ),
                        size = 32.dp
                    )
                }
                
                // Just Token Symbol
                Text(
                    text = tokenSymbol.uppercase(),
                    style = TokenSymbolStyle,
                    color = DarklakeTextSecondary
                )
            }
            
            // Right side - Balance (simplified)
            Text(
                text = balance,
                style = TokenBalanceStyle,
                color = DarklakeTextSecondary,
                textAlign = TextAlign.End
            )
        }
    }
}