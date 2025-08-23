package fi.darklake.wallet.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.darklake.wallet.ui.design.DarklakePrimary
import fi.darklake.wallet.ui.design.DarklakeTertiary

@Composable
fun DarklakeNavItem(
    @DrawableRes iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val iconColor = if (selected) DarklakePrimary else DarklakeTertiary
    val textColor = if (selected) DarklakePrimary else DarklakeTertiary
    
    Column(
        modifier = Modifier
            .wrapContentSize()
            .noRippleClickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = iconRes),
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label.uppercase(),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp
        )
    }
}