package fi.darklake.wallet.storage

import fi.darklake.wallet.crypto.SolanaWallet

interface WalletStorageProvider {
    suspend fun storeWallet(wallet: SolanaWallet): Result<Unit>
    suspend fun getWallet(): Result<SolanaWallet?>
    suspend fun deleteWallet(): Result<Unit>
    suspend fun hasWallet(): Boolean
    
    val isAvailable: Boolean
    val providerName: String
}

sealed class StorageError : Exception() {
    data class NotAvailable(override val message: String) : StorageError()
    data class StorageFailed(override val message: String) : StorageError()
    data class RetrievalFailed(override val message: String) : StorageError()
    data class DeletionFailed(override val message: String) : StorageError()
}