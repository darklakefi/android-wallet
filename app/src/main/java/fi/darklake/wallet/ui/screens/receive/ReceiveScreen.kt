package fi.darklake.wallet.ui.screens.receive

import android.content.ClipData
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import fi.darklake.wallet.R
import fi.darklake.wallet.storage.WalletStorageManager
import fi.darklake.wallet.ui.components.AppButton
import fi.darklake.wallet.ui.components.BackgroundWithOverlay
import fi.darklake.wallet.ui.components.ModalHeader
import fi.darklake.wallet.ui.design.DarklakeBorder
import fi.darklake.wallet.ui.design.DarklakeButtonIcon
import fi.darklake.wallet.ui.design.DarklakeInputBackground
import fi.darklake.wallet.ui.design.DarklakePrimary
import fi.darklake.wallet.ui.design.DarklakeTextPrimary
import fi.darklake.wallet.ui.design.DarklakeTextSecondary
import fi.darklake.wallet.ui.design.DarklakeTextTertiary
import fi.darklake.wallet.ui.design.DarklakeWalletTheme
import fi.darklake.wallet.ui.design.TerminalTextStyle
import fi.darklake.wallet.ui.design.Typography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReceiveScreen(
    onBack: () -> Unit,
    storageManager: WalletStorageManager
) {
    var publicKey by remember { mutableStateOf<String?>(null) }
    var qrCodeBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val clipboardManager = LocalClipboard.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val walletResult = storageManager.getWallet()
            val wallet = walletResult.getOrNull()
            publicKey = wallet?.publicKey
            
            wallet?.publicKey?.let { address ->
                qrCodeBitmap = generateQRCode(address)
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModalHeader(
                    onBackClick = onBack,
                    logoResId = R.drawable.darklake_logo,
                    contentDescription = "Darklake Logo"
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "RECEIVE",
                        style = Typography.headlineLarge,
                        color = DarklakeTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        qrCodeBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } ?: CircularProgressIndicator(
                            color = DarklakePrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Your Wallet Address",
                            style = Typography.labelMedium,
                            color = DarklakeTextTertiary
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarklakeInputBackground, RoundedCornerShape(4.dp))
                                .border(1.dp, DarklakeBorder, RoundedCornerShape(4.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = publicKey ?: "Loading...",
                                style = TerminalTextStyle.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                ),
                                color = DarklakeTextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    AppButton(
                        text = "COPY",
                        onClick = {
                            publicKey?.let { address ->
                                coroutineScope.launch {
                                    val clipData = ClipData.newPlainText("Wallet Address", address)
                                    clipboardManager.setClipEntry(ClipEntry(clipData))
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Default.ContentCopy,
                        iconTint = DarklakeButtonIcon,
                        contentPadding = PaddingValues(10.dp)
                    )
                    
                    AppButton(
                        text = "SHARE",
                        onClick = {
                            publicKey?.let { address ->
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, address)
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, "Share wallet address")
                                context.startActivity(shareIntent)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Default.Share,
                        iconTint = DarklakeButtonIcon,
                        contentPadding = PaddingValues(10.dp)
                    )
                }
            }
        }
    }
}

private fun generateQRCode(text: String): android.graphics.Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(
            text,
            BarcodeFormat.QR_CODE,
            512,
            512,
            mapOf(EncodeHintType.MARGIN to 1)
        )
        
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) Color.Black.toArgb() else Color.White.toArgb()
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Preview(showBackground = true)
@Composable
fun ReceiveScreenPreview() {
    DarklakeWalletTheme {
        ReceiveScreen(
            onBack = {},
            storageManager = WalletStorageManager(LocalContext.current)
        )
    }
}