package fi.darklake.wallet.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.data.model.DisplayToken
import fi.darklake.wallet.ui.design.*

@Composable
internal fun TokensList(
    tokens: List<DisplayToken>,
    isLoading: Boolean,
    onTokenClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 8px gap from tabs
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tokens) { token ->
                    TokenBalanceCard(
                        tokenSymbol = token.symbol,
                        tokenName = token.name,
                        tokenAddress = if (token.mint.length > 10) {
                            "${token.mint.take(4)}...${token.mint.takeLast(4)}"
                        } else null,
                        balance = token.balance,
                        balanceUsd = null,
                        onClick = { onTokenClick(token.mint) },
                        modifier = Modifier.fillMaxWidth()
                    )
            }
            
            if (tokens.isEmpty() && !isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tokens found",
                            style = TerminalTextStyle,
                            color = DarklakeTextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}