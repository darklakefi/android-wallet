package fi.darklake.wallet.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import fi.darklake.wallet.ui.design.DesignTokens
import fi.darklake.wallet.ui.theme.Green100

/**
 * Reusable component for displaying numbered list items with consistent styling.
 * 
 * @param items List of NumberedListItem to display
 * @param modifier Optional modifier for the container
 */
@Composable
fun NumberedList(
    items: List<NumberedListItem>,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.fillMaxWidth()
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))
            }
            
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = DesignTokens.Typography.body,
                        lineHeight = DesignTokens.Typography.lineHeight
                    ),
                    color = DesignTokens.Colors.LineNumber
                )
                Spacer(modifier = Modifier.width(DesignTokens.Spacing.sm))
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = DesignTokens.Typography.body,
                        lineHeight = DesignTokens.Typography.lineHeight,
                        color = item.textColor ?: DesignTokens.Colors.Green300
                    ),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Data class representing a numbered list item with optional highlighted text.
 * 
 * @param text The text content (can include highlighted spans)
 * @param textColor Optional color override for the text
 */
data class NumberedListItem(
    val text: androidx.compose.ui.text.AnnotatedString,
    val textColor: Color? = null
)

/**
 * Helper function to create a NumberedListItem with highlighted text.
 * 
 * @param baseText The base text before the highlight
 * @param highlightedText The text to highlight
 * @param endText Optional text after the highlight
 * @param textColor Optional color override for the text
 */
fun createHighlightedItem(
    baseText: String,
    highlightedText: String,
    endText: String = "",
    textColor: Color? = null
): NumberedListItem {
    return NumberedListItem(
        text = buildAnnotatedString {
            append(baseText)
            withStyle(SpanStyle(color = Green100)) {
                append(highlightedText)
            }
            append(endText)
        },
        textColor = textColor
    )
}
