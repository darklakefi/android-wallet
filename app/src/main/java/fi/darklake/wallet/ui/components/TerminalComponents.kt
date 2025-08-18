package fi.darklake.wallet.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.darklake.wallet.ui.theme.*

// Terminal-style balance display with glow effects
@Composable
fun TerminalBalanceDisplay(
    balance: String,
    modifier: Modifier = Modifier,
    currency: String = "SOL",
    isLoading: Boolean = false
) {
    val glowAnimation by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // Terminal glow effect
                val glowColor = NeonGreen.copy(alpha = glowAnimation * 0.3f)
                drawRoundRect(
                    color = glowColor,
                    size = Size(size.width + 20.dp.toPx(), size.height + 20.dp.toPx()),
                    topLeft = Offset(-10.dp.toPx(), -10.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
            }
            .background(
                color = TerminalBlack,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = NeonGreen,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            TerminalLoadingIndicator()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = balance,
                    style = WalletBalanceStyle,
                    color = NeonGreen,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = currency,
                    style = TerminalHeaderStyle,
                    color = TerminalGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Copyable address display with terminal styling
@Composable
fun TerminalAddressDisplay(
    address: String,
    modifier: Modifier = Modifier,
    label: String = "ADDRESS"
) {
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var showCopied by remember { mutableStateOf(false) }

    LaunchedEffect(showCopied) {
        if (showCopied) {
            kotlinx.coroutines.delay(2000)
            showCopied = false
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "[$label]",
            style = TerminalTextStyle,
            color = TerminalGray,
            fontSize = 11.sp
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = SurfaceContainer,
                    shape = RoundedCornerShape(2.dp)
                )
                .border(
                    width = 1.dp,
                    color = OutlineVariant,
                    shape = RoundedCornerShape(2.dp)
                )
                .clickable {
                    clipboardManager.setText(AnnotatedString(address))
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showCopied = true
                }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = address,
                    style = AddressStyle,
                    color = if (showCopied) NeonGreen else OnSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                AnimatedVisibility(
                    visible = showCopied,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Text(
                        text = "[COPIED]",
                        style = TerminalTextStyle,
                        color = NeonGreen,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// Terminal-style button with hover effects
@Composable
fun TerminalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    
    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            !enabled -> OutlineVariant
            isPressed -> ButtonPressed
            else -> NeonGreen
        },
        animationSpec = tween(150), label = "border"
    )
    
    val animatedBackgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            isPressed -> ButtonPressed
            else -> ButtonPrimary
        },
        animationSpec = tween(150), label = "background"
    )

    Box(
        modifier = modifier
            .background(
                color = animatedBackgroundColor,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(enabled = enabled && !isLoading) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            TerminalLoadingIndicator(size = 16.dp)
        } else {
            content()
        }
    }
}

// Terminal loading indicator with matrix-style animation
@Composable
fun TerminalLoadingIndicator(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "◢",
            style = TerminalTextStyle.copy(
                fontSize = (size.value * 0.8f).sp,
                color = NeonGreen
            ),
            modifier = Modifier.graphicsLayer {
                rotationZ = rotation
            }
        )
    }
}

// Terminal-style card with ASCII borders
@Composable
fun TerminalCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    glowEffect: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val glowAlpha by animateFloatAsState(
        targetValue = if (glowEffect) 0.4f else 0f,
        animationSpec = tween(300), label = "glow"
    )

    Column(
        modifier = modifier
            .drawBehind {
                if (glowEffect) {
                    val glowColor = NeonGreen.copy(alpha = glowAlpha * 0.3f)
                    drawRoundRect(
                        color = glowColor,
                        size = Size(size.width + 10.dp.toPx(), size.height + 10.dp.toPx()),
                        topLeft = Offset(-5.dp.toPx(), -5.dp.toPx()),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
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
        title?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "╭─ $it",
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
        }
        
        content()
        
        if (title != null) {
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

// Network status indicator with terminal styling
@Composable
fun TerminalNetworkStatus(
    isConnected: Boolean,
    networkName: String,
    modifier: Modifier = Modifier
) {
    val statusColor = if (isConnected) NeonGreen else ErrorRed
    val statusText = if (isConnected) "ONLINE" else "OFFLINE"
    
    val blinkAnimation by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = if (isConnected) 1f else 0.3f,
        targetValue = if (isConnected) 1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = "blink"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "●",
            style = TerminalTextStyle,
            color = statusColor.copy(alpha = blinkAnimation),
            fontSize = 8.sp
        )
        
        Text(
            text = "[$networkName]",
            style = TerminalTextStyle,
            color = TerminalGray,
            fontSize = 11.sp
        )
        
        Text(
            text = statusText,
            style = TerminalTextStyle,
            color = statusColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// Terminal-style table for transaction history
@Composable
fun TerminalTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = TerminalBlack,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = NeonGreen.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            headers.forEach { header ->
                Text(
                    text = header.uppercase(),
                    style = TerminalHeaderStyle,
                    color = NeonGreen,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = NeonGreen.copy(alpha = 0.3f),
            thickness = 1.dp
        )
        
        // Rows
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { cell ->
                    Text(
                        text = cell,
                        style = TerminalTextStyle,
                        color = OnSurface,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}