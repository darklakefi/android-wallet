package fi.darklake.wallet.ui.screens.swap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.darklake.wallet.R
import fi.darklake.wallet.data.preferences.SettingsManager
import fi.darklake.wallet.ui.components.BackgroundWithOverlay
import fi.darklake.wallet.ui.components.ModalHeader
import fi.darklake.wallet.ui.design.BitsumishiFontFamily
import fi.darklake.wallet.ui.design.DarklakeBackground
import fi.darklake.wallet.ui.design.DarklakeBorder
import fi.darklake.wallet.ui.design.DarklakeCardBackground
import fi.darklake.wallet.ui.design.DarklakeCardBackgroundAlt
import fi.darklake.wallet.ui.design.DarklakePrimary
import fi.darklake.wallet.ui.design.DarklakeTextPrimary
import fi.darklake.wallet.ui.design.DarklakeTextTertiary
import fi.darklake.wallet.ui.design.TerminalTextStyle
import fi.darklake.wallet.ui.design.Typography
import kotlinx.coroutines.launch

@Composable
fun SlippageSettingsScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedSlippage by remember { mutableFloatStateOf(1.0f) }
    var customSlippage by remember { mutableStateOf("") }
    var isCustomSelected by remember { mutableStateOf(false) }
    
    // Load saved slippage preference
    LaunchedEffect(Unit) {
        val savedSlippage = settingsManager.getSlippageTolerance()
        when (savedSlippage) {
            0.5f -> {
                selectedSlippage = 0.5f
                isCustomSelected = false
            }
            1.0f -> {
                selectedSlippage = 1.0f
                isCustomSelected = false
            }
            2.0f -> {
                selectedSlippage = 2.0f
                isCustomSelected = false
            }
            else -> {
                isCustomSelected = true
                customSlippage = savedSlippage.toString()
            }
        }
    }
    
    BackgroundWithOverlay {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section
            Column(
                verticalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                // Header
                ModalHeader(
                    onBackClick = onBack,
                    logoResId = R.drawable.darklake_logo,
                    contentDescription = "Darklake Logo"
                )
                
                // Title and options
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    Text(
                        text = "SET MAX SLIPPAGE",
                        style = Typography.headlineMedium.copy(
                            fontFamily = BitsumishiFontFamily,
                            fontSize = 28.sp
                        ),
                        color = DarklakePrimary,
                        textAlign = TextAlign.Center
                    )
                    
                    // Slippage options
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 0.5% option
                        SlippageOption(
                            percentage = "0.5%",
                            isSelected = !isCustomSelected && selectedSlippage == 0.5f,
                            onClick = {
                                selectedSlippage = 0.5f
                                isCustomSelected = false
                            }
                        )
                        
                        // 1.0% option
                        SlippageOption(
                            percentage = "1.0%",
                            isSelected = !isCustomSelected && selectedSlippage == 1.0f,
                            onClick = {
                                selectedSlippage = 1.0f
                                isCustomSelected = false
                            }
                        )
                        
                        // 2.0% option
                        SlippageOption(
                            percentage = "2.0%",
                            isSelected = !isCustomSelected && selectedSlippage == 2.0f,
                            onClick = {
                                selectedSlippage = 2.0f
                                isCustomSelected = false
                            }
                        )
                        
                        // Custom option
                        CustomSlippageOption(
                            value = customSlippage,
                            isSelected = isCustomSelected,
                            onValueChange = { value ->
                                customSlippage = value
                                isCustomSelected = true
                            },
                            onClick = {
                                isCustomSelected = true
                            }
                        )
                    }
                }
            }
            
            // Confirm button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = DarklakePrimary,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        scope.launch {
                            val slippageToSave = if (isCustomSelected) {
                                customSlippage.toFloatOrNull() ?: 1.0f
                            } else {
                                selectedSlippage
                            }
                            settingsManager.saveSlippageTolerance(slippageToSave)
                            onBack()
                        }
                    }
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "CONFIRM",
                    style = TerminalTextStyle.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = DarklakeBackground,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun SlippageOption(
    percentage: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) DarklakeCardBackgroundAlt else DarklakeCardBackground,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) DarklakePrimary else DarklakeBorder,
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(12.dp, 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSelected) "[X]" else "[  ]",
                style = TerminalTextStyle,
                color = if (isSelected) DarklakePrimary else DarklakeTextTertiary,
                fontSize = 18.sp
            )
            Text(
                text = percentage,
                style = TerminalTextStyle,
                color = if (isSelected) DarklakePrimary else DarklakeTextPrimary,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun CustomSlippageOption(
    value: String,
    isSelected: Boolean,
    onValueChange: (String) -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) DarklakeCardBackgroundAlt else DarklakeCardBackground,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) DarklakePrimary else DarklakeBorder,
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(12.dp, 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSelected) "[X]" else "[  ]",
                style = TerminalTextStyle,
                color = if (isSelected) DarklakePrimary else DarklakeTextTertiary,
                fontSize = 18.sp
            )
            
            Box(
                modifier = Modifier
                    .width(53.dp)
                    .background(
                        color = DarklakeCardBackgroundAlt,
                        shape = RoundedCornerShape(2.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = DarklakeTextTertiary,
                        shape = RoundedCornerShape(2.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = { newValue ->
                        // Allow only numbers and one decimal point
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            onValueChange(newValue)
                        }
                    },
                    textStyle = TerminalTextStyle.copy(
                        fontSize = 18.sp,
                        color = DarklakeTextPrimary,
                        textAlign = TextAlign.Right
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    cursorBrush = SolidColor(DarklakePrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Text(
                text = "%",
                style = TerminalTextStyle,
                color = DarklakeTextPrimary,
                fontSize = 18.sp
            )
        }
    }
}