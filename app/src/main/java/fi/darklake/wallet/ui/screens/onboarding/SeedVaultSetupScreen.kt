package fi.darklake.wallet.ui.screens.onboarding

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.darklake.wallet.seedvault.SeedVaultManager
import fi.darklake.wallet.ui.components.*
import fi.darklake.wallet.ui.design.*
import kotlinx.coroutines.launch

/**
 * Seed Vault setup screen for Solana Seeker integration.
 *
 * Allows users to select an existing seed from Seed Vault or create a new one.
 * This provides secure hardware-backed key management for Solana Seeker devices.
 *
 * @param onSeedAuthorized Callback when a seed is successfully authorized
 * @param onBack Callback to navigate back
 */
@Composable
fun SeedVaultSetupScreen(
    onSeedAuthorized: (authToken: Long, publicKey: ByteArray) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val seedVaultManager = remember { SeedVaultManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var availableSeeds by remember { mutableStateOf<List<SeedVaultManager.Seed>>(emptyList()) }
    var authorizedSeeds by remember { mutableStateOf<List<SeedVaultManager.Seed>>(emptyList()) }
    var hasUnauthorizedSeeds by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Activity result launcher for Seed Vault authorization
    val authorizeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("SeedVaultSetup", "Authorization result: resultCode=${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val authToken = seedVaultManager.processAuthorizationResult(result.data)
            if (authToken != null) {
                // Successfully authorized, now get the public key
                Log.d("SeedVaultSetup", "Successfully authorized seed with token: $authToken")
                coroutineScope.launch {
                    // Get the public key from the accounts table
                    val publicKey = seedVaultManager.getPublicKeyForAuthToken(authToken)
                    if (publicKey != null && publicKey.isNotEmpty()) {
                        Log.d("SeedVaultSetup", "Got public key: ${publicKey.size} bytes")
                        onSeedAuthorized(authToken, publicKey)
                    } else {
                        // FAIL HARD - don't silently continue
                        val error = "FAILED to get public key for authorized seed with token $authToken"
                        Log.e("SeedVaultSetup", error)
                        errorMessage = error
                        throw IllegalStateException(error)
                    }
                }
            } else {
                errorMessage = "Failed to authorize seed"
            }
        } else {
            // Refresh lists even if cancelled
            coroutineScope.launch {
                availableSeeds = seedVaultManager.getUnauthorizedSeeds()
                authorizedSeeds = seedVaultManager.getAuthorizedSeeds()
            }
        }
    }

    // Activity result launcher for creating new seed
    val createSeedLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val authToken = seedVaultManager.processAuthorizationResult(result.data)
            if (authToken != null) {
                coroutineScope.launch {
                    // Refresh the seeds lists after creation
                    availableSeeds = seedVaultManager.getUnauthorizedSeeds()
                    authorizedSeeds = seedVaultManager.getAuthorizedSeeds()

                    val newSeed = authorizedSeeds.find { it.authToken == authToken }
                    if (newSeed != null) {
                        onSeedAuthorized(authToken, newSeed.publicKey)
                    }
                }
            } else {
                // Even if no auth token returned, refresh the lists
                coroutineScope.launch {
                    availableSeeds = seedVaultManager.getUnauthorizedSeeds()
                    authorizedSeeds = seedVaultManager.getAuthorizedSeeds()
                }
            }
        } else {
            // Even if cancelled, refresh the lists in case seed was created
            coroutineScope.launch {
                availableSeeds = seedVaultManager.getUnauthorizedSeeds()
                authorizedSeeds = seedVaultManager.getAuthorizedSeeds()
            }
        }
    }

    // Load available seeds on mount
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            availableSeeds = seedVaultManager.getUnauthorizedSeeds()
            authorizedSeeds = seedVaultManager.getAuthorizedSeeds()
            hasUnauthorizedSeeds = seedVaultManager.hasUnauthorizedSeeds()
        } catch (e: Exception) {
            Log.e("SeedVaultSetup", "Failed to load seeds", e)
            errorMessage = "Failed to load Seed Vault data"
        }
        isLoading = false
    }

    BackgroundWithOverlay {
        FlexLayout(
            sections = listOf(
                // Top section: Back button and title
                FlexSection(
                    content = {
                        // Back button will be added when component is available

                        Spacer(modifier = Modifier.height(DesignTokens.Layout.componentGap))

                        AppTitle(text = "Seed Vault Setup")

                        Spacer(modifier = Modifier.height(DesignTokens.Layout.componentGap))

                        AppBodyText(text = "Use your device's secure hardware for key management")
                    },
                    position = FlexPosition.Top,
                    topSpacing = DesignTokens.Spacing.top,
                    bottomSpacing = DesignTokens.Spacing.logoToContent
                ),

                // Middle section: Seeds list
                FlexSection(
                    content = {
                        AppContainer(
                            variant = ContainerVariant.Shadowed,
                            horizontalPadding = DesignTokens.Sizing.messageBoxPadding,
                            verticalPadding = DesignTokens.Sizing.messageBoxVerticalPadding,
                            content = {
                            when {
                                isLoading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = DarklakeButtonBg
                                        )
                                    }
                                }

                                errorMessage != null -> {
                                    Text(
                                        text = errorMessage!!,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 14.sp
                                    )
                                }

                                else -> {
                                    Column {
                                        // Show authorized seeds if any
                                        if (authorizedSeeds.isNotEmpty()) {
                                            Text(
                                                text = "AUTHORIZED SEEDS",
                                                color = DarklakeTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )

                                            authorizedSeeds.forEach { seed ->
                                                SeedItem(
                                                    seed = seed,
                                                    isAuthorized = true,
                                                    onClick = {
                                                        // For authorized seeds, we need to get the account info from accounts
                                                        coroutineScope.launch {
                                                            val accountInfo = seedVaultManager.getAccountInfoForAuthToken(seed.authToken)
                                                            if (accountInfo != null && accountInfo.publicKey.isNotEmpty()) {
                                                                Log.d("SeedVaultSetup", "Got account info for authorized seed: ${accountInfo.publicKey.size} bytes, path: ${accountInfo.derivationPath}")
                                                                onSeedAuthorized(seed.authToken, accountInfo.publicKey)
                                                            } else {
                                                                val error = "FAILED to get account info for authorized seed with token ${seed.authToken}"
                                                                Log.e("SeedVaultSetup", error)
                                                                errorMessage = error
                                                            }
                                                        }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }

                                            if (availableSeeds.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                            }
                                        }

                                        // Show available seeds
                                        if (availableSeeds.isNotEmpty()) {
                                            Text(
                                                text = "AVAILABLE SEEDS",
                                                color = DarklakeTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )

                                            availableSeeds.forEach { seed ->
                                                SeedItem(
                                                    seed = seed,
                                                    isAuthorized = false,
                                                    onClick = {
                                                        Log.d("SeedVaultSetup", "Clicked on available seed with purpose: ${seed.purpose}")
                                                        // For unauthorized seeds, use the purpose to create authorization intent
                                                        // Do NOT pass a seed name - existing seeds already have names
                                                        val intent = seedVaultManager.createAuthorizeSeedIntent(
                                                            purpose = seed.purpose // Just pass the purpose
                                                        )
                                                        Log.d("SeedVaultSetup", "Launching authorization intent: $intent")
                                                        try {
                                                            authorizeLauncher.launch(intent)
                                                            Log.d("SeedVaultSetup", "Intent launched successfully")
                                                        } catch (e: Exception) {
                                                            Log.e("SeedVaultSetup", "Failed to launch intent", e)
                                                            errorMessage = "Failed to launch Seed Vault: ${e.message}"
                                                        }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }

                                        // Show message if no seeds available
                                        if (availableSeeds.isEmpty() && authorizedSeeds.isEmpty()) {
                                            if (hasUnauthorizedSeeds) {
                                                Column {
                                                    Text(
                                                        text = "Seeds detected in Seed Vault",
                                                        color = DarklakeTextPrimary,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                    )
                                                    Text(
                                                        text = "To use existing seeds:",
                                                        color = DarklakeTextSecondary,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                                    )
                                                    Text(
                                                        text = "1. Open the Seed Vault app",
                                                        color = DarklakeTextSecondary,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(bottom = 2.dp)
                                                    )
                                                    Text(
                                                        text = "2. Select a seed",
                                                        color = DarklakeTextSecondary,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(bottom = 2.dp)
                                                    )
                                                    Text(
                                                        text = "3. Authorize it for Darklake Wallet",
                                                        color = DarklakeTextSecondary,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(bottom = 16.dp)
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = "No seeds found in Seed Vault",
                                                    color = DarklakeTextSecondary,
                                                    fontSize = 14.sp,
                                                    modifier = Modifier.padding(vertical = 16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                },
                    position = FlexPosition.Flex,
                    spacing = DesignTokens.Spacing.logoToContent
                ),

                // Bottom section: Create new seed button
                FlexSection(
                    content = {
                        AppButton(
                            text = "CREATE NEW SEED",
                            onClick = {
                                // For creating new seeds, we can suggest a name
                                val intent = seedVaultManager.createNewSeedIntent(
                                    seedName = "Darklake Wallet ${System.currentTimeMillis() / 1000}"
                                )
                                createSeedLauncher.launch(intent)
                            },
                            isPrimary = true
                        )

                        Spacer(modifier = Modifier.height(DesignTokens.Layout.buttonGap))

                        AppButton(
                            text = "IMPORT EXISTING SEED",
                            onClick = {
                                // For importing seeds, we can suggest a name
                                val intent = seedVaultManager.createImportSeedIntent(
                                    seedName = "Imported Wallet ${System.currentTimeMillis() / 1000}"
                                )
                                createSeedLauncher.launch(intent)
                            },
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

/**
 * Individual seed item in the list
 */
@Composable
private fun SeedItem(
    seed: SeedVaultManager.Seed,
    isAuthorized: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isAuthorized) DarklakeCardBackground else DarklakeBackground,
        border = if (!isAuthorized) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    Log.d("SeedItem", "Clicked on seed: ${seed.name}, authorized: $isAuthorized, purpose: ${seed.purpose}")
                    onClick()
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = seed.name,
                    color = DarklakeTextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isAuthorized) "Tap to use" else "Tap to authorize",
                    color = DarklakeTextSecondary,
                    fontSize = 12.sp
                )
            }

            if (isAuthorized) {
                Text(
                    text = "✓",
                    color = DarklakeButtonBg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}