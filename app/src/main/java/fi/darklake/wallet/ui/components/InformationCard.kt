package fi.darklake.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.darklake.wallet.ui.design.*

data class InformationCardEntry(
    val label: String,
    val value: String,
    val isClickable: Boolean = false,
    val onClick: (() -> Unit)? = null
)

@Composable
fun InformationCard(
    entries: List<InformationCardEntry>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
            entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (entry.isClickable && entry.onClick != null) {
                                Modifier.clickable { entry.onClick() }
                            } else {
                                Modifier
                            }
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.label,
                        style = TerminalTextStyle,
                        color = DarklakeTextTertiary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = entry.value,
                        style = TerminalTextStyle,
                        color = if (entry.isClickable) DarklakePrimary else DarklakeTextPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewInformationCard() {
    DarklakeWalletTheme {
        InformationCard(
            entries = listOf(
                InformationCardEntry(
                    label = "Slippage Tolerance",
                    value = "0.5%",
                    isClickable = true,
                    onClick = {}
                ),
                InformationCardEntry(
                    label = "Min. Received",
                    value = "99.5 USDC"
                ),
                InformationCardEntry(
                    label = "Network Fee",
                    value = "~0.00005 SOL"
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}