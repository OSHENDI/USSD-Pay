package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

class QrScannerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_RESULT = "qr_result"
    }

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraControl: androidx.camera.core.CameraControl? = null
    private var isTorchOn by mutableStateOf(false)
    private var hasCameraPermission by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasCameraPermission = hasPermission
        if (!hasPermission) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            val isDark = PrefsManager(this).getIsDarkMode()
            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    if (hasCameraPermission) {
                        ScannerScreen(
                            onQrCodeScanned = { rawResult ->
                                val intent = Intent().apply {
                                    putExtra(EXTRA_RESULT, rawResult)
                                }
                                setResult(RESULT_OK, intent)
                                finish()
                            },
                            onClose = { finish() },
                            onToggleTorch = {
                                isTorchOn = !isTorchOn
                                cameraControl?.enableTorch(isTorchOn)
                            },
                            isTorchOn = isTorchOn,
                            onBindCamera = { control ->
                                cameraControl = control
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun ScannerScreen(
    onQrCodeScanned: (String) -> Unit,
    onClose: () -> Unit,
    onToggleTorch: () -> Unit,
    isTorchOn: Boolean,
    onBindCamera: (androidx.camera.core.CameraControl) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    var isScanned by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        if (!isScanned) {
                            processImageProxy(imageProxy) { result ->
                                if (!isScanned && result.isNotBlank()) {
                                    isScanned = true
                                    view.post {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        onQrCodeScanned(result)
                                    }
                                }
                            }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            ctx as androidx.lifecycle.LifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        onBindCamera(camera.cameraControl)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay with scanning frame and cutout
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxSize = size.width * 0.72f
            val left = (size.width - boxSize) / 2f
            val top = (size.height - boxSize) / 2.3f
            val cornerRadius = 24.dp.toPx()

            // Dark semi-transparent scrim
            val scrimPath = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
                addRoundRect(
                    RoundRect(
                        left = left,
                        top = top,
                        right = left + boxSize,
                        bottom = top + boxSize,
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                )
                fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            }
            drawPath(scrimPath, color = Color(0x99000000))

            // Corner brackets
            val cornerLength = 36.dp.toPx()
            val strokeWidth = 5.dp.toPx()
            val primaryColor = Color(0xFF2E7D32) // Palette primary

            // Top-Left
            drawLine(primaryColor, Offset(left, top + cornerLength), Offset(left, top + cornerRadius), strokeWidth, StrokeCap.Round)
            drawArc(primaryColor, 180f, 90f, false, Offset(left, top), Size(cornerRadius * 2, cornerRadius * 2), style = Stroke(strokeWidth, cap = StrokeCap.Round))
            drawLine(primaryColor, Offset(left + cornerRadius, top), Offset(left + cornerLength, top), strokeWidth, StrokeCap.Round)

            // Top-Right
            val right = left + boxSize
            drawLine(primaryColor, Offset(right - cornerLength, top), Offset(right - cornerRadius, top), strokeWidth, StrokeCap.Round)
            drawArc(primaryColor, 270f, 90f, false, Offset(right - cornerRadius * 2, top), Size(cornerRadius * 2, cornerRadius * 2), style = Stroke(strokeWidth, cap = StrokeCap.Round))
            drawLine(primaryColor, Offset(right, top + cornerRadius), Offset(right, top + cornerLength), strokeWidth, StrokeCap.Round)

            // Bottom-Left
            val bottom = top + boxSize
            drawLine(primaryColor, Offset(left, bottom - cornerLength), Offset(left, bottom - cornerRadius), strokeWidth, StrokeCap.Round)
            drawArc(primaryColor, 90f, 90f, false, Offset(left, bottom - cornerRadius * 2), Size(cornerRadius * 2, cornerRadius * 2), style = Stroke(strokeWidth, cap = StrokeCap.Round))
            drawLine(primaryColor, Offset(left + cornerRadius, bottom), Offset(left + cornerLength, bottom), strokeWidth, StrokeCap.Round)

            // Bottom-Right
            drawLine(primaryColor, Offset(right - cornerLength, bottom), Offset(right - cornerRadius, bottom), strokeWidth, StrokeCap.Round)
            drawArc(primaryColor, 0f, 90f, false, Offset(right - cornerRadius * 2, bottom - cornerRadius * 2), Size(cornerRadius * 2, cornerRadius * 2), style = Stroke(strokeWidth, cap = StrokeCap.Round))
            drawLine(primaryColor, Offset(right, bottom - cornerRadius), Offset(right, bottom - cornerLength), strokeWidth, StrokeCap.Round)

            // Animated Laser Line
            val laserY = top + (boxSize * laserPosition)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color(0xFF4CAF50), Color(0xFF81C784), Color(0xFF4CAF50), Color.Transparent),
                    startX = left,
                    endX = right
                ),
                start = Offset(left + 8.dp.toPx(), laserY),
                end = Offset(right - 8.dp.toPx(), laserY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Top App Bar Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0x66000000), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .size(44.dp)
                    .background(if (isTorchOn) Color(0xFF2E7D32) else Color(0x66000000), CircleShape)
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flashlight",
                    tint = Color.White
                )
            }
        }

        // Bottom Prompt Card
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xCC1E1E1E)),
                shape = RoundedCornerShape(20.dp)
            ) {
                val isAr = com.example.PrefsManager(context).getLanguage() == com.example.AppLanguage.AR
                Text(
                    text = if (isAr)
                        "وجه الكاميرا نحو رمز \u2066QR\u2069 الخاص بالتحويل أو رقم الهاتف"
                    else
                        "Point camera at the transfer QR code or phone number",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDirection = if (isAr) TextDirection.Rtl else TextDirection.Ltr
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
                )
            }
        }
    }
}

private fun processImageProxy(imageProxy: ImageProxy, onDecoded: (String) -> Unit) {
    try {
        val buffer = imageProxy.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        val width = imageProxy.width
        val height = imageProxy.height

        val source = PlanarYUVLuminanceSource(
            data,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()
        val result = reader.decodeWithState(bitmap)
        if (result != null && result.text.isNotBlank()) {
            onDecoded(result.text)
        }
    } catch (_: Exception) {
    } finally {
        imageProxy.close()
    }
}
