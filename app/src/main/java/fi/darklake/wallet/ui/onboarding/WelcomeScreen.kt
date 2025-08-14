package fi.darklake.wallet.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.darklake.wallet.R
import fi.darklake.wallet.ui.theme.DarklakeWalletTheme
import fi.darklake.wallet.ui.theme.NeonGreen
import fi.darklake.wallet.ui.theme.ElectricBlue

@Composable
fun WelcomeScreen(
    onCreateWallet: () -> Unit,
    onImportWallet: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    ),
                    radius = 1200f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo placeholder - would use SVG when properly converted
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DARKLAKE",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "DARKLAKE",
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "WALLET",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Your trades become invisible.\nYour moves, untraceable.\nYour value, preserved.\nYour identity, yours again.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Button(
                onClick = onCreateWallet,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "CREATE NEW WALLET",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onImportWallet,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 2.dp
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "IMPORT EXISTING WALLET",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "Secure • Private • Decentralized",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    DarklakeWalletTheme {
        WelcomeScreen(
            onCreateWallet = {},
            onImportWallet = {}
        )
    }
}