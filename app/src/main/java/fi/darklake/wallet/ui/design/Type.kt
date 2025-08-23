package fi.darklake.wallet.ui.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fi.darklake.wallet.R

// ====================
// Font Families
// ====================

// Bitsumishi - Primary display font
val BitsumishiFontFamily = FontFamily(
    Font(R.font.bitsumishi, FontWeight.Normal)
)

// Classic Console Neue - Secondary display font (using clacon2 as substitute)
val ClassicConsoleFontFamily = FontFamily(
    Font(R.font.clacon2, FontWeight.Normal)
)

// Monospace/Terminal font
val MonospaceFontFamily = FontFamily(
    Font(R.font.clacon2, FontWeight.Normal)
)

// Helvetica - Body text font
val HelveticaFontFamily = FontFamily(
    Font(R.font.helvetica, FontWeight.Normal)
)

// ====================
// Component-Specific Styles
// ====================

// Main Balance Display (from Figma: 100sp Bitsumishi)
val BalanceDisplayStyle = TextStyle(
    fontFamily = BitsumishiFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 56.sp,  // Adjusted to fit in container
    lineHeight = 64.sp,
    letterSpacing = 0.sp
)

// Token Balance Text (18sp Classic Console)
val TokenBalanceStyle = TextStyle(
    fontFamily = ClassicConsoleFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.18.sp  // 1% letter spacing from Figma
)

// Token Symbol Text (18sp Classic Console)
val TokenSymbolStyle = TextStyle(
    fontFamily = ClassicConsoleFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.18.sp
)

// Wallet Address (18sp Classic Console)
val WalletAddressStyle = TextStyle(
    fontFamily = ClassicConsoleFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.18.sp
)

// Tab Text (18sp Classic Console UPPER)
val TabTextStyle = TextStyle(
    fontFamily = ClassicConsoleFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.18.sp
)

// Button Text (18sp Classic Console UPPER)
val ButtonTextStyle = TextStyle(
    fontFamily = ClassicConsoleFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.18.sp
)

// Navigation Label (12sp Classic Console UPPER)
val NavigationLabelStyle = TextStyle(
    fontFamily = ClassicConsoleFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp
)

// NFT Title Text
val NftTitleStyle = TextStyle(
    fontFamily = BitsumishiFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.sp
)

// Compressed Label
val CompressedLabelStyle = TextStyle(
    fontFamily = BitsumishiFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 8.sp,
    lineHeight = 12.sp,
    letterSpacing = 0.sp
)

// Terminal/Console text styles
val TerminalTextStyle = TextStyle(
    fontFamily = MonospaceFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp
)

val TerminalHeaderStyle = TextStyle(
    fontFamily = MonospaceFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp
)

// Legacy styles for compatibility
val WalletBalanceStyle = BalanceDisplayStyle
val AddressStyle = WalletAddressStyle

// Darklake Typography System
val Typography = Typography(
    // Headers and titles - Bitsumishi
    displayLarge = TextStyle(
        fontFamily = BitsumishiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = BitsumishiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = BitsumishiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = BitsumishiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = BitsumishiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = BitsumishiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = BitsumishiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = BitsumishiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = BitsumishiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // Body text - Monospace (was Helvetica)
    bodyLarge = TextStyle(
        fontFamily = MonospaceFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = MonospaceFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = MonospaceFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    // Labels - Monospace (was Helvetica)
    labelLarge = TextStyle(
        fontFamily = MonospaceFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = MonospaceFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = MonospaceFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)