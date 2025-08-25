package fi.darklake.wallet.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.darklake.wallet.ui.design.NeonGreen
import fi.darklake.wallet.ui.design.SurfaceContainer
import fi.darklake.wallet.ui.design.TerminalHeaderStyle


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

