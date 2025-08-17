package fi.darklake.wallet.data.swap.models

data class Token(
    val address: String,
    val symbol: String,
    val name: String,
    val decimals: Int,
    val imageUrl: String? = null
)

// Mock token data based on dex-web patterns
object TokenData {
    
    fun getTokensForNetwork(isMainnet: Boolean): List<Token> {
        return if (isMainnet) {
            mainnetTokens
        } else {
            devnetTokens
        }
    }
    
    private val mainnetTokens = listOf(
        Token(
            address = "So11111111111111111111111111111111111111112",
            symbol = "SOL",
            name = "Solana",
            decimals = 9,
            imageUrl = null
        ),
        Token(
            address = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            symbol = "USDC",
            name = "USD Coin",
            decimals = 6,
            imageUrl = null
        ),
        Token(
            address = "9BB6NFEcjBCtnNLFko2FqVQBq8HHM13kCyYcdQbgpump",
            symbol = "Fartcoin",
            name = "Fartcoin",
            decimals = 6,
            imageUrl = null
        ),
        Token(
            address = "DdLxrGFs2sKYbbqVk76eVx9268ASUdTMAhrsqphqDuX",
            symbol = "DuX",
            name = "DuX",
            decimals = 6,
            imageUrl = null
        )
    )
    
    private val devnetTokens = listOf(
        Token(
            address = "So11111111111111111111111111111111111111112",
            symbol = "SOL",
            name = "Solana",
            decimals = 9,
            imageUrl = null
        ),
        Token(
            address = "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU",
            symbol = "USDC",
            name = "USD Coin (Dev)",
            decimals = 6,
            imageUrl = null
        ),
        Token(
            address = "DdLxrGFs2sKYbbqVk76eVx9268ASUdTMAhrsqphqDuX",
            symbol = "DuX",
            name = "DuX",
            decimals = 6,
            imageUrl = null
        ),
        Token(
            address = "HXsKnhXPtGr2mq4uTpxbxyy7ZydYWJwx4zMuYPEDukY",
            symbol = "DukY",
            name = "DukY",
            decimals = 9,
            imageUrl = null
        )
    )
}