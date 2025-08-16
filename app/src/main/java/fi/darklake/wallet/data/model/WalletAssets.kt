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
    val rentEpoch: String? = null  // Use String to handle numeric overflow from ULong.MAX_VALUE
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

// Jupiter API token metadata response
@Serializable
data class JupiterToken(
    @SerialName("address")
    val address: String,
    @SerialName("chainId")
    val chainId: Int,
    @SerialName("decimals")
    val decimals: Int,
    @SerialName("name")
    val name: String,
    @SerialName("symbol")
    val symbol: String,
    @SerialName("logoURI")
    val logoURI: String?,
    @SerialName("tags")
    val tags: List<String>? = null
)

// Helius DAS API response models
@Serializable
data class HeliusDasResponse(
    @SerialName("jsonrpc")
    val jsonrpc: String,
    @SerialName("id")
    val id: String,
    @SerialName("result")
    val result: HeliusDasResult? = null,
    @SerialName("error")
    val error: HeliusError? = null
)

@Serializable
data class HeliusDasResult(
    @SerialName("total")
    val total: Int,
    @SerialName("page")
    val page: Int,
    @SerialName("limit")
    val limit: Int,
    @SerialName("items")
    val items: List<HeliusDasAsset>
)

@Serializable
data class HeliusDasAsset(
    @SerialName("interface")
    val `interface`: String,
    @SerialName("id")
    val id: String,
    @SerialName("content")
    val content: HeliusDasContent? = null,
    @SerialName("authorities")
    val authorities: List<HeliusDasAuthority>? = null,
    @SerialName("compression")
    val compression: HeliusDasCompression? = null,
    @SerialName("grouping")
    val grouping: List<HeliusDasGrouping>? = null,
    @SerialName("royalty")
    val royalty: HeliusDasRoyalty? = null,
    @SerialName("creators")
    val creators: List<HeliusDasCreator>? = null,
    @SerialName("ownership")
    val ownership: HeliusDasOwnership? = null,
    @SerialName("supply")
    val supply: HeliusDasSupply? = null,
    @SerialName("mutable")
    val mutable: Boolean? = null,
    @SerialName("burnt")
    val burnt: Boolean? = null
)

@Serializable
data class HeliusDasContent(
    @SerialName("schema")
    val schema: String? = null,
    @SerialName("json_uri")
    val jsonUri: String? = null,
    @SerialName("files")
    val files: List<HeliusDasFile>? = null,
    @SerialName("metadata")
    val metadata: HeliusDasMetadata? = null,
    @SerialName("links")
    val links: HeliusDasLinks? = null
)

@Serializable
data class HeliusDasFile(
    @SerialName("uri")
    val uri: String? = null,
    @SerialName("mime")
    val mime: String? = null,
    @SerialName("quality")
    val quality: HeliusDasQuality? = null
)

@Serializable
data class HeliusDasQuality(
    @SerialName("schema")
    val schema: String? = null
)

@Serializable
data class HeliusDasMetadata(
    @SerialName("attributes")
    val attributes: List<HeliusDasAttribute>? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("symbol")
    val symbol: String? = null,
    @SerialName("external_url")
    val externalUrl: String? = null
)

@Serializable
data class HeliusDasAttribute(
    @SerialName("value")
    val value: String? = null,
    @SerialName("trait_type")
    val traitType: String? = null
)

@Serializable
data class HeliusDasLinks(
    @SerialName("image")
    val image: String? = null,
    @SerialName("external_url")
    val externalUrl: String? = null
)

@Serializable
data class HeliusDasAuthority(
    @SerialName("address")
    val address: String,
    @SerialName("scopes")
    val scopes: List<String>
)

@Serializable
data class HeliusDasCompression(
    @SerialName("eligible")
    val eligible: Boolean,
    @SerialName("compressed")
    val compressed: Boolean,
    @SerialName("data_hash")
    val dataHash: String? = null,
    @SerialName("creator_hash")
    val creatorHash: String? = null,
    @SerialName("asset_hash")
    val assetHash: String? = null,
    @SerialName("tree")
    val tree: String? = null,
    @SerialName("seq")
    val seq: Long? = null,
    @SerialName("leaf_id")
    val leafId: Long? = null
)

@Serializable
data class HeliusDasGrouping(
    @SerialName("group_key")
    val groupKey: String,
    @SerialName("group_value")
    val groupValue: String
)

@Serializable
data class HeliusDasRoyalty(
    @SerialName("royalty_model")
    val royaltyModel: String,
    @SerialName("target")
    val target: String? = null,
    @SerialName("percent")
    val percent: Double,
    @SerialName("basis_points")
    val basisPoints: Int,
    @SerialName("primary_sale_happened")
    val primarySaleHappened: Boolean,
    @SerialName("locked")
    val locked: Boolean
)

@Serializable
data class HeliusDasCreator(
    @SerialName("address")
    val address: String,
    @SerialName("share")
    val share: Int,
    @SerialName("verified")
    val verified: Boolean
)

@Serializable
data class HeliusDasOwnership(
    @SerialName("frozen")
    val frozen: Boolean,
    @SerialName("delegated")
    val delegated: Boolean,
    @SerialName("delegate")
    val delegate: String? = null,
    @SerialName("ownership_model")
    val ownershipModel: String,
    @SerialName("owner")
    val owner: String
)

@Serializable
data class HeliusDasSupply(
    @SerialName("print_max_supply")
    val printMaxSupply: Long? = null,
    @SerialName("print_current_supply")
    val printCurrentSupply: Long? = null,
    @SerialName("edition_nonce")
    val editionNonce: Long? = null
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