package fi.darklake.wallet.data.swap.repository

import fi.darklake.wallet.data.model.SolanaNetwork
import fi.darklake.wallet.data.swap.models.Token
import fi.darklake.wallet.data.swap.models.TokenData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TokenRepository {
    
    fun getTokens(
        network: SolanaNetwork,
        query: String = "",
        limit: Int = 50
    ): Flow<List<Token>> = flow {
        val isMainnet = network == SolanaNetwork.MAINNET
        val allTokens = TokenData.getTokensForNetwork(isMainnet)
        
        val filteredTokens = if (query.isBlank()) {
            allTokens
        } else {
            allTokens.filter { token ->
                token.symbol.contains(query, ignoreCase = true) ||
                token.name.contains(query, ignoreCase = true) ||
                token.address.contains(query, ignoreCase = true)
            }
        }
        
        emit(filteredTokens.take(limit))
    }
    
    fun getTokenByAddress(address: String, network: SolanaNetwork): Token? {
        val isMainnet = network == SolanaNetwork.MAINNET
        return TokenData.getTokensForNetwork(isMainnet)
            .find { it.address == address }
    }
    
    fun convertToTokenInfo(token: Token): fi.darklake.wallet.data.swap.models.TokenInfo {
        return fi.darklake.wallet.data.swap.models.TokenInfo(
            address = token.address,
            symbol = token.symbol,
            name = token.name,
            decimals = token.decimals
        )
    }
    
    fun convertFromTokenInfo(tokenInfo: fi.darklake.wallet.data.swap.models.TokenInfo): Token {
        return Token(
            address = tokenInfo.address,
            symbol = tokenInfo.symbol,
            name = tokenInfo.name,
            decimals = tokenInfo.decimals
        )
    }
}