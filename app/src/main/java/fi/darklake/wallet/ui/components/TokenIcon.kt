package fi.darklake.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fi.darklake.wallet.data.swap.models.TokenInfo
import fi.darklake.wallet.data.tokens.TokenColor
import fi.darklake.wallet.data.tokens.TokenMetadataService
import fi.darklake.wallet.ui.design.*
import kotlinx.coroutines.launch

/**
 * Composable that displays a token icon
 * Tries to load from URL first, falls back to letter with colored background
 */
@Composable
fun TokenIcon(
    token: TokenInfo?,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    if (token == null) {
        // Empty state
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(DarklakeTokenDefaultBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "?",
                style = TerminalTextStyle,
                color = DarklakeTextPrimary,
                fontSize = (size.value * 0.5f).sp
            )
        }
        return
    }
    
    val context = LocalContext.current
    val tokenMetadataService = remember { TokenMetadataService.getInstance(context) }
    var logoUrl by remember(token.address) { mutableStateOf<String?>(null) }
    var tokenColor by remember(token.address) { mutableStateOf(TokenColor.DEFAULT) }
    val scope = rememberCoroutineScope()
    
    // Fetch metadata if we have an address
    LaunchedEffect(token.address) {
        if (token.address.isNotEmpty()) {
            scope.launch {
                // Try to get from cache first
                val cachedMetadata = tokenMetadataService.getCachedMetadata(token.address)
                if (cachedMetadata != null) {
                    logoUrl = cachedMetadata.logoUri.takeIf { it.isNotEmpty() }
                    tokenColor = tokenMetadataService.getTokenColor(token.address)
                } else {
                    // Fetch from server
                    val metadata = tokenMetadataService.getTokenMetadata(token.address)
                    logoUrl = metadata?.logoUri?.takeIf { it.isNotEmpty() }
                    tokenColor = tokenMetadataService.getTokenColor(token.address)
                }
            }
        } else {
            // No address, try to determine color from symbol
            tokenColor = when (token.symbol) {
                "SOL" -> TokenColor.SOL
                "USDC" -> TokenColor.USDC
                "USDT" -> TokenColor.USDT
                "BONK" -> TokenColor.BONK
                else -> TokenColor.DEFAULT
            }
        }
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    ) {
        if (!logoUrl.isNullOrEmpty()) {
            // Try to load image from URL
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "${token.symbol} icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
                onError = {
                    // On error, we'll fall back to letter display
                    logoUrl = null
                }
            )
        }
        
        // Show letter as fallback or if no URL
        if (logoUrl.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .size(size)
                    .background(
                        color = Color(android.graphics.Color.parseColor(tokenColor.hex)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = token.symbol.firstOrNull()?.toString() ?: "?",
                    style = TerminalTextStyle.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    fontSize = (size.value * 0.5f).sp
                )
            }
        }
    }
}

/**
 * Token icon with address-based metadata fetching
 */
@Composable
fun TokenIconByAddress(
    tokenAddress: String,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokenMetadataService = remember { TokenMetadataService.getInstance(context) }
    var tokenInfo by remember(tokenAddress) { mutableStateOf<TokenInfo?>(null) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(tokenAddress) {
        scope.launch {
            val metadata = tokenMetadataService.getTokenMetadata(tokenAddress)
            metadata?.let {
                tokenInfo = TokenInfo(
                    address = it.address,
                    symbol = it.symbol,
                    name = it.name,
                    decimals = it.decimals
                )
            }
        }
    }
    
    TokenIcon(
        token = tokenInfo,
        size = size,
        modifier = modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewTokenIcon() {
    DarklakeWalletTheme {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            TokenIcon(
                token = TokenInfo("", "SOL", "Solana", 9),
                size = 32.dp
            )
            TokenIcon(
                token = TokenInfo("", "USDC", "USD Coin", 6),
                size = 32.dp
            )
            TokenIcon(
                token = TokenInfo("", "BONK", "Bonk", 5),
                size = 32.dp
            )
            TokenIcon(
                token = null,
                size = 32.dp
            )
        }
    }
}