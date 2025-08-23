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
import fi.darklake.wallet.ui.design.*


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

