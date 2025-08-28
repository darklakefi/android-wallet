package fi.darklake.wallet.diagnostics

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class DiagnosticLogger private constructor() {
    
    companion object {
        private const val TAG = "DiagnosticLogger"
        private const val MAX_LOG_ENTRIES = 1000
        private const val LOG_FILE_NAME = "diagnostic_logs.txt"
        
        @Volatile
        private var INSTANCE: DiagnosticLogger? = null
        
        fun getInstance(): DiagnosticLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DiagnosticLogger().also { INSTANCE = it }
            }
        }
    }
    
    private val logEntries = ConcurrentLinkedQueue<LogEntry>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    data class LogEntry(
        val timestamp: String,
        val level: String,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null
    )
    
    fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )
        
        logEntries.offer(entry)
        
        // Keep only the most recent entries
        while (logEntries.size > MAX_LOG_ENTRIES) {
            logEntries.poll()
        }
        
        // Also log to system logcat
        when (level.uppercase()) {
            "DEBUG" -> Log.d(tag, message, throwable)
            "INFO" -> Log.i(tag, message, throwable)
            "WARN" -> Log.w(tag, message, throwable)
            "ERROR" -> Log.e(tag, message, throwable)
            else -> Log.v(tag, message, throwable)
        }
    }
    
    fun debug(tag: String, message: String, throwable: Throwable? = null) {
        log("DEBUG", tag, message, throwable)
    }
    
    fun info(tag: String, message: String, throwable: Throwable? = null) {
        log("INFO", tag, message, throwable)
    }
    
    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        log("WARN", tag, message, throwable)
    }
    
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        log("ERROR", tag, message, throwable)
    }
    
    fun generateDiagnosticReport(context: Context): String {
        val deviceInfo = getDeviceInfo(context)
        val appInfo = getAppInfo(context)
        val logContent = getLogContent()
        
        return buildString {
            appendLine("=== DARKLAKE WALLET DIAGNOSTIC REPORT ===")
            appendLine("Generated: ${dateFormat.format(Date())}")
            appendLine()
            
            appendLine("=== DEVICE INFORMATION ===")
            appendLine(deviceInfo)
            appendLine()
            
            appendLine("=== APP INFORMATION ===")
            appendLine(appInfo)
            appendLine()
            
            appendLine("=== RECENT LOGS ===")
            appendLine(logContent)
        }
    }
    
    private fun getDeviceInfo(context: Context): String {
        return buildString {
            appendLine("Manufacturer: ${android.os.Build.MANUFACTURER}")
            appendLine("Model: ${android.os.Build.MODEL}")
            appendLine("Android Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Device: ${android.os.Build.DEVICE}")
            appendLine("Product: ${android.os.Build.PRODUCT}")
            appendLine("Available Storage: ${context.filesDir.usableSpace / (1024 * 1024)} MB")
            appendLine("Total Storage: ${context.filesDir.totalSpace / (1024 * 1024)} MB")
        }
    }
    
    private fun getAppInfo(context: Context): String {
        return buildString {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            appendLine("Package: ${context.packageName}")
            appendLine("Version: ${packageInfo.versionName} (${packageInfo.versionCode})")
            appendLine("Install Time: ${Date(packageInfo.firstInstallTime)}")
            appendLine("Last Update: ${Date(packageInfo.lastUpdateTime)}")
        }
    }
    
    private fun getLogContent(): String {
        return buildString {
            val entries = logEntries.toList()
            if (entries.isEmpty()) {
                appendLine("No logs available")
            } else {
                entries.forEach { entry ->
                    appendLine("[${entry.timestamp}] ${entry.level}/${entry.tag}: ${entry.message}")
                    entry.throwable?.let { throwable ->
                        appendLine("  Exception: ${throwable.javaClass.simpleName}: ${throwable.message}")
                        appendLine("  Stack trace: ${throwable.stackTraceToString()}")
                    }
                }
            }
        }
    }
    
    fun saveLogsToFile(context: Context): File {
        val file = File(context.filesDir, LOG_FILE_NAME)
        FileWriter(file).use { writer ->
            writer.write(generateDiagnosticReport(context))
        }
        return file
    }
    
    fun clearLogs() {
        logEntries.clear()
    }
}
