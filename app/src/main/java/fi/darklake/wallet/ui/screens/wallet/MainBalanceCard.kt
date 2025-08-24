package fi.darklake.wallet.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.darklake.wallet.R
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
            Button(
                onClick = onReceiveClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarklakeCardBackground,
                    contentColor = DarklakePrimary
                ),
                shape = RoundedCornerShape(0.dp),
                contentPadding = PaddingValues(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Receive",
                    modifier = Modifier.size(20.dp),
                    tint = DarklakeButtonIcon
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RECEIVE",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 18.sp
                )
            }
            
            // Send Button
            Button(
                onClick = onSendClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarklakeCardBackground,
                    contentColor = DarklakePrimary
                ),
                shape = RoundedCornerShape(0.dp),
                contentPadding = PaddingValues(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp),
                    tint = DarklakeButtonIcon
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SEND",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 18.sp
                )
            }
            
            // Refresh Button
            IconButton(
                onClick = onRefreshClick,
                modifier = Modifier
                    .background(
                        color = DarklakeCardBackground,
                        shape = RoundedCornerShape(0.dp)
                    )
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_refresh),
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
            contentAlignment = Alignment.Center
        ) {
            val parts = balance.split(".", " ")
            val mainNumber = parts.getOrNull(0) ?: ""
            val decimal = parts.getOrNull(1)?.substringBefore(" ") ?: ""
            val currency = if (balance.contains("SOL")) "SOL" else ""
            
            Text(
                text = buildAnnotatedString {
                    // Main number (full size)
                    withStyle(style = SpanStyle(fontSize = 50.sp)) {
                        append(mainNumber)
                    }
                    if (decimal.isNotEmpty()) {
                        // Decimal point and decimal numbers (half size)
                        withStyle(style = SpanStyle(fontSize = 25.sp)) {
                            append(".")
                            append(decimal)
                        }
                    }
                    if (currency.isNotEmpty()) {
                        append(" ")
                        // Currency (half size)
                        withStyle(style = SpanStyle(fontSize = 25.sp)) {
                            append(currency)
                        }
                    }
                },
                style = BalanceDisplayStyle.copy(lineHeight = 64.sp),
                color = DarklakeBalanceText,
                modifier = Modifier.padding(horizontal = 20.dp),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}