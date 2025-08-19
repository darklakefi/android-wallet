package fi.darklake.wallet.ui.utils

import java.math.BigDecimal
import java.text.DecimalFormat

/**
 * Utility functions for formatting numbers and amounts in the UI
 */
object FormatUtils {
    
    /**
     * Formats a balance value for display
     * @param balance The balance to format
     * @return Formatted string representation
     */
    fun formatBalance(balance: BigDecimal): String {
        return when {
            balance == BigDecimal.ZERO -> "0"
            balance < BigDecimal("0.0001") -> "<0.0001"
            else -> DecimalFormat("#,##0.####").format(balance)
        }
    }
    
    /**
     * Formats an amount with up to 6 decimal places
     * @param amount The amount to format
     * @return Formatted string representation
     */
    fun formatAmount(amount: Double): String {
        return DecimalFormat("#,##0.######").format(amount)
    }
    
    /**
     * Formats an amount as BigDecimal
     * @param amount The amount to format
     * @return Formatted string representation
     */
    fun formatAmount(amount: BigDecimal): String {
        return formatAmount(amount.toDouble())
    }
    
    /**
     * Filters numeric input to only allow valid decimal numbers
     * @param input The input string to filter
     * @return Filtered string containing only valid numeric characters
     */
    fun filterNumericInput(input: String): String {
        // Only allow digits, one decimal point, and commas
        return input.filter { it.isDigit() || it == '.' || it == ',' }
            .let { filtered ->
                // Prevent multiple decimal points
                val dotCount = filtered.count { it == '.' }
                if (dotCount > 1) {
                    val firstDotIndex = filtered.indexOf('.')
                    filtered.substring(0, firstDotIndex + 1) + 
                    filtered.substring(firstDotIndex + 1).replace(".", "")
                } else {
                    filtered
                }
            }
    }
    
    /**
     * Formats a percentage value
     * @param percentage The percentage value
     * @param decimals Number of decimal places to show
     * @return Formatted percentage string
     */
    fun formatPercentage(percentage: Double, decimals: Int = 2): String {
        val format = DecimalFormat("#,##0.${"#".repeat(decimals)}")
        return "${format.format(percentage)}%"
    }
}