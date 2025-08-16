package fi.darklake.wallet.ui.design

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

/**
 * Design tokens for consistent spacing, typography, and layout across the app.
 * This serves as the single source of truth for all design values.
 */
object DesignTokens {
    
    /**
     * Spacing scale based on 8dp base unit for consistency
     */
    object Spacing {
        val xs = 4.dp    // 0.5x base unit
        val sm = 8.dp    // 1x base unit
        val md = 16.dp   // 2x base unit
        val lg = 24.dp   // 3x base unit
        val xl = 32.dp   // 4x base unit
        val xxl = 48.dp  // 6x base unit
        val xxxl = 64.dp // 8x base unit
        
        // Custom spacing for specific use cases
        val top = 80.dp  // Top section spacing
        val bottom = 40.dp // Bottom section spacing
        val logoToMessage = 32.dp // Space between logo section and message box
        val logoToContent = xxl // Space between logo and content sections (48dp)
    }
    
    /**
     * Typography scale for consistent text sizing
     */
    object Typography {
        val title = 28.sp
        val body = 18.sp
        val caption = 14.sp
        val button = 18.sp
        val lineHeight = 24.sp
    }
    
    /**
     * Layout patterns for consistent screen structure
     */
    object Layout {
        val screenPadding = Spacing.xl
        val sectionGap = Spacing.xxl
        val componentGap = Spacing.md
        val buttonGap = Spacing.sm
    }
    
    /**
     * Component sizing for consistent UI elements
     */
    object Sizing {
        val logo = 80.dp
        val buttonHeight = 56.dp
        val messageBoxPadding = Spacing.lg
        val messageBoxVerticalPadding = Spacing.lg
        val buttonVerticalPadding = 8.dp
    }
    
    /**
     * Colors for consistent theming
     */
    object Colors {
        const val SHADOW_OFFSET = 4
        const val SHADOW_COLOR = 0xFF010F06
        const val BUTTON_TEXT_COLOR = 0xFF010F06
        val Green100 = Color(0xFF2CFF8E)
        val Green300 = Color(0xFF1A9A56)
        val Warning = Color(0xFFE6CC2E)
        val LineNumber = Color(0xFF0D4F2B)
    }
}
