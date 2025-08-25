package fi.darklake.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.R
import fi.darklake.wallet.ui.design.DarklakeBackground
import fi.darklake.wallet.ui.design.DarklakeWalletTheme
import fi.darklake.wallet.ui.design.DesignTokens
import fi.darklake.wallet.ui.design.Green100

/**
 * Reusable modal header component for screens with back button and logo.
 * 
 * @param onBackClick Callback when back button is clicked
 * @param logoResId Resource ID for the logo (defaults to darklake_logo)
 * @param logoSize Size of the logo (defaults to 32dp for header usage)
 * @param contentDescription Content description for accessibility
 */
@Composable
fun ModalHeader(
    onBackClick: () -> Unit,
    logoResId: Int = R.drawable.darklake_logo,
    logoSize: Int = 32,
    contentDescription: String = "Darklake Logo"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Green100
            )
        }
        
        AppLogo(
            logoResId = logoResId,
            contentDescription = contentDescription,
            size = logoSize.dp
        )
        
        // Empty space for balance
        Spacer(modifier = Modifier.width(DesignTokens.Spacing.xxl))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewModalHeader() {
    DarklakeWalletTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(24.dp)
        ) {
            ModalHeader(
                onBackClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010F06)
@Composable
fun PreviewModalHeaderCustomLogo() {
    DarklakeWalletTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarklakeBackground)
                .padding(24.dp)
        ) {
            ModalHeader(
                onBackClick = {},
                logoSize = 40,
                contentDescription = "Custom Logo"
            )
        }
    }
}
