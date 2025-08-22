package fi.darklake.wallet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import fi.darklake.wallet.ui.theme.DarklakePrimary
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.R

@Composable
fun DarklakeLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = DarklakePrimary
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.darklake_logo),
            contentDescription = "Darklake Logo",
            modifier = Modifier.size(size),
            colorFilter = ColorFilter.tint(tint)
        )
    }
}