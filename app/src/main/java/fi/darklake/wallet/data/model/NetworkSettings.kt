package fi.darklake.wallet.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkSettings(
    val network: SolanaNetwork = SolanaNetwork.DEVNET,
    val customRpcUrl: String? = null,
    val heliusApiKey: String? = null
)

enum class SolanaNetwork(
    val displayName: String,
    val rpcUrl: String
) {
    DEVNET("Devnet", "https://api.devnet.solana.com"),
    MAINNET("Mainnet", "https://api.mainnet-beta.solana.com")
}

fun NetworkSettings.getHeliusRpcUrl(): String {
    val apiKey = heliusApiKey ?: return network.rpcUrl
    return when (network) {
        SolanaNetwork.MAINNET -> "https://mainnet.helius-rpc.com/?api-key=$apiKey"
        SolanaNetwork.DEVNET -> "https://devnet.helius-rpc.com/?api-key=$apiKey"
    }
}