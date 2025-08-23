package fi.darklake.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            .background(DarklakeBackground)
            .border(
                width = 1.dp,
                color = DarklakeBorder,
                shape = RoundedCornerShape(0.dp)
            )
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { tab ->
            DarklakeNavItem(
                icon = tab.icon,
                label = tab.title,
                selected = currentRoute == tab.route,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}