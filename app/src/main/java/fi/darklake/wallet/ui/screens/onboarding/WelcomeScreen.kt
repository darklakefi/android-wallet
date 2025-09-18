package fi.darklake.wallet.ui.screens.onboarding

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import fi.darklake.wallet.R
import fi.darklake.wallet.ui.components.AppBodyText
import fi.darklake.wallet.ui.components.AppButton
import fi.darklake.wallet.ui.components.AppContainer
import fi.darklake.wallet.ui.components.AppLogo
import fi.darklake.wallet.ui.components.AppTitle
import fi.darklake.wallet.ui.components.BackgroundWithOverlay
import fi.darklake.wallet.ui.components.ContainerVariant
import fi.darklake.wallet.ui.components.FlexLayout
import fi.darklake.wallet.ui.components.FlexPosition
import fi.darklake.wallet.ui.components.FlexSection
import fi.darklake.wallet.ui.components.NumberedList
import fi.darklake.wallet.ui.components.createHighlightedItem
import fi.darklake.wallet.ui.design.DarklakeWalletTheme
import fi.darklake.wallet.ui.design.DesignTokens

/**
 * Welcome screen for the Darklake wallet onboarding flow.
 *
 * Displays the app logo, tagline, and main action buttons for creating
 * or importing a wallet. Uses a flexible layout system to position
 * content with proper spacing and visual hierarchy.
 *
 * @param onCreateWallet Callback when user wants to create a new wallet
 * @param onImportWallet Callback when user wants to import existing wallet
 * @param onUseSeedVault Callback when user wants to use Seed Vault
 */
@Composable
fun WelcomeScreen(
    onCreateWallet: () -> Unit,
    onImportWallet: () -> Unit,
    onUseSeedVault: () -> Unit = {}
) {
    val context = LocalContext.current
    val seedVaultAvailable = remember { mutableStateOf(false) }

    // Check if Seed Vault is available on this device
    LaunchedEffect(Unit) {
        val seedVaultManager = fi.darklake.wallet.seedvault.SeedVaultManager(context)
        seedVaultAvailable.value = seedVaultManager.isSeedVaultAvailable()
    }
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

                        // Show Seed Vault option only if available
                        if (seedVaultAvailable.value) {
                            Spacer(modifier = Modifier.height(DesignTokens.Layout.buttonGap))

                            AppButton(
                                text = stringResource(R.string.welcome_use_seed_vault),
                                onClick = onUseSeedVault,
                                isPrimary = false,
                                hasUnderline = false
                            )
                        }
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