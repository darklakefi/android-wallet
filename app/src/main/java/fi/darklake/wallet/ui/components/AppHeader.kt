package fi.darklake.wallet.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.R
import fi.darklake.wallet.ui.design.DesignTokens
import fi.darklake.wallet.ui.design.Green100

/**
 * Reusable header component for screens with back button and logo.
 * 
 * @param onBackClick Callback when back button is clicked
 * @param logoResId Resource ID for the logo (defaults to darklake_logo)
 * @param logoSize Size of the logo (defaults to 32dp for header usage)
 * @param contentDescription Content description for accessibility
 */
@Composable
fun AppHeader(
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
                Icons.Default.ArrowBack, 
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
