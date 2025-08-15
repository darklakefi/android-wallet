package fi.darklake.wallet.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AssetImage(
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    tint: Color? = null,
    fallback: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<BitmapPainter?>(null) }
    var error by remember { mutableStateOf(false) }
    
    LaunchedEffect(assetPath) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("AssetImage", "Attempting to load asset: $assetPath")
                context.assets.open(assetPath).use { inputStream ->
                    val androidBitmap = BitmapFactory.decodeStream(inputStream)
                    if (androidBitmap != null) {
                        Log.d("AssetImage", "Successfully loaded asset: $assetPath (${androidBitmap.width}x${androidBitmap.height})")
                        bitmap = BitmapPainter(androidBitmap.asImageBitmap())
                        error = false
                    } else {
                        Log.e("AssetImage", "Failed to decode bitmap from asset: $assetPath")
                        error = true
                    }
                }
            } catch (e: Exception) {
                Log.e("AssetImage", "Error loading asset: $assetPath", e)
                e.printStackTrace()
                error = true
            }
        }
    }
    
    when {
        bitmap != null -> {
            Image(
                painter = bitmap!!,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                colorFilter = tint?.let { ColorFilter.tint(it) }
            )
        }
        error -> {
            fallback()
        }
    }
}