package fi.darklake.wallet.ui.design

import androidx.compose.ui.graphics.Color

// ====================
// Core Brand Colors
// ====================
val DarklakePrimary = Color(0xFF2CFF8E)        // Primary brand green
val DarklakeSecondary = Color(0xFF35D688)      // Lighter green variant
val DarklakeTertiary = Color(0xFF1A9A56)       // Muted green

// ====================
// Background Colors
// ====================
val DarklakeBackground = Color(0xFF010F06)     // Main app background
val DarklakeCardBackground = Color(0xFF062916) // Card/container background
val DarklakeCardBackgroundAlt = Color(0xFF041C0F) // Alternative card bg
val DarklakeTokenDefaultBg = Color(0xFF0A2818) // Default token icon bg

// ====================
// Text Colors
// ====================
val DarklakeTextPrimary = Color(0xFFFFFFFF)    // Primary text (white)
val DarklakeTextSecondary = Color(0xFF35D688)  // Secondary text (bright green)
val DarklakeTextTertiary = Color(0xFF1A9A56)   // Tertiary text (muted green)
val DarklakeTextGray = Color(0xFF888888)       // Gray text
val DarklakeTextDark = Color(0xFF09351D)       // Dark text on light bg
val DarklakeTextMuted = Color(0xFF0D4F2B)      // Very muted green text

// ====================
// Component Colors
// ====================
// Balance Display
val DarklakeBalanceBg = Color(0xFF2CFF8E)      // Balance background
val DarklakeBalanceText = Color(0xFF09351D)    // Balance text color

// Buttons
val DarklakeButtonBg = Color(0xFF062916)       // Button background
val DarklakeButtonText = Color(0xFF2CFF8E)     // Button text
val DarklakeButtonIcon = Color(0xFF1A9A56)     // Button icon tint

// Tab Selector
val DarklakeTabActive = Color(0xFF062916)      // Active tab background
val DarklakeTabTextActive = Color(0xFF35D688)  // Active tab text
val DarklakeTabTextInactive = Color(0xFF1A9A56) // Inactive tab text

// Borders
val DarklakeBorder = Color(0xFF062916)         // Default border color

// ====================
// Token Specific Colors
// ====================
val TokenSolBackground = Color(0xFF000000)     // SOL token background
val TokenSolText = Color(0xFF2CFF8E)           // SOL token text
val TokenUsdcBackground = Color(0xFF2775CA)    // USDC token background
val TokenBonkBackground = Color(0xFFFF8C00)    // BONK token background
val TokenBtcBackground = Color(0xFFFF9500)     // BTC token background
val TokenEthBackground = Color(0xFF627EEA)     // ETH token background

// ====================
// Semantic Colors
// ====================
val DarklakeSuccess = Color(0xFF4CAF50)        // Success state
val DarklakeSuccessLight = Color(0xFF4CAF50).copy(alpha = 0.1f) // Success bg
val DarklakeWarning = Color(0xFFFFA726)        // Warning state
val DarklakeWarningAlt = Color(0xFFE6CC2E)     // Alternative warning
val DarklakeError = Color(0xFFFF3333)          // Error state
val ErrorRed = DarklakeError                   // Alias for compatibility
val DarklakeInfo = Color(0xFF2196F3)           // Info state

// ====================
// Input Field Colors
// ====================
val DarklakeInputBackground = Color(0xFF03160B) // Background for input fields

// ====================
// Swap Screen Colors
// ====================
val SwapPriceImpactLow = Color(0xFF4CAF50)     // < 1% price impact
val SwapPriceImpactMedium = Color(0xFFFFA726)  // 1-5% price impact
val SwapPriceImpactHigh = DarklakeError        // > 5% price impact

// ====================
// Legacy/Utility Colors
// ====================
val TerminalBlack = Color(0xFF000000)          // Pure black
val TerminalGray = Color(0xFF888888)           // Terminal gray
val ShadowColor = Color(0x80000000)            // Drop shadows

// ====================
// Special Effects Colors
// ====================
val NearBlack = DarklakeBackground             // Primary background alias
val NeonGreen = DarklakePrimary                // Neon green alias
val ElectricBlue = DarklakePrimary             // Electric blue alias (using green)
val BrightCyan = DarklakePrimary               // Bright cyan alias (using green)
val DeepNavy = DarklakeCardBackground          // Deep navy alias
val MatrixBlue = DarklakeCardBackgroundAlt     // Matrix blue alias
val MutedGreen = Color(0xFF009923)             // Muted green
val TerminalWhite = DarklakeTextPrimary        // Terminal white alias
val GridLines = Color(0x1a00FF3B)              // Grid pattern overlay
val ScanlineOverlay = Color(0x1A2CFF8E)        // Subtle green overlay
val NoiseOverlay = Color(0x0a808080)           // Noise texture
val OnSurface = DarklakeTextPrimary            // Text on surfaces
val OnSurfaceVariant = TerminalGray            // Secondary text on surfaces
val SurfaceVariant = DarklakeCardBackground    // Card/surface backgrounds
val SurfaceContainer = DarklakeCardBackgroundAlt // Surface container alias
val Outline = DarklakeBorder                   // Borders and dividers
val OutlineVariant = DarklakeCardBackground    // Outline variant alias
val DarkGray = DarklakeCardBackground          // Dark gray alias
val ButtonPrimary = Color(0x0000FF3B)          // Transparent green for buttons
val ButtonPressed = Color(0x6600FF3B)          // Green pressed state

// Token Selection Fallback Colors (for unknown tokens)
val TokenFallback1 = Color(0xFF4CAF50)
val TokenFallback2 = Color(0xFF2196F3)
val TokenFallback3 = Color(0xFFFF5722)
val TokenFallback4 = Color(0xFF9C27B0)
val TokenFallback5 = Color(0xFFFF9800)
val TokenFallback6 = Color(0xFF795548)

// ====================
// Legacy Green Palette (for backward compatibility)
// ====================
val Green50 = Color(0xFF88FFAB)
val Green100 = DarklakePrimary
val Green200 = Color(0xFF44FF73)
val Green300 = DarklakeTertiary
val Green400 = DarklakeTextMuted
val Green500 = Color(0xFF00CC2F)
val Green600 = Color(0xFF009923)
val Green700 = DarklakeCardBackgroundAlt
val Green800 = DarklakeBackground
val Green900 = Color(0xFF001A05)
val Green950 = Color(0xFF010503)