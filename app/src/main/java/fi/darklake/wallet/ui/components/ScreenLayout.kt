package fi.darklake.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.ui.design.DesignTokens
import androidx.compose.ui.graphics.Color

/**
 * Reusable screen layout that provides consistent structure across all screens.
 * Gives precise control over top, middle, and bottom content positioning.
 */
@Composable
fun ScreenLayout(
    topContent: @Composable () -> Unit = {},
    middleContent: @Composable () -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    topSpacing: Dp = DesignTokens.Spacing.top,
    bottomSpacing: Dp = DesignTokens.Spacing.xs,
    backgroundBrush: Brush? = null
) {
    Box(
        modifier = Modifier.fillMaxSize().then(
            if (backgroundBrush != null) {
                Modifier.background(backgroundBrush)
            } else {
                Modifier
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.Layout.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Top section with custom spacing
            if (topSpacing > DesignTokens.Spacing.xs) {
                Spacer(modifier = Modifier.height(topSpacing))
            }
            topContent()
            
            // Middle section with fixed spacing
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.xxl))
            middleContent()
            
            // Bottom section with flexible spacing
            Spacer(modifier = Modifier.weight(1f))
            bottomContent()
            
            // Bottom spacing
            if (bottomSpacing > DesignTokens.Spacing.xs) {
                Spacer(modifier = Modifier.height(bottomSpacing))
            }
        }
    }
}

/**
 * Default background brush for screens
 */
@Composable
fun defaultScreenBackground(): Brush {
    return Brush.radialGradient(
        colors = listOf(
            Color(0xFF010F06),  // Your specified color
            Color(0xFF010F06)   // Same color for solid background
        ),
        radius = 1200f
    )
}
