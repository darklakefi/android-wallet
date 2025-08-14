package fi.darklake.wallet.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.darklake.wallet.ui.theme.*
import kotlin.math.sin
import kotlin.random.Random

// Enhanced retro grid background with matrix effects
@Composable
fun EnhancedRetroBackground(
    modifier: Modifier = Modifier,
    enableMatrixEffect: Boolean = true,
    enableScanlines: Boolean = true,
    enableNoise: Boolean = true,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "retro_effects")
    
    // Matrix falling code animation
    val matrixOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "matrix"
    )
    
    // Scanline animation
    val scanlineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "scanlines"
    )
    
    // Grid pulse animation
    val gridPulse by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "grid_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NearBlack)
            .drawWithContent {
                // Draw the main content first
                drawContent()
                
                // Draw grid pattern
                drawGrid(gridPulse)
                
                // Draw matrix effect
                if (enableMatrixEffect) {
                    drawMatrixEffect(matrixOffset)
                }
                
                // Draw scanlines
                if (enableScanlines) {
                    drawScanlines(scanlineOffset)
                }
                
                // Draw noise overlay
                if (enableNoise) {
                    drawNoiseOverlay()
                }
            }
    ) {
        content()
    }
}

// Grid pattern drawing function
private fun DrawScope.drawGrid(pulse: Float) {
    val gridSize = 40.dp.toPx()
    val gridColor = GridLines.copy(alpha = pulse)
    
    // Vertical lines
    var x = 0f
    while (x < size.width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.dp.toPx()
        )
        x += gridSize
    }
    
    // Horizontal lines
    var y = 0f
    while (y < size.height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx()
        )
        y += gridSize
    }
}

// Matrix falling characters effect
private fun DrawScope.drawMatrixEffect(offset: Float) {
    val matrixChars = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F")
    val columnWidth = 20.dp.toPx()
    val charHeight = 16.dp.toPx()
    val numColumns = (size.width / columnWidth).toInt()
    
    val textSizePx = 12.sp.toPx()
    val paint = Paint().apply {
        color = NeonGreen.copy(alpha = 0.3f)
    }
    
    for (col in 0 until numColumns) {
        val x = col * columnWidth
        val startY = (offset % (size.height + 200)) - 200
        var y = startY
        
        while (y < size.height + charHeight) {
            val alpha = when {
                y < 0 -> 0f
                y > size.height -> 0f
                else -> (1f - (y - startY) / size.height) * 0.4f
            }
            
            if (alpha > 0) {
                val char = matrixChars[Random.nextInt(matrixChars.size)]
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        char,
                        x,
                        y,
                        paint.asFrameworkPaint().apply {
                            this.color = NeonGreen.copy(alpha = alpha).toArgb()
                            this.textSize = textSizePx
                        }
                    )
                }
            }
            y += charHeight * 2
        }
    }
}

// Scanline effect
private fun DrawScope.drawScanlines(offset: Float) {
    val scanlineHeight = 2.dp.toPx()
    val scanlineSpacing = 4.dp.toPx()
    val totalHeight = scanlineHeight + scanlineSpacing
    
    var y = -totalHeight + (offset * size.height)
    while (y < size.height) {
        drawRect(
            color = ScanlineOverlay,
            topLeft = Offset(0f, y),
            size = Size(size.width, scanlineHeight)
        )
        y += totalHeight
    }
}

// Noise texture overlay
private fun DrawScope.drawNoiseOverlay() {
    val noiseSize = 4.dp.toPx()
    val numDotsX = (size.width / noiseSize).toInt()
    val numDotsY = (size.height / noiseSize).toInt()
    
    for (x in 0 until numDotsX) {
        for (y in 0 until numDotsY) {
            if (Random.nextFloat() < 0.1f) {
                drawRect(
                    color = NoiseOverlay,
                    topLeft = Offset(x * noiseSize, y * noiseSize),
                    size = Size(noiseSize, noiseSize)
                )
            }
        }
    }
}

// Typewriter text effect
@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = OnSurface,
    typingSpeed: Int = 50, // milliseconds per character
    startDelay: Int = 0
) {
    var displayedText by remember { mutableStateOf("") }
    var currentIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(text) {
        currentIndex = 0
        displayedText = ""
        kotlinx.coroutines.delay(startDelay.toLong())
        
        while (currentIndex < text.length) {
            displayedText = text.substring(0, currentIndex + 1)
            currentIndex++
            kotlinx.coroutines.delay(typingSpeed.toLong())
        }
    }
    
    androidx.compose.material3.Text(
        text = "$displayedText${if (currentIndex < text.length) "_" else ""}",
        modifier = modifier,
        style = style,
        color = color
    )
}

// Glitch text effect
@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = NeonGreen,
    glitchIntensity: Float = 0.1f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glitch")
    
    val glitchOffset by infiniteTransition.animateValue(
        initialValue = Offset.Zero,
        targetValue = Offset(2f, 1f),
        typeConverter = Offset.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glitch_offset"
    )
    
    val shouldGlitch by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ), label = "should_glitch"
    )
    
    Box(modifier = modifier) {
        // Original text
        androidx.compose.material3.Text(
            text = text,
            style = style,
            color = color
        )
        
        // Glitch layers
        if (shouldGlitch > 0.95f && Random.nextFloat() < glitchIntensity) {
            androidx.compose.material3.Text(
                text = text,
                style = style,
                color = ElectricBlue.copy(alpha = 0.5f),
                modifier = Modifier.offset(
                    x = (glitchOffset.x * Random.nextFloat()).dp,
                    y = (glitchOffset.y * Random.nextFloat()).dp
                )
            )
            
            androidx.compose.material3.Text(
                text = text,
                style = style,
                color = ErrorRed.copy(alpha = 0.3f),
                modifier = Modifier.offset(
                    x = (-glitchOffset.x * Random.nextFloat()).dp,
                    y = (-glitchOffset.y * Random.nextFloat()).dp
                )
            )
        }
    }
}

// CRT screen curvature effect
@Composable
fun CRTEffect(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .drawWithContent {
                drawContent()
                
                // CRT screen reflection
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.3f, size.height * 0.2f),
                        radius = size.width * 0.6f
                    )
                )
                
                // Screen edges darkening
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f)
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.width * 0.8f
                    )
                )
            }
    ) {
        content()
    }
}

// Neon glow modifier
fun Modifier.neonGlow(
    glowColor: Color = NeonGreen,
    glowRadius: androidx.compose.ui.unit.Dp = 8.dp,
    alpha: Float = 0.6f
) = this.drawWithContent {
    val glowRadiusPx = glowRadius.toPx()
    
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            color = glowColor.copy(alpha = alpha)
            isAntiAlias = true
        }
        
        // Create blur effect
        paint.asFrameworkPaint().apply {
            maskFilter = android.graphics.BlurMaskFilter(
                glowRadiusPx,
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }
        
        // Draw the glow
        canvas.saveLayer(
            androidx.compose.ui.geometry.Rect(
                -glowRadiusPx,
                -glowRadiusPx,
                size.width + glowRadiusPx,
                size.height + glowRadiusPx
            ),
            paint
        )
        
        drawContent()
        
        canvas.restore()
    }
    
    // Draw the original content on top
    drawContent()
}