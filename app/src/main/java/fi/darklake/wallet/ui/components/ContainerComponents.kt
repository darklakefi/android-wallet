package fi.darklake.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.ui.design.DesignTokens
import fi.darklake.wallet.ui.theme.Green700

/**
 * Flexible container component for semantic sections
 */
@Composable
fun AppContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    variant: ContainerVariant = ContainerVariant.Default,
    padding: Dp = DesignTokens.Spacing.md,
    horizontalPadding: Dp? = null,
    verticalPadding: Dp? = null
) {
    val actualHorizontalPadding = horizontalPadding ?: padding
    val actualVerticalPadding = verticalPadding ?: padding
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                when (variant) {
                    ContainerVariant.Default -> Modifier
                    ContainerVariant.Highlighted -> Modifier
                        .background(
                            color = Green700,
                            shape = RoundedCornerShape(0.dp)
                        )
                    ContainerVariant.Shadowed -> Modifier
                        .offset(x = DesignTokens.Colors.SHADOW_OFFSET.dp, y = DesignTokens.Colors.SHADOW_OFFSET.dp)
                        .background(color = Color(DesignTokens.Colors.SHADOW_COLOR))
                        .offset(x = (-DesignTokens.Colors.SHADOW_OFFSET).dp, y = (-DesignTokens.Colors.SHADOW_OFFSET).dp)
                        .background(
                            color = Green700,
                            shape = RoundedCornerShape(0.dp)
                        )
                    ContainerVariant.Card -> Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(DesignTokens.Spacing.sm)
                        )
                }
            )
            .padding(
                horizontal = actualHorizontalPadding,
                vertical = actualVerticalPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Container variants for different semantic purposes
 */
enum class ContainerVariant {
    Default,        // No special styling
    Highlighted,    // Background color for emphasis
    Shadowed,       // Sharp shadow effect (like message box)
    Card           // Card-like appearance
}

/**
 * Flexible screen layout that can handle any number of sections
 */
@Composable
fun FlexibleScreenLayout(
    sections: List<@Composable () -> Unit>,
    spacing: Dp = DesignTokens.Spacing.md,
    topSpacing: Dp = DesignTokens.Spacing.top,
    bottomSpacing: Dp = DesignTokens.Spacing.xs,
    backgroundBrush: androidx.compose.ui.graphics.Brush? = null
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
            // Top spacing
            if (topSpacing > DesignTokens.Spacing.xs) {
                Spacer(modifier = Modifier.height(topSpacing))
            }
            
            // Render all sections with spacing
            sections.forEachIndexed { index, section ->
                section()
                
                // Add spacing between sections (but not after the last one)
                if (index < sections.size - 1) {
                    Spacer(modifier = Modifier.height(spacing))
                }
            }
            
            // Bottom spacing
            if (bottomSpacing > DesignTokens.Spacing.xs) {
                Spacer(modifier = Modifier.height(bottomSpacing))
            }
        }
    }
}

/**
 * Convenience function for simple 3-section layouts
 */
@Composable
fun ThreeSectionLayout(
    topContent: @Composable () -> Unit = {},
    middleContent: @Composable () -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    topSpacing: Dp = DesignTokens.Spacing.top,
    bottomSpacing: Dp = DesignTokens.Spacing.xs,
    backgroundBrush: androidx.compose.ui.graphics.Brush? = null
) {
    FlexibleScreenLayout(
        sections = listOf(topContent, middleContent, bottomContent),
        spacing = DesignTokens.Spacing.xxl,
        topSpacing = topSpacing,
        bottomSpacing = bottomSpacing,
        backgroundBrush = backgroundBrush
    )
}

/**
 * FlexLayout provides CSS flexbox-like functionality for flexible layouts.
 * 
 * This layout system allows granular control over section positioning and spacing,
 * similar to CSS flexbox. Each section can be positioned at the top, middle, bottom,
 * or with flexible spacing.
 * 
 * Usage example:
 * ```
 * FlexLayout(
 *     sections = listOf(
 *         FlexSection(
 *             content = { /* header content */ },
 *             position = FlexPosition.Top
 *         ),
 *         FlexSection(
 *             content = { /* main content */ },
 *             position = FlexPosition.Flex,
 *             spacing = 16.dp
 *         ),
 *         FlexSection(
 *             content = { /* footer content */ },
 *             position = FlexPosition.Bottom
 *         )
 *     )
 * )
 * ```
 * 
 * @param sections List of FlexSection objects defining the layout structure
 * @param backgroundBrush Optional background brush for the entire layout
 * @param modifier Optional modifier for the layout container
 */
@Composable
fun FlexLayout(
    sections: List<FlexSection>,
    modifier: Modifier = Modifier,
    backgroundBrush: androidx.compose.ui.graphics.Brush? = null
) {
    Box(
        modifier = modifier.fillMaxSize().then(
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
            sections.forEach { section ->
                when (section.position) {
                    FlexPosition.Top -> {
                        Spacer(modifier = Modifier.height(section.topSpacing))
                        section.content()
                        Spacer(modifier = Modifier.height(section.bottomSpacing))
                    }
                    FlexPosition.Middle -> {
                        Spacer(modifier = Modifier.weight(1f))
                        section.content()
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    FlexPosition.Bottom -> {
                        Spacer(modifier = Modifier.weight(1f))
                        section.content()
                        Spacer(modifier = Modifier.height(section.bottomSpacing))
                    }
                    FlexPosition.Flex -> {
                        section.content()
                        if (section.spacing > 0.dp) {
                            Spacer(modifier = Modifier.height(section.spacing))
                        }
                    }
                }
            }
        }
    }
}

/**
 * FlexSection represents a section in the flex layout with positioning and spacing.
 * 
 * Each section defines its content and how it should be positioned within the layout.
 * 
 * @param content The composable content for this section
 * @param position How this section should be positioned (Top, Middle, Bottom, Flex)
 * @param topSpacing Spacing above the section (used with Top position)
 * @param bottomSpacing Spacing below the section (used with Top and Bottom positions)
 * @param spacing Spacing below the section (used with Flex position)
 */
data class FlexSection(
    val content: @Composable () -> Unit,
    val position: FlexPosition = FlexPosition.Flex,
    val topSpacing: Dp = 0.dp,
    val bottomSpacing: Dp = 0.dp,
    val spacing: Dp = 0.dp
)

/**
 * FlexPosition defines how a section should be positioned in the layout.
 * 
 * - [Top]: Positioned at the top with custom top and bottom spacing
 * - [Middle]: Centered vertically with equal weight above and below
 * - [Bottom]: Positioned at the bottom with custom bottom spacing
 * - [Flex]: Flexible positioning with custom spacing
 */
enum class FlexPosition {
    Top,        // Positioned at the top with custom spacing
    Middle,     // Centered vertically with equal weight above and below
    Bottom,     // Positioned at the bottom with custom bottom spacing
    Flex        // Flexible positioning with custom spacing
}


