package fi.darklake.wallet.ui.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = TerminalBlack,
    primaryContainer = MutedGreen,
    onPrimaryContainer = TerminalWhite,
    
    secondary = ElectricBlue,
    onSecondary = TerminalBlack,
    secondaryContainer = MatrixBlue,
    onSecondaryContainer = TerminalWhite,
    
    tertiary = BrightCyan,
    onTertiary = TerminalBlack,
    tertiaryContainer = DeepNavy,
    onTertiaryContainer = TerminalWhite,
    
    background = NearBlack,
    onBackground = TerminalWhite,
    
    surface = DarkGray,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceVariant,
    surfaceContainerHighest = Color(0xFF3a3a3a),
    
    outline = Outline,
    outlineVariant = OutlineVariant,
    
    error = ErrorRed,
    onError = TerminalBlack,
    errorContainer = Color(0xFF4d1a1a),
    onErrorContainer = ErrorRed,
    
    inverseSurface = TerminalWhite,
    inverseOnSurface = TerminalBlack,
    inversePrimary = MutedGreen
)


@Composable
fun DarklakeWalletTheme(
    // Always use dark theme for Darklake brand
    content: @Composable () -> Unit
) {
    // Always use the custom dark color scheme - no light theme
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}