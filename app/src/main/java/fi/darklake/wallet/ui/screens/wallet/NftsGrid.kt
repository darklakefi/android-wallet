package fi.darklake.wallet.ui.screens.wallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.R
import fi.darklake.wallet.data.model.DisplayNft
import fi.darklake.wallet.ui.design.*

@Composable
internal fun NftsGrid(
    nfts: List<DisplayNft>,
    isLoading: Boolean,
    onNftClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 8px gap from tabs
        Spacer(modifier = Modifier.height(8.dp))
        
        if (nfts.isEmpty() && !isLoading) {
            // Empty state with custom graphic
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Empty state image
                    Image(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_empty_nft_state),
                        contentDescription = "No NFTs",
                        modifier = Modifier.size(80.dp),
                        colorFilter = ColorFilter.tint(DarklakeTertiary)
                    )
                    Text(
                        text = "NO NFTS YET",
                        style = TerminalTextStyle,
                        color = DarklakeTextTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
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