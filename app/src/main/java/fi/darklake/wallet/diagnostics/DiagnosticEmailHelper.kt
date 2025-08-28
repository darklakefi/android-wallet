package fi.darklake.wallet.diagnostics

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class DiagnosticEmailHelper {
    
    companion object {
        private const val EMAIL_ADDRESS = "support@darklake.fi"
        private const val EMAIL_SUBJECT = "Darklake Wallet - Diagnostic Report"
    }
    
    private val emailBody = """
Hello Darklake Wallet Support,

I'm experiencing an issue with the app and would like to submit diagnostic information to help with troubleshooting.

Issue Description:
[Please describe the issue you're experiencing here]

Additional Information:
- This diagnostic report contains device information, app details, and recent logs
- No sensitive wallet data (private keys, mnemonics, or addresses) is included
- The logs may contain technical information that can help identify the problem

Thank you for your assistance.

Best regards,
[Your name]
    """.trimIndent()
    
    fun createEmailIntent(context: Context, logFile: File): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL_ADDRESS))
            putExtra(Intent.EXTRA_SUBJECT, EMAIL_SUBJECT)
            putExtra(Intent.EXTRA_TEXT, emailBody)
            
            // Attach the log file
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                logFile
            )
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        return Intent.createChooser(intent, "Send diagnostic report via email")
    }
    
    fun createEmailIntentWithText(context: Context, diagnosticText: String): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL_ADDRESS))
            putExtra(Intent.EXTRA_SUBJECT, EMAIL_SUBJECT)
            putExtra(Intent.EXTRA_TEXT, "$emailBody\n\n=== DIAGNOSTIC REPORT ===\n$diagnosticText")
        }
        
        return Intent.createChooser(intent, "Send diagnostic report via email")
    }
}
