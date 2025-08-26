package fi.darklake.wallet.ui.screens.send

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fi.darklake.wallet.ui.design.*

@Composable
fun NftPreviewCard(
    nftName: String?,
    nftImageUrl: String?,
    nftMint: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = SurfaceContainer,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = NeonGreen.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            // Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "╭─ NFT_ASSET",
                    style = TerminalHeaderStyle,
                    color = NeonGreen,
                    fontSize = 12.sp
                )
                Text(
                    text = "─╮",
                    style = TerminalHeaderStyle,
                    color = NeonGreen,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // NFT image
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            color = TerminalBlack,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = BrightCyan.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (nftImageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(nftImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = nftName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "[NFT]",
                            style = TerminalHeaderStyle,
                            color = BrightCyan
                        )
                    }
                }
                
                // NFT details
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "[${nftName ?: "UNKNOWN_NFT"}]",
                        style = TerminalHeaderStyle,
                        color = BrightCyan,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = "MINT: ${nftMint.take(8)}...${nftMint.takeLast(8)}",
                        style = TerminalTextStyle,
                        color = TerminalGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = "✓ UNIQUE ASSET",
                            modifier = Modifier.padding(8.dp),
                            style = TerminalTextStyle,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Bottom border
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "╰─",
                    style = TerminalHeaderStyle,
                    color = NeonGreen,
                    fontSize = 12.sp
                )
                Text(
                    text = "─╯",
                    style = TerminalHeaderStyle,
                    color = NeonGreen,
                    fontSize = 12.sp
                )
            }
        }
    }
}