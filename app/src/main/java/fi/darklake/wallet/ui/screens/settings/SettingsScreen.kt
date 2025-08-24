package fi.darklake.wallet.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.R
import fi.darklake.wallet.data.model.NetworkSettings
import fi.darklake.wallet.data.model.SolanaNetwork
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.ui.components.*
import fi.darklake.wallet.ui.design.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    storageManager: fi.darklake.wallet.storage.WalletStorageManager,
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel { SettingsViewModel(settingsManager, storageManager) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    
    var showApiKey by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(uiState.networkSettings.heliusApiKey ?: "") }
    var expandedNetwork by remember { mutableStateOf(false) }
    
    var walletAddress by remember { mutableStateOf("") }
    
    LaunchedEffect(uiState.networkSettings.heliusApiKey) {
        apiKeyInput = uiState.networkSettings.heliusApiKey ?: ""
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadWalletAddress()
        walletAddress = viewModel.getWalletAddress()
    }
    
    BackgroundWithOverlay {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Logo and Wallet Address
            AppHeader(
                walletAddress = walletAddress.ifEmpty { "Not connected" }
            )
            
            // Settings Title
            Text(
                text = "SETTINGS",
                style = Typography.headlineMedium.copy(
                    fontFamily = BitsumishiFontFamily,
                    fontSize = 28.sp
                ),
                color = DarklakeTextSecondary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            
            // Settings Sections
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                // Network Section
                SettingsSection(
                    title = "NETWORK"
                ) {
                    // Network Selector
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (expandedNetwork) DarklakeCardBackgroundAlt else DarklakeCardBackground,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = DarklakeBorder,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { expandedNetwork = !expandedNetwork }
                            .padding(12.dp, 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when(uiState.networkSettings.network) {
                                    SolanaNetwork.MAINNET -> "MAINNET-BETA"
                                    SolanaNetwork.DEVNET -> "DEVNET"
                                },
                                style = TerminalTextStyle,
                                color = DarklakePrimary,
                                fontSize = 18.sp
                            )
                            Icon(
                                imageVector = if (expandedNetwork) 
                                    Icons.Default.KeyboardArrowUp 
                                else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand",
                                tint = DarklakeTextTertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Network Options Dropdown
                    AnimatedVisibility(visible = expandedNetwork) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = DarklakeCardBackgroundAlt,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = DarklakeBorder,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        ) {
                            SolanaNetwork.values().forEach { network ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateNetwork(network)
                                            expandedNetwork = false
                                        }
                                        .padding(12.dp, 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when(network) {
                                                SolanaNetwork.MAINNET -> "MAINNET-BETA"
                                                SolanaNetwork.DEVNET -> "DEVNET"
                                            },
                                            style = TerminalTextStyle,
                                            color = if (network == uiState.networkSettings.network) 
                                                DarklakePrimary 
                                            else DarklakeTextPrimary,
                                            fontSize = 16.sp
                                        )
                                        if (network == uiState.networkSettings.network) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = DarklakePrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                if (network != SolanaNetwork.values().last()) {
                                    Divider(
                                        color = DarklakeBorder,
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Helius API Key Section
                SettingsSection(
                    title = "HELIUS API KEY"
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = DarklakeCardBackground,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = DarklakeBorder,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(12.dp, 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OutlinedTextField(
                                    value = apiKeyInput,
                                    onValueChange = { apiKeyInput = it },
                                    placeholder = { 
                                        Text(
                                            "Enter your Helius API key",
                                            style = TerminalTextStyle,
                                            color = DarklakeTextTertiary.copy(alpha = 0.5f),
                                            fontSize = 14.sp
                                        )
                                    },
                                    visualTransformation = if (showApiKey) 
                                        VisualTransformation.None 
                                    else PasswordVisualTransformation(),
                                    textStyle = TerminalTextStyle.copy(
                                        fontSize = 14.sp,
                                        color = DarklakeTextPrimary,
                                        fontFamily = MonospaceFontFamily
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DarklakePrimary,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = DarklakePrimary,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    trailingIcon = {
                                        Row {
                                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                                Icon(
                                                    imageVector = if (showApiKey) 
                                                        Icons.Default.VisibilityOff 
                                                    else Icons.Default.Visibility,
                                                    contentDescription = if (showApiKey) 
                                                        "Hide API key" 
                                                    else "Show API key",
                                                    tint = DarklakeTextTertiary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            if (apiKeyInput != uiState.networkSettings.heliusApiKey) {
                                                IconButton(
                                                    onClick = { 
                                                        viewModel.updateHeliusApiKey(apiKeyInput)
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Save,
                                                        contentDescription = "Save API key",
                                                        tint = DarklakePrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                // RPC Endpoint Section (Read-only display)
                SettingsSection(
                    title = "RPC ENDPOINT"
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = DarklakeCardBackground,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = DarklakeBorder,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(12.dp, 16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val rpcEndpoint = if (apiKeyInput.isNotEmpty()) {
                                when(uiState.networkSettings.network) {
                                    SolanaNetwork.MAINNET -> "HELIUS RPC (MAINNET)"
                                    SolanaNetwork.DEVNET -> "HELIUS RPC (DEVNET)"
                                }
                            } else {
                                when(uiState.networkSettings.network) {
                                    SolanaNetwork.MAINNET -> "PUBLIC RPC (MAINNET)"
                                    SolanaNetwork.DEVNET -> "PUBLIC RPC (DEVNET)"
                                }
                            }
                            
                            Text(
                                text = rpcEndpoint,
                                style = TerminalTextStyle,
                                color = DarklakePrimary,
                                fontSize = 18.sp
                            )
                            
                            if (apiKeyInput.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "API KEY: ${apiKeyInput.take(8)}...",
                                        style = TerminalTextStyle,
                                        color = DarklakeTextTertiary,
                                        fontSize = 14.sp,
                                        fontFamily = MonospaceFontFamily
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(apiKeyInput))
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy API key",
                                            tint = DarklakeTextTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Additional Settings Sections can be added here
                
                // About Section
                SettingsSection(
                    title = "ABOUT"
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = DarklakeCardBackground,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = DarklakeBorder,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(12.dp, 16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DARKLAKE WALLET",
                                style = TerminalTextStyle,
                                color = DarklakePrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "VERSION 1.0.0",
                                style = TerminalTextStyle,
                                color = DarklakeTextTertiary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "OPEN SOURCE SOLANA WALLET",
                                style = TerminalTextStyle,
                                color = DarklakeTextTertiary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = TerminalTextStyle.copy(
                fontSize = 18.sp,
                letterSpacing = 0.18.sp,
                fontWeight = FontWeight.Medium
            ),
            color = DarklakeTextSecondary
        )
        content()
    }
}