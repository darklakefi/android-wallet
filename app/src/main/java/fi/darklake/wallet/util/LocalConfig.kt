package fi.darklake.wallet.util

import android.content.Context
import java.util.Properties

object LocalConfig {
    private var properties: Properties? = null
    
    fun init(context: Context) {
        if (properties == null) {
            properties = Properties()
            try {
                context.assets.open("local.properties").use { stream ->
                    properties?.load(stream)
                }
            } catch (e: Exception) {
                // File doesn't exist or can't be read - that's OK
                println("local.properties not found or couldn't be read: ${e.message}")
            }
        }
    }
    
    fun getHeliusApiKey(): String? {
        return properties?.getProperty("helius.api.key")
    }
}