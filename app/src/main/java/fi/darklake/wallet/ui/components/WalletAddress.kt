package fi.darklake.wallet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fi.darklake.wallet.ui.theme.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import fi.darklake.wallet.R

val BitsumishiFontAddress = FontFamily(
    Font(R.font.bitsumishi)
)

@Composable
fun WalletAddress(
    address: String,
    modifier: Modifier = Modifier,
    showFullAddress: Boolean = false,
    onCopyClick: (() -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
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
                    clipboardManager.setText(AnnotatedString(address))
                    Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                }
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Address text
        Text(
            text = displayAddress,
            color = DarklakeTextMuted,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.18.sp,
            fontFamily = BitsumishiFontAddress
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