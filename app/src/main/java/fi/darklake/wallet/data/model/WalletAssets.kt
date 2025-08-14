package fi.darklake.wallet.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WalletBalance(
    @SerialName("lamports")
    val lamports: Long,
    @SerialName("sol")
    val sol: Double
)

@Serializable
data class TokenBalance(
    @SerialName("mint")
    val mint: String,
    @SerialName("amount")
    val amount: String,
    @SerialName("decimals")
    val decimals: Int,
    @SerialName("uiAmount")
    val uiAmount: Double?,
    @SerialName("uiAmountString")
    val uiAmountString: String?
)

@Serializable
data class TokenMetadata(
    @SerialName("mint")
    val mint: String,
    @SerialName("name")
    val name: String?,
    @SerialName("symbol")
    val symbol: String?,
    @SerialName("description")
    val description: String?,
    @SerialName("image")
    val image: String?,
    @SerialName("decimals")
    val decimals: Int = 6
)

@Serializable
data class TokenInfo(
    @SerialName("balance")
    val balance: TokenBalance,
    @SerialName("metadata")
    val metadata: TokenMetadata?
)

@Serializable
data class NftMetadata(
    @SerialName("mint")
    val mint: String,
    @SerialName("name")
    val name: String?,
    @SerialName("symbol")
    val symbol: String?,
    @SerialName("description")
    val description: String?,
    @SerialName("image")
    val image: String?,
    @SerialName("external_url")
    val externalUrl: String?,
    @SerialName("attributes")
    val attributes: List<NftAttribute>? = null,
    @SerialName("collection")
    val collection: NftCollection? = null
)

@Serializable
data class NftAttribute(
    @SerialName("trait_type")
    val traitType: String,
    @SerialName("value")
    val value: String
)

@Serializable
data class NftCollection(
    @SerialName("name")
    val name: String?,
    @SerialName("family")
    val family: String?
)

@Serializable
data class HeliusBalanceResponse(
    @SerialName("jsonrpc")
    val jsonrpc: String,
    @SerialName("id")
    val id: String,
    @SerialName("result")
    val result: HeliusBalanceResult? = null,
    @SerialName("error")
    val error: HeliusError? = null
)

@Serializable
data class HeliusError(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String
)

@Serializable
data class HeliusBalanceResult(
    @SerialName("context")
    val context: HeliusContext,
    @SerialName("value")
    val value: Long
)

@Serializable
data class HeliusContext(
    @SerialName("slot")
    val slot: Long
)

@Serializable
data class HeliusTokenResponse(
    @SerialName("jsonrpc")
    val jsonrpc: String,
    @SerialName("id")
    val id: String,
    @SerialName("result")
    val result: HeliusTokenResult? = null,
    @SerialName("error")
    val error: HeliusError? = null
)

@Serializable
data class HeliusTokenResult(
    @SerialName("context")
    val context: HeliusContext,
    @SerialName("value")
    val value: List<HeliusTokenAccount>
)

@Serializable
data class HeliusTokenAccount(
    @SerialName("account")
    val account: HeliusTokenAccountData,
    @SerialName("pubkey")
    val pubkey: String
)

@Serializable
data class HeliusTokenAccountData(
    @SerialName("data")
    val data: HeliusTokenAccountInfo,
    @SerialName("executable")
    val executable: Boolean,
    @SerialName("lamports")
    val lamports: Long,
    @SerialName("owner")
    val owner: String,
    @SerialName("rentEpoch")
    val rentEpoch: Long
)

@Serializable
data class HeliusTokenAccountInfo(
    @SerialName("parsed")
    val parsed: HeliusTokenParsedData,
    @SerialName("program")
    val program: String,
    @SerialName("space")
    val space: Long
)

@Serializable
data class HeliusTokenParsedData(
    @SerialName("info")
    val info: HeliusTokenInfo,
    @SerialName("type")
    val type: String
)

@Serializable
data class HeliusTokenInfo(
    @SerialName("isNative")
    val isNative: Boolean,
    @SerialName("mint")
    val mint: String,
    @SerialName("owner")
    val owner: String,
    @SerialName("state")
    val state: String,
    @SerialName("tokenAmount")
    val tokenAmount: HeliusTokenAmount
)

@Serializable
data class HeliusTokenAmount(
    @SerialName("amount")
    val amount: String,
    @SerialName("decimals")
    val decimals: Int,
    @SerialName("uiAmount")
    val uiAmount: Double?,
    @SerialName("uiAmountString")
    val uiAmountString: String?
)

// Simplified models for UI
data class WalletAssets(
    val solBalance: Double,
    val tokens: List<TokenInfo>,
    val nfts: List<NftMetadata>
)

data class DisplayToken(
    val mint: String,
    val name: String,
    val symbol: String,
    val balance: String,
    val imageUrl: String?,
    val usdValue: Double? = null
)

data class DisplayNft(
    val mint: String,
    val name: String,
    val imageUrl: String?,
    val collectionName: String?
)