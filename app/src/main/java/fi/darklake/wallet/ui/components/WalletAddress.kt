package fi.darklake.wallet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalContext
import android.content.ClipData
import androidx.compose.ui.unit.dp
import android.widget.Toast
import fi.darklake.wallet.ui.design.*

@Composable
fun WalletAddress(
    address: String,
    modifier: Modifier = Modifier,
    showFullAddress: Boolean = false,
    onCopyClick: (() -> Unit)? = null
) {
    val clipboardManager = LocalClipboard.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Truncate address if needed
    val displayAddress = if (showFullAddress || address.length <= 13) {
        address
    } else {
        "${address.take(5)}...${address.takeLast(5)}"
    }
    
    Row(
        modifier = modifier
            .clickable { 
                if (onCopyClick != null) {
                    onCopyClick()
                } else {
                    coroutineScope.launch {
                        val clipData = ClipData.newPlainText("Wallet Address", address)
                        clipboardManager.setClipEntry(ClipEntry(clipData))
                        Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Address text
        Text(
            text = displayAddress,
            style = WalletAddressStyle,
            color = DarklakeTextMuted
        )
        
        // Copy icon
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy address",
            modifier = Modifier.size(16.dp),
            tint = DarklakeTextMuted
        )
    }
}