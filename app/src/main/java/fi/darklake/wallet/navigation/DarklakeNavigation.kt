package fi.darklake.wallet.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.darklake.wallet.ui.screens.onboarding.WelcomeScreen
import fi.darklake.wallet.ui.screens.onboarding.CreateWalletScreen
import fi.darklake.wallet.ui.screens.onboarding.ImportWalletScreen
import fi.darklake.wallet.ui.screens.onboarding.MnemonicDisplayScreen
import fi.darklake.wallet.ui.screens.onboarding.MnemonicVerificationScreen
import fi.darklake.wallet.ui.screens.onboarding.SharedWalletViewModel
import fi.darklake.wallet.ui.screens.MainScreen
import fi.darklake.wallet.ui.screens.wallet.WalletViewModel
import fi.darklake.wallet.ui.screens.send.SendSolScreen
import fi.darklake.wallet.ui.screens.send.SendTokenScreen
import fi.darklake.wallet.ui.screens.send.SendNftScreen
import fi.darklake.wallet.ui.screens.receive.ReceiveScreen
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.data.preferences.SettingsManager

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object CreateWallet : Screen("create_wallet")
    data object ImportWallet : Screen("import_wallet")
    data object MnemonicDisplay : Screen("mnemonic_display")
    data object MnemonicVerification : Screen("mnemonic_verification")
    data object Wallet : Screen("wallet")
    data object SendSol : Screen("send_sol")
    data object SendToken : Screen("send_token/{tokenMint}")
    data object SendNft : Screen("send_nft/{nftMint}")
    data object Receive : Screen("receive")
    
    companion object {
        fun sendToken(tokenMint: String) = "send_token/$tokenMint"
        fun sendNft(nftMint: String) = "send_nft/$nftMint"
    }
}

@Composable
fun DarklakeNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Welcome.route,
    storageManager: WalletStorageManager,
    settingsManager: SettingsManager
) {
    val sharedViewModel: SharedWalletViewModel = viewModel()
    val walletViewModel: WalletViewModel = viewModel { WalletViewModel(storageManager, settingsManager) }
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
            MainScreen(
                storageManager = storageManager,
                settingsManager = settingsManager,
                onNavigateToSendSol = {
                    navController.navigate(Screen.SendSol.route)
                },
                onNavigateToSendToken = { tokenMint ->
                    navController.navigate(Screen.sendToken(tokenMint))
                },
                onNavigateToSendNft = { nftMint ->
                    navController.navigate(Screen.sendNft(nftMint))
                },
                onNavigateToReceive = {
                    navController.navigate(Screen.Receive.route)
                }
            )
        }
        
        composable(Screen.SendSol.route) {
            SendSolScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSuccess = {
                    walletViewModel.refresh()
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
                onSuccess = {
                    walletViewModel.refresh()
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
                onSuccess = {
                    walletViewModel.refresh()
                },
                storageManager = storageManager,
                settingsManager = settingsManager
            )
        }
        
        composable(Screen.Receive.route) {
            ReceiveScreen(
                onBack = {
                    navController.popBackStack()
                },
                storageManager = storageManager
            )
        }
    }
}