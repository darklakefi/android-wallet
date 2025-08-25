package fi.darklake.wallet.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

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
