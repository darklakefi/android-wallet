package fi.darklake.wallet.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Reusable error message card component
 */
@Composable
fun ErrorMessageCard(
    message: String,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            onDismiss?.let {
                TextButton(onClick = it) {
                    Text("DISMISS")
                }
            }
        }
    }
}

/**
 * Reusable success message card component
 */
@Composable
fun SuccessMessageCard(
    message: String,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = Color(0xFF4CAF50),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            onDismiss?.let {
                TextButton(
                    onClick = it,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("DISMISS")
                }
            }
        }
    }
}

/**
 * Reusable info message card component (used for LP screen success messages)
 */
@Composable
fun InfoMessageCard(
    message: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onTertiaryContainer
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}