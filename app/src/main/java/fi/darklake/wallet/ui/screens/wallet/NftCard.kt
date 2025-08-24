package fi.darklake.wallet.ui.screens.wallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.data.model.DisplayNft
import fi.darklake.wallet.ui.design.*

@Composable
internal fun NftCard(
    nft: DisplayNft,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = DarklakeCardBackground
        ),
        shape = RoundedCornerShape(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = nft.name.take(20),
                    style = NftTitleStyle,
                    color = DarklakeTextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (nft.compressed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "COMPRESSED",
                        style = CompressedLabelStyle,
                        color = DarklakeTextTertiary
                    )
                }
            }
        }
    }
}