package fi.darklake.wallet.ui.screens

import androidx.annotation.DrawableRes
import fi.darklake.wallet.R

sealed class MainTab(
    val route: String,
    val title: String,
    @DrawableRes val iconRes: Int
) {
    data object Wallet : MainTab(
        "main_wallet", 
        "wallet", 
        R.drawable.ic_nav_wallet
    )
    data object Swap : MainTab(
        "main_swap", 
        "swap", 
        R.drawable.ic_nav_swap
    )
    data object LP : MainTab(
        "main_lp", 
        "liquidity", 
        R.drawable.ic_nav_liquidity
    )
    data object More : MainTab(
        "main_more", 
        "settings", 
        R.drawable.ic_nav_settings
    )
}