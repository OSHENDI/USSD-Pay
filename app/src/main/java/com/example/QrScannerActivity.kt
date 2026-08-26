package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QrScannerActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            QrScannerScreen(
                onClose = { finish() },
                onQrCodeScanned = { result ->
                    val data = Intent().apply {
                        putExtra(EXTRA_RESULT, result)
                    }
                    setResult(RESULT_OK, data)
                    finish()
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        const val EXTRA_RESULT = "qr_scanner_result"
    }
}

@Composable
fun QrScannerScreen(onClose: () -> Unit, onQrCodeScanned: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var hasFinished by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var isDisposed = false
        var cameraProvider: ProcessCameraProvider? = null

        val listener = Runnable {
            if (isDisposed) return@Runnable
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                provider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val reader = MultiFormatReader().apply {
                    val hints = mapOf<DecodeHintType, Any>(
                        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
                    )
                    setHints(hints)
                }

                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    if (isDisposed || hasFinished) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val data = try {
                        val buffer = imageProxy.planes[0].buffer
                        buffer.rewind()
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        bytes
                    } catch (e: Exception) {
                        null
                    }

                    if (data == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val width = imageProxy.width
                    val height = imageProxy.height

                    val source = PlanarYUVLuminanceSource(
                        data, width, height, 0, 0, width, height, false
                    )
                    val bitmap = BinaryBitmap(HybridBinarizer(source))

                    try {
                        val result = reader.decode(bitmap)
                        val text = result.text
                        if (!isDisposed && !hasFinished && text != null) {
                            mainExecutor.execute {
                                if (!hasFinished) {
                                    hasFinished = true
                                    onQrCodeScanned(text)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // No QR found in this frame
                    } finally {
                        imageProxy.close()
                    }
                }

                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        cameraProviderFuture.addListener(listener, mainExecutor)

        onDispose {
            isDisposed = true
            try {
                cameraProvider?.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                analysisExecutor.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay focusing square, semi-transparent background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val boxSize = 260.dp.toPx()
            val left = (canvasWidth - boxSize) / 2
            val top = (canvasHeight - boxSize) / 2
            val rect = Rect(left, top, left + boxSize, top + boxSize)

            val path = Path().apply {
                addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
                addRoundRect(
                    RoundRect(
                        rect = rect,
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                )
                fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            }

            drawPath(path = path, color = Color.Black.copy(alpha = 0.6f))
        }

        // Green frame outline over the cutout window
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxSize = size.width
                val stroke = 4.dp.toPx()
                val len = 24.dp.toPx()
                val color = Color(0xFF2E7D32) // colorPrimary

                // Top Left
                drawLine(color, Offset(0f, 0f), Offset(len, 0f), strokeWidth = stroke)
                drawLine(color, Offset(0f, 0f), Offset(0f, len), strokeWidth = stroke)

                // Top Right
                drawLine(color, Offset(boxSize, 0f), Offset(boxSize - len, 0f), strokeWidth = stroke)
                drawLine(color, Offset(boxSize, 0f), Offset(boxSize, len), strokeWidth = stroke)

                // Bottom Left
                drawLine(color, Offset(0f, boxSize), Offset(len, boxSize), strokeWidth = stroke)
                drawLine(color, Offset(0f, boxSize), Offset(0f, boxSize - len), strokeWidth = stroke)

                // Bottom Right
                drawLine(color, Offset(boxSize, boxSize), Offset(boxSize - len, boxSize), strokeWidth = stroke)
                drawLine(color, Offset(boxSize, boxSize), Offset(boxSize, boxSize - len), strokeWidth = stroke)
            }
        }

        // Top controls
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = IconButtonDefaults.filledShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }

        // Center visual guidelines text
        val isArabic = remember {
            val prefs = PrefsManager(context)
            prefs.getLanguage() == AppLanguage.AR
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isArabic) "وجّه الكاميرا نحو الرمز" else "Point camera at QR code",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Convert bytebuffer to bytearray helper
private fun ByteBuffer.toByteArray(): ByteArray {
    rewind()
    val data = ByteArray(remaining())
    get(data)
    return data
}
