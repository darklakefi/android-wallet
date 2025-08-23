package fi.darklake.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.ui.design.DarklakeBackground
import fi.darklake.wallet.ui.design.DarklakeBorder

@Composable
fun DarklakeBottomNavigation(
    currentRoute: String?,
    tabs: List<MainTab>,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // Draw top border only
                drawLine(
                    color = DarklakeBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .background(DarklakeBackground)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { tab ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                DarklakeNavItem(
                    iconRes = tab.iconRes,
                    label = tab.title,
                    selected = currentRoute == tab.route,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}