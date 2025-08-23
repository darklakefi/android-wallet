package fi.darklake.wallet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import fi.darklake.wallet.ui.design.DesignTokens
import fi.darklake.wallet.ui.design.Green100
import fi.darklake.wallet.ui.design.Green300
import fi.darklake.wallet.ui.design.Green700
import fi.darklake.wallet.R
import androidx.compose.ui.tooling.preview.Preview
import fi.darklake.wallet.ui.design.DarklakeWalletTheme

/**
 * Reusable button component with consistent styling and accessibility support.
 * 
 * Provides primary and secondary button variants with consistent theming
 * and proper accessibility attributes.
 * 
 * @param text The button text to display
 * @param onClick Callback when the button is clicked
 * @param isPrimary Whether this is a primary (filled) or secondary (outlined) button
 * @param hasUnderline Whether to show an underline on the text (for secondary buttons)
 * @param modifier Optional modifier for the button
 * @param enabled Whether the button is enabled and clickable
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    hasUnderline: Boolean = false,
    enabled: Boolean = true
) {
    val buttonColors = if (isPrimary) {
        ButtonDefaults.buttonColors(
            containerColor = Green100,
            contentColor = Color(DesignTokens.Colors.BUTTON_TEXT_COLOR),
            disabledContainerColor = Green100.copy(alpha = 0.5f),
            disabledContentColor = Color(DesignTokens.Colors.BUTTON_TEXT_COLOR).copy(alpha = 0.5f)
        )
    } else {
        ButtonDefaults.outlinedButtonColors(
            contentColor = Green100,
            disabledContentColor = Green100.copy(alpha = 0.5f)
        )
    }
    
    val buttonBorder = if (isPrimary) {
        null
    } else {
        ButtonDefaults.outlinedButtonBorder.copy(width = 0.dp)
    }
    
    val buttonShape = RoundedCornerShape(0.dp)
    
    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            colors = buttonColors,
            shape = buttonShape,
            enabled = enabled
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = DesignTokens.Typography.button
                )
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            colors = buttonColors,
            border = buttonBorder,
            shape = buttonShape,
            enabled = enabled
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = DesignTokens.Typography.button,
                    textDecoration = if (hasUnderline) androidx.compose.ui.text.style.TextDecoration.Underline else null
                )
            )
        }
    }
}

/**
 * Reusable message box component with shadow effect
 */
@Composable
fun MessageBox(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(x = DesignTokens.Colors.SHADOW_OFFSET.dp, y = DesignTokens.Colors.SHADOW_OFFSET.dp)
            .background(color = Color(DesignTokens.Colors.SHADOW_COLOR))
            .offset(x = (-DesignTokens.Colors.SHADOW_OFFSET).dp, y = (-DesignTokens.Colors.SHADOW_OFFSET).dp)
            .background(color = Green700)
            .padding(
                horizontal = DesignTokens.Sizing.messageBoxPadding,
                vertical = DesignTokens.Sizing.messageBoxVerticalPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Reusable logo component
 */
@Composable
fun AppLogo(
    logoResId: Int,
    size: Dp = DesignTokens.Sizing.logo,
    contentDescription: String = "App Logo"
) {
    Image(
        painter = painterResource(id = logoResId),
        contentDescription = contentDescription,
        modifier = Modifier.size(size)
    )
}

/**
 * Reusable title text component
 */
@Composable
fun AppTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = DesignTokens.Typography.title,
    color: Color = Green100
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = fontSize),
        textAlign = TextAlign.Center,
        color = color,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Reusable body text component
 */
@Composable
fun AppBodyText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = DesignTokens.Typography.body,
    color: Color = Green300
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize),
        textAlign = TextAlign.Center,
        color = color,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Reusable highlighted text component for consistent text styling.
 * 
 * Allows highlighting specific parts of text with different colors
 * while maintaining consistent typography and spacing.
 * 
 * @param text The base text content
 * @param highlightedWords List of words to highlight
 * @param highlightColor Color for highlighted words
 * @param baseColor Color for non-highlighted text
 * @param textAlign Text alignment
 * @param modifier Optional modifier for the text
 */
@Composable
fun HighlightedText(
    text: String,
    highlightedWords: List<String>,
    modifier: Modifier = Modifier,
    highlightColor: Color = Green100,
    baseColor: Color = Green300,
    textAlign: TextAlign = TextAlign.Center
) {
    Text(
        text = buildAnnotatedString {
            var currentText = text
            highlightedWords.forEach { word ->
                val startIndex = currentText.indexOf(word)
                if (startIndex != -1) {
                    // Add text before the highlighted word
                    if (startIndex > 0) {
                        withStyle(SpanStyle(color = baseColor)) {
                            append(currentText.substring(0, startIndex))
                        }
                    }
                    // Add the highlighted word
                    withStyle(SpanStyle(color = highlightColor)) {
                        append(word)
                    }
                    // Update currentText to continue from after this word
                    currentText = currentText.substring(startIndex + word.length)
                }
            }
            // Add any remaining text
            if (currentText.isNotEmpty()) {
                withStyle(SpanStyle(color = baseColor)) {
                    append(currentText)
                }
            }
        },
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = DesignTokens.Typography.body,
            lineHeight = DesignTokens.Typography.lineHeight
        ),
        textAlign = textAlign,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Background with overlay image for screens.
 * 
 * Combines the default green background with the overlay PNG image.
 */
@Composable
fun BackgroundWithOverlay(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Green background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(defaultScreenBackground())
        )
        
        // Overlay image
        Image(
            painter = painterResource(id = R.drawable.bg_overlay),
            contentDescription = "Background Overlay",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
        )
        
        // Content on top
        content()
    }
}


/**
 * Component showcase for development and testing.
 * 
 * Displays all available components with their different variants.
 * Useful for developers to see available options and for testing.
 */
@Composable
fun ComponentShowcase() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DesignTokens.Layout.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Layout.componentGap)
    ) {
        // Logo showcase
        AppLogo(
            logoResId = R.drawable.darklake_logo,
            contentDescription = "Darklake Logo"
        )
        
        // Title showcase
        AppTitle(text = "COMPONENT SHOWCASE")
        
        // Body text showcase
        AppBodyText(text = "This shows all available components")
        
        // Highlighted text showcase
        HighlightedText(
            text = "This text has HIGHLIGHTED words for emphasis",
            highlightedWords = listOf("HIGHLIGHTED", "emphasis")
        )
        
        // Button variants
        AppButton(
            text = "Primary Button",
            onClick = {},
            isPrimary = true
        )
        
        AppButton(
            text = "Secondary Button",
            onClick = {},
            isPrimary = false
        )
        
        AppButton(
            text = "Underlined Button",
            onClick = {},
            isPrimary = false,
            hasUnderline = true
        )
        
        AppButton(
            text = "Disabled Button",
            onClick = {},
            isPrimary = true,
            enabled = false
        )
        
        // Container variants
        AppContainer(
            variant = ContainerVariant.Default,
            content = {
                Text("Default Container")
            }
        )
        
        AppContainer(
            variant = ContainerVariant.Shadowed,
            content = {
                Text("Shadowed Container")
            }
        )
        
        AppContainer(
            variant = ContainerVariant.Highlighted,
            content = {
                Text("Highlighted Container")
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ComponentShowcasePreview() {
    DarklakeWalletTheme {
        ComponentShowcase()
    }
}
