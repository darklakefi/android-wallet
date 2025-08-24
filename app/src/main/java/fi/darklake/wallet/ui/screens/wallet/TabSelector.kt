package fi.darklake.wallet.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.ui.design.*

@Composable
internal fun TabSelector(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarklakeCardBackgroundAlt)
    ) {
        // Tokens Tab
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onTabSelected(0) }
                .background(if (selectedTab == 0) DarklakeTabActive else Color.Transparent)
                .padding(vertical = 8.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "[ TOKENS ]",
                style = TabTextStyle,
                color = if (selectedTab == 0) DarklakeTabTextActive else DarklakeTabTextInactive
            )
        }
        
        // NFTs Tab
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onTabSelected(1) }
                .background(if (selectedTab == 1) DarklakeTabActive else Color.Transparent)
                .padding(vertical = 8.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "[ NFTS ]",
                style = TabTextStyle,
                color = if (selectedTab == 1) DarklakeTabTextActive else DarklakeTabTextInactive
            )
        }
    }
}