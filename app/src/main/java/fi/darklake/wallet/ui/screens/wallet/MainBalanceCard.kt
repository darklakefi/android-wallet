package fi.darklake.wallet.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.ui.design.*
import fi.darklake.wallet.ui.components.AppButton

@Composable
fun MainBalanceCard(
    balance: String,
    onSendClick: () -> Unit = {},
    onReceiveClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Receive Button
            AppButton(
                text = "RECEIVE",
                onClick = onReceiveClick,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Default.ArrowDownward,
                iconTint = DarklakeButtonIcon,
                contentPadding = PaddingValues(10.dp)
            )
            
            // Send Button
            AppButton(
                text = "SEND",
                onClick = onSendClick,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Default.ArrowUpward,
                iconTint = DarklakeButtonIcon,
                contentPadding = PaddingValues(10.dp)
            )
            
            // Refresh Button
            IconButton(
                onClick = onRefreshClick,
                modifier = Modifier
                    .background(
                        color = DarklakeButtonBg,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    )
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(20.dp),
                    tint = DarklakeButtonIcon
                )
            }
        }
        
        // Balance Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .background(DarklakeBalanceBg),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = balance,
                style = BalanceDisplayStyle,
                color = DarklakeBalanceText,
                modifier = Modifier.padding(horizontal = 20.dp),
                textAlign = TextAlign.Start,
                maxLines = 1
            )
        }
    }
}