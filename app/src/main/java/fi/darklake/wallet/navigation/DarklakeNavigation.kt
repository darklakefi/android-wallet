package fi.darklake.wallet.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.ui.onboarding.WelcomeScreen
import fi.darklake.wallet.ui.onboarding.CreateWalletScreen
import fi.darklake.wallet.ui.onboarding.ImportWalletScreen
import fi.darklake.wallet.ui.onboarding.MnemonicDisplayScreen
import fi.darklake.wallet.ui.onboarding.MnemonicVerificationScreen
import fi.darklake.wallet.ui.onboarding.SharedWalletViewModel
import fi.darklake.wallet.ui.wallet.WalletScreen
import fi.darklake.wallet.ui.settings.SettingsScreen
import fi.darklake.wallet.ui.send.SendSolScreen
import fi.darklake.wallet.ui.send.SendTokenScreen
import fi.darklake.wallet.ui.send.SendNftScreen
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.data.preferences.SettingsManager

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object CreateWallet : Screen("create_wallet")
    data object ImportWallet : Screen("import_wallet")
    data object MnemonicDisplay : Screen("mnemonic_display")
    data object MnemonicVerification : Screen("mnemonic_verification")
    data object Wallet : Screen("wallet")
    data object Settings : Screen("settings")
    data object SendSol : Screen("send_sol")
    data object SendToken : Screen("send_token/{tokenMint}")
    data object SendNft : Screen("send_nft/{nftMint}")
    
    fun sendToken(tokenMint: String) = "send_token/$tokenMint"
    fun sendNft(nftMint: String) = "send_nft/$nftMint"
}

@Composable
fun DarklakeNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Welcome.route,
    storageManager: WalletStorageManager,
    settingsManager: SettingsManager
) {
    val sharedViewModel: SharedWalletViewModel = viewModel()
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onCreateWallet = {
                    navController.navigate(Screen.CreateWallet.route)
                },
                onImportWallet = {
                    navController.navigate(Screen.ImportWallet.route)
                }
            )
        }
        
        composable(Screen.CreateWallet.route) {
            CreateWalletScreen(
                onWalletCreated = { mnemonic ->
                    sharedViewModel.setMnemonic(mnemonic)
                    navController.navigate(Screen.MnemonicDisplay.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.ImportWallet.route) {
            ImportWalletScreen(
                onWalletImported = {
                    navController.navigate(Screen.Wallet.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.MnemonicDisplay.route) {
            MnemonicDisplayScreen(
                onContinue = {
                    navController.navigate(Screen.MnemonicVerification.route)
                },
                onBack = {
                    navController.popBackStack()
                },
                viewModel = sharedViewModel
            )
        }
        
        composable(Screen.MnemonicVerification.route) {
            MnemonicVerificationScreen(
                onVerified = {
                    navController.navigate(Screen.Wallet.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                },
                viewModel = sharedViewModel
            )
        }
        
        composable(Screen.Wallet.route) {
            WalletScreen(
                storageManager = storageManager,
                settingsManager = settingsManager,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToSendSol = {
                    navController.navigate(Screen.SendSol.route)
                },
                onNavigateToSendToken = { tokenMint ->
                    navController.navigate(Screen.sendToken(tokenMint))
                },
                onNavigateToSendNft = { nftMint ->
                    navController.navigate(Screen.sendNft(nftMint))
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                settingsManager = settingsManager,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.SendSol.route) {
            SendSolScreen(
                onBack = {
                    navController.popBackStack()
                },
                storageManager = storageManager,
                settingsManager = settingsManager
            )
        }
        
        composable(Screen.SendToken.route) { backStackEntry ->
            val tokenMint = backStackEntry.arguments?.getString("tokenMint") ?: ""
            SendTokenScreen(
                tokenMint = tokenMint,
                onBack = {
                    navController.popBackStack()
                },
                storageManager = storageManager,
                settingsManager = settingsManager
            )
        }
        
        composable(Screen.SendNft.route) { backStackEntry ->
            val nftMint = backStackEntry.arguments?.getString("nftMint") ?: ""
            SendNftScreen(
                nftMint = nftMint,
                onBack = {
                    navController.popBackStack()
                },
                storageManager = storageManager,
                settingsManager = settingsManager
            )
        }
    }
}