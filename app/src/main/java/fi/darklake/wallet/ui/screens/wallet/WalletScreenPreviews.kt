package fi.darklake.wallet.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.data.model.DisplayToken
import fi.darklake.wallet.data.model.DisplayNft
import fi.darklake.wallet.ui.design.DarklakeBackground

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewTabSelector() {
    TabSelector(
        selectedTab = 0,
        onTabSelected = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewTokensList() {
    val mockTokens = listOf(
        DisplayToken(
            mint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            symbol = "USDC",
            name = "USD Coin",
            balance = "123,333.12",
            imageUrl = null
        ),
        DisplayToken(
            mint = "So11111111111111111111111111111111111111112",
            symbol = "SOL",
            name = "Solana",
            balance = "456.78",
            imageUrl = null
        ),
        DisplayToken(
            mint = "DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB263",
            symbol = "BONK",
            name = "Bonk",
            balance = "999,999,999.00",
            imageUrl = null
        )
    )
    
    TokensList(
        tokens = mockTokens,
        isLoading = false,
        onTokenClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewNftsGrid() {
    val mockNfts = listOf(
        DisplayNft(
            mint = "nft1",
            name = "Cool NFT #1234",
            imageUrl = null,
            collectionName = "Cool Collection",
            compressed = false
        ),
        DisplayNft(
            mint = "nft2",
            name = "Compressed NFT #5678 with a really long name",
            imageUrl = null,
            collectionName = "Compressed Collection",
            compressed = true
        ),
        DisplayNft(
            mint = "nft3",
            name = "Art NFT #999",
            imageUrl = null,
            collectionName = "Art Collection",
            compressed = false
        ),
        DisplayNft(
            mint = "nft4",
            name = "Gaming NFT",
            imageUrl = null,
            collectionName = "Gaming Collection",
            compressed = true
        )
    )
    
    NftsGrid(
        nfts = mockNfts,
        isLoading = false,
        onNftClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewNftCard() {
    val mockNft = DisplayNft(
        mint = "mock123",
        name = "Cool NFT #1234",
        imageUrl = null,
        collectionName = "Cool Collection",
        compressed = false
    )
    
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(DarklakeBackground)
            .padding(16.dp)
    ) {
        NftCard(nft = mockNft, onClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewNftCardCompressed() {
    val mockNft = DisplayNft(
        mint = "mock456",
        name = "Compressed NFT with Long Name",
        imageUrl = null,
        collectionName = "Compressed Collection",
        compressed = true
    )
    
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(DarklakeBackground)
            .padding(16.dp)
    ) {
        NftCard(nft = mockNft, onClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewMainBalanceCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarklakeBackground)
            .padding(16.dp)
    ) {
        MainBalanceCard(
            balance = "123,553.12 SOL",
            onReceiveClick = {},
            onSendClick = {},
            onRefreshClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewTokenBalanceCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarklakeBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TokenBalanceCard(
            tokenSymbol = "USDC",
            tokenName = "USD Coin",
            tokenAddress = null,
            balance = "123,333.12",
            balanceUsd = null,
            onClick = {}
        )
        TokenBalanceCard(
            tokenSymbol = "SOL",
            tokenName = "Solana",
            tokenAddress = null,
            balance = "456.78",
            balanceUsd = null,
            onClick = {}
        )
        TokenBalanceCard(
            tokenSymbol = "BONK",
            tokenName = "Bonk",
            tokenAddress = null,
            balance = "999,999,999.00",
            balanceUsd = null,
            onClick = {}
        )
    }
}