package fi.darklake.wallet.ui.screens.send

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fi.darklake.wallet.ui.components.TerminalCard
import fi.darklake.wallet.ui.design.*

@Composable
fun TokenInfoCard(
    tokenSymbol: String?,
    tokenName: String?,
    tokenBalance: String?,
    tokenImageUrl: String?,
    modifier: Modifier = Modifier
) {
    TerminalCard(
        title = "TOKEN_INFO",
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Token icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (tokenImageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(tokenImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = tokenName,
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = tokenSymbol?.take(1) ?: "T",
                        style = TerminalHeaderStyle,
                        color = NeonGreen
                    )
                }
            }
            
            Column {
                Text(
                    text = "[${tokenSymbol ?: "UNKNOWN"}]",
                    style = TerminalHeaderStyle,
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold
                )
                tokenName?.let { name ->
                    if (name != tokenSymbol) {
                        Text(
                            text = name,
                            style = TerminalTextStyle,
                            color = TerminalGray.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = "BALANCE: ${tokenBalance ?: "0"}",
                    style = TerminalTextStyle,
                    color = TerminalGray
                )
            }
        }
    }
}