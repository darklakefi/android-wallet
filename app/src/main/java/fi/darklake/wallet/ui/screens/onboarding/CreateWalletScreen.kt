package fi.darklake.wallet.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import fi.darklake.wallet.ui.design.DarklakeWalletTheme
import fi.darklake.wallet.ui.design.Green100
import fi.darklake.wallet.ui.design.DesignTokens
import fi.darklake.wallet.ui.components.FlexLayout
import fi.darklake.wallet.ui.components.FlexSection
import fi.darklake.wallet.ui.components.FlexPosition
import fi.darklake.wallet.ui.components.BackgroundWithOverlay
import fi.darklake.wallet.ui.components.AppButton
import fi.darklake.wallet.ui.components.AppTitle
import fi.darklake.wallet.ui.components.AppBodyText
import fi.darklake.wallet.ui.components.AppContainer
import fi.darklake.wallet.ui.components.ContainerVariant
import fi.darklake.wallet.ui.components.AppLogo
import fi.darklake.wallet.ui.components.NumberedList
import fi.darklake.wallet.ui.components.createHighlightedItem
import fi.darklake.wallet.ui.components.ModalHeader
import androidx.compose.ui.res.stringResource
import fi.darklake.wallet.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWalletScreen(
    onWalletCreated: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: CreateWalletViewModel = viewModel(
        factory = CreateWalletViewModelFactory(context.applicationContext as android.app.Application)
    )
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.walletCreated) {
        if (uiState.walletCreated) {
            uiState.mnemonic?.let { mnemonic ->
                onWalletCreated(mnemonic)
            }
        }
    }
    
    BackgroundWithOverlay {
        FlexLayout(
            sections = listOf(
                // Top section: Back button + Logo
                FlexSection(
                    content = {
                        ModalHeader(onBackClick = onBack)
                    },
                    position = FlexPosition.Top,
                    topSpacing = DesignTokens.Spacing.sm,
                    bottomSpacing = DesignTokens.Spacing.logoToContent
                ),
                // Middle section: Title and content
                FlexSection(
                    content = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AppTitle(text = stringResource(R.string.create_wallet_title))
                            
                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.xxl))
                            
                            // Warning container
                            AppContainer(
                                variant = ContainerVariant.Shadowed,
                                horizontalPadding = DesignTokens.Sizing.messageBoxPadding,
                                verticalPadding = DesignTokens.Sizing.messageBoxVerticalPadding,
                                content = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Warning icon
                    Icon(
                                            Icons.Default.Warning,
                                            contentDescription = stringResource(R.string.warning),
                                            tint = DesignTokens.Colors.Warning,
                                            modifier = Modifier.size(DesignTokens.Spacing.xl)
                                        )
                                        
                                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))
                                        
                                                                                    AppTitle(text = stringResource(R.string.mnemonic_display_warning_important))
                                            
                                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))
                                        
                                        NumberedList(
                                            items = listOf(
                                                createHighlightedItem("WE'LL GENERATE A SECURE 12-WORD ", "RECOVERY PHRASE"),
                                                createHighlightedItem("WE SUGGEST WRITING THIS ON A PIECE OF PAPER AND ", "STORE IT IN A SECURE PLACE"),
                                                createHighlightedItem("DON'T ", "SHARE", " IT WITH ANYONE"),
                                                createHighlightedItem("DARKLAKE ", "CANNOT RECOVER", " YOUR WALLET IF YOU LOSE THIS PHRASE")
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    },
                    position = FlexPosition.Flex,
                    spacing = DesignTokens.Spacing.logoToContent
                ),
                // Bottom section: Generate button
                FlexSection(
                    content = {
                        AppButton(
                            text = if (uiState.isLoading) stringResource(R.string.create_wallet_generating) else stringResource(R.string.create_wallet_button),
                            onClick = { viewModel.createWallet() },
                            isPrimary = true,
                            enabled = !uiState.isLoading
                        )
                        
                        // Error message
                uiState.error?.let { error ->
                            Spacer(modifier = Modifier.height(DesignTokens.Layout.componentGap))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
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
fun CreateWalletScreenPreview() {
    DarklakeWalletTheme {
        BackgroundWithOverlay {
            FlexLayout(
                sections = listOf(
                    // Top section: Back button + Logo
                    FlexSection(
                        content = {
                            ModalHeader(onBackClick = {})
                        },
                        position = FlexPosition.Top,
                        topSpacing = DesignTokens.Spacing.sm,
                        bottomSpacing = DesignTokens.Spacing.logoToContent
                    ),
                    // Middle section: Title and content
                    FlexSection(
                        content = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AppTitle(text = "Create your Secure Wallet")
                                
                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.xxl))
                                
                                // Warning container
                                AppContainer(
                                    variant = ContainerVariant.Shadowed,
                                    horizontalPadding = DesignTokens.Sizing.messageBoxPadding,
                                    verticalPadding = DesignTokens.Sizing.messageBoxVerticalPadding,
                                    content = {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Warning icon
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = "Warning",
                                                tint = DesignTokens.Colors.Warning,
                                                modifier = Modifier.size(DesignTokens.Spacing.xl)
                                            )
                                            
                                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))
                                            
                                            AppTitle(text = "Important")
                                            
                                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))
                                            
                                            NumberedList(
                                                items = listOf(
                                                    createHighlightedItem("WE'LL GENERATE A SECURE 12-WORD ", "RECOVERY PHRASE"),
                                                    createHighlightedItem("WE SUGGEST WRITING THIS ON A PIECE OF PAPER AND ", "STORE IT IN A SECURE PLACE"),
                                                    createHighlightedItem("DON'T ", "SHARE", " IT WITH ANYONE"),
                                                    createHighlightedItem("DARKLAKE ", "CANNOT RECOVER", " YOUR WALLET IF YOU LOSE THIS PHRASE")
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                        },
                        position = FlexPosition.Flex,
                        spacing = DesignTokens.Spacing.logoToContent
                    ),
                    // Bottom section: Generate button
                    FlexSection(
                        content = {
                            AppButton(
                                text = "GENERATE SECURE WALLET",
                                onClick = {},
                                isPrimary = true
                            )
                        },
                        position = FlexPosition.Bottom,
                        bottomSpacing = DesignTokens.Spacing.xs
                    )
                )
            )
        }
    }
}