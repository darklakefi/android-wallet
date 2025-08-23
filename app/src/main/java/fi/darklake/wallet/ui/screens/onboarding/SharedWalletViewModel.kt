package fi.darklake.wallet.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import fi.darklake.wallet.crypto.SolanaWallet

class SharedWalletViewModel : ViewModel() {
    var currentMnemonic: List<String>? = null
        private set
    
    var currentWallet: SolanaWallet? = null
        private set
    
    fun setMnemonic(mnemonic: List<String>) {
        currentMnemonic = mnemonic
    }
    
    fun setWallet(wallet: SolanaWallet) {
        currentWallet = wallet
    }
    
    fun confirmWallet() {
        // Here we would save the wallet to secure storage
        // For now, just keep it in memory
    }
    
    fun clearWalletData() {
        currentMnemonic = null
        currentWallet = null
    }
}