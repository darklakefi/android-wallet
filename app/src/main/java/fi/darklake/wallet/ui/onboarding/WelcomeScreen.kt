package fi.darklake.wallet.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.R
import fi.darklake.wallet.ui.theme.DarklakeWalletTheme
import fi.darklake.wallet.ui.design.DesignTokens
import fi.darklake.wallet.ui.components.FlexLayout
import fi.darklake.wallet.ui.components.FlexSection
import fi.darklake.wallet.ui.components.FlexPosition
import fi.darklake.wallet.ui.components.defaultScreenBackground
import fi.darklake.wallet.ui.components.AppLogo
import fi.darklake.wallet.ui.components.AppTitle
import fi.darklake.wallet.ui.components.AppBodyText
import fi.darklake.wallet.ui.components.AppButton
import fi.darklake.wallet.ui.components.AppContainer
import fi.darklake.wallet.ui.components.ContainerVariant
import fi.darklake.wallet.ui.components.HighlightedText
import fi.darklake.wallet.ui.components.BackgroundWithOverlay
import fi.darklake.wallet.ui.components.NumberedList
import fi.darklake.wallet.ui.components.createHighlightedItem
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import fi.darklake.wallet.ui.theme.Green100
import fi.darklake.wallet.ui.theme.Green300

/**
 * Welcome screen for the Darklake wallet onboarding flow.
 * 
 * Displays the app logo, tagline, and main action buttons for creating
 * or importing a wallet. Uses a flexible layout system to position
 * content with proper spacing and visual hierarchy.
 * 
 * @param onCreateWallet Callback when user wants to create a new wallet
 * @param onImportWallet Callback when user wants to import existing wallet
 */
@Composable
fun WelcomeScreen(
    onCreateWallet: () -> Unit,
    onImportWallet: () -> Unit
) {
    BackgroundWithOverlay {
        FlexLayout(
            sections = listOf(
                // Top section: Logo + Title + Tagline
                FlexSection(
                    content = {
                        AppLogo(
                            logoResId = R.drawable.darklake_logo,
                            contentDescription = stringResource(R.string.app_name)
                        )
                        
                        Spacer(modifier = Modifier.height(DesignTokens.Layout.componentGap))
                        
                        AppTitle(text = stringResource(R.string.welcome_title))
                        
                        Spacer(modifier = Modifier.height(DesignTokens.Layout.componentGap))
                        
                        AppBodyText(text = stringResource(R.string.welcome_subtitle))
                    },
                    position = FlexPosition.Top,
                    topSpacing = DesignTokens.Spacing.top,
                    bottomSpacing = DesignTokens.Spacing.logoToContent
                ),
                // Middle section: Message box
                FlexSection(
                    content = {
                        AppContainer(
                            variant = ContainerVariant.Shadowed,
                            horizontalPadding = DesignTokens.Sizing.messageBoxPadding,
                            verticalPadding = DesignTokens.Sizing.messageBoxVerticalPadding,
                            content = {
                                NumberedList(
                                    items = listOf(
                                        createHighlightedItem("YOUR TRADES BECOME ", "INVISIBLE", "."),
                                        createHighlightedItem("YOUR MOVES, ", "UNTRACEABLE", "."),
                                        createHighlightedItem("YOUR VALUE, ", "PRESERVED", "."),
                                        createHighlightedItem("YOUR IDENTITY, ", "YOURS AGAIN", ".")
                                    )
                                )
                            }
                        )
                    },
                    position = FlexPosition.Flex,
                    spacing = DesignTokens.Spacing.logoToContent
                ),
                // Bottom section: Buttons (now properly positioned at bottom)
                FlexSection(
                    content = {
                        AppButton(
                            text = stringResource(R.string.welcome_create_wallet),
                            onClick = onCreateWallet,
                            isPrimary = true
                        )
                        
                        Spacer(modifier = Modifier.height(DesignTokens.Layout.buttonGap))
                        
                        AppButton(
                            text = stringResource(R.string.welcome_import_wallet),
                            onClick = onImportWallet,
                            isPrimary = false,
                            hasUnderline = true
                        )
                    },
                    position = FlexPosition.Bottom,
                    bottomSpacing = DesignTokens.Spacing.xs
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    DarklakeWalletTheme {
        WelcomeScreen(
            onCreateWallet = {},
            onImportWallet = {}
        )
    }
}