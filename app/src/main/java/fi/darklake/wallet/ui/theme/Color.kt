package fi.darklake.wallet.ui.theme

import androidx.compose.ui.graphics.Color

// Monochromatic Green Palette (Figma Design)
val Green50 = Color(0xFF88FFAB)    // Lightest green
val Green100 = Color(0xFF2CFF8E)   // Logo and highlight text - FIGMA
val Green200 = Color(0xFF44FF73)
val Green300 = Color(0xFF1A9A56)   // Body color - FIGMA
val Green400 = Color(0xFF0D4F2B)   // Secondary body color - FIGMA
val Green500 = Color(0xFF00CC2F)   // Primary green
val Green600 = Color(0xFF009923)
val Green700 = Color(0xFF041C0F)   // 4-line text background - FIGMA
val Green800 = Color(0xFF03160B)   // Background - FIGMA
val Green900 = Color(0xFF001A05)
val Green950 = Color(0xFF010503)   // Darkest green

// Background Colors (Monochromatic)
val NearBlack = Green800           // Primary background - CHANGED!
val DarkGray = Green900            // Secondary background
val TerminalBlack = Color(0xFF000000)     // Pure black for terminal
val ScanlineOverlay = Color(0x1100FF3B)   // Very subtle green for scanlines

// Primary Brand Colors (Monochromatic)
val NeonGreen = Green400           // Primary brand neon green
val MutedGreen = Green600          // Muted dark green
val TerminalGreen = Green300       // Brighter terminal green
val GreenGlow = Color(0x4000FF3B)  // Semi-transparent green for glow effects

// Secondary Colors (Monochromatic)
val ElectricBlue = Green500        // Electric green accent (was blue)
val BrightCyan = Green300          // Bright green (was cyan)
val DeepNavy = Green800            // Deep green for subtle accents
val MatrixBlue = Green700          // Matrix-style green

// Terminal/Text Colors (Monochromatic)
val TerminalWhite = Green300       // Body copy - CHANGED!
val TerminalGray = Green600        // Gray-green for secondary text
val HighlightText = Green100       // Highlight copy - NEW!

// Semantic Colors (Keep these for functionality)
val ErrorRed = Color(0xFFff3333)          // Error states
val WarningAmber = Color(0xFFffaa00)      // Warning states

// UI State Colors (Monochromatic)
val SurfaceVariant = Green900      // Card/surface backgrounds
val SurfaceContainer = Green950    // Container backgrounds
val OnSurface = Green300           // Text on surfaces - CHANGED!
val OnSurfaceVariant = Green600    // Secondary text
val Outline = Green800             // Borders and dividers
val OutlineVariant = Green700      // Subtle borders

// Interactive Colors (Monochromatic)
val ButtonPrimary = Color(0x0000FF3B)     // Transparent green for buttons
val ButtonHover = Color(0x3300FF3B)       // Green hover state
val ButtonPressed = Color(0x6600FF3B)     // Green pressed state
val RippleEffect = Color(0x3300FF3B)      // Ripple color

// Special Effects Colors (Monochromatic)
val GlowEffect = Color(0x8000FF3B)        // Glow around elements
val ShadowColor = Color(0x80000000)       // Drop shadows
val GridLines = Color(0x1a00FF3B)         // Grid pattern overlay
val NoiseOverlay = Color(0x0a808080)      // Noise texture