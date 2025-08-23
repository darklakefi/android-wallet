package fi.darklake.wallet.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.data.model.DisplayNft
import fi.darklake.wallet.ui.design.*

@Composable
internal fun NftsGrid(
    nfts: List<DisplayNft>,
    isLoading: Boolean,
    onNftClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarklakeCardBackground)
            .padding(12.dp)
    ) {
        if (nfts.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No NFTs found",
                    style = TerminalTextStyle,
                    color = DarklakeTextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(nfts) { nft ->
                    NftCard(
                        nft = nft,
                        onClick = { onNftClick(nft.mint) }
                    )
                }
            }
        }
    }
}