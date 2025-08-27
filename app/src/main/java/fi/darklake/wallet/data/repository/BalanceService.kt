package fi.darklake.wallet.data.repository

import android.content.Context
import fi.darklake.wallet.data.api.HeliusApiService
import fi.darklake.wallet.data.model.getHeliusRpcUrl
import fi.darklake.wallet.data.preferences.SettingsManager

/**
 * Singleton service for managing wallet balances across the app
 */
class BalanceService private constructor(
    private val settingsManager: SettingsManager,
    private val context: Context
) {
    private var balanceRepository: BalanceRepository? = null
    
    companion object {
        @Volatile
        private var INSTANCE: BalanceService? = null
        
        fun getInstance(context: Context): BalanceService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val settingsManager = SettingsManager(context)
                    BalanceService(settingsManager, context).also { INSTANCE = it }
                }
            }
        }
    }
    
    fun getRepository(): BalanceRepository {
        if (balanceRepository == null) {
            val networkSettings = settingsManager.networkSettings.value
            val apiService = HeliusApiService { networkSettings.getHeliusRpcUrl() }
            balanceRepository = BalanceRepository(apiService, context)
        }
        return balanceRepository!!
    }
    
    /**
     * Clear repository when network changes
     */
    fun clearRepository() {
        balanceRepository?.clearCache()
        balanceRepository = null
    }
}