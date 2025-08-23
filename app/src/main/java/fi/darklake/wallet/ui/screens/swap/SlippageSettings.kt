package fi.darklake.wallet.ui.screens.swap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.R
import fi.darklake.wallet.ui.utils.FormatUtils

@Composable
internal fun SlippageSettings(
    slippagePercent: Double,
    onSlippageChange: (Double, Boolean) -> Unit
) {
    var showCustom by remember { mutableStateOf(false) }
    var customSlippage by remember { mutableStateOf("") }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.swap_slippage_tolerance),
                style = MaterialTheme.typography.labelMedium
            )
            
            Text(
                text = "$slippagePercent%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preset options
            listOf(0.1, 0.5, 1.0).forEach { slippage ->
                FilterChip(
                    selected = slippagePercent == slippage && !showCustom,
                    onClick = {
                        showCustom = false
                        onSlippageChange(slippage, false)
                    },
                    label = { Text("$slippage%") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Custom option
            FilterChip(
                selected = showCustom,
                onClick = { showCustom = !showCustom },
                label = { Text(stringResource(R.string.swap_slippage_custom)) },
                modifier = Modifier.weight(1f)
            )
        }
        
        AnimatedVisibility(visible = showCustom) {
            OutlinedTextField(
                value = customSlippage,
                onValueChange = { value ->
                    val filteredValue = FormatUtils.filterNumericInput(value)
                    customSlippage = filteredValue
                    filteredValue.toDoubleOrNull()?.let { slippage ->
                        if (slippage in 0.0..50.0) {
                            onSlippageChange(slippage, true)
                        }
                    }
                },
                label = { Text(stringResource(R.string.swap_slippage_custom_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true
            )
        }
    }
}