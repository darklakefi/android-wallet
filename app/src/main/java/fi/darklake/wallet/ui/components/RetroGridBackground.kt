package fi.darklake.wallet.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Legacy compatibility - redirect to enhanced RetroGridBackground
@Composable
fun RetroGridBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    // Use the enhanced version from RetroEffects.kt
    EnhancedRetroBackground(
        modifier = modifier,
        enableMatrixEffect = false, // Keep subtle for main screens
        enableScanlines = true,
        enableNoise = false,
        content = content
    )
}