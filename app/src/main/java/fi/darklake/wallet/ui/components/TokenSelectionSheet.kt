package fi.darklake.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.data.swap.models.Token

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenSelectionSheet(
    tokens: List<Token>,
    selectedTokenAddress: String?,
    onTokenSelected: (Token) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredTokens = remember(tokens, searchQuery) {
        if (searchQuery.isBlank()) {
            tokens
        } else {
            tokens.filter { token ->
                token.symbol.contains(searchQuery, ignoreCase = true) ||
                token.name.contains(searchQuery, ignoreCase = true) ||
                token.address.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .padding(16.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Token",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search tokens or paste address") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Token list
            if (filteredTokens.isEmpty()) {
                // No results
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No tokens found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Try a different search term",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTokens) { token ->
                        TokenListItem(
                            token = token,
                            isSelected = token.address == selectedTokenAddress,
                            onClick = { onTokenSelected(token) }
                        )
                    }
                }
            }
            
            // Bottom padding for safe area
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TokenListItem(
    token: Token,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Token icon
            TokenIcon(
                symbol = token.symbol,
                imageUrl = token.imageUrl,
                modifier = Modifier.size(40.dp)
            )
            
            // Token info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = token.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Address badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = truncateAddress(token.address),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Text(
                    text = token.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Selected indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TokenIcon(
    symbol: String,
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    // For now, use a simple colored circle with the first letter
    // Later this can be enhanced with actual token images
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                color = getTokenColor(symbol)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol.firstOrNull()?.toString() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

private fun getTokenColor(symbol: String): Color {
    // Simple color mapping based on token symbol
    return when (symbol.uppercase()) {
        "SOL" -> Color(0xFF9945FF)
        "USDC" -> Color(0xFF2775CA)
        "BTC" -> Color(0xFFFF9500)
        "ETH" -> Color(0xFF627EEA)
        else -> {
            // Generate a color based on symbol hash
            val hash = symbol.hashCode()
            val colors = listOf(
                Color(0xFF4CAF50),
                Color(0xFF2196F3),
                Color(0xFFFF5722),
                Color(0xFF9C27B0),
                Color(0xFFFF9800),
                Color(0xFF795548)
            )
            colors[Math.abs(hash) % colors.size]
        }
    }
}

private fun truncateAddress(address: String): String {
    return if (address.length > 8) {
        "${address.take(4)}...${address.takeLast(4)}"
    } else {
        address
    }
}