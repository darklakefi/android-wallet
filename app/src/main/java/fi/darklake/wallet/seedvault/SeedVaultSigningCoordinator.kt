package fi.darklake.wallet.seedvault

import android.content.Intent
import android.util.Log
import com.solana.core.Transaction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Coordinator for Seed Vault transaction signing operations.
 * Manages the communication between the wallet layer and the UI layer for activity-based signing.
 */
object SeedVaultSigningCoordinator {
    private const val TAG = "SeedVaultSigningCoordinator"

    data class SigningRequest(
        val transaction: Transaction,
        val authToken: Long,
        val intent: Intent,
        val deferred: CompletableDeferred<ByteArray>
    )

    private var currentRequest: SigningRequest? = null

    private val _signingState = MutableStateFlow<SigningState>(SigningState.Idle)
    val signingState: StateFlow<SigningState> = _signingState

    sealed class SigningState {
        object Idle : SigningState()
        data class RequiresUserAction(val intent: Intent) : SigningState()
        object WaitingForResult : SigningState()
        object Completed : SigningState()
        data class Error(val message: String) : SigningState()
    }

    /**
     * Initiate a signing request.
     * @return A deferred that will complete with the signed transaction bytes
     */
    fun requestSigning(
        transaction: Transaction,
        authToken: Long,
        messageBytes: ByteArray
    ): CompletableDeferred<ByteArray> {
        Log.d(TAG, "Requesting Seed Vault signing for transaction")

        val deferred = CompletableDeferred<ByteArray>()
        // Create intent without context - just need to build the intent
        val intent = Intent(SeedVaultManager.ACTION_SIGN_TRANSACTION).apply {
            setPackage("com.solanamobile.seedvaultimpl")
            putExtra(SeedVaultManager.EXTRA_SEED_AUTH_TOKEN, authToken)
            putExtra(SeedVaultManager.EXTRA_TRANSACTION, messageBytes)
        }

        currentRequest = SigningRequest(
            transaction = transaction,
            authToken = authToken,
            intent = intent,
            deferred = deferred
        )

        _signingState.value = SigningState.RequiresUserAction(intent)
        return deferred
    }

    /**
     * Get the current signing intent to launch
     */
    fun getCurrentSigningIntent(): Intent? {
        return currentRequest?.intent
    }

    /**
     * Process the result from Seed Vault signing activity
     */
    fun processSigningResult(resultCode: Int, data: Intent?) {
        val request = currentRequest
        if (request == null) {
            Log.e(TAG, "No current signing request to process")
            _signingState.value = SigningState.Error("No signing request found")
            return
        }

        _signingState.value = SigningState.WaitingForResult

        if (resultCode == android.app.Activity.RESULT_OK && data != null) {
            val signedTransaction = data.getByteArrayExtra(SeedVaultManager.EXTRA_SIGNED_TRANSACTION)
            if (signedTransaction != null) {
                Log.d(TAG, "Successfully received signed transaction: ${signedTransaction.size} bytes")
                request.deferred.complete(signedTransaction)
                _signingState.value = SigningState.Completed
            } else {
                Log.e(TAG, "No signed transaction in result")
                request.deferred.completeExceptionally(Exception("No signed transaction in result"))
                _signingState.value = SigningState.Error("No signed transaction received")
            }
        } else {
            Log.e(TAG, "Seed Vault signing cancelled or failed")
            request.deferred.completeExceptionally(Exception("Seed Vault signing cancelled"))
            _signingState.value = SigningState.Error("Signing cancelled")
        }

        currentRequest = null
    }

    /**
     * Cancel the current signing request
     */
    fun cancelSigning() {
        currentRequest?.deferred?.completeExceptionally(Exception("Signing cancelled by user"))
        currentRequest = null
        _signingState.value = SigningState.Idle
    }

    /**
     * Reset the coordinator to idle state
     */
    fun reset() {
        currentRequest = null
        _signingState.value = SigningState.Idle
    }
}