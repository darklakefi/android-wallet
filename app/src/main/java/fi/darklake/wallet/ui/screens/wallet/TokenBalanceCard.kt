package fi.darklake.wallet.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // Define token-specific colors
    val iconBackgroundColor = when (tokenSymbol.uppercase()) {
        "USDC" -> TokenUsdcBackground
        "SOL" -> TokenSolBackground
        "BONK" -> TokenBonkBackground
        else -> DarklakeTokenDefaultBg
    }
    
    val iconTextColor = when (tokenSymbol.uppercase()) {
        "USDC", "BONK" -> Color.White
        "SOL" -> TokenSolText
        else -> DarklakePrimary
    }
    
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
                // Token Icon Circle with specific colors
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconText ?: tokenSymbol.take(2).uppercase(),
                        color = iconTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BitsumishiFontFamily
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